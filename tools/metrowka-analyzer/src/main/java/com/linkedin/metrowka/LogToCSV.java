package com.linkedin.metrowka;

public class LogToCSV {

	public static void main(String[] args) {
		if (args.length < 3 || args.length > 4) {
			System.out.println("Incorrect arguments, expecting: LOG_FILE LOG_PATTERN_LAYOUT OUTPUT_CSV_FILE\n"
					+ "  BASE_LOCATION           - location of Analyzer\n"
					+ "  LOG_FILE                - location of the log file\n"
					+ "  LOG_PATTERN_LAYOUT      - patter layout used in a log file, typically part of the log configuration e.g. \"%d{yyyy-MM-dd HH:mm:ss} %p %c - %m%n\"\n"
					+ "  OUTPUT_CSV_FILE         - output csv file");
			System.exit(1);
		}
	}

}
