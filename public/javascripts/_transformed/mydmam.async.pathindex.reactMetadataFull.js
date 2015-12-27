/** This file is automatically generated! Do not edit. */ (function(pathindex) { /*
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

pathindex.reactMetadataFull = React.createClass({displayName: "reactMetadataFull",
	render: function() {
		if (!this.props.mtdsummary | !this.props.reference) {
			return null;
		}
		var mtdsummary = this.props.mtdsummary;
		var reference = this.props.reference;
		// console.log("TODO", this.props);

		var file_hash = md5(reference.storagename + ":" + reference.path);
		var master_as_preview_type = '';
		var master_as_preview_url = null;

		if (mtdsummary.master_as_preview) {
			master_as_preview_type = mtdsummary.mimetype.substring(0, mtdsummary.mimetype.indexOf("/"));
			var ext = reference.path.substring(reference.path.lastIndexOf("."), reference.path.length);
			master_as_preview_url = pathindex.metadatas.getFileURL(file_hash, "master_as_preview", "default" + ext);
		}

		var preview = null;

		if (mtdsummary.previews) {
			var previews = mtdsummary.previews;

			if ((previews.video_lq_pvw != null) | (previews.video_sd_pvw != null) | (previews.video_hd_pvw != null) | (master_as_preview_type == "video")) {
				preview = (React.createElement(pathindex.metadatas.Video, {file_hash: file_hash, mtdsummary: mtdsummary, reference: reference, master_as_preview_url: master_as_preview_url}));
			} else if ((previews.audio_pvw != null) | (master_as_preview_type == "audio")) {
				preview = (React.createElement(pathindex.metadatas.Audio, {file_hash: file_hash, mtdsummary: mtdsummary, reference: reference, master_as_preview_url: master_as_preview_url}));
			} else if ((previews.full_size_thumbnail != null) | (previews.cartridge_thumbnail != null) | (previews.icon_thumbnail != null)) {
				preview = (React.createElement(pathindex.metadatas.Image, {file_hash: file_hash, previews: previews}));
			}
		} else {
			//Only master_as_preview (maybe a small element)
			if (master_as_preview_type == "video") {
				preview = (React.createElement(pathindex.metadatas.Video, {file_hash: file_hash, mtdsummary: mtdsummary, reference: reference, master_as_preview_url: master_as_preview_url}));
			} else if (master_as_preview_type == "audio") {
				preview = (React.createElement(pathindex.metadatas.Audio, {file_hash: file_hash, mtdsummary: mtdsummary, reference: reference, master_as_preview_url: master_as_preview_url}));
			}
			//It never be an image as master, this may be
			//security/confidentiality problems.
		}
		
		var analyser_results = [];
		if (mtdsummary.summaries) {
			for (var analyser in mtdsummary.summaries) {
				analyser_results.push(
					React.createElement("blockquote", {key: analyser, style: {marginTop: "1em"}}, 
						React.createElement("p", null, 
							mtdsummary.summaries[analyser]
						), 
						React.createElement("small", null, 
							analyser
						)
					)
				);
			}
		}

		return (
			React.createElement("div", null, 
				preview, 
				analyser_results
			)
		);
	}
});
})(window.mydmam.async.pathindex);
// Generated by hd3gtv.mydmam.web.JSProcessor for the module internal
// Source hash: 1ccf04055a7bb47c36e47aa993b3699a
