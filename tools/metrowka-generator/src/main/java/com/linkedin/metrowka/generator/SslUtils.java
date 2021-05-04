package com.linkedin.metrowka.generator;

import static sun.nio.ch.IOStatus.EOF;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;


/**
 * @author solu
 * Date: 12/6/18
 */
public class SslUtils {
  private static final String JKS_STORE_TYPE_NAME = "JKS";
  static final String P12_STORE_TYPE_NAME = "PKCS12";
  private static final String DEFAULT_ALGORITHM = "SunX509";
  private static final String DEFAULT_PROTOCOL = "TLS";

  /**
   * The keyStoreFile takes a File object of p12 or jks file depends on keyStoreType
   * The trustStoreFile always takes a File object of JKS file.
   */
  public static SslContext build(File keyStoreFile, String keyStorePassword, String keyStoreType) throws Exception {
    if (!keyStoreType.equalsIgnoreCase(P12_STORE_TYPE_NAME) && !keyStoreType.equalsIgnoreCase(JKS_STORE_TYPE_NAME)) {
      throw new Exception("Unsupported keyStoreType: " + keyStoreType);
    }

    // Load the key Store
    final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
    keyStore.load(toInputStream(keyStoreFile), keyStorePassword.toCharArray());


    // Set key manager from key store
    final KeyManagerFactory kmf = KeyManagerFactory.getInstance(DEFAULT_ALGORITHM);
    kmf.init(keyStore, keyStorePassword.toCharArray());

    // Create a trust manager that does not validate certificate chains
    TrustManager trustAllCerts = new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
          public void checkClientTrusted(X509Certificate[] certs, String authType) {}
          public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    };

    return SslContextBuilder.forClient().trustManager(trustAllCerts).keyManager(kmf).protocols("TLSv1").build();
  }

  private static InputStream toInputStream(File storeFile) throws IOException {
    byte[] data = readFileToByteArray(storeFile);
    return new ByteArrayInputStream(data);
  }

  public static byte[] readFileToByteArray(final File file) throws IOException {
    try (InputStream in = openInputStream(file)) {
      final long fileLength = file.length();
      // file.length() may return 0 for system-dependent entities, treat 0 as unknown length - see IO-453
      return fileLength > 0 ? toByteArray(in, fileLength) : toByteArray(in, 0);
    }
  }

  public static FileInputStream openInputStream(final File file) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canRead() == false) {
        throw new IOException("File '" + file + "' cannot be read");
      }
    } else {
      throw new FileNotFoundException("File '" + file + "' does not exist");
    }
    return new FileInputStream(file);
  }

  public static byte[] toByteArray(final InputStream input, final long size) throws IOException {

    if (size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Size cannot be greater than Integer max value: " + size);
    }

    return toByteArray(input, (int) size);
  }

  public static byte[] toByteArray(final InputStream input, final int size) throws IOException {

    if (size < 0) {
      throw new IllegalArgumentException("Size must be equal or greater than zero: " + size);
    }

    if (size == 0) {
      return new byte[0];
    }

    final byte[] data = new byte[size];
    int offset = 0;
    int read;

    while (offset < size && (read = input.read(data, offset, size - offset)) != EOF) {
      offset += read;
    }

    if (offset != size) {
      throw new IOException("Unexpected read size. current: " + offset + ", expected: " + size);
    }

    return data;
  }
}
