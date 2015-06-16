package com.linkedin.metrowka;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AnalyzerMain {

  public static void main(String[] args) throws Exception {
    if (args.length < 3 || args.length > 4) {
      System.out.println("Incorrect arguments, expecting: BASE_LOCATION LOG_FILE_LOCATION LOG_FILE_PATTERN_LAYOUT <PORT>\n"
          + "  BASE_LOCATION           - location of Analyzer\n"
          + "  LOG_FILE_LOCATION       - location of the log file\n"
          + "  LOG_FILE_PATTERN_LAYOUT - patter layout used in a log file\n"
          + "  <PORT>                  - optional port number, default is " + Constants.DEFAULT_PORT);
      System.exit(1);
    }
    final Path logFileLocation = Paths.get(args[1]);
    final int port = (args.length == 4) ? Integer.parseInt(args[3]) : Constants.DEFAULT_PORT;

    new Analyzer(logFileLocation, args[2], port, Paths.get(args[0])).start();
  }

}
