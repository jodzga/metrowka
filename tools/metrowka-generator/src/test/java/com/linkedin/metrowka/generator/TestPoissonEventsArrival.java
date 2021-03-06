package com.linkedin.metrowka.generator;

import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

public class TestPoissonEventsArrival {

  @Test
  public void testAvg() {
    final long MEAN_INTERARRIVAL_NANO = TimeUnit.SECONDS.toNanos(1) / 1000;
    final double LAMBDA_NANO = 1.0 / MEAN_INTERARRIVAL_NANO;
    final double VARIANCE_NANO = 1.0 / (LAMBDA_NANO * LAMBDA_NANO);
    final int ITERATIONS = 1000000;
    final long[] values = new long[ITERATIONS];
    PoissonEventsArrival dist = new PoissonEventsArrival(1000, TimeUnit.SECONDS);
    long sum = 0;
    long variance = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long nanos = dist.nanosToNextEvent();
      sum += nanos;
      variance += (nanos - MEAN_INTERARRIVAL_NANO) * (nanos - MEAN_INTERARRIVAL_NANO);
      values[i] = nanos;
    }
    System.out.println(sum/ITERATIONS);
    System.out.println(MEAN_INTERARRIVAL_NANO);
    variance = variance / ITERATIONS;
    System.out.println("variance: " + variance + ", model variance: " + VARIANCE_NANO + ", diff: " + Math.abs(VARIANCE_NANO - variance));
    double distanceFromMean = Math.abs((sum / ITERATIONS) - (MEAN_INTERARRIVAL_NANO));
    double stdDev =  Math.sqrt(MEAN_INTERARRIVAL_NANO);
    assertTrue(distanceFromMean < 6 * stdDev, "avg is farther from mean than 6 stdDev");

    Arrays.sort(values);
    long median = values[ITERATIONS / 2];
    System.out.println("mean interarrival nano: " + MEAN_INTERARRIVAL_NANO + ", median nano: " + median);
  }

}
