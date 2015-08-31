package com.linkedin.metrowka.metrics.vm;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public interface ThreadMXBeanEx extends java.lang.management.ThreadMXBean {

  public long[] getThreadCpuTime(long[] ids);

  public long[] getThreadUserTime(long[] ids);

  public long[] getThreadAllocatedBytes(long[] ids);

  public static class BeanHelper {

      public static final ObjectName THREADING_MBEAN = name("java.lang:type=Threading");

      private static ObjectName name(String name) {
          try {
              return new ObjectName(name);
          } catch (MalformedObjectNameException e) {
              throw new RuntimeException(e);
          }
      }

      public static ThreadMXBeanEx connectThreadMXBean(MBeanServerConnection mserver) {
              return JMX.newMXBeanProxy(mserver, THREADING_MBEAN, ThreadMXBeanEx.class);
      }
  }
}
