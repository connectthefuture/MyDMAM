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
package hd3gtv.mydmam.db;

import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.search.SearchHit;

public class ElasticsearchCrawlerToDelete implements ElasticsearchCrawlerHitToBulk {
	
	public boolean onFoundHit(SearchHit hit, ElasticsearchBulkOperation bulk) throws Exception {
		bulk.refresh();
		
		DeleteRequestBuilder request = new DeleteRequestBuilder(bulk.getClient());
		request.setId(hit.getId()).setIndex(hit.getIndex()).setType(hit.getType());
		bulk.add(request.request());
		return true;
	}
	
}
