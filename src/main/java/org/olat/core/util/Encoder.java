/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.core.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

import com.oreilly.servlet.Base64Encoder;


/**
 * Description: it's our hash factory
 * 
 * 
 * @author Sabina Jeger
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class Encoder {
	
	private static final OLog log = Tracing.createLoggerFor(Encoder.class);
	
	public enum Algorithm {
		md5("MD5", 1, true),
		md5_noSalt("MD5", 1, false),
		sha1("SHA-1", 100, true),
		sha256("SHA-256", 100, true),
		sha512("SHA-512", 100, true),
		pbkdf2("PBKDF2WithHmacSHA1", 20000, true);

		private final boolean salted;
		private final int iterations;
		private final String algorithm;
		
		private Algorithm(String algorithm, int iterations, boolean salted) {
			this.algorithm = algorithm;
			this.iterations = iterations;
			this.salted = salted;
		}
		
		public boolean isSalted() {
			return salted;
		}
		
		public String getAlgorithm() {
			return algorithm;
		}
		
		public int getIterations() {
			return iterations;
		}
		
		public static final Algorithm find(String str) {
			if(StringHelper.containsNonWhitespace(str)) {
				for(Algorithm value:values()) {
					if(value.name().equals(str)) {
						return value;
					}
				}
			}
			return md5;
		}
	}

	/**
	 * The MD5 helper object for this class.
	 */
	public static final MD5Encoder md5Encoder = new MD5Encoder();

	/**
	 * encrypt the supplied argument with md5.
	 * 
	 * @param s
	 * @return MD5 encrypted string
	 */
	public static String md5hash(String s) {
		return md5(s, null);
	}
	
	public static String encrypt(String s, String salt, Algorithm algorithm) {
		switch(algorithm) {
			case md5: return md5(s, salt);
			case sha1:
			case sha256:
			case sha512:
				return digest(s, salt, algorithm);
			case pbkdf2:
				return secretKey(s, salt, algorithm);
			default: return md5(s, salt);
		}
	}

	protected static String md5(String s, String salt) {
		try {
			byte[] inbytes = s.getBytes();
			MessageDigest digest = MessageDigest.getInstance(Algorithm.md5.algorithm);
			digest.reset();
			if(salt != null) {
				digest.update(salt.getBytes());
			}
			byte[] outbytes = digest.digest(inbytes);
			return md5Encoder.encode(outbytes);
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
			return null;
		}
	}
	
	protected static String digest(String password, String salt, Algorithm algorithm) {
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithm());
			digest.reset();
			if(salt != null) {
				digest.update(salt.getBytes());
			}
			byte[] input = password.getBytes("UTF-8");
			for(int i=algorithm.getIterations(); i-->0; ) {
				input = digest.digest(input);
			}
			return byteToBase64(input);
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
			return null;
		} catch (UnsupportedEncodingException e) {
			log.error("", e);
			return null;
		}
	}
	
	protected static String secretKey(String password, String salt, Algorithm algorithm) {
		try {
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), algorithm.getIterations(), 160);
			SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm.getAlgorithm());
			return byteToBase64(f.generateSecret(spec).getEncoded());
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
			return null;
		} catch (InvalidKeySpecException e) {
			log.error("", e);
			return null;
		}
	}
	
	public static String getSalt() {
	    try {
				//Always use a SecureRandom generator
				SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
				//Create array for salt
				byte[] salt = new byte[16];
				//Get a random salt
				sr.nextBytes(salt);
				//return salt
				return byteToBase64(salt);
			} catch (NoSuchAlgorithmException e) {
				log.error("", e);
				return null;
			}
	}

  public static String byteToBase64(byte[] data){
  	return Base64Encoder.encode(data);
  }
}