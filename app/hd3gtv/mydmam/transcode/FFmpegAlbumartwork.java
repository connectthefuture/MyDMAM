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
package hd3gtv.mydmam.transcode;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.analysis.MetadataIndexerResult;
import hd3gtv.mydmam.analysis.PreviewType;
import hd3gtv.mydmam.analysis.RenderedElement;
import hd3gtv.mydmam.analysis.Renderer;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class FFmpegAlbumartwork implements Renderer {
	
	private String ffmpeg_bin;
	
	public FFmpegAlbumartwork() {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists() & TranscodeProfileManager.isEnabled();
	}
	
	public boolean canProcessThis(String mimetype) {
		return FFprobeAnalyser.canProcessThisAudioOnly(mimetype);
	}
	
	public String getLongName() {
		return "FFmpeg album artwork extracting";
	}
	
	public List<RenderedElement> process(MetadataIndexerResult analysis_result) throws Exception {
		JSONObject analysed_result = FFprobeAnalyser.getAnalysedProcessresult(analysis_result);
		if (analysed_result == null) {
			return null;
		}
		
		/**
		 * Must not have real video stream.
		 */
		if (FFprobeAnalyser.hasVideo(analysed_result)) {
			return null;
		}
		
		/**
		 * Must have fake video stream : artwork
		 */
		boolean containt_artwork = false;
		if (analysed_result.containsKey("streams") == false) {
			return null;
		}
		JSONArray streams = (JSONArray) analysed_result.get("streams");
		for (int pos = 0; pos < streams.size(); pos++) {
			JSONObject stream = (JSONObject) streams.get(pos);
			String codec_type = (String) stream.get("codec_type");
			if (codec_type.equalsIgnoreCase("video")) {
				containt_artwork = true;
				break;
			}
		}
		if (containt_artwork == false) {
			return null;
		}
		
		TranscodeProfile tprofile = TranscodeProfileManager.getProfile(new Profile("ffmpeg", "ffmpeg_album_artwork"));
		
		ArrayList<RenderedElement> result = new ArrayList<RenderedElement>();
		RenderedElement element = new RenderedElement("album_artwork", tprofile.getExtension("jpg"));
		
		ArrayList<String> param = tprofile.makeCommandline(analysis_result.getOrigin().getAbsolutePath(), element.getTempFile().getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ffmpeg_bin, param);
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				dump.add("file", analysis_result.getOrigin());
				dump.add("mime", analysis_result.getMimetype());
				if (process.getRunprocess().getExitvalue() == 1) {
					dump.add("stderr", process.getResultstderr().toString().trim());
					Log2.log.error("Invalid data found when processing input", null, dump);
				} else {
					dump.add("stdout", process.getResultstdout().toString().trim());
					dump.add("stderr", process.getResultstderr().toString().trim());
					dump.add("exitcode", process.getRunprocess().getExitvalue());
					Log2.log.error("Problem with ffmpeg", null, dump);
				}
			}
			throw e;
		}
		
		result.add(element);
		
		return result;
	}
	
	public String getElasticSearchIndexType() {
		return "ffalbumartwork";
	}
	
	public PreviewType getPreviewTypeForRenderer(LinkedHashMap<String, JSONObject> all_metadatas_for_element, List<RenderedElement> rendered_elements) {
		return PreviewType.full_size_thumbnail;
	}
	
	public JSONObject getPreviewConfigurationForRenderer(PreviewType preview_type, LinkedHashMap<String, JSONObject> all_metadatas_for_element, List<RenderedElement> rendered_elements) {
		return null;
	}
	
}
