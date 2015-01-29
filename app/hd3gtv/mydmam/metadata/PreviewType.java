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
package hd3gtv.mydmam.metadata;

/**
 * All definitions should exists and implemented in JS (in mydmam.metadatas)
 */
public enum PreviewType {
	// TODO add in JS side cartridge_thumbnail and icon_thumbnail
	// TODO failback the choose of jwplayer bg image to cartridge_thumbnail and icon_thumbnail,
	// TODO failback the choose of audio artwork image to cartridge_thumbnail and icon_thumbnail,
	// TODO failback the choose of any images to cartridge_thumbnail and icon_thumbnail ?
	
	full_size_thumbnail, video_sd_pvw, video_hd_pvw, video_lq_pvw, audio_pvw, cartridge_thumbnail, icon_thumbnail;
}
