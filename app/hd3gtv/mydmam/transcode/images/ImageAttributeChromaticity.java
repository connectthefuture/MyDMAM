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
package hd3gtv.mydmam.transcode.images;

public class ImageAttributeChromaticity {
	ImageAttributeDimension redPrimary;
	ImageAttributeDimension greenPrimary;
	ImageAttributeDimension bluePrimary;
	ImageAttributeDimension whitePrimary;
	
	public ImageAttributeDimension getBluePrimary() {
		return bluePrimary;
	}
	
	public ImageAttributeDimension getGreenPrimary() {
		return greenPrimary;
	}
	
	public ImageAttributeDimension getRedPrimary() {
		return redPrimary;
	}
	
	public ImageAttributeDimension getWhitePrimary() {
		return whitePrimary;
	}
}