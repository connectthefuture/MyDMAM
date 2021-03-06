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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.simple.parser.ParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.IndexQuery;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.serializers.StringSerializer;

import controllers.Secure;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.DeployColumnDef;
import hd3gtv.mydmam.gson.GsonDeSerializer;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.tools.StoppableProcessing;

/**
 * Use AppManager to for create job.
 */
public final class JobNG {
	
	private static final ColumnFamily<String, String> CF_QUEUE = new ColumnFamily<String, String>("mgrQueue", StringSerializer.get(), StringSerializer.get());
	private static Keyspace keyspace;
	private static JsonParser parser = new JsonParser();
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_QUEUE.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_QUEUE.getName(), false);
				String queue_name = CF_QUEUE.getName();
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "status", queue_name + "_status", DeployColumnDef.ColType_AsciiType);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "creator_hostname", queue_name + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "expiration_date", queue_name + "_expiration_date", DeployColumnDef.ColType_LongType);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "update_date", queue_name + "_update_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "delete", queue_name + "_delete", DeployColumnDef.ColType_Int32Type);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "indexingdebug", queue_name + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
			}
		} catch (Exception e) {
			Loggers.Manager.error("Can't init database CFs", e);
			System.exit(1);
		}
	}
	
	static ColumnPrefixDistributedRowLock<String> prepareLock() {
		return new ColumnPrefixDistributedRowLock<String>(keyspace, CF_QUEUE, "BROKER_LOCK");
	}
	
	/**
	 * 7 days
	 */
	final static int TTL_WAITING = 3600 * 24 * 7;
	final static int TTL_DELETE_AFTER_COMPLETED = 300;
	final static int TTL_PREPARING = 60;
	final static int TTL_PROCESSING = 120;
	final static int TTL_DONE = 3600 * 24;
	final static int TTL_TROUBLES = 3600 * 24 * 3;
	
	public enum JobStatus {
		TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING, TOO_LONG_DURATION;
		
		public boolean isInThisStatus(JobStatus... status) {
			if (status == null) {
				return false;
			}
			if (status.length == 0) {
				return false;
			}
			return Arrays.asList(status).stream().anyMatch(s -> {
				return s == this;
			});
		}
		
	}
	
	public enum AlterJobOrderName {
		delete, stop, setinwait, cancel, hipriority, noexpiration, postponed
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
	private Class<?> creator;
	private boolean urgent;
	private String name;
	private long expiration_date;
	private long max_execution_time;
	private static final long default_max_execution_time = 1000 * 3600 * 24;
	@SuppressWarnings("unused")
	private long create_date;
	private boolean delete_after_completed;
	@SuppressWarnings("unused")
	private String instance_status_creator_key;
	private String instance_status_creator_hostname;
	private int priority;
	
	@GsonIgnore
	private JobContext context;
	@GsonIgnore
	private List<String> required_keys;
	
	/**
	 * Activity vars
	 */
	private JobStatus status;
	long update_date;
	private GsonThrowable processing_error;
	private JobProgression progression;
	private long start_date;
	private long end_date;
	private String worker_reference;
	@SuppressWarnings("unused")
	private Class<?> worker_class;
	@SuppressWarnings("unused")
	private String instance_status_executor_key;
	@SuppressWarnings("unused")
	private String instance_status_executor_hostname;
	
	JobNG(JobContext context) throws ClassNotFoundException {
		this.context = context;
		MyDMAM.factory.checkIsAccessibleClass(context.getClass(), false);
		key = "job:" + UUID.randomUUID().toString();
		name = "Generic job created at " + (new Date()).toString();
		urgent = false;
		priority = 0;
		delete_after_completed = false;
		expiration_date = System.currentTimeMillis() + (default_max_execution_time * 10);
		max_execution_time = default_max_execution_time;
		status = JobStatus.WAITING;
		required_keys = new ArrayList<String>(1);
		
		instance_status_creator_key = InstanceStatus.getStatic().summary.getInstanceNamePid();
		instance_status_creator_hostname = InstanceStatus.getStatic().summary.getHostName();
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
	
	public JobNG setCreator(Class<?> creator) {
		this.creator = creator;
		return this;
	}
	
	public JobNG setUrgent() {
		urgent = true;
		return this;
	}
	
	public JobNG setRequiredCompletedJob(JobNG... require) {
		if (require == null) {
			return this;
		}
		if (require.length == 0) {
			return this;
		}
		required_keys = new ArrayList<String>(require.length);
		for (int pos = 0; pos < require.length; pos++) {
			if (require[pos] == null) {
				continue;
			}
			required_keys.add(require[pos].key);
		}
		
		if (required_keys.isEmpty()) {
			required_keys = null;
		}
		return this;
	}
	
	public JobNG setRequiredCompletedJob(Iterable<JobNG> require) {
		if (require == null) {
			return this;
		}
		if (require.iterator().hasNext() == false) {
			/**
			 * No first item == no items
			 */
			return this;
		}
		required_keys = new ArrayList<String>();
		for (Iterator<JobNG> iterator = require.iterator(); iterator.hasNext();) {
			JobNG job = iterator.next();
			required_keys.add(job.key);
		}
		return this;
	}
	
	/**
	 * @return can be null if no required jobs
	 */
	public List<String> getRequiredJobKeys() {
		return required_keys;
	}
	
	boolean isRequireIsDone() throws ConnectionException {
		if (required_keys == null) {
			return true;
		}
		if (required_keys.isEmpty()) {
			return true;
		}
		
		Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(required_keys).withColumnSlice("status").execute().getResult();
		if (rows == null) {
			return false;
		}
		if (rows.isEmpty()) {
			return false;
		}
		for (Row<String, String> row : rows) {
			if (row.getColumns().getStringValue("status", JobStatus.WAITING.name()).equals(JobStatus.DONE.name()) == false) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isRequireHasProblemThisSwitchStatus() throws ConnectionException {
		if (required_keys == null) {
			return false;
		}
		if (required_keys.isEmpty()) {
			return false;
		}
		
		Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(required_keys).withColumnSlice("status").execute().getResult();
		if (rows == null) {
			return true;
		}
		if (rows.isEmpty()) {
			return true;
		}
		for (Row<String, String> row : rows) {
			String status = row.getColumns().getStringValue("status", JobStatus.WAITING.name());
			
			if (status.equals(JobStatus.WAITING.name())) {
				continue;
			} else if (status.equals(JobStatus.DONE.name())) {
				continue;
			} else if (status.equals(JobStatus.PROCESSING.name())) {
				continue;
			} else if (status.equals(JobStatus.ERROR.name())) {
				this.status = JobStatus.ERROR;
				return true;
			} else if (status.equals(JobStatus.CANCELED.name())) {
				if (this.status != JobStatus.ERROR) {
					this.status = JobStatus.CANCELED;
				}
				return true;
			} else if (status.equals(JobStatus.STOPPED.name())) {
				if (this.status != JobStatus.ERROR) {
					this.status = JobStatus.CANCELED;
				}
				return true;
			} else if (status.equals(JobStatus.TOO_LONG_DURATION.name())) {
				if (this.status != JobStatus.ERROR) {
					this.status = JobStatus.CANCELED;
				}
				return true;
			} else if (status.equals(JobStatus.TOO_OLD.name())) {
				if (this.status != JobStatus.ERROR) {
					this.status = JobStatus.CANCELED;
				}
				return true;
			}
		}
		return false;
	}
	
	boolean isTooOldjob() {
		return (expiration_date < System.currentTimeMillis());
	}
	
	int getPriority() {
		return priority;
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
	
	boolean isMaxExecutionTimeIsReached() {
		if (getStatus() != JobStatus.PROCESSING) {
			return false;
		}
		if (start_date == -1) {
			return false;
		}
		return start_date + max_execution_time < System.currentTimeMillis();
		
	}
	
	boolean hasAMaxExecutionTime() {
		return max_execution_time < default_max_execution_time;
	}
	
	/**
	 * Doesn't change current status if POSTPONED, PROCESSING or PREPARING.
	 */
	public JobNG setPostponed() {
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
	
	boolean isDeleteAfterCompleted() {
		return delete_after_completed;
	}
	
	public JobContext getContext() {
		return context;
	}
	
	/**
	 * @return this
	 */
	public JobNG publish() throws ConnectionException {
		if (Loggers.Job.isInfoEnabled()) {
			Loggers.Job.info("Publish new job:\t" + toStringLight());
		}
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		publish(mutator);
		mutator.execute();
		return this;
	}
	
	/**
	 * Compute priority based on active wait job count.
	 */
	void setMaxPriority() throws ConnectionException {
		IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
		index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
		index_query.withColumnSlice("status", "name");
		OperationResult<Rows<String, String>> rows = index_query.execute();
		priority = rows.getResult().size() + 1;
	}
	
	/**
	 * @return this
	 */
	public JobNG publish(MutationBatch mutator) throws ConnectionException {
		create_date = System.currentTimeMillis();
		if (urgent) {
			setMaxPriority();
		}
		saveChanges(mutator);
		if (Loggers.Job.isDebugEnabled()) {
			Loggers.Job.debug("Prepare publish:\t" + toStringLight());
		}
		return this;
	}
	
	/**
	 * To force changes (sync method) with set() functions.
	 * If job is actually POSTPONED, PROCESSING or PREPARING, changes will be canceled by the executor worker.
	 * @throws ConnectionException
	 */
	void saveChanges() throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		saveChanges(mutator);
		mutator.execute();
	}
	
	/**
	 * To force changes (sync method) with set() functions.
	 * If job is actually POSTPONED, PROCESSING or PREPARING, changes will be canceled by the executor worker.
	 * @throws ConnectionException
	 */
	public void saveChanges(MutationBatch mutator) {
		if (Loggers.Job.isTraceEnabled()) {
			Loggers.Job.trace("Prepare save actual changes:\t" + toString());
		} else if (Loggers.Job.isDebugEnabled()) {
			Loggers.Job.debug("Prepare save actual changes:\t" + toStringLight());
		}
		
		update_date = System.currentTimeMillis();
		exportToDatabase(mutator.withRow(CF_QUEUE, key));
	}
	
	public static class Serializer implements GsonDeSerializer<JobNG> {
		public JobNG deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext jcontext) throws JsonParseException {
			JsonObject json = (JsonObject) jejson;
			JobNG job = MyDMAM.gson_kit.getGsonSimple().fromJson(json, JobNG.class);
			if (json.has("required_keys")) {
				job.required_keys = MyDMAM.gson_kit.getGsonSimple().fromJson(json.get("required_keys"), GsonKit.type_ArrayList_String);
			} else {
				job.required_keys = new ArrayList<String>(1);
			}
			job.context = MyDMAM.gson_kit.getGson().fromJson(json.get("context"), JobContext.class);
			job.processing_error = MyDMAM.gson_kit.getGson().fromJson(json.get("processing_error"), GsonThrowable.class);
			return job;
		}
		
		public JsonElement serialize(JobNG src, Type typeOfSrc, JsonSerializationContext jcontext) {
			JsonObject result = (JsonObject) MyDMAM.gson_kit.getGsonSimple().toJsonTree(src);
			result.add("required_keys", MyDMAM.gson_kit.getGsonSimple().toJsonTree(src.required_keys));
			result.add("context", MyDMAM.gson_kit.getGson().toJsonTree(src.context, JobContext.class));
			result.add("processing_error", MyDMAM.gson_kit.getGsonSimple().toJsonTree(src.processing_error));
			return result;
		}
	}
	
	public JsonObject toJson() {
		return MyDMAM.gson_kit.getGson().toJsonTree(this).getAsJsonObject();
	}
	
	/**
	 * @param mutator update new status and new update_date to DB, to protect new next job search to found it.
	 */
	void prepareProcessing(MutationBatch mutator) {
		update_date = System.currentTimeMillis();
		status = JobStatus.PREPARING;
		mutator.withRow(CF_QUEUE, key).putColumn("status", status.name(), TTL_PREPARING);
		mutator.withRow(CF_QUEUE, key).putColumn("update_date", update_date, TTL_PREPARING);
	}
	
	JobProgression startProcessing(AppManager manager, WorkerNG worker) {
		update_date = System.currentTimeMillis();
		start_date = update_date;
		status = JobStatus.PROCESSING;
		worker_class = worker.getClass();
		worker_reference = worker.getReferenceKey();
		instance_status_executor_key = manager.getInstanceStatus().summary.getInstanceNamePid();
		instance_status_executor_hostname = manager.getInstanceStatus().summary.getHostName();
		progression = new JobProgression(this);
		if (Loggers.Job.isDebugEnabled()) {
			Loggers.Job.debug("Start processing:\t" + toString());
		}
		return progression;
	}
	
	synchronized void endProcessing_Done() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.DONE;
	}
	
	synchronized void endProcessing_Stopped() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.STOPPED;
	}
	
	synchronized void endProcessing_TooLongDuration() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.TOO_LONG_DURATION;
	}
	
	synchronized void endProcessing_Canceled() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.CANCELED;
	}
	
	synchronized void endProcessing_Error(Exception e) {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.ERROR;
		processing_error = new GsonThrowable(e);
	}
	
	public JobStatus getStatus() {
		return status;
	}
	
	long getEndDate() {
		return end_date;
	}
	
	public void delete(MutationBatch mutator) throws ConnectionException {
		if (Loggers.Job.isDebugEnabled()) {
			Loggers.Job.debug("Prepare delete job:\t" + toStringLight());
		}
		mutator.withRow(CF_QUEUE, key).delete();
	}
	
	boolean isThisStatus(JobStatus... statuses) {
		if (statuses == null) {
			return false;
		}
		if (statuses.length == 0) {
			return false;
		}
		for (int pos = 0; pos < statuses.length; pos++) {
			if (status == statuses[pos]) {
				return true;
			}
		}
		return false;
	}
	
	static boolean isThisStatus(String status_name, JobStatus... statuses) {
		if (statuses == null) {
			return false;
		}
		if (statuses.length == 0) {
			return false;
		}
		for (int pos = 0; pos < statuses.length; pos++) {
			if (status_name.equalsIgnoreCase(statuses[pos].toString())) {
				return true;
			}
		}
		return false;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getName() {
		return name;
	}
	
	private void exportToDatabase(ColumnListMutation<String> mutator) {
		/**
		 * POSTPONED | WAITING
		 */
		int ttl = TTL_WAITING;
		
		if (delete_after_completed && isThisStatus(JobStatus.DONE, JobStatus.CANCELED)) {
			/**
			 * Short ttl if Broker is closed before it can delete this.
			 */
			ttl = TTL_DELETE_AFTER_COMPLETED;
		} else if (isThisStatus(JobStatus.TOO_OLD, JobStatus.CANCELED, JobStatus.STOPPED, JobStatus.ERROR, JobStatus.TOO_LONG_DURATION)) {
			ttl = TTL_TROUBLES;
		} else if (isThisStatus(JobStatus.PREPARING)) {
			ttl = TTL_PREPARING;
		} else if (isThisStatus(JobStatus.DONE)) {
			ttl = TTL_DONE;
		} else if (isThisStatus(JobStatus.PROCESSING)) {
			ttl = TTL_PROCESSING;
		}
		
		mutator.putColumn("context_class", context.getClass().getName(), ttl);
		mutator.putColumn("status", status.name(), ttl);
		mutator.putColumn("creator_hostname", instance_status_creator_hostname, ttl);
		mutator.putColumn("expiration_date", expiration_date, ttl);
		mutator.putColumn("update_date", update_date, ttl);
		mutator.putColumn("delete_after_completed", delete_after_completed, ttl);
		/**
		 * Workaround for Cassandra index select bug.
		 */
		mutator.putColumn("indexingdebug", 1, ttl);
		mutator.putColumn("source", MyDMAM.gson_kit.getGson().toJson(this), ttl);
		
		if (Loggers.Job.isTraceEnabled()) {
			Loggers.Job.trace("Prepare export to db job:\t" + toString() + " with ttl " + ttl);
		}
		
	}
	
	private static void exportToDatabase(ColumnListMutation<String> mutator, JsonObject json_job) {
		boolean delete_after_completed = json_job.get("delete_after_completed").getAsBoolean();
		String status = json_job.get("status").getAsString();
		
		/**
		 * POSTPONED | WAITING
		 */
		int ttl = TTL_WAITING;
		
		if (delete_after_completed && isThisStatus(status, JobStatus.DONE, JobStatus.CANCELED)) {
			/**
			 * Short ttl if Broker is closed before it can delete this.
			 */
			ttl = TTL_DELETE_AFTER_COMPLETED;
		} else if (isThisStatus(status, JobStatus.TOO_OLD, JobStatus.CANCELED, JobStatus.STOPPED, JobStatus.ERROR, JobStatus.TOO_LONG_DURATION)) {
			ttl = TTL_TROUBLES;
		} else if (isThisStatus(status, JobStatus.PREPARING)) {
			ttl = TTL_PREPARING;
		} else if (isThisStatus(status, JobStatus.DONE)) {
			ttl = TTL_DONE;
		} else if (isThisStatus(status, JobStatus.PROCESSING)) {
			ttl = TTL_PROCESSING;
		}
		
		mutator.putColumn("context_class", json_job.get("context").getAsJsonObject().get("classname").getAsString(), ttl);
		mutator.putColumn("status", status, ttl);
		mutator.putColumn("creator_hostname", json_job.get("instance_status_creator_hostname").getAsString(), ttl);
		mutator.putColumn("expiration_date", json_job.get("expiration_date").getAsLong(), ttl);
		mutator.putColumn("update_date", json_job.get("update_date").getAsLong(), ttl);
		mutator.putColumn("delete_after_completed", delete_after_completed, ttl);
		/**
		 * Workaround for Cassandra index select bug.
		 */
		mutator.putColumn("indexingdebug", 1, ttl);
		mutator.putColumn("source", json_job.toString(), ttl);
		
		if (Loggers.Job.isDebugEnabled()) {
			Loggers.Job.debug("Prepare export to db job (for json):\t" + json_job.toString() + " with ttl " + ttl);
		}
	}
	
	public static final class Utility {
		
		/**
		 * Check before if you can instanciate the JobContext class.
		 */
		static JobNG importFromDatabase(ColumnList<String> columnlist) {
			return MyDMAM.gson_kit.getGson().fromJson(columnlist.getColumnByName("source").getStringValue(), JobNG.class);
		}
		
		static List<JobNG> watchOldAbandonedJobs(MutationBatch mutator, InstanceStatus instance_status) throws ConnectionException {
			if (Loggers.Job.isDebugEnabled()) {
				Loggers.Job.debug("Search old abandoned jobs");
			}
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
			index_query.addExpression().whereColumn("creator_hostname").equals().value(instance_status.summary.getHostName());
			index_query.addExpression().whereColumn("expiration_date").lessThan().value(System.currentTimeMillis());
			index_query.withColumnSlice("source", "context_class");
			
			List<JobNG> result = new ArrayList<JobNG>();
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			for (Row<String, String> row : rows.getResult()) {
				if (MyDMAM.factory.isClassExists(row.getColumns().getStringValue("context_class", "null")) == false) {
					continue;
				}
				JobNG job = JobNG.Utility.importFromDatabase(row.getColumns());
				job.status = JobStatus.TOO_OLD;
				job.update_date = System.currentTimeMillis();
				job.exportToDatabase(mutator.withRow(CF_QUEUE, job.key));
				result.add(job);
			}
			if (Loggers.Job.isInfoEnabled() & result.isEmpty() == false) {
				Loggers.Job.info("Found old abandoned jobs:\t" + result);
			}
			return result;
		}
		
		/**
		 * If a job depends to an error job > set to error
		 */
		static void searchRequiredAndErrorJobsAndSetError(MutationBatch mutator) throws ConnectionException {
			if (Loggers.Job.isDebugEnabled()) {
				Loggers.Job.debug("Search required and error jobs and set error");
			}
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
			// index_query.addExpression().whereColumn("indexingdebug").equals().value(1);
			index_query.withColumnSlice("source");
			
			ArrayList<JobNG> deleted_jobs = new ArrayList<>(1);
			
			JobNG job;
			OperationResult<Rows<String, String>> rows = index_query.execute();
			for (Row<String, String> row : rows.getResult()) {
				if (MyDMAM.factory.isClassExists(row.getColumns().getStringValue("context_class", "null")) == false) {
					continue;
				}
				job = JobNG.Utility.importFromDatabase(row.getColumns());
				
				if (job.isRequireHasProblemThisSwitchStatus()) {
					job.update_date = System.currentTimeMillis();
					job.exportToDatabase(mutator.withRow(CF_QUEUE, job.key));
					deleted_jobs.add(job);
				}
			}
			
			if (deleted_jobs.isEmpty() == false) {
				Loggers.Job.info("This job requires the success of other jobs. Some of these jobs are in error, so it also puts these job in error: " + deleted_jobs);
			}
		}
		
		static void removeMaxDateForPostponedJobs(MutationBatch mutator, String creator_hostname) throws ConnectionException {
			if (Loggers.Job.isDebugEnabled()) {
				Loggers.Job.debug("Search for remove max date for postponed jobs");
			}
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.POSTPONED.name());
			index_query.addExpression().whereColumn("creator_hostname").equals().value(creator_hostname);
			index_query.addExpression().whereColumn("expiration_date").lessThan().value(System.currentTimeMillis() + default_max_execution_time);
			index_query.withColumnSlice("source", "context_class");
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			for (Row<String, String> row : rows.getResult()) {
				if (MyDMAM.factory.isClassExists(row.getColumns().getStringValue("context_class", "null")) == false) {
					continue;
				}
				JobNG job = JobNG.Utility.importFromDatabase(row.getColumns());
				job.expiration_date = System.currentTimeMillis() + (default_max_execution_time * 7);
				job.update_date = System.currentTimeMillis();
				if (Loggers.Job.isDebugEnabled()) {
					Loggers.Job.info("Remove max date for this postponed job:\t" + job);
				}
				job.exportToDatabase(mutator.withRow(CF_QUEUE, job.key));
			}
		}
		
		public static void truncateAllJobs() throws ConnectionException {
			Loggers.Job.info("Truncate all jobs from DB");
			CassandraDb.truncateColumnFamilyString(keyspace, CF_QUEUE.getName());
		}
		
		public static void dropJobsQueueCF() throws ConnectionException {
			Loggers.Job.info("Drop CF " + CF_QUEUE.getName() + " from DB");
			CassandraDb.dropColumnFamilyString(keyspace, CF_QUEUE.getName());
		}
		
		/**
		 * @return never null if keys is not empty
		 */
		public static LinkedHashMap<String, JobStatus> getJobsStatusByKeys(Collection<String> keys) throws ConnectionException {
			if (keys == null) {
				return null;
			}
			if (keys.size() == 0) {
				return null;
			}
			LinkedHashMap<String, JobStatus> status = new LinkedHashMap<String, JobStatus>(keys.size());
			Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(keys).withColumnSlice("status").execute().getResult();
			if (rows.isEmpty()) {
				return status;
			}
			for (Row<String, String> row : rows) {
				if (row.getColumns().isEmpty()) {
					continue;
				}
				status.put(row.getKey(), JobStatus.valueOf(row.getColumns().getStringValue("status", JobStatus.POSTPONED.name())));
			}
			return status;
		}
		
		/**
		 * @return never null if keys is not empty
		 */
		public static LinkedHashMap<String, JobStatus> getJobsStatus(Collection<JobNG> jobs) throws ConnectionException {
			if (jobs == null) {
				return null;
			}
			if (jobs.size() == 0) {
				return null;
			}
			ArrayList<String> keys = new ArrayList<String>();
			for (JobNG job : jobs) {
				keys.add(job.key);
			}
			return getJobsStatusByKeys(keys);
		}
		
		/**
		 * Storted by priority
		 */
		static List<JobNG> getJobsByStatus(JobStatus status) throws ConnectionException {
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(status.name());
			index_query.withColumnSlice("source", "context_class");
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			ArrayList<JobNG> result = new ArrayList<JobNG>();
			for (Row<String, String> row : rows.getResult()) {
				if (MyDMAM.factory.isClassExists(row.getColumns().getStringValue("context_class", "null")) == false) {
					continue;
				}
				JobNG new_job = JobNG.Utility.importFromDatabase(row.getColumns());
				result.add(new_job);
			}
			
			Collections.sort(result, job_priority_comparator);
			
			return result;
		}
		
		private static JobPriorityComparator job_priority_comparator = new JobPriorityComparator();
		
		private static class JobPriorityComparator implements Comparator<JobNG> {
			public int compare(JobNG o1, JobNG o2) {
				if (o1.priority == o2.priority) {
					return 0;
				} else if (o1.priority < o2.priority) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		
		/**
		 * @return deleted raw json jobs
		 */
		public static List<JsonObject> deleteJobsByStatus(JobStatus status) throws ConnectionException {
			Loggers.Job.info("Remove all jobs by status (" + status + ") from DB");
			ArrayList<JsonObject> result = new ArrayList<JsonObject>();
			
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(status.name());
			index_query.withColumnSlice("source");
			index_query.setRowLimit(1000);
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			if (rows.getResult().isEmpty()) {
				return result;
			}
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			for (Row<String, String> row : rows.getResult()) {
				result.add(parser.parse(row.getColumns().getStringValue("source", "{}")).getAsJsonObject());
				if (Loggers.Job.isInfoEnabled()) {
					Loggers.Job.info("Remove job by status (" + status + "): [" + row.getKey() + "]");
				}
				mutator.withRow(CF_QUEUE, row.getKey()).delete();
			}
			mutator.execute();
			
			return result;
		}
		
		public static JsonObject alterJobsByStatus(JobStatus status, AlterJobOrderName order) throws ConnectionException {
			JsonObject result = new JsonObject();
			
			int count = 1;
			while (count > 0) {
				Loggers.Manager.debug("alterJobsByStatus loop IndexQuery/MutationBatch (" + count + ")");
				
				MutationBatch mutator = CassandraDb.prepareMutationBatch();
				IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
				index_query.addExpression().whereColumn("status").equals().value(status.name());
				index_query.withColumnSlice("source");
				
				OperationResult<Rows<String, String>> rows = index_query.execute();
				count = 0;
				for (Row<String, String> row : rows.getResult()) {
					count++;
					alterJobsFromJson(row.getKey(), row.getColumns(), mutator, order, result);
				}
				mutator.execute();
			}
			
			return result;
		}
		
		public static JsonObject alterJobByKey(String job_key, AlterJobOrderName order) throws ConnectionException {
			JsonObject result = new JsonObject();
			ColumnList<String> cols = keyspace.prepareQuery(CF_QUEUE).getRow(job_key).withColumnSlice("source").execute().getResult();
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			alterJobsFromJson(job_key, cols, mutator, order, result);
			mutator.execute();
			return result;
		}
		
		/**
		 * @param job_keys can be null or empty
		 */
		public static void removeJobsByKeys(Collection<String> job_keys) throws ConnectionException {
			if (job_keys == null) {
				return;
			}
			if (job_keys.isEmpty() == false) {
				return;
			}
			
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			job_keys.forEach(job_key -> {
				mutator.withRow(CF_QUEUE, job_key).delete();
			});
			mutator.execute();
			return;
		}
		
		private static void alterJobsFromJson(String job_key, ColumnList<String> cols, MutationBatch mutator, AlterJobOrderName order, JsonObject result) throws ConnectionException {
			String source = cols.getStringValue("source", "{}");
			if (source.equals("{}")) {
				return;
			}
			JsonObject current = parser.parse(source).getAsJsonObject();
			
			switch (order) {
			case delete:
				mutator.withRow(CF_QUEUE, job_key).delete();
				result.add(current.get("key").getAsString(), JsonNull.INSTANCE);
				return;
			case stop:
				String worker_ref = current.get("worker_reference").getAsString();
				WorkerExporter worker_exporter = WorkerExporter.getWorkerStatusByKey(worker_ref);
				if (worker_exporter == null) {
					current.addProperty("status", JobStatus.STOPPED.toString());
					current.addProperty("update_date", System.currentTimeMillis());
				} else if (worker_exporter.getCurrent_job_key() == null) {
					current.addProperty("status", JobStatus.STOPPED.toString());
					current.addProperty("update_date", System.currentTimeMillis());
				} else if (worker_exporter.getCurrent_job_key().equals(job_key)) {
					JsonObject json_order = new JsonObject();
					json_order.addProperty("state", "stop");
					InstanceAction.addNew("WorkerNG", worker_ref, json_order, Secure.getRequestAddress());
				} else {
					current.addProperty("status", JobStatus.STOPPED.toString());
					current.addProperty("update_date", System.currentTimeMillis());
				}
				break;
			case setinwait:
				current.addProperty("status", JobStatus.WAITING.toString());
				current.addProperty("update_date", System.currentTimeMillis());
				current.add("progression", JsonNull.INSTANCE);
				current.add("processing_error", JsonNull.INSTANCE);
				current.addProperty("start_date", -1);
				current.addProperty("end_date", -1);
				break;
			case cancel:
				current.addProperty("update_date", System.currentTimeMillis());
				current.addProperty("status", JobStatus.CANCELED.toString());
				break;
			case postponed:
				current.addProperty("status", JobStatus.POSTPONED.toString());
				current.addProperty("urgent", false);
				current.addProperty("priority", 0);
				current.addProperty("update_date", System.currentTimeMillis());
				current.add("progression", JsonNull.INSTANCE);
				current.add("processing_error", JsonNull.INSTANCE);
				current.addProperty("start_date", -1);
				current.addProperty("end_date", -1);
				break;
			case hipriority:
				IndexQuery<String, String> index_query_hip = null;
				OperationResult<Rows<String, String>> rows_hip = null;
				index_query_hip = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
				index_query_hip.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
				index_query_hip.withColumnSlice("status", "name");
				rows_hip = index_query_hip.execute();
				
				current.addProperty("priority", rows_hip.getResult().size() + 1);
				current.addProperty("urgent", true);
				current.addProperty("update_date", System.currentTimeMillis());
				break;
			case noexpiration:
				current.addProperty("delete_after_completed", false);
				current.addProperty("expiration_date", System.currentTimeMillis() + (default_max_execution_time * 10));
				current.addProperty("max_execution_time", default_max_execution_time);
				current.addProperty("update_date", System.currentTimeMillis());
				break;
			}
			
			result.add(current.get("key").getAsString(), current);
			exportToDatabase(mutator.withRow(CF_QUEUE, job_key), current);
		}
		
		/**
		 * @return never null if keys is not empty
		 */
		public static JsonObject getJsonJobsByKeys(Collection<String> keys, boolean is_public_access) throws ConnectionException, ParseException {
			if (keys == null) {
				return null;
			}
			if (keys.size() == 0) {
				return null;
			}
			JsonObject result = new JsonObject();
			
			Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(keys).withColumnSlice("source").execute().getResult();
			for (Row<String, String> row : rows) {
				String source = row.getColumns().getStringValue("source", "{}");
				if (source.equals("{}")) {
					continue;
				}
				JsonObject current = parser.parse(source).getAsJsonObject();
				if (is_public_access) {
					current.remove("context");
					current.remove("worker_class");
					current.remove("worker_reference");
					current.remove("processing_error");
					current.remove("instance_status_creator_hostname");
					current.remove("instance_status_creator_key");
					current.remove("instance_status_executor_hostname");
					current.remove("instance_status_executor_key");
					if (current.has("progression")) {
						current.get("progression").getAsJsonObject().remove("last_caller");
					}
				}
				result.add(current.get("key").getAsString(), current);
			}
			
			return result;
		}
		
		/**
		 * @return never null if keys is not empty
		 */
		public static List<JobNG> getJobsListByKeys(Collection<String> keys) throws ConnectionException {
			if (keys == null) {
				return null;
			}
			if (keys.size() == 0) {
				return null;
			}
			List<JobNG> result = new ArrayList<JobNG>(keys.size());
			Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(keys).withColumnSlice("source", "context_class").execute().getResult();
			for (Row<String, String> row : rows) {
				String source = row.getColumns().getStringValue("source", "{}");
				if (source.equals("{}")) {
					continue;
				}
				if (MyDMAM.factory.isClassExists(row.getColumns().getStringValue("context_class", "null")) == false) {
					continue;
				}
				result.add(MyDMAM.gson_kit.getGson().fromJson(source, JobNG.class));
			}
			return result;
		}
		
		/**
		 * @return never null if keys is not empty
		 * @see getJobsListByKeys
		 */
		public static Map<String, JobNG> getJobsMapByKeys(Collection<String> keys) throws ConnectionException {
			if (keys == null) {
				return null;
			}
			if (keys.size() == 0) {
				return null;
			}
			HashMap<String, JobNG> result = new HashMap<String, JobNG>(keys.size());
			Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(keys).withColumnSlice("source", "context_class").execute().getResult();
			for (Row<String, String> row : rows) {
				String source = row.getColumns().getStringValue("source", "{}");
				if (source.equals("{}")) {
					continue;
				}
				if (MyDMAM.factory.isClassExists(row.getColumns().getStringValue("context_class", "null")) == false) {
					continue;
				}
				result.put(row.getKey(), MyDMAM.gson_kit.getGson().fromJson(source, JobNG.class));
			}
			return result;
		}
		
		/**
		 * @param since_date set to 0 for disabled range selection
		 */
		public static JsonObject getJobsFromUpdateDate(long since_date) throws Exception {
			final JsonObject result = new JsonObject();
			if (since_date == 0) {
				CassandraDb.allRowsReader(CF_QUEUE, new AllRowsFoundRow() {
					public void onFoundRow(Row<String, String> row) throws Exception {
						String source = row.getColumns().getStringValue("source", "{}");
						if (source.equals("{}")) {
							return;
						}
						JsonObject current = parser.parse(source).getAsJsonObject();
						result.add(current.get("key").getAsString(), current);
					}
				}, "source");
			} else {
				IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
				index_query.addExpression().whereColumn("indexingdebug").equals().value(1);
				index_query.addExpression().whereColumn("update_date").greaterThanEquals().value(since_date - 1000);
				index_query.withColumnSlice("source");
				OperationResult<Rows<String, String>> rows = index_query.execute();
				
				for (Row<String, String> row : rows.getResult()) {
					String source = row.getColumns().getStringValue("source", "{}");
					if (source.equals("{}")) {
						continue;
					}
					JsonObject current = parser.parse(source).getAsJsonObject();
					result.add(current.get("key").getAsString(), current);
				}
			}
			return result;
		}
		
		/**
		 * Blocking operation.
		 * @param job_keys will be empty at the end.
		 * @return null when all jobs are CANCELED | POSTPONED | DONE | STOPPED
		 * @throws Exception if some jobs are in trouble (TOO_OLD | ERROR | TOO_LONG_DURATION)
		 */
		public static void waitAllJobsProcessing(List<String> job_keys, StoppableProcessing stoppable) throws Exception {
			List<String> jobs_keys_in_errors = new ArrayList<String>();
			HashMap<String, JobNG.JobStatus> current_state = null;
			
			while ((job_keys.isEmpty() == false) & (stoppable.isWantToStopCurrentProcessing() == false)) {
				if (job_keys.size() > 10) {
					current_state = getJobsStatusByKeys(job_keys.subList(0, 10));
				} else {
					current_state = getJobsStatusByKeys(job_keys);
				}
				
				for (Map.Entry<String, JobNG.JobStatus> entry : current_state.entrySet()) {
					switch (entry.getValue()) {
					case CANCELED:
					case POSTPONED:
					case DONE:
					case STOPPED:
						job_keys.remove(entry.getKey());
						break;
					case TOO_OLD:
					case ERROR:
					case TOO_LONG_DURATION:
						jobs_keys_in_errors.add(entry.getKey());
						job_keys.remove(entry.getKey());
						break;
					default:
						break;
					}
				}
				
				for (int pos = 0; pos < 300; pos++) {
					if (stoppable.isWantToStopCurrentProcessing()) {
						break;
					}
					Thread.sleep(10);
				}
			}
			
			if (jobs_keys_in_errors.isEmpty() == false) {
				Exception e = new Exception("Trouble with some processed jobs");
				List<JobNG> error_jobs = getJobsListByKeys(jobs_keys_in_errors);
				for (int pos = 0; pos < error_jobs.size(); pos++) {
					Loggers.Job.error("Trouble with processed job (" + (pos + 1) + "/" + error_jobs.size() + "):\t" + error_jobs.get(pos));
				}
				throw e;
			}
		}
		
	}
	
	public String toString() {
		return MyDMAM.gson_kit.getGsonPretty().toJson(this);
	}
	
	public String toStringLight() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("name", getName());
		if (getContext() != null) {
			log.put("context", getContext().getClass().getSimpleName());
			if (getContext().hookednames != null) {
				if (getContext().hookednames.isEmpty() == false) {
					log.put("hookednames", getContext().hookednames);
				}
			}
			if (getContext().neededstorages != null) {
				if (getContext().neededstorages.isEmpty() == false) {
					log.put("neededstorages", getContext().neededstorages);
				}
			}
		}
		log.put("key", getKey());
		return log.toString();
	}
	
	public String getWorker_reference() {
		return worker_reference;
	}
	
}
