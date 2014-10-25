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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.pathindexing;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;

import java.io.IOException;
import java.util.ArrayList;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.json.simple.parser.ParseException;

public abstract class Importer {
	
	public static final String ES_INDEX = "pathindex";
	public static final String ES_TYPE_FILE = "file";
	public static final String ES_TYPE_DIRECTORY = "directory";
	
	protected Client client;
	private int window_update_size;
	
	static {
		try {
			Elasticsearch.enableTTL(ES_INDEX, ES_TYPE_FILE);
			Elasticsearch.enableTTL(ES_INDEX, ES_TYPE_DIRECTORY);
		} catch (Exception e) {
			Log2.log.error("Can't to set TTL for ES", e);
		}
	}
	
	public Importer() throws IOException, ParseException {
		client = Elasticsearch.getClient();
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		window_update_size = 10000;
	}
	
	/**
	 * Don"t forget to add root path (Storage).
	 * @return number of imported elements
	 */
	protected abstract long doIndex(IndexingEvent elementpush) throws Exception;
	
	protected abstract String getName();
	
	/**
	 * @return in seconds, set 0 to disable.
	 */
	protected abstract long getTTL();
	
	private class ElasticSearchPushElement implements IndexingEvent {
		
		public void onRemoveFile(String storagename, String path) throws Exception {
		}
		
		BulkRequestBuilder bulkrequest_index;
		long ttl;
		ArrayList<SourcePathIndexerElement> l_elements_problems;
		
		public ElasticSearchPushElement() {
			bulkrequest_index = client.prepareBulk();
			ttl = getTTL();
			l_elements_problems = new ArrayList<SourcePathIndexerElement>();
		}
		
		private boolean searchForbiddenChars(String filename) {
			if (filename.indexOf("/") > -1) {
				return true;
			}
			if (filename.indexOf("\\") > -1) {
				return true;
			}
			if (filename.indexOf(":") > -1) {
				return true;
			}
			if (filename.indexOf("*") > -1) {
				return true;
			}
			if (filename.indexOf("?") > -1) {
				return true;
			}
			if (filename.indexOf("\"") > -1) {
				return true;
			}
			if (filename.indexOf("<") > -1) {
				return true;
			}
			if (filename.indexOf(">") > -1) {
				return true;
			}
			if (filename.indexOf("|") > -1) {
				return true;
			}
			return false;
		}
		
		public boolean onFoundElement(SourcePathIndexerElement element) {
			if (bulkrequest_index.numberOfActions() > (window_update_size - 1)) {
				execute_Bulks();
			}
			
			if (element.parentpath != null) {
				String filename = element.currentpath.substring(element.currentpath.lastIndexOf("/"), element.currentpath.length());
				if (searchForbiddenChars(filename)) {
					l_elements_problems.add(element);
					/**
					 * Disabled this : there is too many bad file name
					 */
					// Log2.log.info("Bad filename", element);
				}
			}
			
			String index_type = null;
			if (element.directory) {
				index_type = ES_TYPE_DIRECTORY;
			} else {
				index_type = ES_TYPE_FILE;
			}
			
			/**
			 * Push it
			 */
			if (ttl > 0) {
				bulkrequest_index.add(client.prepareIndex(ES_INDEX, index_type, element.prepare_key()).setSource(element.toJson().toJSONString()).setTTL(ttl));
			} else {
				bulkrequest_index.add(client.prepareIndex(ES_INDEX, index_type, element.prepare_key()).setSource(element.toJson().toJSONString()));
			}
			
			return true;
		}
		
		public void execute_Bulks() {
			Log2Dump dump = new Log2Dump();
			dump = new Log2Dump();
			dump.add("name", getName());
			dump.add("indexed", bulkrequest_index.numberOfActions());
			Log2.log.debug("Prepare to update Elasticsearch database", dump);
			
			BulkResponse bulkresponse;
			
			if (bulkrequest_index.numberOfActions() > 0) {
				bulkresponse = bulkrequest_index.execute().actionGet();
				if (bulkresponse.hasFailures()) {
					dump = new Log2Dump();
					dump.add("name", getName());
					dump.add("type", "index");
					dump.add("failure message", bulkresponse.buildFailureMessage());
					Log2.log.error("Errors during indexing", null, dump);
				}
				bulkrequest_index = client.prepareBulk();
			}
			
		}
		
		public void end() {
			execute_Bulks();
		}
		
	}
	
	public final long index() throws Exception {
		ElasticSearchPushElement push = new ElasticSearchPushElement();
		long result = doIndex(push);
		push.end();
		
		if (push.l_elements_problems.size() > 0) {
			/**
			 * Alert ?
			 * Disabled this : there is too many bad file name
			 */
		}
		
		return result;
	}
}
