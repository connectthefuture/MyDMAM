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
package hd3gtv.mydmam.web.stat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

public final class AsyncStatResult {
	
	private LinkedHashMap<String, AsyncStatResultElement> selected_path_elements;
	
	private transient List<SourcePathIndexerElement> all_path_elements;
	private transient int page_from = 0;
	private transient int page_size = 100;
	
	public AsyncStatResult(List<String> pathelementskeys) {
		selected_path_elements = new LinkedHashMap<String, AsyncStatResultElement>();
		for (int pos_k = 0; pos_k < pathelementskeys.size(); pos_k++) {
			selected_path_elements.put(pathelementskeys.get(pos_k), new AsyncStatResultElement());
		}
	}
	
	/**
	 * Create an empty stat result
	 */
	public AsyncStatResult() {
		selected_path_elements = new LinkedHashMap<String, AsyncStatResultElement>();
	}
	
	void setPage_from(int page_from) {
		this.page_from = page_from;
	}
	
	void setPage_size(int page_size) {
		this.page_size = page_size;
	}
	
	void setReference(String item_key, SourcePathIndexerElement reference) {
		selected_path_elements.get(item_key).reference = reference;
		if (all_path_elements == null) {
			all_path_elements = new ArrayList<SourcePathIndexerElement>();
		}
		all_path_elements.add(reference);
	}
	
	void setItemTotalCount(String item_key, Long count) {
		selected_path_elements.get(item_key).items_total = count;
	}
	
	void setMtdSummary(String item_key, Map<String, Object> mtdsummary) {
		selected_path_elements.get(item_key).mtdsummary = mtdsummary;
	}
	
	List<SourcePathIndexerElement> getAllPathElements() {
		if (all_path_elements == null) {
			all_path_elements = new ArrayList<SourcePathIndexerElement>();
		}
		return all_path_elements;
	}
	
	void populateDirListsForItems(HashMap<String, Explorer.DirectoryContent> map_dir_list, boolean sub_items_count_items, boolean request_dir_count_items, boolean has_search) throws Exception {
		LinkedHashMap<String, SourcePathIndexerElement> dir_list;
		HashMap<String, Long> count_result;
		String element_key;
		
		for (Map.Entry<String, AsyncStatResultElement> entry : selected_path_elements.entrySet()) {
			if (map_dir_list.containsKey(entry.getKey()) == false) {
				entry.getValue().search_return_nothing = has_search;
				continue;
			}
			dir_list = map_dir_list.get(entry.getKey()).directory_content;
			if (dir_list.isEmpty()) {
				continue;
			}
			
			entry.getValue().items = new LinkedHashMap<String, AsyncStatResultSubElement>(dir_list.size());
			for (Map.Entry<String, SourcePathIndexerElement> dir_list_entry : dir_list.entrySet()) {
				AsyncStatResultElement s_element = new AsyncStatResultElement();
				s_element.reference = dir_list_entry.getValue();
				if (sub_items_count_items & s_element.reference.directory) {
					element_key = s_element.reference.prepare_key();
					count_result = PathElementStat.request_response_cache.countDirectoryContentElements(Arrays.asList(element_key));
					if (count_result.containsKey(element_key)) {
						s_element.items_total = count_result.get(element_key);
					}
				}
				entry.getValue().items.put(dir_list_entry.getKey(), s_element);
			}
			
			if (request_dir_count_items) {
				entry.getValue().items_total = map_dir_list.get(entry.getKey()).directory_size;
			}
			entry.getValue().items_page_from = page_from;
			entry.getValue().items_page_size = page_size;
			// System.out.println(map_dir_list.get(entry.getKey()).has_search);
		}
		
	}
	
	void populateSummariesForItems(Map<String, Map<String, Object>> summaries) {
		Map<String, AsyncStatResultSubElement> items;
		for (Map.Entry<String, AsyncStatResultElement> entry : selected_path_elements.entrySet()) {
			items = entry.getValue().items;
			if (items == null) {
				/**
				 * If can't found a selected_path_element
				 */
				continue;
			}
			for (Map.Entry<String, AsyncStatResultSubElement> entry_item : items.entrySet()) {
				if (summaries.containsKey(entry_item.getKey()) == false) {
					continue;
				}
				entry_item.getValue().mtdsummary = summaries.get(entry_item.getKey());
			}
		}
	}
	
	public static class Serializer implements JsonSerializer<AsyncStatResult> {
		public JsonElement serialize(AsyncStatResult src, Type typeOfSrc, JsonSerializationContext context) {
			Map<String, AsyncStatResultElement> items = new HashMap<String, AsyncStatResultElement>(src.selected_path_elements.size() + 1);
			for (Map.Entry<String, AsyncStatResultElement> entry : src.selected_path_elements.entrySet()) {
				if (entry.getValue().isEmpty()) {
					continue;
				}
				items.put(entry.getKey(), entry.getValue());
			}
			return MyDMAM.gson_kit.getGson().toJsonTree(items);
		}
		
	}
	
	public String toJSONString() {
		return MyDMAM.gson_kit.getGson().toJson(this);
	}
}
