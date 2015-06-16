package com.linkedin.metrowka.logging;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.log4j.chainsaw.LogFilePatternLayoutBuilder;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LogFilePatternReceiver;

public class Log4jParser extends LogFilePatternReceiver {

  private final Consumer<LoggingEvent> _consumer;

  public Log4jParser(String patternLayout, Path file, Consumer<LoggingEvent> consumer) {
    _consumer = consumer;
    setFileURL(file.toAbsolutePath().toUri().toString());
    setLogFormat(LogFilePatternLayoutBuilder.getLogFormatFromPatternLayout(patternLayout));
    setTimestampFormat(LogFilePatternLayoutBuilder.getTimeStampFormat(patternLayout));
    setTailing(false);
    setUseCurrentThread(true);
  }

  @Override
  public void doPost(LoggingEvent event) {
    _consumer.accept(event);
  }

  public void run() {
    activateOptions();
  }

}
