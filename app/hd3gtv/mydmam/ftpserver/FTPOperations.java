/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.ftpserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationClusterItem;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.StoppableThread;

public class FTPOperations {
	
	private static Explorer explorer = new Explorer();
	private static final long DELAY_ACTIVE_GROUPS_PATHINDEXING = TimeUnit.SECONDS.toMillis(60);
	private static final long DELAY_BACKGROUD_OPERATIONS = TimeUnit.MINUTES.toMillis(10);
	
	private static FTPOperations global;
	
	static {
		global = new FTPOperations();
	}
	
	public static FTPOperations get() {
		return global;
	}
	
	private Internal internal;
	
	public void start() {
		stop();
		Loggers.FTPserver.info("Start FTP Operations watchdog");
		internal = new Internal();
		internal.setLogger(Loggers.FTPserver);
		internal.start();
	}
	
	/**
	 * Blocking
	 */
	public synchronized void stop() {
		if (internal == null) {
			return;
		}
		Loggers.FTPserver.info("Stop FTP Operations watchdog");
		internal.waitToStop();
	}
	
	private HashSet<FTPUser> active_users_with_group_pathindex;
	
	private FTPOperations() {
		active_users_with_group_pathindex = new HashSet<FTPUser>();
	}
	
	/**
	 * An active user do write operations throw the FTP server.
	 */
	synchronized void addActiveUser(FTPUser ftp_user) {
		if (active_users_with_group_pathindex.contains(ftp_user)) {
			return;
		}
		if (ftp_user.getGroup().getPathindexStoragename() != null) {
			Loggers.FTPserver.debug("Add active user: " + ftp_user);
			active_users_with_group_pathindex.add(ftp_user);
		}
	}
	
	synchronized void removeActiveUser(FTPUser ftp_user) {
		if (active_users_with_group_pathindex.contains(ftp_user)) {
			Loggers.FTPserver.trace("Remove active user: " + ftp_user);
			active_users_with_group_pathindex.remove(ftp_user);
		}
	}
	
	private class Internal extends StoppableThread {
		
		public Internal() {
			super("FTPWatchdog");
		}
		
		public void run() {
			HashSet<FTPGroup> current_active_user_groups = new HashSet<FTPGroup>(1);
			ArrayList<String> users_list = new ArrayList<String>();
			List<SourcePathIndexerElement> storages_active_group_to_refresh = new ArrayList<SourcePathIndexerElement>();
			List<SourcePathIndexerElement> storages_regular_to_refresh = new ArrayList<SourcePathIndexerElement>();
			List<SourcePathIndexerElement> storages_to_force_refresh = new ArrayList<SourcePathIndexerElement>();
			ElasticsearchBulkOperation bulk_op;
			MutationBatch mutator;
			List<FTPUser> expirable_users = new ArrayList<FTPUser>(1);
			List<FTPUser> trashable_users = new ArrayList<FTPUser>(1);
			List<FTPUser> purgeable_users = new ArrayList<FTPUser>(1);
			List<FTPUser> actual_users;
			long next_backgroud_operations = System.currentTimeMillis() + DELAY_BACKGROUD_OPERATIONS;
			SourcePathIndexerElement storage;
			
			while (isWantToRun()) {
				try {
					
					/**
					 * Get an immutable current active user groups list
					 */
					current_active_user_groups.clear();
					users_list.clear();
					synchronized (active_users_with_group_pathindex) {
						for (FTPUser ftpuser : active_users_with_group_pathindex) {
							current_active_user_groups.add(ftpuser.getGroup());
							users_list.add(ftpuser.toString());
						}
					}
					if (Loggers.FTPserver.isTraceEnabled() & users_list.isEmpty() == false) {
						Loggers.FTPserver.trace("Active users: " + users_list + ", active groups: " + current_active_user_groups);
					}
					
					if (current_active_user_groups.isEmpty() == false) {
						bulk_op = Elasticsearch.prepareBulk();
						/**
						 * Search active groups recently added => force refresh
						 */
						storages_to_force_refresh.clear();
						for (FTPGroup ftpgroup : current_active_user_groups) {
							storage = SourcePathIndexerElement.prepareStorageElement(ftpgroup.getPathindexStoragename());
							if (storages_active_group_to_refresh.contains(storage) == false) {
								storages_to_force_refresh.add(storage);
								
							}
						}
						if (Loggers.FTPserver.isTraceEnabled() & storages_to_force_refresh.isEmpty() == false) {
							Loggers.FTPserver.trace("Storage list to force refresh: " + storages_to_force_refresh);
						}
						
						/**
						 * Previously active groups added => normal refresh
						 */
						storages_active_group_to_refresh.clear();
						for (FTPGroup ftpgroup : current_active_user_groups) {
							storage = SourcePathIndexerElement.prepareStorageElement(ftpgroup.getPathindexStoragename());
							if (storages_to_force_refresh.contains(storage) == false) {
								storages_active_group_to_refresh.add(storage);
							}
						}
						if (Loggers.FTPserver.isTraceEnabled() & storages_active_group_to_refresh.isEmpty() == false) {
							Loggers.FTPserver.trace("Storage active group to force refresh: " + storages_active_group_to_refresh);
						}
						
						explorer.refreshStoragePath(bulk_op, storages_active_group_to_refresh, false, DELAY_ACTIVE_GROUPS_PATHINDEXING * 2);
						explorer.refreshStoragePath(bulk_op, storages_to_force_refresh, true, DELAY_ACTIVE_GROUPS_PATHINDEXING * 2);
						bulk_op.terminateBulk();
					}
					
					if (next_backgroud_operations < System.currentTimeMillis()) {
						next_backgroud_operations = System.currentTimeMillis() + DELAY_BACKGROUD_OPERATIONS;
						
						/**
						 * Trash/purge operations
						 */
						FTPUser.getExpirableUsers(expirable_users);
						if (expirable_users.isEmpty() == false) {
							FTPUser.getTrashableUsers(expirable_users, trashable_users);
							FTPUser.getPurgeableUsers(expirable_users, purgeable_users);
							
							mutator = CassandraDb.prepareMutationBatch();
							
							if (trashable_users.isEmpty() == false) {
								Loggers.FTPserver.info("Some user content needs to send to trash: " + trashable_users);
								
								for (int pos = 0; pos < trashable_users.size(); pos++) {
									trashable_users.get(pos).setDisabled(true);
									trashable_users.get(pos).save(mutator);
									trashable_users.get(pos).getGroup().trashUserHomeDirectory(trashable_users.get(pos));
									FTPActivity.purgeUserActivity(trashable_users.get(pos).getUserId());
								}
							}
							
							if (purgeable_users.isEmpty() == false) {
								Loggers.FTPserver.info("Some user content needs to be purged: " + purgeable_users);
								
								mutator = CassandraDb.prepareMutationBatch();
								for (int pos = 0; pos < purgeable_users.size(); pos++) {
									purgeable_users.get(pos).removeUser(mutator);
									purgeable_users.get(pos).getGroup().deleteUserHomeDirectory(purgeable_users.get(pos));
									FTPActivity.purgeUserActivity(purgeable_users.get(pos).getUserId());
								}
							}
							
							mutator.execute();
						} /** end trash operation */
						
						/**
						 * Group operations
						 */
						storages_regular_to_refresh.clear();
						for (FTPGroup group : FTPGroup.getDeclaredGroups().values()) {
							/**
							 * Free space
							 */
							group.checkFreeSpace();
							
							/**
							 * Orphan directories
							 */
							actual_users = group.listAllActualUsers();
							if (Loggers.FTPserver.isTraceEnabled()) {
								Loggers.FTPserver.trace("Actual users for group " + group + ": " + actual_users);
							}
							
							for (int pos = 0; pos < actual_users.size(); pos++) {
								if (actual_users.get(pos).isValidInDB() == false) {
									group.deleteUserHomeDirectory(actual_users.get(pos));
									Loggers.FTPserver.info("Group \"" + group.getName() + "\" has an orphan directory: " + actual_users.get(pos));
								}
							}
							
							/**
							 * Refresh pathindex
							 */
							if (group.getPathindexStoragename() != null) {
								if (current_active_user_groups.contains(group) == false) {
									/**
									 * Configuration set a pathindexing, and group is not active.
									 */
									storages_regular_to_refresh.add(SourcePathIndexerElement.prepareStorageElement(group.getPathindexStoragename()));
								}
							}
							
						} /** end for each groups */
						
						if (storages_regular_to_refresh.isEmpty() == false) {
							if (Loggers.FTPserver.isTraceEnabled()) {
								Loggers.FTPserver.debug("Regular pathindex refresh: " + storages_regular_to_refresh);
							}
							
							bulk_op = Elasticsearch.prepareBulk();
							explorer.refreshStoragePath(bulk_op, storages_regular_to_refresh, false, DELAY_BACKGROUD_OPERATIONS * 5);
							bulk_op.terminateBulk();
						}
						
					} /** end next_backgroud_operations */
					
				} catch (ConnectionException e) {
					Loggers.FTPserver.warn("Can't access to db", e);
				} catch (Exception e) {
					Loggers.FTPserver.error("General operation error", e);
				}
				
				stoppableSleep(DELAY_ACTIVE_GROUPS_PATHINDEXING);
			}
		}
	}
	
	public static FtpServer ftpServerFactory(String name, HashMap<String, ConfigurationItem> all_instances_confs, HashMap<String, Ftplet> ftplets) throws Exception {
		FtpServerFactory server_factory = new FtpServerFactory();
		
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		
		DataConnectionConfigurationFactory dccf = new DataConnectionConfigurationFactory();
		dccf.setActiveEnabled(true);
		dccf.setActiveIpCheck(true);
		log.put("active", dccf.isActiveEnabled());
		log.put("active IP check", dccf.isActiveIpCheck());
		
		ConfigurationClusterItem local = Configuration.getClusterConfiguration(all_instances_confs, name, "active", "0.0.0.0", 20).get(0);
		dccf.setActiveLocalAddress(local.address);
		dccf.setActiveLocalPort(local.port);
		log.put("active local", dccf.getActiveLocalAddress() + ":" + dccf.getActiveLocalPort());
		
		int idle = Configuration.getValue(all_instances_confs, name, "idle", 300);
		dccf.setIdleTime(idle);
		log.put("Idle time", dccf.getIdleTime());
		
		dccf.setImplicitSsl(false);
		dccf.setPassiveAddress(Configuration.getValue(all_instances_confs, name, "passive-internal", "0.0.0.0"));
		dccf.setPassiveExternalAddress(Configuration.getValue(all_instances_confs, name, "passive-external", "0.0.0.0"));
		dccf.setPassivePorts(Configuration.getValue(all_instances_confs, name, "passive-ports", "30000-40000"));
		log.put("passive", dccf.getPassiveAddress() + ">" + dccf.getPassiveExternalAddress());
		log.put("passive ports", dccf.getPassivePorts());
		
		ListenerFactory factory = new ListenerFactory();
		factory.setPort(Configuration.getValue(all_instances_confs, name, "listen", 21));
		factory.setIpFilter(FTPIpFilter.getFilter());
		factory.setIdleTimeout(idle);
		log.put("port", factory.getPort());
		
		factory.setDataConnectionConfiguration(dccf.createDataConnectionConfiguration());
		
		server_factory.addListener("default", factory.createListener());
		
		FTPUserManager ftpum;
		if (name == "default") {
			ftpum = new FTPUserManager("");
		} else {
			ftpum = new FTPUserManager(name);
			server_factory.setUserManager(ftpum);
		}
		server_factory.setUserManager(ftpum);
		log.put("User Manager", ftpum);
		
		server_factory.setFtplets(ftplets);
		
		Loggers.FTPserver.info("Start FTP Server: " + log);
		
		FtpServer server = server_factory.createServer();
		server.start();
		return server;
	}
	
}
