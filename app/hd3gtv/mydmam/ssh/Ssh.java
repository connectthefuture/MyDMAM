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
package hd3gtv.mydmam.ssh;

import hd3gtv.javasimpleservice.ServiceManager;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.tools.ExecprocessGettext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class Ssh {
	
	private static Ssh global;
	private static boolean debug_ssh_mode = false;
	
	static {
		JSch.setLogger(new Logger() {
			
			public boolean isEnabled(int level) {
				if (debug_ssh_mode) {
					return true;
				} else if (level == DEBUG) {
					return false;
				} else if (level == INFO) {
					return false;
				} else {
					return true;
				}
			}
			
			public void log(int level, String message) {
				switch (level) {
				case DEBUG:
					Log2.log.debug(message);
					break;
				case INFO:
					Log2.log.debug(message);
					break;
				case WARN:
					Log2.log.info(message);
					break;
				case ERROR:
					Log2.log.error(message, null);
					break;
				case FATAL:
					Log2.log.error(message, null);
					break;
				default:
					Log2.log.info(message);
					break;
				}
			}
		});
	}
	
	public static Ssh getGlobal() throws IOException, JSchException {
		if (global == null) {
			File service_config_path = (new File(System.getProperty("service.config.path", "conf/app.d"))).getCanonicalFile();
			global = new Ssh(new File(service_config_path.getParent() + File.separator + "ssh"));
		}
		return global;
	}
	
	private String private_key;
	private String public_key;
	private String known_hosts;
	private ConnectionStore store;
	
	/**
	 * In msec
	 */
	public int timeout = 10000;
	
	public Ssh(File configuration_path) throws IOException, JSchException {
		if (configuration_path == null) {
			throw new NullPointerException("\"configuration_path\" can't to be null");
		}
		if (configuration_path.exists() == false) {
			if (configuration_path.mkdirs() == false) {
				throw new IOException("Can't create " + configuration_path.getPath());
			}
		} else {
			if (configuration_path.isDirectory() == false) {
				throw new FileNotFoundException(configuration_path.getPath() + " is not a directory");
			}
		}
		
		private_key = configuration_path.getAbsolutePath() + File.separator + "id_rsa";
		public_key = configuration_path.getAbsolutePath() + File.separator + "id_rsa.pub";
		known_hosts = configuration_path.getAbsolutePath() + File.separator + "known_hosts";
		
		if ((new File(private_key)).exists() == false) {
			generateKeys();
		} else if ((new File(public_key)).exists() == false) {
			generateKeys();
		}
		
		store = new ConnectionStore(configuration_path, this);
	}
	
	private void generateKeys() throws JSchException, IOException {
		String comment = "mydmam-" + ServiceManager.getInstancename(false) + "@" + InetAddress.getLocalHost().getHostName();
		
		JSch jsch = new JSch();
		
		KeyPair kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
		kpair.writePrivateKey(private_key);
		kpair.writePublicKey(public_key, comment);
		kpair.dispose();
		
		String os_name = System.getProperty("os.name").toLowerCase();
		if (os_name.indexOf("win") == -1) {
			ArrayList<String> params = new ArrayList<String>();
			params.add("600");
			params.add(private_key);
			ExecprocessGettext exec = new ExecprocessGettext("chmod", params);
			exec.start();
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("Public key", new File(public_key));
		dump.add("Private key", new File(private_key));
		dump.add("Finger print", kpair.getFingerPrint());
		Log2.log.info("Generate SSH Keys for MyDMAM", dump);
	}
	
	public Ssh declareHost(String host, int port, String username, final String password, String connection_name, boolean create_remote_authorized_file) throws JSchException, IOException {
		JSch jsch = new JSch();
		
		jsch.addIdentity(private_key, public_key, null);
		jsch.setKnownHosts(known_hosts);
		// jsch.setHostKeyRepository(hkrepo)
		
		Session session = jsch.getSession(username, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
		// session.setConfig("ConnectTime", String.valueOf(timeout));
		// session.setConfig("PreferredAuthentications", "password,keyboard-interactive"); // ,publickey
		// session.setConfig("ForwardAgent", "no");
		// session.setConfig("HashKnownHosts", "no");
		// session.setConfig("PasswordAuthentication", "no");
		/*session.setServerAliveInterval(interval)
		session.setServerAliveCountMax(count)
		session.set*/
		
		session.setPassword(password);
		
		session.setUserInfo(new Interactive(password));
		
		session.connect();
		
		if (create_remote_authorized_file) {
			Channel channel = session.openChannel("shell");
			
			StringBuffer sb = new StringBuffer();
			sb.append("mkdir -p .ssh\n");
			sb.append("echo \"");
			
			File public_key_file = new File(public_key);
			FileInputStream fis = new FileInputStream(public_key_file);
			byte[] public_key_filename_content = new byte[(int) public_key_file.length()];
			fis.read(public_key_filename_content, 0, (int) public_key_file.length());
			fis.close();
			
			sb.append(new String(public_key_filename_content).trim());
			sb.append("\" >> .ssh/authorized_keys2\n");
			sb.append("exit\n");
			
			ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
			
			channel.setInputStream(bais);
			channel.setOutputStream(System.out);
			
			channel.connect(timeout);
			
			while (channel.isClosed() == false) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			channel.disconnect();
			
			store.addConnection(host, port, username, connection_name);
		} else {
			store.addConnection(host, port, username, password, connection_name);
		}
		session.disconnect();
		
		return this;
	}
	
	Session getSession(String host, int port, String username, String password) throws JSchException {
		JSch jsch = new JSch();
		jsch.addIdentity(private_key, public_key, null);
		jsch.setKnownHosts(known_hosts);
		Session session = jsch.getSession(username, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(password);
		session.setUserInfo(new Interactive(password));
		session.connect();
		return session;
	}
	
	Session getSession(String host, int port, String username) throws JSchException {
		JSch jsch = new JSch();
		jsch.addIdentity(private_key, public_key, null);
		jsch.setKnownHosts(known_hosts);
		Session session = jsch.getSession(username, host, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		return session;
	}
	
	public Remote getRemote(String connection_name) throws JSchException {
		return store.getConnection(connection_name);
	}
	
	public boolean isRemoteExists(String connection_name) {
		return store.exists(connection_name);
	}
	
	private class Interactive implements UserInfo, UIKeyboardInteractive {
		public String getPassword() {
			Log2.log.debug("gP");
			return passwd;
		}
		
		public boolean promptYesNo(String message) {
			if (message.equals(known_hosts + " does not exist.\nAre you sure you want to create it?")) {
				return true;
			}
			Log2.log.debug(message);
			return false;
		}
		
		private String passwd;
		
		public Interactive(String password) {
			this.passwd = password;
		}
		
		public String getPassphrase() {
			Log2.log.debug("gP");
			return null;
		}
		
		public boolean promptPassphrase(String message) {
			Log2.log.debug(message);
			return true;
		}
		
		public boolean promptPassword(String message) {
			Log2.log.debug(message);
			return true;
		}
		
		public void showMessage(String message) {
			Log2.log.debug(message);
		}
		
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
			return null;
		}
	}
	
}
