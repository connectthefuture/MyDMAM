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
package hd3gtv.mydmam.auth;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;

public class RoleNG {
	
	private String key;
	private String role_name;
	private HashSet<String> privileges;
	
	private transient ArrayList<GroupNG> role_groups;
	private transient ArrayList<UserNG> role_groups_users;
	private transient AuthTurret turret;
	
	private static Type hashset_privileges_typeOfT = new TypeToken<HashSet<String>>() {
	}.getType();
	
	RoleNG save(ColumnListMutation<String> mutator) {
		mutator.putColumnIfNotNull("role_name", role_name);
		if (privileges != null) {
			mutator.putColumnIfNotNull("group_roles", turret.getGson().toJson(privileges, hashset_privileges_typeOfT));
		}
		return this;
	}
	
	RoleNG loadFromDb(ColumnList<String> cols) {
		if (cols.isEmpty()) {
			return this;
		}
		role_name = cols.getStringValue("role_name", null);
		
		if (cols.getColumnByName("privileges") != null) {
			privileges = turret.getGson().fromJson(cols.getColumnByName("privileges").getStringValue(), hashset_privileges_typeOfT);
		}
		
		return this;
	}
	
	RoleNG(AuthTurret turret, String key, boolean load_from_db) {
		this.turret = turret;
		if (turret == null) {
			throw new NullPointerException("\"turret\" can't to be null");
		}
		this.key = key;
		if (key == null) {
			throw new NullPointerException("\"key\" can't to be null");
		}
		if (load_from_db) {
			try {
				loadFromDb(turret.prepareQuery().getKey(key).execute().getResult());
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
	}
	
	public HashSet<String> getPrivileges() {
		synchronized (privileges) {
			if (privileges == null) {
				privileges = new HashSet<String>(1);
				ColumnList<String> cols;
				try {
					cols = turret.prepareQuery().getKey(key).withColumnSlice("privileges").execute().getResult();
					if (cols.getColumnByName("privileges").hasValue()) {
						privileges = turret.getGson().fromJson(cols.getColumnByName("privileges").getStringValue(), hashset_privileges_typeOfT);
					}
				} catch (ConnectionException e) {
					turret.onConnectionException(e);
				}
			}
		}
		return privileges;
	}
	
	public String getKey() {
		return key;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(", ");
		sb.append(role_name);
		return sb.toString();
	}
	
	public JsonObject exportForAdmin() {
		JsonObject jo = new JsonObject();
		jo.addProperty("role_name", role_name);
		jo.add("privileges", turret.getGson().toJsonTree(getPrivileges()));
		return jo;
	}
	
	// TODO CRUD
	// TODO Gson (de) serializers
	
	// @see Privileges.getAllSortedPrivileges()
}
