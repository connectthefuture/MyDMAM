/** This file is automatically generated! Do not edit. */ (function(ftpserver) { /*
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

ftpserver.ActivityList = React.createClass({displayName: "ActivityList",
	getInitialState: function() {
		return {
			activities: null,
			interval: null,
			max_items: 20,
			searched_text: null,
			searched_action_type: "ALL",
		};
	},
	onWantRefresh: function() {
		var request = {
			max_items: this.state.max_items,
			searched_text: this.state.searched_text,
			searched_action_type: this.state.searched_action_type,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities});
		}.bind(this));
	},
	onWantExpandList: function() {
		var request = {
			max_items: this.state.max_items + 20,
			searched_text: this.state.searched_text,
			searched_action_type: this.state.searched_action_type,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities, max_items: request.max_items});
		}.bind(this));
	},
	componentWillMount: function() {
		this.onWantRefresh();
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.onWantRefresh, 10000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	onTextSearch: function(request_searched_text) {
		var searched_text = request_searched_text.trim();
		if (searched_text == "") {
			searched_text = null;
		}
		var request = {
			max_items: this.state.max_items,
			searched_text: searched_text,
			searched_action_type: this.state.searched_action_type,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities, searched_text: request.searched_text});
		}.bind(this));
	},
	onSelectActionTypeChange: function() {
		var request = {
			max_items: this.state.max_items,
			searched_text: this.state.searched_text,
			searched_action_type: React.findDOMNode(this.refs.select_action).value,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities, searched_action_type: request.searched_action_type});
		}.bind(this));
	},
	render: function(){
		var activities = this.state.activities;
		if (activities == null) {
			return (React.createElement(mydmam.async.PageLoadingProgressBar, null));
		}

		var no_activity_label = null;
		if (activities.length === 0) {
			if (this.state.searched_text == null & this.state.searched_action_type == "ALL") {
				return (React.createElement(mydmam.async.AlertInfoBox, null, i18n("ftpserver.activities.emptylist")));
			}
			no_activity_label = (React.createElement(mydmam.async.AlertInfoBox, null, i18n("ftpserver.activities.noresults")));
		}

		var th_display_user = null;
		if (this.props.user_id == null) {
			th_display_user = (React.createElement("th", null, i18n("ftpserver.activities.user")));
		}

		var lines = [];
		for(var pos in activities) {
			var activity = activities[pos];

			var size_offset = null;
			if (activity.file_size > 0) {
				if (activity.file_offset > 0) {
					size_offset = (React.createElement("span", {style: {marginLeft: 5}, className: "label label-important pull-right"}, 
						i18n("ftpserver.activities.bytes", activity.file_offset), " / ", i18n("ftpserver.activities.bytes", activity.file_size)
					));
				} else {
					size_offset = (React.createElement("span", {style: {marginLeft: 5}, className: "label label-important pull-right"}, 
						i18n("ftpserver.activities.bytes", activity.file_size)
					));
				}
			}

			var td_display_user = null;
			if (this.props.user_id == null) {
				td_display_user = (React.createElement("td", null, 
					React.createElement("strong", null, activity.user_domain), " :: ", activity.user_name, " ", React.createElement("span", {className: "label label-inverse pull-right"}, activity.user_group)
				));
			}

			lines.push(React.createElement("tr", {key: pos}, 
				td_display_user, 
				React.createElement("td", null, 
					React.createElement("span", {style: {backgroundColor: "#" + activity.session_key.substring(0,6), borderRadius: 5, paddingRight: 12, marginRight: 5, border: "1px solid black"}}, " "), 
					React.createElement(mydmam.async.pathindex.reactDate, {date: activity.activity_date, style: {marginRight: 5}}), 
					React.createElement(mydmam.async.pathindex.reactSinceDate, {date: activity.activity_date, i18nlabel: "ftpserver.activities.since"})
				), 
				React.createElement("td", null, 
					React.createElement("span", {className: "badge badge-info"}, i18n("ftpserver.activities.actionenum." + activity.action))		
				), 
				React.createElement("td", null, 
					React.createElement("code", {style: {whiteSpace: "normal", color: "#080"}}, activity.working_directory), 
					React.createElement("code", {style: {whiteSpace: "normal", color: "black"}}, activity.argument), 
					size_offset
				), 
				React.createElement("td", null, 
					activity.client_host
				)
			));
		}

		var button_show_next_items = null;
		if (activities.length >= 20) {
			if (this.state.max_items < 100) {
				button_show_next_items = (React.createElement("button", {className: "btn btn-mini btn-block", style: {marginBottom: "2em"}, onClick: this.onWantExpandList}, 
					React.createElement("i", {className: "icon-arrow-down"}), " ", i18n("ftpserver.activities.next"), " ", React.createElement("i", {className: "icon-arrow-down"})
				));
			} else {
				button_show_next_items = (React.createElement("button", {className: "btn btn-mini btn-block disabled", style: {marginBottom: "2em"}}, 
					i18n("ftpserver.activities.nextdisabled", this.state.max_items)
				));
			}
		}

		return (React.createElement("div", null, 
			React.createElement("span", {className: "lead", style: {marginLeft: "0.5em"}}, i18n("ftpserver.activities.activityandsessions")), 
		    React.createElement("form", {className: "form-search pull-right"}, 
				React.createElement("select", {ref: "select_action", onChange: this.onSelectActionTypeChange}, 
					React.createElement("option", {key: "ALL", value: "ALL"}, "  ", i18n("ftpserver.activities.search_by_select.ALL")), 
					React.createElement("option", {key: "IO", value: "IO"}, "  ", i18n("ftpserver.activities.search_by_select.IO")), 
					React.createElement("option", {key: "STORE", value: "STORE"}, " ", i18n("ftpserver.activities.search_by_select.STORE")), 
					React.createElement("option", {key: "RESTOR", value: "RESTOR"}, i18n("ftpserver.activities.search_by_select.RESTOR")), 
					React.createElement("option", {key: "DELETE", value: "DELETE"}, i18n("ftpserver.activities.search_by_select.DELETE")), 
					React.createElement("option", {key: "MKDIR", value: "MKDIR"}, " ", i18n("ftpserver.activities.search_by_select.MKDIR")), 
					React.createElement("option", {key: "RENAME", value: "RENAME"}, i18n("ftpserver.activities.search_by_select.RENAME"))
				), 
				React.createElement(mydmam.async.SearchInputBox, {style: {marginLeft: "0.5em"}, onKeyPress: this.onTextSearch})
		    ), 
			React.createElement("table", {style: {marginBottom: "1em"}, className: "table table-striped table-hover table-bordered table-condensed"}, 
				React.createElement("thead", null, 
					React.createElement("tr", null, 
						th_display_user, 
						React.createElement("th", null, i18n("ftpserver.activities.session.color"), " • ", i18n("ftpserver.activities.session.date")), 
						React.createElement("th", null, i18n("ftpserver.activities.title.action")), 
						React.createElement("th", null, i18n("ftpserver.activities.title.directory"), " • ", i18n("ftpserver.activities.title.name"), " • ", i18n("ftpserver.activities.title.size")), 
						React.createElement("th", null, i18n("ftpserver.activities.title.clientip"))
					)
				), 
				React.createElement("tbody", null, 
					lines
				)
			), 
			no_activity_label, 
			button_show_next_items
		));
	},
});

})(window.mydmam.async.ftpserver);
// Generated by hd3gtv.mydmam.web.JSProcessor for the module internal
// Source hash: 2ac5622434b6c5eb7362c15f5d3f71ef