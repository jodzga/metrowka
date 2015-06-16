package com.linkedin.metrowka;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.metrowka.logging.HistogramEvent;
import com.linkedin.metrowka.logging.Log4jParser;
import com.linkedin.metrowka.logging.LogEventHistogramSerializer;

public class Analyzer {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyzerMain.class);

  private final Path _logFileLocation;
  private final String _logFilePatternLayout;
  private final Path _staticContentLocation;
  private final int _port;

  private final Map<String, List<HistogramEvent>> _histograms = new HashMap<String, List<HistogramEvent>>();

  public Analyzer(Path logFileLocation, String logFilePatternLayout, int port, final Path baseLocation) {
    _logFileLocation = logFileLocation;
    _logFilePatternLayout = logFilePatternLayout;
    _port = port;
    _staticContentLocation = baseLocation.resolve(Constants.STATIC_CONTENT_SUBDIRECTORY);
  }

  private enum RequestType {
    listOfHistograms,
    histogramData,
    allHistogramsData
  }

  private enum HistogramDataType {
    distribution,
    percentiles
  }

  private RequestType getRequestType(String target) {
    try {
      return RequestType.valueOf(target.split("/")[0]);
    } catch (Exception e) {
      return null;
    }
  }

  private JSONObject handleRequest(RequestType requestType, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) {
    switch (requestType) {
      case listOfHistograms: return getListOfHistogramDefsJSON(baseRequest, request, response);
      case histogramData: return getHistogramDataJSON(baseRequest, request, response);
      case allHistogramsData: return getAllHistogramDataJSON(baseRequest, request, response);
      default: return null;
    }
  }

  private JSONObject getAllHistogramDataJSON(Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) {
    String name = request.getParameter("name");
    return getAllHistogramDataJSON(name);
  }

  /**
   * null is an identy in this operation
   */
  private HistogramEvent sumUpHistopgramEvents(HistogramEvent left, HistogramEvent right) {
    if (left == null && right == null) {
      return null;
    } else if (right == null) {
      return sumUpHistopgramEvents(right, left);
    } else {
      //left might be null, right is not null
      if (left == null) {
        HistogramEvent he = new HistogramEvent(right.getHistogram().copy(), right.getType(), right.getName());
        he.getHistogram().setAutoResize(true);
        return he;
      } else {
        left.getHistogram().add(right.getHistogram());
        left.getHistogram().setEndTimeStamp(right.getHistogram().getEndTimeStamp());
        return left;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private JSONObject getAllHistogramDataJSON(String name) {
    List<HistogramEvent> histograms = _histograms.get(name);
    HistogramEvent sum = histograms.stream().reduce(null, this::sumUpHistopgramEvents);
    JSONObject hDef = new JSONObject();
    hDef.put("name", name);
    hDef.put("type", sum.getType().toString());
    hDef.put("start", sum.getHistogram().getStartTimeStamp());
    hDef.put("end", sum.getHistogram().getEndTimeStamp());
    hDef.put("min", sum.getHistogram().getMinValue());
    hDef.put("max", sum.getHistogram().getMaxValue());
    hDef.put("avg", getMean(sum.getHistogram()));
    hDef.put("stdDeviation", getStdDeviation(sum.getHistogram()));
    hDef.put("pct50", sum.getHistogram().getValueAtPercentile(50.0));
    hDef.put("pct90", sum.getHistogram().getValueAtPercentile(90.0));
    hDef.put("pct99", sum.getHistogram().getValueAtPercentile(99.0));
    hDef.put("pct999", sum.getHistogram().getValueAtPercentile(99.9));
    hDef.put("pct9999", sum.getHistogram().getValueAtPercentile(99.99));
    hDef.put("pct99999", sum.getHistogram().getValueAtPercentile(99.999));
    hDef.put("pct999999", sum.getHistogram().getValueAtPercentile(99.9999));
    hDef.put("pct9999999", sum.getHistogram().getValueAtPercentile(99.99999));

    JSONArray data = new JSONArray();
    histograms.forEach(he -> {

      Histogram histogram = he.getHistogram();

      JSONObject ev = new JSONObject();
      ev.put("start", histogram.getStartTimeStamp());
      ev.put("end", histogram.getEndTimeStamp());
      ev.put("start", histogram.getStartTimeStamp());
      ev.put("end", histogram.getEndTimeStamp());
      ev.put("min", histogram.getMinValue());
      ev.put("max", histogram.getMaxValue());
      ev.put("avg", getMean(histogram));
      ev.put("pct50", histogram.getValueAtPercentile(50.0));
      ev.put("pct90", histogram.getValueAtPercentile(90.0));
      ev.put("pct99", histogram.getValueAtPercentile(99.0));
      ev.put("pct999", histogram.getValueAtPercentile(99.9));
      ev.put("pct9999", histogram.getValueAtPercentile(99.99));

      data.add(ev);
    });
    hDef.put("data", data);
    return hDef;
  }

  @SuppressWarnings("unchecked")
  private JSONObject getHistogramDefJSON(String name) {
    List<HistogramEvent> histograms = _histograms.get(name);
    JSONObject hDef = new JSONObject();
    hDef.put("name", name);
    HistogramEvent first = histograms.get(0);
    hDef.put("type", first.getType().toString());
    JSONArray times = new JSONArray();
    histograms.forEach(he -> {
      JSONObject ev = new JSONObject();
      ev.put("start", he.getHistogram().getStartTimeStamp());
      ev.put("end", he.getHistogram().getEndTimeStamp());
      times.add(ev);
    });
    hDef.put("times", times);
    return hDef;
  }

  private BigDecimal getTotalCount(Histogram histogram) {
    BigDecimal tc = BigDecimal.ZERO;
    Iterator<HistogramIterationValue> iterator = histogram.recordedValues().iterator();

    while (iterator.hasNext()) {
      HistogramIterationValue iterationValue = iterator.next();
      tc = tc.add(BigDecimal.valueOf(iterationValue.getCountAtValueIteratedTo()));
    }
    return tc;
  }

  private BigDecimal getMean(Histogram histogram) {
    BigDecimal tc = BigDecimal.ZERO;
    BigDecimal tv = BigDecimal.ZERO;

    Iterator<HistogramIterationValue> iterator = histogram.recordedValues().iterator();

    while (iterator.hasNext()) {
      HistogramIterationValue iterationValue = iterator.next();
      BigDecimal cnt = BigDecimal.valueOf(iterationValue.getCountAtValueIteratedTo());
      BigDecimal val = BigDecimal.valueOf(histogram.medianEquivalentValue(iterationValue.getValueIteratedTo()));
      tv = tv.add(val.multiply(cnt));
      tc = tc.add(cnt);
    }
    return tv.divide(tc, 2, RoundingMode.HALF_UP);
  }

  private BigDecimal getStdDeviation(Histogram histogram) {
    final BigDecimal tc = getTotalCount(histogram);
    if (tc.equals(BigDecimal.ZERO)) {
      return tc;
    }
    final BigDecimal mean = getMean(histogram);
    BigDecimal geometricDeviationTotal = BigDecimal.ZERO;

    Iterator<HistogramIterationValue> iterator = histogram.recordedValues().iterator();

    while (iterator.hasNext()) {
      HistogramIterationValue iterationValue = iterator.next();
      BigDecimal cnt = BigDecimal.valueOf(iterationValue.getCountAtValueIteratedTo());
      BigDecimal val = BigDecimal.valueOf(histogram.medianEquivalentValue(iterationValue.getValueIteratedTo()));
      BigDecimal deviation = val.subtract(mean);
      geometricDeviationTotal = geometricDeviationTotal.add(deviation.multiply(deviation).multiply(cnt));
    }
    return sqrt(geometricDeviationTotal.divide(tc, 2, RoundingMode.HALF_UP));
  }

  private static BigDecimal sqrt(BigDecimal value) {
    BigDecimal x = new BigDecimal(Math.sqrt(value.doubleValue()));
    return x.add(new BigDecimal(value.subtract(x.multiply(x)).doubleValue() / (x.doubleValue() * 2.0)));
  }

  @SuppressWarnings("unchecked")
  private JSONObject getHistogramDataJSON(String name, long startTimestamp, HistogramDataType dataType) {
    Histogram histogram = _histograms.get(name).stream()
        .filter(he -> he.getHistogram().getStartTimeStamp() == startTimestamp)
        .findAny().get().getHistogram();

    final JSONObject result = new JSONObject();
    final JSONArray data = new JSONArray();

    switch(dataType) {
      case distribution: populateDistribution(data, histogram); break;
      case percentiles: populatePercentiles(data, histogram); break;
    }

    result.put("data", data);

    result.put("min", histogram.getMinValue());
    result.put("max", histogram.getMaxValue());
    result.put("avg", getMean(histogram));
    result.put("stdDeviation", getStdDeviation(histogram));
    result.put("totalCount", getTotalCount(histogram));

    return result;
  }

  private void populatePercentiles(JSONArray data, Histogram histogram) {
    Iterator<HistogramIterationValue> iterator = histogram.percentiles(10).iterator();
    while (iterator.hasNext()) {
      HistogramIterationValue iterationValue = iterator.next();
      addDataPoint(iterationValue.getPercentileLevelIteratedTo(), iterationValue.getValueIteratedTo(), data);
    }
  }

  @SuppressWarnings("unchecked")
  private void populateDistribution(JSONArray data, Histogram histogram) {

    long min = histogram.getMinValue();
    long max = histogram.getMaxValue();
    
    Iterator<HistogramIterationValue> iterator =
        histogram.allValues().iterator();

    while (iterator.hasNext()) {
      HistogramIterationValue v = iterator.next();
      long value = v.getValueIteratedTo();
      if (value > max) {
        break;
      }
      if (value >= min) {
        JSONObject bucket = new JSONObject();
        bucket.put("lo", histogram.lowestEquivalentValue(value));
        bucket.put("hi", histogram.highestEquivalentValue(value));
        bucket.put("count", v.getCountAtValueIteratedTo());
        data.add(bucket);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void addDataPoint(double x, double y, JSONArray data) {
    JSONObject point = new JSONObject();
    point.put("x", x);
    point.put("y", y);
    data.add(point);
  }

  @SuppressWarnings("unchecked")
  private JSONObject getListOfHistogramDefsJSON(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
    final JSONObject result = new JSONObject();
    final JSONArray histograms = new JSONArray();
    _histograms.keySet().stream().sorted().forEach(name -> {
      histograms.add(getHistogramDefJSON(name));
    });
    result.put("histograms", histograms);
    return result;
  }

  private JSONObject getHistogramDataJSON(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
    String name = request.getParameter("name");
    long startTimeStamp = Long.parseLong(request.getParameter("startTimestamp"));
    return getHistogramDataJSON(name, startTimeStamp,  HistogramDataType.valueOf(request.getParameter("type")));
  }

  public void start() throws Exception {
    LOG.info("Starting Analyzer on port: " + _port + ", log file location: " + _logFileLocation
        + ", log file pattern layout: '" + _logFilePatternLayout + "'");

    alanyzeLog();

    Server server = new Server(_port);

    ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setDirectoriesListed(true);
    resourceHandler.setWelcomeFiles(new String[] { "index.html" });
    resourceHandler.setResourceBase(_staticContentLocation.toString());

    final Handler dataHAndler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException {
        if (target.startsWith("/data/")) {
          baseRequest.setHandled(true);
          RequestType requestType = getRequestType(target.substring("/data/".length()));
          if (requestType != null) {
            try {
              JSONObject value = handleRequest(requestType, baseRequest, request, response);
              response.setContentType("application/json");
              PrintWriter writer = response.getWriter();
              writer.write(value.toJSONString());
              response.setStatus(HttpServletResponse.SC_OK);
            } catch (IOException e) {
              LOG.error("error writing response", e);
              response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
          } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          }
        }
      }
    };


    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] { dataHAndler, resourceHandler, new DefaultHandler() });
    server.setHandler(handlers);

    try {
      server.start();
      server.join();
    } finally {
      server.stop();
    }
  }

  private void alanyzeLog() {
    LogEventHistogramSerializer serializer = new LogEventHistogramSerializer();
    new Log4jParser(_logFilePatternLayout, _logFileLocation, logEvent -> {
      if (logEvent.getLoggerName().equals("metrowka")) {
        HistogramEvent he = serializer.deserialize(logEvent.getMessage().toString());
        if (!_histograms.containsKey(he.getName())) {
          _histograms.put(he.getName(), new ArrayList<>());
        }
        List<HistogramEvent> list = _histograms.get(he.getName());
        list.add(he);
      }
    }).run();
    //sort each histogram list by start timestamp
    _histograms.values().forEach(histograms -> {
      Collections.sort(histograms, Comparator.comparing(event -> event.getHistogram().getStartTimeStamp()));
    });
  }


}
