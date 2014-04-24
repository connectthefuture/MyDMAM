/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam;

import hd3gtv.configuration.Configuration;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class MyDMAM {
	
	/**
	 * @param filename without path
	 * @return S0000000 or 00000000 or null
	 */
	public static String getIdFromFilename(String filename) {
		if (filename == null) {
			return null;
		}
		if (filename.length() < 8) {
			return null;
		}
		char[] chars = filename.toCharArray();
		char curchar;
		for (int pos = 0; pos < 8; pos++) {
			curchar = chars[pos];
			if ((curchar > 47) && (curchar < 58)) {
				/**
				 * from 0 to 9
				 */
				continue;
			}
			if (((curchar == 83) || (curchar == 115)) && (pos == 0)) {
				/**
				 * Start by "S" or "s"
				 */
				continue;
			}
			return null;
		}
		
		return filename.substring(0, 8);
	}
	
	public static final String byteToString(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xFF;
			if (v < 16) {
				sb.append(0);
			}
			sb.append(Integer.toString(v, 16).toLowerCase());
		}
		return sb.toString();
	}
	
	private volatile static Properties configured_messages;
	
	/**
	 * @return Properties key/values by yaml configuration via "message" entry.
	 *         Restart to see changes.
	 */
	public static Properties getconfiguredMessages() {
		if (configured_messages == null) {
			configured_messages = new Properties();
			if (Configuration.global.isElementExists("messages")) {
				LinkedHashMap<String, String> conf = Configuration.global.getValues("messages");
				for (Map.Entry<String, String> entry : conf.entrySet()) {
					configured_messages.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return configured_messages;
	}
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	public static void testIllegalKeySize() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256", "BC");
			byte[] key = md.digest("".getBytes());
			SecretKey skeySpec = new SecretKeySpec(key, "AES");
			IvParameterSpec salt = new IvParameterSpec(key, 0, 16);
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, salt);
			return;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			if (e.getMessage().equals("Illegal key size")) {
				System.err.println("");
				System.err.println("");
				System.err.println("--------~~~~~~===============~~~~~~--------");
				System.err.println("               Fatal error !");
				System.err.println("You must to setup Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files");
				System.err.println("");
				System.err.println("Go to http://www.oracle.com/technetwork/java/javase/downloads/index.html");
				System.err.println("And download it (it's free and legal)");
				System.err.println("");
				System.err.println("Unzip, and copy US_export_policy.jar and local_policy.jar to this directory:");
				System.err.println("");
				System.out.println(" " + System.getProperty("java.home") + "/lib/security/");
				System.err.println("");
				System.err.println("Overwrite the actual jar files");
				System.err.println("--------~~~~~~==============~~~~~~--------");
				System.err.println("");
			} else {
				e.printStackTrace();
			}
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		System.exit(1);
	}
	
}