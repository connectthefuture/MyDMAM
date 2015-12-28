/** This file is automatically generated! Do not edit. */ (function(navigate) { /*
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

navigate.BreadCrumb = React.createClass({displayName: "BreadCrumb",
	render: function() {
		var url_navigate = mydmam.metadatas.url.navigate_react;
		
		var storagename = this.props.storagename;
		var path = this.props.path;
		if (storagename == null) {
			return (
				React.createElement("ul", {className: "breadcrumb"}, 
					React.createElement("li", {className: "active"}, 
						i18n('browser.storagestitle')
					)
				)
			);
		}
		
		var element_subpaths = path.split("/");
		var currentpath = "";
		var newpath = "";
		var items = [];
		for (var pos = 1; pos < element_subpaths.length; pos++) {
			newpath = storagename + ':' + currentpath + "/" + element_subpaths[pos];
			if (pos + 1 < element_subpaths.length) {
				items.push(
					React.createElement("li", {key: pos}, 
						React.createElement("span", {className: "divider"}, "/"), 
						React.createElement("a", {href: url_navigate + "#" + newpath, onClick: this.props.navigate, "data-navigatetarget": newpath}, 
							element_subpaths[pos]
						)
					)
				);
			} else {
				items.push(
					React.createElement("li", {key: pos, className: "active"}, 
						React.createElement("span", {className: "divider"}, "/"), 
						element_subpaths[pos]
					)
				);
			}
			currentpath = currentpath + "/" + element_subpaths[pos];
		}

		var header = [];
		if (items.length > 0) {
			header.push(
				React.createElement("li", {key: "storagestitle"}, 
					React.createElement("a", {href: url_navigate, onClick: this.props.navigate, "data-navigatetarget": ""}, 
						i18n('browser.storagestitle')
					), 
					React.createElement("span", {className: "divider"}, "::")
				)
			);
			if (path != "/") {
				header.push(
					React.createElement("li", {key: "root"}, 
						React.createElement("a", {href: url_navigate + "#" + storagename + ':/', onClick: this.props.navigate, "data-navigatetarget": storagename + ':/'}, 
							storagename
						)
					)
				);
			} else {
				header.push(
					React.createElement("li", {key: "root", className: "active"}, 
						storagename
					)
				);
			}
			return (
				React.createElement("ul", {className: "breadcrumb"}, 
					header, 
					items
				)
			);
		} 
		return null;
	}
});
})(window.mydmam.async.navigate);
// Generated by hd3gtv.mydmam.web.JSProcessor for the module internal
// Source hash: 27b08b6c461e871ae6453ea3ec6c826b