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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.manager;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.DeployColumnDef;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.IndexQuery;
import com.netflix.astyanax.serializers.StringSerializer;

/**
 * Use AppManager to for create job.
 */
public final class JobNG {
	
	private static int TTL = 3600 * 24 * 7;
	
	public enum JobStatus {
		TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING;
		
		/*void pushToDatabase(MutationBatch mutator, String key, int ttl) {
		 * 			mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("status", this.name().toUpperCase(), ttl);		}
		static TaskJobStatus pullFromDatabase(ColumnList<String> columns) {
			return fromString(columns.getStringValue("status", "WAITING"));		}
		static void selectByName(IndexQuery<String, String> index_query, TaskJobStatus status) {
			index_query.addExpression().whereColumn("status").equals().value(status.name().toUpperCase());
		}
		*/
	}
	
	@SuppressWarnings("unused")
	private JobNG() {
		/**
		 * For Gson, and from DB
		 */
	}
	
	/**
	 * Declaration & configuration vars
	 */
	private String key;
	@SuppressWarnings("unused")
	private Class creator;
	private boolean urgent;
	@SuppressWarnings("unused")
	private String name;
	private long expiration_date;
	@SuppressWarnings("unused")
	private long max_execution_time;
	@SuppressWarnings("unused")
	private String require_key;
	private long create_date;
	private boolean delete_after_completed;
	@SuppressWarnings("unused")
	private String instance_status_creator_key;
	private String instance_status_creator_hostname;
	@SuppressWarnings("unused")
	private int priority;
	
	@GsonIgnore
	private JobContext context;
	
	/**
	 * Activity vars
	 */
	private JobStatus status;
	private long update_date;
	@SuppressWarnings("unused")
	private GsonThrowable processing_error;
	private Progression progression;
	@SuppressWarnings("unused")
	private long start_date;
	@SuppressWarnings("unused")
	private long end_date;
	@SuppressWarnings("unused")
	private String worker_reference;
	@SuppressWarnings("unused")
	private Class worker_class;
	@SuppressWarnings("unused")
	private String instance_status_executor_key;
	@SuppressWarnings("unused")
	private String instance_status_executor_hostname;
	
	JobNG(AppManager manager, JobContext context) throws ClassNotFoundException {
		this.context = context;
		MyDMAM.checkIsAccessibleClass(context.getClass(), false);
		key = "job:" + UUID.randomUUID().toString();
		name = "Generic job created at " + (new Date()).toString();
		urgent = false;
		priority = 0;
		delete_after_completed = false;
		expiration_date = Long.MAX_VALUE;
		status = JobStatus.WAITING;
		
		instance_status_creator_key = manager.getInstance_status().getDatabaseKey();
		instance_status_creator_hostname = manager.getInstance_status().getHostName();
		progression = null;
		processing_error = null;
		update_date = -1;
		start_date = -1;
		end_date = -1;
	}
	
	public JobNG setName(String name) {
		this.name = name;
		return this;
	}
	
	public JobNG setCreator(Class creator) {
		this.creator = creator;
		return this;
	}
	
	public JobNG setUrgent() {
		urgent = true;
		return this;
	}
	
	public JobNG setRequireCompletedJob(JobNG require) {
		require_key = require.key;
		return this;
	}
	
	public JobNG setExpirationTime(long duration, TimeUnit unit) {
		if (duration < 0) {
			return this;
		}
		expiration_date = System.currentTimeMillis() + unit.toMillis(duration);
		return this;
	}
	
	public JobNG setMaxExecutionTime(long duration, TimeUnit unit) {
		this.max_execution_time = unit.toMillis(duration);
		return this;
	}
	
	/**
	 * Doesn't change current status if POSTPONED, PROCESSING or PREPARING.
	 */
	public JobNG setPostponed() {
		if (status == JobStatus.POSTPONED) {
			return this;
		}
		if (status == JobStatus.PROCESSING) {
			return this;
		}
		if (status == JobStatus.PREPARING) {
			return this;
		}
		status = JobStatus.POSTPONED;
		return this;
	}
	
	public JobNG setDeleteAfterCompleted() {
		delete_after_completed = true;
		return this;
	}
	
	public JobContext getContext() {
		return context;
	}
	
	public void publish() {
		create_date = System.currentTimeMillis();
		update_date = create_date;
		if (urgent) {
			// priority = countTasksFromStatus(TaskJobStatus.WAITING) + 1;//TODO compute priority
		}
		// TODO access to broker, and publish job...
	}
	
	/**
	 * To force changes (sync method) with set() functions.
	 * If job is actually POSTPONED, PROCESSING or PREPARING, changes will be canceled by the executor worker.
	 */
	public void saveChanges() {
		update_date = System.currentTimeMillis();
		// TODO access to broker, and push job...
	}
	
	static class Serializer implements JsonSerializer<JobNG>, JsonDeserializer<JobNG> {
		public JobNG deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext jcontext) throws JsonParseException {
			JsonObject json = (JsonObject) jejson;
			String context_class = json.get("context_class").getAsString();
			json.remove("context_class");
			
			JobNG job = AppManager.getGson().fromJson(json, JobNG.class);
			try {
				job.context = (JobContext) Class.forName(context_class).newInstance();
				job.context.contextFromJson(json.getAsJsonObject("context"));
			} catch (Exception e) {
				throw new JsonParseException("Invalid context class", e);
			}
			return job;
		}
		
		public JsonElement serialize(JobNG src, Type typeOfSrc, JsonSerializationContext jcontext) {
			JsonObject result = (JsonObject) AppManager.getGson().toJsonTree(src);
			result.addProperty("context_class", src.context.getClass().getName());
			result.add("context", src.context.contextToJson());
			return null;
		}
		
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
	public JsonObject toJson() {
		return AppManager.getGson().toJsonTree(this).getAsJsonObject();
	}
	
	public class Progression {
		private volatile int progress = 0;
		private volatile int progress_size = 0;
		private volatile int step = 0;
		private volatile int step_count = 0;
		private volatile String last_message;
		
		/**
		 * Async update.
		 * @param last_message can be null.
		 */
		public void update(String last_message) {
			update_date = System.currentTimeMillis();
			this.last_message = last_message;
		}
		
		/**
		 * Async update.
		 */
		public void updateStep(int step, int step_count) {
			update_date = System.currentTimeMillis();
			this.step = step;
			this.step_count = step_count;
		}
		
		/**
		 * Async update.
		 */
		public void updateProgress(int progress, int progress_size) {
			update_date = System.currentTimeMillis();
			this.progress = progress;
			this.progress_size = progress_size;
		}
	}
	
	// TODO call by broker.
	void prepareProcessing(AppManager manager, WorkerNG worker) {
		update_date = System.currentTimeMillis();
		status = JobStatus.PREPARING;
		worker_class = worker.getClass();
		worker_reference = worker.getReference();
		instance_status_executor_key = manager.getInstance_status().getDatabaseKey();
		instance_status_executor_hostname = manager.getInstance_status().getHostName();
	}
	
	Progression startProcessing() {
		update_date = System.currentTimeMillis();
		start_date = update_date;
		status = JobStatus.PROCESSING;
		progression = new Progression();
		return progression;
	}
	
	void endProcessing_Done() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.DONE;
	}
	
	void endProcessing_Stopped() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.STOPPED;
	}
	
	public void endProcessing_Canceled() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.CANCELED;
	}
	
	void endProcessing_Error(Exception e) {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.ERROR;
		processing_error = new GsonThrowable(e);
	}
	
	void exportToDatabase(ColumnListMutation<String> mutator) {
		mutator.putColumn("context_class", context.getClass().getName(), TTL);
		mutator.putColumn("status", status.name(), TTL);
		mutator.putColumn("creator_hostname", instance_status_creator_hostname, TTL);
		mutator.putColumn("expiration_date", expiration_date, TTL);
		mutator.putColumn("update_date", update_date, TTL);
		mutator.putColumn("delete_after_completed", delete_after_completed, TTL);
		/**
		 * Workaround for Cassandra index select bug.
		 */
		mutator.putColumn("indexingdebug", 1, TTL);
		mutator.putColumn("source", AppManager.getGson().toJson(this), TTL);
	}
	
	void pushToDatabaseEndLifejob(ColumnListMutation<String> mutator) {
		mutator.putColumn("delete", 1, TTL);
	}
	
	public String getDatabaseKey() {
		return key;
	}
	
	static JobNG importFromDatabase(ColumnList<String> columnlist) {
		return AppManager.getGson().fromJson(columnlist.getColumnByName("source").getStringValue(), JobNG.class);
	}
	
	private static class Db {
		private static final ColumnFamily<String, String> CF_QUEUE = new ColumnFamily<String, String>("mgrQueue", StringSerializer.get(), StringSerializer.get());
		
		static {
			try {
				Keyspace keyspace = CassandraDb.getkeyspace();
				String default_keyspacename = CassandraDb.getDefaultKeyspacename();
				if (CassandraDb.isColumnFamilyExists(keyspace, CF_QUEUE.getName()) == false) {
					CassandraDb.createColumnFamilyString(default_keyspacename, CF_QUEUE.getName(), false);
					String queue_name = CF_QUEUE.getName();
					CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "status", queue_name + "_status", DeployColumnDef.ColType_AsciiType);
					CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "creator_hostname", queue_name + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
					CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "expiration_date", queue_name + "_expiration_date", DeployColumnDef.ColType_LongType);
					CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "updatedate", queue_name + "_updatedate", DeployColumnDef.ColType_LongType);
					CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "delete", queue_name + "_delete", DeployColumnDef.ColType_Int32Type);
					CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "indexingdebug", queue_name + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
				}
			} catch (Exception e) {
				Log2.log.error("Can't init database CFs", e);
			}
		}
		
		static void selectWaiting(IndexQuery<String, String> index_query, String category) {
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
		}
		
		static void selectTooOldWaitingTasks(IndexQuery<String, String> index_query, String hostname) {
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
			index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
			index_query.addExpression().whereColumn("expiration_date").lessThan().value(System.currentTimeMillis());
		}
		
		static void selectPostponedTasksWithMaxAge(IndexQuery<String, String> index_query, String hostname) {
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.POSTPONED.name());
			index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
			index_query.addExpression().whereColumn("expiration_date").lessThan().value(Long.MAX_VALUE);
		}
		
		static void selectAllLastTasksAndJobs(IndexQuery<String, String> index_query, long since_date) {
			index_query.addExpression().whereColumn("indexingdebug").equals().value(1);
			index_query.addExpression().whereColumn("updatedate").greaterThanEquals().value(since_date);
		}
		
		static void selectEndLifeJobs(IndexQuery<String, String> index_query) {
			index_query.addExpression().whereColumn("delete").equals().value(1);
			index_query.withColumnSlice("delete");
		}
	}
	
}
