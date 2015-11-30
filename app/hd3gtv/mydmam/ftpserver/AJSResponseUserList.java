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

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.web.AsyncJSResponseObject;
import hd3gtv.mydmam.web.AsyncJSSerializer;

public class AJSResponseUserList implements AsyncJSResponseObject {
	
	ArrayList<AJSUser> users = new ArrayList<AJSUser>(1);
	
	static class Serializer implements AsyncJSSerializer<AJSResponseUserList> {
		
		public JsonElement serialize(AJSResponseUserList src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = new JsonObject();
			result.add("users", FTPOperations.getGson().toJsonTree(src.users, AJSUser.type_List_User));
			
			return result;
		}
		
		public Class<AJSResponseUserList> getEnclosingClass() {
			return AJSResponseUserList.class;
		}
		
	}
	
}
