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

import com.netflix.astyanax.ColumnListMutation;

public interface AuthEntry extends Comparable<AuthEntry> {
	
	void delete(ColumnListMutation<String> mutator);
	
	public String getKey();
	
	public void save(ColumnListMutation<String> mutator);
	
	public String getName();
	
	default public int compareTo(AuthEntry o) {
		return getName().compareToIgnoreCase(o.getName());
	}
	
}
