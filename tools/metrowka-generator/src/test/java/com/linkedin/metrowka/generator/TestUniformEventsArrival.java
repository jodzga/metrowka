package com.linkedin.metrowka.generator;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

public class TestUniformEventsArrival {

  @Test
  public void testAvg() {
    final int ITERATIONS = 1000000;
    UniformEventsArrival dist = new UniformEventsArrival(1000, TimeUnit.SECONDS);
    long sum = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long nanos = dist.nanosToNextEvent();
      sum += nanos;
    }
    assertEquals(sum / ITERATIONS, 1000000000L / 1000, "avg should be exact for uniform distributed arrivals");
  }

}
