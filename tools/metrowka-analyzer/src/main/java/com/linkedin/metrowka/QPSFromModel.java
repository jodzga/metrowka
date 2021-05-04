package com.linkedin.metrowka;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.HdrHistogram.Recorder;

public class QPSFromModel {
	
	static int windowMs = 0;
	static final Recorder latencyRecorder = new Recorder(1, 1000000, 3);
	static final Recorder windowEventsRecorder = new Recorder(1, 1000000, 3);
	
	static boolean initialized = false; 
	static long head, tail;
	static int eventsInWindow = 0;
	static int windowsCount = 0;

	public static void main(String[] args) throws IOException, ParseException {
		windowMs = Integer.parseInt(args[1]);
		InputStream fileStream = new FileInputStream(args[0]);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		Reader decoder = new InputStreamReader(gzipStream);
		BufferedReader buffered = new BufferedReader(decoder);
		String line = buffered.readLine(); 
		int lineCount = 0;
		while(line != null) {
			feedLine(line);
			lineCount++;
			if (lineCount == 2000000) {
				break;
			}
			line = buffered.readLine();
		}
		System.out.println("latencyRecorder:");
		latencyRecorder.getIntervalHistogram().outputPercentileDistribution(System.out, 1.0);
		System.out.println("windowEventsRecorder:");
		windowEventsRecorder.getIntervalHistogram().outputPercentileDistribution(System.out, ((double)windowMs)/1000);
		System.out.println("windowsCount: " + windowsCount);
		buffered.close();
	}
	
	private static void feedLine(String line) throws ParseException {
//		long timestamp = sdf.parse(matcher.group(1)).toInstant().toEpochMilli();
//		int latency = Integer.parseInt(matcher.group(2));
//		if (!initialized) {
//			initialized = true;
//			head = timestamp;
//			tail = timestamp;
//		}
//		step(timestamp, latency);
	}

	private static void step(long timestamp, int latency) {
		latencyRecorder.recordValue(latency + 1);
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
