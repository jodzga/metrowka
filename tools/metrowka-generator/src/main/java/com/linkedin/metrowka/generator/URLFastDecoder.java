package com.linkedin.metrowka.generator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.netty.util.AsciiString;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.PlatformDependent;

public class URLFastDecoder {
	  
	  public static CharSequence URLFastDecode(CharSequence s)
	  {
		  if (s instanceof AsciiString)
		  {
			  return URLDecodeAsciiString((AsciiString)s);
		  } else
		  {
			  return URLCodec.decode(s, StandardCharsets.UTF_8);
		  }
	  }
	  
	  private static final FastThreadLocal<char[]> LOCAL_STRING_BUILDER = new FastThreadLocal<char[]>();
	  
	  private static final byte[] HEX2B;
	  static {
	      HEX2B = new byte[Character.MAX_VALUE + 1];
	      Arrays.fill(HEX2B, (byte) -1);
	      HEX2B['0'] = (byte) 0;
	      HEX2B['1'] = (byte) 1;
	      HEX2B['2'] = (byte) 2;
	      HEX2B['3'] = (byte) 3;
	      HEX2B['4'] = (byte) 4;
	      HEX2B['5'] = (byte) 5;
	      HEX2B['6'] = (byte) 6;
	      HEX2B['7'] = (byte) 7;
	      HEX2B['8'] = (byte) 8;
	      HEX2B['9'] = (byte) 9;
	      HEX2B['A'] = (byte) 10;
	      HEX2B['B'] = (byte) 11;
	      HEX2B['C'] = (byte) 12;
	      HEX2B['D'] = (byte) 13;
	      HEX2B['E'] = (byte) 14;
	      HEX2B['F'] = (byte) 15;
	      HEX2B['a'] = (byte) 10;
	      HEX2B['b'] = (byte) 11;
	      HEX2B['c'] = (byte) 12;
	      HEX2B['d'] = (byte) 13;
	      HEX2B['e'] = (byte) 14;
	      HEX2B['f'] = (byte) 15;
	  }

	  public static int decodeHexNibble(final char c) {
	      // Character.digit() is not used here, as it addresses a larger
	      // set of characters (both ASCII and full-width latin letters).
	      final int index = c;
	      return HEX2B[index];
	  }
	  
	  private static byte decodeHexByte(CharSequence s, int pos) {
	      int hi = decodeHexNibble(s.charAt(pos));
	      int lo = decodeHexNibble(s.charAt(pos + 1));
	      if (hi == -1 || lo == -1) {
	          throw new IllegalArgumentException(String.format(
	                  "invalid hex byte '%s' at index %d of '%s'", s.subSequence(pos, pos + 2), pos, s));
	      }
	      return (byte) ((hi << 4) + lo);
	  }
	  
	  public static CharSequence URLDecodeAsciiString(AsciiString as) {
		  final byte[] bytes = as.array();
		  final int offset = as.arrayOffset();
		  final int length = as.length();
		  char[] ca = LOCAL_STRING_BUILDER.get();
		  if (ca == null || ca.length < length) {
			  ca = new char[length];
			  LOCAL_STRING_BUILDER.set(ca);
		  }
		  int caLength = 0;
		  byte[] buf = null;
		  int bufIdx;
		  for (int i = 0; i < length; i++) {
			  char c;
		      // Try to use unsafe to avoid checking the index bounds
		      if (PlatformDependent.hasUnsafe()) {
		    	  c = (char) (PlatformDependent.getByte(bytes, offset + i) & 0xFF);
		      } else {
		    	  c = (char) (bytes[offset + i] & 0xFF);
		      }
		      if (c != '%') {
		    	  if (c == '+') {
		    		  ca[caLength++] = ' ';
		    	  } else {
		    		  ca[caLength++] = c;
		    	  }
	          } else {
	              if (i + 3 > length) {
	                  throw new IllegalArgumentException("unterminated escape sequence at index " + i + " of: " + as);
	              }
	              byte b = decodeHexByte(as, i + 1);
	              i+=2;
	              if ((b & 0x80) == 0) {
	            	  //fast path for 1-byte encoded UTF-8 characters
	            	  c = (char) (b & 0xFF);
		    		  ca[caLength++] = c;
	              } else {
	            	  if (buf == null) {
	            		  buf = PlatformDependent.allocateUninitializedArray((length - i) / 3);
	            	  }
	            	  buf[0] = b; 
	            	  bufIdx = 1;
	            	  while (i < length && as.charAt(i) == '%') {
	                      buf[bufIdx++] = decodeHexByte(as, i + 1);
	                      i += 3;
	            	  }
	            	  String s = new String(buf, 0, bufIdx, StandardCharsets.UTF_8);
	            	  for (int j = 0; i < s.length(); j++) {
			    		  ca[caLength++] = s.charAt(j);
	            	  }
	              }
	          }
		  }
		  return new String(ca, 0, caLength);
	  }
	
}
