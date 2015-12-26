/** This file is automatically generated! Do not edit. */ (function(watchfolders) { /*
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
watchfolders.table =  React.createClass({displayName: "table",
	loadActualItemList: function() {
		mydmam.async.request("watchfolders", "list", {}, function(data) {
			this.setState({items: data.items, jobs: data.jobs});
		}.bind(this));
	},
	getInitialState: function() {
		return {
			items: [],
			jobs: {},
		};
	},
	componentWillMount: function() {
		this.loadActualItemList();
		this.setState({interval: setInterval(this.loadActualItemList, 1000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	onDelete: function(abstract_founded_file) {
		var storage_name = abstract_founded_file.storage_name;
		var path = abstract_founded_file.path;
		var key = md5(storage_name + ":" + path);

		var pos_state_item = -1;
		for (pos in this.state.items) {
			if (this.state.items[pos].storage_name === storage_name && this.state.items[pos].path === path) {
				pos_state_item = pos;
				break;
			}
		}

		this.state.items.splice(pos_state_item, 1);
		this.setState({items : this.state.items});

		if (this.state.interval) {
			clearInterval(this.state.interval);
		}

		mydmam.async.request("watchfolders", "remove", {key: key}, function(data) {
			this.setState({interval : setInterval(this.loadActualItemList, 1000)});
		}.bind(this));
	},
	render: function() {
		var items = this.state.items;
		var jobs = this.state.jobs;

		var items = items.sort(function(a, b) {
		    return a.last_checked - b.last_checked;
		});

		var table_lines = [];
		for (pos in items) {
			table_lines.push(React.createElement(watchfolders.AbstractFoundedFile, {key: pos, abstract_founded_file: items[pos], jobs: jobs, onDelete: this.onDelete}));
		}

		return (React.createElement("table", {className: "table table-striped table-bordered table-hover table-condensed"}, 
			React.createElement("thead", null, 
				React.createElement("tr", null, 
					React.createElement("th", null, i18n("manager.watchfolders.table.file")), 
					React.createElement("th", null, i18n("manager.watchfolders.table.filedate")), 
					React.createElement("th", null, i18n("manager.watchfolders.table.size")), 
					React.createElement("th", null, i18n("manager.watchfolders.table.lastchecked")), 
					React.createElement("th", null, i18n("manager.watchfolders.table.status")), 
					React.createElement("th", null, i18n("manager.watchfolders.table.jobs")), 
					React.createElement("th", null)
				)
			), 
			React.createElement("tbody", null, 	
				table_lines
			)	
		));
	},
});

mydmam.routes.push("watchfolders", "watchfolders", watchfolders.table, [{name: "watchfolders", verb: "list"}]);	

})(window.mydmam.async.watchfolders);
// Generated by hd3gtv.mydmam.web.JSProcessor the Sat Dec 26 03:36:04 CET 2015 with MyDMAM git version: dvl 6a57be20
