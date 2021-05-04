package com.linkedin.metrowka.generator;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import org.json.JSONWriter;


public class InvocationContextBuilder {

  private static final int VERSION_NUMBER = 5;

  private static final int TREE_ID_LENGTH = 16;

  private static final String VERSION_FIELD = "version";
  private static final String TREE_ID_FIELD = "treeId";
  private static final String REQUEST_ID_FIELD = "rqId";
  private static final String CALLER_FIELD = "caller";
  private static final String TASK_FIELD = "taskId";
  private static final String CONTEXT_FIELD = "context";
  private static final String PARENT_TRACE_DATA_FIELD = "parent";
  private static final String RPC_TRACE_FIELD = "rpcTrace";
  private static final String SCALE_FIELD = "scale";
  private static final String ENABLED_FIELD = "enabled";

  private static final String CALLER_ENVIRONMENT_FIELD = "env";
  private static final String CALLER_CONTAINER_FIELD = "container";
  private static final String CALLER_SERVICE_FIELD = "service";
  private static final String CALLER_VERSION_FIELD = "version";
  private static final String CALLER_MACHINE_FIELD = "machine";
  private static final String CALLER_PHYSICAL_FIELD = "physical";
  private static final String CALLER_INSTANCE_FIELD = "instance";
  private static final String CALLER_SLICE_FIELD = "slice";
  private static final String CALLER_SLICE_INSTANCE_FIELD = "sliceInstance";

  private final static String DEFAULT_DATE_FORMAT = "YYYY/MM/dd HH:mm:ss.SSS";
  private final static int DEFAULT_MAX_CONTEXT_LENGTH = 248;
  private final static char SEPARATOR = ',';
  private final static char SLASH = '/';

  private final DateTimeFormatter _format = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
  private final Random _rand = new Random();

  private String _environmentName;
  private String _containerName;
  private String _serviceName;
  private String _versionNumber;
  private String _machineName;
  private String _physicalHost;
  private String _instanceId;
  private double _scaleFactor;

  public InvocationContextBuilder()
    throws UnknownHostException, UnsupportedEncodingException
  {
    setEnvironmentName("ei");
    setContainerName("metrowka-generator");
    setServiceName("metrowka-generator");
    setVersionNumber("0.0.1");
    setInstanceId("i001");
    setScaleFactor(0.2); // 20%
    setMachineName(InetAddress.getLocalHost().getHostName());
    setPhysicalHost(getMachineName());
  }

//  private String initRpcTraceBasePre()
//    throws UnsupportedEncodingException
//  {
//    StringBuilder builder = new StringBuilder();
//
//    builder.append('(');
//    builder.append(getMachineName()).append(SEPARATOR);
//    builder.append(getServiceName());
//    builder.append(SEPARATOR); //.append(getTimestamp()).append(')');
//
//    return URLEncoder.encode(builder.toString(), "UTF-8");
//  }
//
//  private String initRpcString(int payLoadSize)
//    throws UnsupportedEncodingException
//  {
//    System.out.println("Booyah: Initializing payload with size =" + payLoadSize);
//
//    StringBuilder builder = new StringBuilder();
//
//    int hop = 1;
//    while (payLoadSize > 0)
//    {
//      builder.append('[');
//      builder.append(_machineName).append(SEPARATOR);
//      builder.append(SLASH).append(getServiceName()).append(hop).append(SLASH);
//
//      byte[] payload = new byte[DEFAULT_MAX_CONTEXT_LENGTH];
//      for (int i = 0; i < payload.length; i ++) {
//        payload[i] = (byte)(i%26);
//      }
//      String payloadContext = (new String(Base64.getEncoder().encode(payload))).substring(0, DEFAULT_MAX_CONTEXT_LENGTH);
//      builder.append(payloadContext);
//      builder.append(" GET]");
//
//      payLoadSize -= payloadContext.length();
//      payLoadSize -= 60;
//
//      hop += 1;
//    }
//
//    return URLEncoder.encode(builder.toString(), "UTF-8");
//  }

  public String getInvocationContext(Map<String, String> context, String rpcTrace, String pageKey) {
		try {
		    // only set events for 20% of the traffic and PayLoad is set.
		    boolean isTwentyPercentRate = _rand.nextInt(10) >= 8;
		    StringBuilder stringBuilder = new StringBuilder();
		    buildTraceData(context, isTwentyPercentRate, stringBuilder);
		    String traceData;
				traceData = URLEncoder.encode(stringBuilder.toString(), "UTF-8");
			stringBuilder.setLength(0);
		    stringBuilder.append("rpc.pageKey=").append(pageKey).append(',');
		    stringBuilder.append("serviceCallTraceData=").append(traceData).append(',');
		    stringBuilder.append("com.linkedin.container.rpc.trace.rpcTrace=").append(URLEncoder.encode(rpcTrace.substring(0, rpcTrace.lastIndexOf('[')), "UTF-8"));
		    return stringBuilder.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
  }

  public void buildTraceData(Map<String, String> context, boolean traceEnabled, Appendable appendable) {
    JSONWriter writer = new JSONWriter(appendable);

    Random random = ThreadLocalRandom.current();

    byte[] treeId = new byte[TREE_ID_LENGTH];
    random.nextBytes(treeId);

    int requestId = random.nextInt();

    double scaleFactor = getScaleFactor();

    writer.object()
        .key(VERSION_FIELD).value(VERSION_NUMBER)
        .key(TREE_ID_FIELD).value(Base64.getEncoder().encodeToString(treeId))
        .key(REQUEST_ID_FIELD).value(requestId)
        .key(CALLER_FIELD).object()
            .key(CALLER_ENVIRONMENT_FIELD).value(getEnvironmentName())
            .key(CALLER_CONTAINER_FIELD).value(getContainerName())
            .key(CALLER_SERVICE_FIELD).value(getServiceName())
            .key(CALLER_VERSION_FIELD).value(getVersionNumber())
            .key(CALLER_MACHINE_FIELD).value(getMachineName())
            .key(CALLER_PHYSICAL_FIELD).value(getPhysicalHost())
            .key(CALLER_INSTANCE_FIELD).value(getInstanceId())
            .endObject()
        .key(SCALE_FIELD).value(scaleFactor)
        .key(ENABLED_FIELD).value(traceEnabled)
        .key(CONTEXT_FIELD).value(context)
        .endObject();
  }

  public String getEnvironmentName() {
    return _environmentName;
  }

  public void setEnvironmentName(String environmentName) {
    _environmentName = environmentName;
  }

  public String getContainerName() {
    return _containerName;
  }

  public void setContainerName(String containerName) {
    _containerName = containerName;
  }

  public String getServiceName() {
    return _serviceName;
  }

  public void setServiceName(String serviceName) {
    _serviceName = serviceName;
  }

  public String getVersionNumber() {
    return _versionNumber;
  }

  public void setVersionNumber(String versionNumber) {
    _versionNumber = versionNumber;
  }

  public String getMachineName() {
    return _machineName;
  }

  public void setMachineName(String machineName) {
    _machineName = machineName;
  }

  public String getPhysicalHost() {
    return _physicalHost;
  }

  public void setPhysicalHost(String physicalHost) {
    _physicalHost = physicalHost;
  }

  public String getInstanceId() {
    return _instanceId;
  }

  public void setInstanceId(String instanceId) {
    _instanceId = instanceId;
  }

  public double getScaleFactor() {
    return _scaleFactor;
  }

  public void setScaleFactor(double scaleFactor) {
    _scaleFactor = scaleFactor;
  }
}