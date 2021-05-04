package com.linkedin.metrowka.generator;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.HdrHistogram.Base64CompressedHistogramSerializer;
import org.HdrHistogram.Recorder;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.Interval;
import com.linkedin.metrowka.Metrowka;
import com.linkedin.metrowka.logging.LogEventHistogramSerializer;
import com.linkedin.metrowka.logging.LoggingReaper;
import com.linkedin.metrowka.metrics.vm.Hiccup;

import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;

public class EspressoRouterLoadGenerator {

  private static final Logger _logger = LoggerFactory.getLogger(EspressoRouterLoadGenerator.class);
  private static final long INITIAL_DELAY_SECONDS = 5;
  static final Pattern CONTENT_STATUS_PATTERN = Pattern.compile("X-ESPRESSO-Content-Status: (\\d+) ");
  static final Pattern PA_LOG_PATTERN = Pattern.compile("\\d+/\\d+/\\d+ \\d+:\\d+:\\d+.\\d+ \\[.*?\\] \\[(.*?)\\] \\[(.*?)\\] \\[espresso-router\\] (\\w+) (\\S*?) ");
  static final Pattern RPC_TRACE_MAIN_PATTERN = Pattern.compile("\\((.*?)\\)(.*)");
  static final Pattern RPC_TRACE_HOP_PATTERN = Pattern.compile("\\[(.*?)\\]");

  static final ConcurrentMap<Integer, AtomicLong> statuses = new ConcurrentHashMap<>();
  static final List<RequestParameters> requestParameters = new ArrayList<>();
  
  private static void updateStatusCount(int status) {
	  AtomicLong counter = statuses.computeIfAbsent(status, k -> new AtomicLong());
	  counter.incrementAndGet();
  }
  
  static class RequestParameters {
	final String icHeader;
	final String path;
	public RequestParameters(String icHeader, String path) {
		this.icHeader = icHeader;
		this.path = path;
	}
  }

	static final Base64CompressedHistogramSerializer serializer = new Base64CompressedHistogramSerializer();

	private static String adjustRPCTraceSize(String rpcTrace, double rpcTraceSizeMultiplier) {
		int newSize = (int)(rpcTrace.length()*rpcTraceSizeMultiplier);
		Matcher matcher = RPC_TRACE_MAIN_PATTERN.matcher(rpcTrace);
		if (matcher.find()) {
			String traceToken = matcher.group(1);
			String hops = matcher.group(2);
			if (newSize <= 4) {
				return "()[]";
			} else {
				int toFill = newSize - 4;
				if (toFill <= traceToken.length()) {
					return "(" + traceToken.substring(0, toFill) + ")[]";
				} else {
					toFill -= traceToken.length();
					StringBuilder sb = new StringBuilder();
					sb.append("(");
					sb.append(traceToken);
					sb.append(")");
					Matcher m = RPC_TRACE_HOP_PATTERN.matcher(hops);
					while(toFill > 1) {
						if (m.find() || m.find(0)) {
							String s = m.group(1);
							int available = Math.min(toFill, s.length());
							sb.append("[");
							sb.append(s.substring(0, available));
							sb.append("]");
							toFill -= (available + 2);
						} else {
							throw new RuntimeException("Invalid RPCTrace: " + rpcTrace + ", hops: " + hops);
						}
					}
					return sb.toString();
				}
				
			}
		} else {
			throw new RuntimeException("Invalid RPCTrace: " + rpcTrace);
		}
	}
  
  /**
   * espresso-router_partial_public_access.log.2020-11-16-13.gz log file taken from lor1-app13960.prod.linkedin.com (ESPRESSO_LTS)
 * @param rpcTraceSizeMultiplier 
   */
  private static void prepareRequestParameters(String partialPALog, String filter, double rpcTraceSizeMultiplier) throws IOException {
		final Recorder rpcTraceSizeRecorder = new Recorder(1, 1000000, 3);
	  	InvocationContextBuilder icBuilder = new InvocationContextBuilder(); 
	  	Pattern filterPattern = Pattern.compile(filter);
	  	Files.list(new File(partialPALog).toPath())
        .forEach(p -> {
        	if (p.toString().endsWith(".gz")) {
        		InputStream fileStream;
    			try {
    				_logger.info("Processing " + p);
    				fileStream = new FileInputStream(p.toFile());
    	    		InputStream gzipStream = new GZIPInputStream(fileStream);
    	    		Reader decoder = new InputStreamReader(gzipStream);
    	    		BufferedReader buffered = new BufferedReader(decoder);
    	    		buffered.lines().forEach(line -> {
    	    			Matcher filterMatcher = filterPattern.matcher(line);
    	    			if (filterMatcher.find()) {
    	    				Matcher matcher = PA_LOG_PATTERN.matcher(line);
    	    				if (matcher.find()) {
    	    					String rpcTrace = adjustRPCTraceSize(matcher.group(1), rpcTraceSizeMultiplier);
    	    					String pageKey = matcher.group(2);
    	    					String method = matcher.group(3);
    	    					String path = matcher.group(4);
    	    					if (method.equals("GET")) {
    	    						rpcTraceSizeRecorder.recordValue(rpcTrace.length());
    	    						requestParameters.add(new RequestParameters(icBuilder.getInvocationContext(Collections.emptyMap(), rpcTrace, pageKey), path));
    	    					}
    	    				}
    	    			}
    	    		});
    	    		buffered.close();
    			} catch (IOException e) {
    				_logger.error("Failed to prepare request parameters", e);
    				throw new RuntimeException(e);
    			}
        	}
        });
	  	_logger.info("RPCTraceSize:" + serializer.serlialize(rpcTraceSizeRecorder.getIntervalHistogram()));
  }

  
  public static 
  Iterable<Map.Entry<CharSequence, AsciiString>> decodeIC2(final  AsciiString text) {
    return () -> new Iterator<Map.Entry<CharSequence, AsciiString>>() {
      int _position = 0;

      @Override
      public boolean hasNext() {
        return _position >= 0;
      }

      @Override
      public Map.Entry<CharSequence, AsciiString> next() {
        int next = text.indexOf(',', _position);
        AsciiString entry = next < 0 ? text.subSequence(_position, text.length(), false) : text.subSequence(_position, next++, false);
        int eqPos = entry.indexOf('=', 0);
        final CharSequence key = eqPos < 0 ? text : entry.subSequence(0, eqPos, false);
        final AsciiString value = eqPos < 0 ? AsciiString.EMPTY_STRING : entry.subSequence(eqPos + 1, entry.length(), false);
        _position = next;
        return new Map.Entry<CharSequence, AsciiString>() {

			@Override
			public CharSequence getKey() {
				return key;
			}

			@Override
			public AsciiString getValue() {
				return value;
			}

			@Override
			public AsciiString setValue(AsciiString value) {
				throw new UnsupportedOperationException();
			}
        	
        };
      }
    };
  }
  
  /**
   * tunnel: ssh -L11937:localhost:11937 lor1-app18323.prod.linkedin.com
   */
  public static void main(String[] args) throws Exception {
	  
    final double rps = Double.parseDouble(args[0]);
    final long durationSeconds = Long.parseLong(args[1]);
    final String urlBase = args[2];
    final String arrivalType = args[3];
    final String keyStoreFile = args[4];
    final String partialPALog = args[5];
    final String filter = args[6];
    final int timeout = Integer.parseInt(args[7]);
    final int maxInFlight = Integer.parseInt(args[8]);
    final double rpcTraceSizeMultiplier = Double.parseDouble(args[9]);

    _logger.info("RPS: " + rps + ", duration: " + durationSeconds + "s (after " + INITIAL_DELAY_SECONDS + "s of initial delay), url base: " +
    		urlBase + ", arrivalType: " + arrivalType + ", keyStoreFile: " + keyStoreFile + ", partial PA logs: " + partialPALog + ", filter: " + 
    		filter + ", timeout: " + timeout + ", maxInFlight: " + maxInFlight + ", rpcTraceSizeMultiplier: " + rpcTraceSizeMultiplier);

    _logger.info("Preparing request parameters from partial PA logs");
    prepareRequestParameters(partialPALog, filter, rpcTraceSizeMultiplier);
    
    AtomicLong length = new AtomicLong();
    for (int i = 0; i < 100; i++) {
    	long start = System.nanoTime();
    	_logger.info("Started iteration: " + i);
        for (RequestParameters rp : requestParameters) {
        	decodeIC2(AsciiString.of(rp.icHeader)).forEach(entry -> {
//        		length.addAndGet(URLCodec.decode(entry.getValue(), StandardCharsets.UTF_8).toString().length());
        		length.addAndGet(URLFastDecoder.URLDecodeAsciiString(entry.getValue()).toString().length());
//        		if (!a.equals(b)) {
//            		System.out.println(a);
//            		System.out.println(b);
//        		}
        	});
        }
    	_logger.info("Finished iteration: " + i);
    	long end = System.nanoTime();
    	_logger.info("Duration s: " + (end - start) / 1000000000d);
    }
    _logger.info("Checksum: " + length.get());
    
    
    //TODO
    System.exit(0);
    
    final AtomicInteger requestIndex = new AtomicInteger(0);
    
    final AtomicLong successes = new AtomicLong(0);
    final AtomicLong failures = new AtomicLong(0);
    final AtomicLong skipped = new AtomicLong(0);
    final AtomicInteger inFlight = new AtomicInteger(0);

    final SslContext sslContext = SslUtils.build(new File(keyStoreFile), "work_around_jdk-6879539", SslUtils.P12_STORE_TYPE_NAME);
    final AsyncHttpClient client = asyncHttpClient(config().setSslContext(sslContext));

    final Metrowka metrowka = new Metrowka(60*1000);
    final Base64CompressedHistogramSerializer serializer = new Base64CompressedHistogramSerializer();
    final LoggingReaper loggingReaper = new LoggingReaper(LoggerFactory.getLogger("metrowka"), new LogEventHistogramSerializer());

    final Hiccup hiccups = new Hiccup();
    hiccups.start();

    final Interval eventsGeneratorJitter = new Interval("eventsGeneratorJitter", 1, TimeUnit.MINUTES.toNanos(60), 3);
    final EventsArrival arrivalDistribution = EventsArrival.fromName(arrivalType, rps, TimeUnit.SECONDS);

    final Interval latency = new Interval("latency", 1, TimeUnit.MINUTES.toNanos(60), 3);

    final BlockingQueue<Long> queue = new LinkedBlockingDeque<Long>(100000);
    final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    EventsGenerator generator = new EventsGenerator(arrivalDistribution, queue, executor,
        event -> {
          eventsGeneratorJitter.record(Math.abs(event.getActualNanoTimestamp() - event.getExpectedNanoTimestamp()));

          final AsyncCompletionHandler<Response> responseHandler = new AsyncCompletionHandler<Response>() {

            @Override
            public Response onCompleted(Response response) throws Exception {
              inFlight.decrementAndGet();
              successes.incrementAndGet();
              latency.record(System.nanoTime() - event.getActualNanoTimestamp());
              if (response.getStatusCode() == 207) {
            	  BufferedReader reader = new BufferedReader(new InputStreamReader(response.getResponseBodyAsStream()));
            	  reader.lines().forEach(line -> {
            		  Matcher matcher = CONTENT_STATUS_PATTERN.matcher(line);
            		  if (matcher.find()) {
            			int status = Integer.parseInt(matcher.group(1));
            			updateStatusCount(status);
            		  }
            	  });
              } else {
            	  updateStatusCount(response.getStatusCode());
            	  //consume
            	  response.getResponseBodyAsBytes();
              }
              return response;
            }

            @Override
            public void onThrowable(Throwable t) {
              failures.incrementAndGet();
              inFlight.decrementAndGet();
              super.onThrowable(t);
            }
          };
          
          int index = requestIndex.getAndIncrement();
          if (index >= requestParameters.size()) {
        	  requestIndex.set(0);
        	  index = requestIndex.getAndIncrement();
          }
          RequestParameters rp = requestParameters.get(index);
          
          byte[] reqId = new byte[16];
          ThreadLocalRandom.current().nextBytes(reqId);
          
          if (inFlight.get() < maxInFlight) {
        	  inFlight.incrementAndGet();
              client.prepareGet(urlBase + rp.path)
            	.setHeader("X-LI-R2-W-IC-1", rp.icHeader)
            	.setHeader("X-ESPRESSO-Request-Id", Base64.getEncoder().encodeToString(reqId))
            	.setHeader("X-ESPRESSO-Request-Timeout", timeout)
            	.execute(responseHandler);
          } else {
        	  skipped.incrementAndGet();
          }
          
        });

    generator.start(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS);

    metrowka.register(hiccups, loggingReaper);
    metrowka.register(eventsGeneratorJitter, loggingReaper);
    metrowka.register(latency, loggingReaper);
    metrowka.start();

    TimeUnit.SECONDS.sleep(durationSeconds + INITIAL_DELAY_SECONDS);
    
    _logger.info("Finished, successful: " + successes.get() + ", failed: " + failures.get());
    loggingReaper.consumeWarmedUpTotals((name, histogram) -> _logger.info("warmed-up histogram summary " + name + ":" + serializer.serlialize(histogram)));
    loggingReaper.consumeTotals((name, histogram) -> _logger.info("histogram summary " + name + ":" + serializer.serlialize(histogram)));
    statuses.forEach((status, counter) -> _logger.info("Status " + status + " count: " + counter.get()));

    _logger.info("Shutting down generator");
    generator.stop();
    metrowka.stop();
    hiccups.stop();
    _logger.info("Shutting down executor");
    executor.shutdown();
    _logger.info("Shutting down HTTP client");
    client.close();

  }

}
