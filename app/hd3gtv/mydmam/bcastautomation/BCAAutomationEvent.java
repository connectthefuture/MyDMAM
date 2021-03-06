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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.Timecode;

public abstract class BCAAutomationEvent {
	
	public abstract long getStartDate();
	
	public abstract String getName();
	
	public abstract String getAutomationId();
	
	public abstract String getFileId();
	
	public abstract boolean isRecording();
	
	public abstract String getVideoSource();
	
	public abstract Timecode getDuration();
	
	public abstract boolean isAutomationPaused();
	
	public abstract Timecode getSOM();
	
	public abstract String getComment();
	
	public abstract String getChannel();
	
	public abstract JsonObject getOtherProperties(HashMap<String, ConfigurationItem> import_other_properties_configuration);
	
	public abstract String getMaterialType();
	
	public abstract String getAutomationType();
	
	final long getLongDuration() {
		Timecode duration = getDuration();
		return (long) (duration.getValue() * 1000f);
	}
	
	final long getEndDate() {
		return getStartDate() + getLongDuration();
	}
	
	/**
	 * Never check if is aired or not
	 */
	final boolean isAsrun() {
		return getEndDate() < System.currentTimeMillis();
	}
	
	/**
	 * Never check if is aired or not
	 */
	final boolean isPlaylist() {
		return getStartDate() > System.currentTimeMillis();
	}
	
	/**
	 * Never check if is aired or not
	 */
	final boolean isOnair() {
		return isAsrun() == false && isPlaylist() == false;
	}
	
	/**
	 * @param import_other_properties_configuration can to be null
	 */
	public final JsonObject serialize(HashMap<String, ConfigurationItem> import_other_properties_configuration) {
		JsonObject jo = new JsonObject();
		long start_date = getStartDate();
		
		jo.addProperty("startdate", start_date);
		jo.addProperty("name", getName());
		jo.addProperty("automation_id", getAutomationId());
		jo.addProperty("file_id", getFileId());
		jo.addProperty("recording", isRecording());
		jo.addProperty("video_source", getVideoSource());
		jo.addProperty("duration", getDuration().toString());
		jo.addProperty("enddate", start_date + getLongDuration());
		jo.addProperty("automation_paused", isAutomationPaused());
		jo.addProperty("som", getSOM().toString());
		jo.addProperty("comment", getComment());
		jo.addProperty("channel", getChannel());
		if (import_other_properties_configuration != null) {
			jo.add("other", getOtherProperties(import_other_properties_configuration));
		}
		jo.addProperty("material_type", getMaterialType());
		jo.addProperty("automation_type", getAutomationType());
		return jo;
	}
	
	private transient String computed_key;
	
	final String getPreviouslyComputedKey() {
		return computed_key;
	}
	
	/**
	 * Compute event unique key
	 */
	final String computeKey(HashMap<String, ConfigurationItem> import_other_properties_configuration) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Can't load MD5", e);
		}
		
		md.reset();
		if (isRecording()) {
			md.update(Longs.toByteArray(1l));
		} else {
			md.update(Longs.toByteArray(0l));
		}
		if (isAutomationPaused()) {
			md.update(Longs.toByteArray(1l));
		} else {
			md.update(Longs.toByteArray(0l));
		}
		md.update(getAutomationId().getBytes());
		md.update(getChannel().getBytes());
		md.update(getSOM().toString().getBytes());
		md.update(getDuration().toString().getBytes());
		md.update(Longs.toByteArray(getStartDate()));
		md.update(getVideoSource().getBytes());
		md.update(getMaterialType().getBytes());
		md.update(getFileId().getBytes());
		md.update(getName().getBytes());
		md.update(getComment().getBytes());
		if (import_other_properties_configuration != null) {
			md.update(getOtherProperties(import_other_properties_configuration).toString().getBytes());
		}
		computed_key = MyDMAM.byteToString(md.digest());
		return computed_key;
	}
	
}
