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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.ftpserver.FTPUser.FTPSimpleUser;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.tools.GsonIgnoreStrategy;

public class FTPOperations {
	
	private static final Gson gson;
	// private static final Gson simple_gson;
	// private static final Gson pretty_gson;
	
	private static FTPOperations global;
	
	static {
		global = new FTPOperations();
		
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		/*builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		builder.registerTypeAdapter(new TypeToken<ArrayList<WatchFolderEntry>>() {
		}.getType(), new WatchFolderEntry.SerializerList());
		builder.registerTypeAdapter(new TypeToken<LinkedHashMap<String, TranscoderWorker>>() {
		}.getType(), new TranscoderWorker.SerializerMap());
		builder.registerTypeAdapter(TranscodeProfile.class, new TranscodeProfile.Serializer());*/
		
		// simple_gson = builder.create();
		
		/*builder.registerTypeAdapter(InstanceStatus.class, new InstanceStatus.Serializer());
		builder.registerTypeAdapter(InstanceAction.class, new InstanceAction.Serializer());
		builder.registerTypeAdapter(JobNG.class, new JobNG.Serializer());
		builder.registerTypeAdapter(JobAction.class, new JobAction.Serializer());
		builder.registerTypeAdapter(GsonThrowable.class, new GsonThrowable.Serializer());
		builder.registerTypeAdapter(WorkerCapablitiesExporter.class, new WorkerCapablitiesExporter.Serializer());
		builder.registerTypeAdapter(WorkerExporter.class, new WorkerExporter.Serializer());
		
		builder.registerTypeAdapter(JobContext.class, new JobContext.Serializer());
		builder.registerTypeAdapter(new TypeToken<ArrayList<JobContext>>() {
		}.getType(), new JobContext.SerializerList());
		
		builder.registerTypeAdapter(JobCreatorDeclarationSerializer.class, new JobCreatorDeclarationSerializer());
		builder.registerTypeAdapter(TriggerJobCreator.class, TriggerJobCreator.serializer);
		builder.registerTypeAdapter(CyclicJobCreator.class, CyclicJobCreator.serializer);*/
		
		gson = builder.create();
		// pretty_gson = builder.setPrettyPrinting().create();
	}
	
	public static FTPOperations get() {
		return global;
	}
	
	public static Gson getGson() {
		return gson;
	}
	
	/*public static Gson getSimpleGson() {
		return simple_gson;
	}*/
	
	private boolean stop;
	private Internal internal;
	
	public void start() {
		stop();
		
		internal = new Internal();
		internal.start();
	}
	
	/**
	 * Blocking
	 */
	public synchronized void stop() {
		if (internal == null) {
			return;
		}
		stop = true;
		try {
			while (internal.isAlive()) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			Loggers.FTPserver.warn("Can't sleep during stop", e);
		}
	}
	
	private HashSet<FTPUser> active_users;
	
	private FTPOperations() {
		active_users = new HashSet<FTPUser>();
	}
	
	synchronized void addActiveUser(FTPUser ftp_user) {
		if (active_users.contains(ftp_user)) {
			return;
		}
		if (ftp_user.getGroup().getPathindexStoragenameLiveUpdate() != null) {
			active_users.add(ftp_user);
		}
	}
	
	synchronized void removeActiveUser(FTPUser ftp_user) {
		active_users.remove(ftp_user);
	}
	
	private static Explorer explorer = new Explorer();
	public static final long DELAY_2_INDEX_FOR_PATHINDEX = TimeUnit.SECONDS.toMillis(60);
	
	private class Internal extends Thread {
		
		public Internal() {
			setName("FTPWatchdog");
			setDaemon(true);
		}
		
		public void run() {
			stop = false;
			
			HashSet<FTPUser> current_active_users = new HashSet<FTPUser>(1);
			ElasticsearchBulkOperation bulk_op;
			MutationBatch mutator;
			List<FTPUser> trashable_users;
			List<FTPUser> purgeable_users;
			List<FTPSimpleUser> actual_users;
			
			try {
				while (stop == false) {
					try {
						synchronized (active_users) {
							current_active_users.clear();
							current_active_users.addAll(active_users);
						}
						
						bulk_op = Elasticsearch.prepareBulk();
						
						for (FTPUser ftpuser : current_active_users) {
							if (ftpuser.getLastPathIndexRefreshed() + DELAY_2_INDEX_FOR_PATHINDEX < System.currentTimeMillis()) {
								// TODO live index
								// String storage_name = .getPathindexStoragenameLiveUpdate();
								// explorer.refreshCurrentStoragePath(bulk_op, elements, purge_before);
								// explorer.refreshStoragePath(bulk_op, elements, purge_before);
								ftpuser.setLastPathIndexRefreshed();
								ftpuser.save();
							}
						}
						
						// TODO index for all group has pathindex_storagename_for_live_update, but not from current_active_users
						bulk_op.terminateBulk();
						
						trashable_users = FTPUser.getTrashableUsers();
						
						if (trashable_users.isEmpty() == false) {
							try {
								mutator = CassandraDb.prepareMutationBatch();
								for (int pos = 0; pos < trashable_users.size(); pos++) {
									trashable_users.get(pos).setDisabled(true);
									trashable_users.get(pos).save(mutator);
									trashable_users.get(pos).getGroup().trashUserHomeDirectory(trashable_users.get(pos));
								}
								mutator.execute();
								
								purgeable_users = FTPUser.getPurgeableUsers(trashable_users);
								if (purgeable_users.isEmpty() == false) {
									mutator = CassandraDb.prepareMutationBatch();
									for (int pos = 0; pos < purgeable_users.size(); pos++) {
										purgeable_users.get(pos).removeUser(mutator);
										purgeable_users.get(pos).getGroup().deleteUserHomeDirectory(purgeable_users.get(pos));
									}
									mutator.execute();
								}
							} catch (Exception e) {
								Loggers.FTPserver.error("Can't do clean (trash/purge) operations", e);
							}
						}
						
						for (FTPGroup group : FTPGroup.getDeclaredGroups().values()) {
							group.checkFreeSpace();
							
							try {
								actual_users = group.listAllActualUsers();
								for (int pos = 0; pos < actual_users.size(); pos++) {
									if (actual_users.get(pos).isValidInDB() == false) {
										actual_users.get(pos).getGroup().deleteUserHomeDirectory(actual_users.get(pos));
									}
								}
							} catch (IOException e) {
								Loggers.FTPserver.error("Can't do clean (orphan directory) operations", e);
								e.printStackTrace();
							}
						}
						
					} catch (ConnectionException e) {
						Loggers.FTPserver.error("Can't access to db", e);
					}
					
					for (int pos_sleep = 0; pos_sleep < 10 * 60 * 10; pos_sleep++) {
						if (stop) {
							return;
						}
						sleep(100);
					}
				}
			} catch (InterruptedException e) {
				Loggers.FTPserver.error("Can't sleep", e);
			}
		}
	}
}