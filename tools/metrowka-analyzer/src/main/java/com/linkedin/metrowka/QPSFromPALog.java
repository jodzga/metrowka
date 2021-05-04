package com.linkedin.metrowka;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.HdrHistogram.Base64CompressedHistogramSerializer;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

public class QPSFromPALog {
	
	static final Base64CompressedHistogramSerializer serializer = new Base64CompressedHistogramSerializer();
	
	static final Pattern pattern = Pattern.compile("(\\d+/\\d+/\\d+ \\d+:\\d+:\\d+.\\d+) \\[.*?\\] \\[(.*?)\\] \\[.*?\\] \\[espresso-router\\] (\\w+) (\\S*?) \\(Request-Id=.* in (\\d+) ms");
	static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	static int windowMs = 0;
	static final Recorder latencyRecorder = new Recorder(1, 1000000000, 3);
	static final Recorder windowEventsRecorder = new Recorder(1, 1000000, 3);
	static final Rate rate = new Rate("QPS", 1, Long.MAX_VALUE, 3, MeasureUnit.other);
	static final Recorder rpcTraceSizeRecorder = new Recorder(1, 1000000, 3);
	static final Recorder pathSizeRecorder = new Recorder(1, 1000000, 3);
	static final Recorder pathWitrpcTraceSizeSizeRecorder = new Recorder(1, 1000000, 3);

	static final Map<String, Recorder> pathSizes = new HashMap<>();
	
	
	static boolean initialized = false; 
	static long head, tail;
	static int eventsInWindow = 0;
	static int windowsCount = 0;
	
	static long lastTimestamp = 0;
	static long rateCount = 0;
	
	private static void printHistogram(String name, Histogram h, double scale) {
		System.out.println(name + ":");
		h.outputPercentileDistribution(System.out, scale);
		System.out.println(serializer.serlialize(h));
	}

	public static void main(String[] args) throws IOException, ParseException {
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		windowMs = Integer.parseInt(args[1]);
		InputStream fileStream = new FileInputStream(args[0]);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream);
		BufferedReader buffered = new BufferedReader(decoder);
		String line = buffered.readLine();
		String lastLine = line;
		System.out.println(lastLine);
		int lineCount = 0;
		while(line != null) {
			feedLine(line);
			lineCount++;
			if (lineCount == 4000000) {
				break;
			}
			lastLine = line;
			line = buffered.readLine();
		}
		printHistogram("latencyRecorder", latencyRecorder.getIntervalHistogram(), 1.0);
		printHistogram("windowEventsRecorder", windowEventsRecorder.getIntervalHistogram(), 1.0);
		printHistogram("windowEventsRecorder", windowEventsRecorder.getIntervalHistogram(), 1.0);
		System.out.println("windowsCount: " + windowsCount);
		rate.harvest((h, t, s) -> {
			printHistogram(s, h, Rate.MAX_INTERVAL_BETWEEN_EVENTS_IN_NS/1000000000.0);
		});
		printHistogram("rpcTraceSize", rpcTraceSizeRecorder.getIntervalHistogram(), 1.0);
		printHistogram("pathSize", pathSizeRecorder.getIntervalHistogram(), 1.0);
		printHistogram("pathWitrpcTraceSize", pathWitrpcTraceSizeSizeRecorder.getIntervalHistogram(), 1.0);
		buffered.close();
		System.out.println(lastLine);
		pathSizes.forEach((method, recorder) -> printHistogram(method, recorder.getIntervalHistogram(), 1.0));
	}
	
	private static final Pattern FILTER_PATTERN = Pattern.compile("CareersMemberJobActivitiesDB");
	
	private static void feedLine(String line) throws ParseException {
		Matcher filter = FILTER_PATTERN.matcher(line);
		if (filter.find()) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				long timestamp = sdf.parse(matcher.group(1)).toInstant().toEpochMilli();
				if (timestamp >= 1610590740000L && timestamp <= 1610597880000L) {
					rpcTraceSizeRecorder.recordValue(matcher.group(2).length());
					Recorder pathByMethod = pathSizes.computeIfAbsent(matcher.group(3), k -> new Recorder(1, 1000000, 3));
					pathByMethod.recordValue(matcher.group(4).length());
					pathSizeRecorder.recordValue(matcher.group(4).length());
					pathWitrpcTraceSizeSizeRecorder.recordValue(matcher.group(2).length() + matcher.group(4).length());
					int latency = Integer.parseInt(matcher.group(5));
					if (!initialized) {
						initialized = true;
						head = timestamp;
						tail = timestamp;
						lastTimestamp = timestamp;
					}
					step(timestamp, latency);
				}
			} else {
				System.out.println("Failed on line: " + line);
				System.exit(1);
			}
		}
	}

	private static void step(long timestamp, int latency) {
		
		//update rate
		if (timestamp > lastTimestamp) {
			rate.record(rateCount, timestamp * 1000000);
			lastTimestamp = timestamp;
			rateCount = 1;
		} else {
			rateCount++;
		}

		latencyRecorder.recordValue(latency * 1000000 + 1);
		head = timestamp;
		if (head - tail >= windowMs) {
			windowEventsRecorder.recordValue(eventsInWindow);
			eventsInWindow = 0;
			tail = head;
			windowsCount++;
		} else {
			eventsInWindow++;
		}
	}

}
