/** This file is automatically generated! Do not edit. */ (function(auth) { /*
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

auth.Privileges = React.createClass({displayName: "Privileges",
	getInitialState: function() {
		return {
			fulllist: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("auth", "getallprivilegeslist", null, function(rawfulllist) {
			var fulllist = {};
			for (p in rawfulllist) {
				fulllist[p] = rawfulllist[p].sort();
			}
			this.setState({fulllist: fulllist});
		}.bind(this));
	},
	render: function(){
		var fulllist = this.state.fulllist;
		var items = [];

		var toList = function (list) {
			var ctrl_list = [];
			for (pos in list) {
				ctrl_list.push(React.createElement("div", {key: pos}, list[pos]));
			}
			return ctrl_list;
		};

		for (privilege_name in fulllist) {
			items.push(React.createElement("tr", {key: privilege_name}, 
				React.createElement("td", null, privilege_name), 
				React.createElement("td", null, toList(fulllist[privilege_name]))
			));
		}

		return (React.createElement("div", null, 
			React.createElement("table", {className: "table table-bordered table-striped table-condensed"}, 
				React.createElement("thead", null, 
					React.createElement("tr", null, 
						React.createElement("th", null, i18n("auth.privilege")), 
						React.createElement("th", null, i18n("auth.privilege.controllers"))
					)
				), 
				React.createElement("tbody", null, 
					items
				)
			)
		));
	}
});

})(window.mydmam.async.auth);
// Generated by hd3gtv.mydmam.web.JSProcessor for the module internal
// Source hash: 72637997a500b03fde6765a8c3ac697a
