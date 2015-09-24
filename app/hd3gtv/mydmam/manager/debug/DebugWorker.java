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
package hd3gtv.mydmam.manager.debug;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2Event;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;

public class DebugWorker extends WorkerNG {
	
	private static int sleep_time = 1;
	private static int nb_tasks_by_core = 4;
	private static File datalog;
	private static String instance_name;
	
	public static void declareWorkers(AppManager manager) throws Exception {
		if (Configuration.global.isElementKeyExists("service", "debug_multiple_workers") == false) {
			return;
		}
		if (Configuration.global.getValueBoolean("service", "debug_multiple_workers") == false) {
			return;
		}
		
		int cores = Runtime.getRuntime().availableProcessors();
		for (int pos = 0; pos < cores; pos++) {
			manager.workerRegister(new DebugWorker());
		}
		
		datalog = new File("debug_worker.log");
		instance_name = manager.getInstance_status().getInstanceNamePid();
		
		FileUtils.writeStringToFile(datalog, Log2Event.dateLog(System.currentTimeMillis()) + "\tinit\t" + instance_name + "\t\n", true);
		
		String job_key;
		for (int pos = 0; pos < cores; pos++) {
			for (int pos_c = 0; pos_c < nb_tasks_by_core; pos_c++) {
				job_key = AppManager.createJob(new JobContextDebug()).setCreator(DebugWorker.class).setDeleteAfterCompleted().setName("Debug").publish().getKey();
				FileUtils.writeStringToFile(datalog, Log2Event.dateLog(System.currentTimeMillis()) + "\tcreate\t" + instance_name + "\t" + job_key + "\n", true);
			}
		}
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Debug Worker";
	}
	
	public String getWorkerVendorName() {
		return "Internal MyDMAM";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return WorkerCapablities.createList(JobContextDebug.class);
	}
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		FileUtils.writeStringToFile(datalog, Log2Event.dateLog(System.currentTimeMillis()) + "\texec\t" + instance_name + "\t" + progression.getJobKey() + "\n", true);
		
		for (int pos = 0; pos < sleep_time * 10; pos++) {
			progression.updateProgress((pos + 1) * 10, sleep_time);
			Thread.sleep(100);
		}
	}
	
	protected void forceStopProcess() throws Exception {
	}
	
	protected boolean isActivated() {
		if (Configuration.global.isElementKeyExists("service", "debug_multiple_workers") == false) {
			return false;
		}
		if (Configuration.global.getValueBoolean("service", "debug_multiple_workers") == false) {
			return false;
		}
		return true;
	}
	
}