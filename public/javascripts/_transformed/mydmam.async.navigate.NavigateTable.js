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

navigate.NavigateTable = React.createClass({displayName: "NavigateTable",
	getInitialState: function() {
		return {
			sorted_col: null,
			sorted_order: null,
		};
	},
	handleChangeSort: function(colname, previous_order) {
		var order = null;
		if (previous_order == null) {
			order = "desc";
		} else if (previous_order === "desc") {
			order = "asc";
		}
		this.setState({
			sorted_col: colname,
			sorted_order: order,
		});
		this.props.changeOrderSort(colname, order);
	},
	render: function() {
		var url_navigate = mydmam.metadatas.url.navigate_react;
		
		var items = this.props.stat.items;
		if (!items) {
			return null;
		}
		if (items.length === 0) {
			return null;
		}
		var reference = this.props.stat.reference;

		var dircontent = [];
		for (var item in items) {
			var newitem = items[item];
			newitem.key = item;
			dircontent.push(newitem);
		}

		var ButtonSort = mydmam.async.ButtonSort;

		var thead = null;
		if (reference.storagename) {
			var order_path = (this.state.sorted_col === 'path' ? this.state.sorted_order : null);
			var order_size = (this.state.sorted_col === 'size' ? this.state.sorted_order : null);
			var order_date = (this.state.sorted_col === 'date' ? this.state.sorted_order : null);

			thead = (
				React.createElement("thead", null, React.createElement("tr", null, 
					React.createElement("td", null, React.createElement(ButtonSort, {onChangeState: this.handleChangeSort, colname: "path", order: order_path})), 
					React.createElement("td", {className: "pathindex-col-size"}, React.createElement(ButtonSort, {onChangeState: this.handleChangeSort, colname: "size", order: order_size})), 
					React.createElement("td", {className: "pathindex-col-date"}, React.createElement(ButtonSort, {onChangeState: this.handleChangeSort, colname: "date", order: order_date})), 
					React.createElement("td", null, " "), 
					React.createElement("td", null, " ")
				))
			);
		}
		var tbody = [];
		for (var pos = 0; pos < dircontent.length; pos++) {
			var elementkey = dircontent[pos].key;
			var element = dircontent[pos].reference;
			var element_items_total = dircontent[pos].items_total;

			var td_element_name = null;
			var td_element_attributes = null;
			var td_element_date = (React.createElement("td", null));

			if (element.directory) {
				var name = null;
				if (reference.storagename) {
					name = (
						React.createElement("a", {
							className: "tlbdirlistitem", 
							href: url_navigate + "#" + element.storagename + ":" + element.path, 
							onClick: this.props.navigate, 
							"data-navigatetarget": element.storagename + ":" + element.path}, 

							element.path.substring(element.path.lastIndexOf("/") + 1)
						)
					);
				} else {
					name = (
						React.createElement("a", {
							className: "tlbdirlistitem", 
							href: url_navigate + "#" + element.storagename + ":/", 
							onClick: this.props.navigate, 
							"data-navigatetarget": element.storagename + ":/"}, 

							element.storagename
						)
					);
				}

				var empty_badge = null;
				if (element_items_total === 0) {
					empty_badge = (
						React.createElement("span", {className: "badge badge-success", style: {marginLeft: 5}}, 
							i18n('browser.emptydir')
						)
					);
				}

				td_element_name = (
					React.createElement("th", null, 
						React.createElement(mydmam.async.pathindex.reactBasketButton, {pathindexkey: elementkey}), 
						name, 
						empty_badge
					)
				);
			} else {
				var elementid = null;
				if (element.id) {
					elementid = (React.createElement("span", {className: "label label-info", style: {marginLeft: 5, marginRight: 5}}, element.id));
				}
				td_element_name = (
					React.createElement("td", null, 
						React.createElement(mydmam.async.pathindex.reactBasketButton, {pathindexkey: elementkey}), 
						React.createElement("a", {
							className: "tlbdirlistitem", 
							href: url_navigate + "#" + element.storagename + ":" + element.path, 
							onClick: this.props.navigate, 
							"data-navigatetarget": element.storagename + ":" + element.path}, 

							elementid, 
							element.path.substring(element.path.lastIndexOf("/") + 1)
						)
					)
				);
			}

			if (element.directory) {
				var title = i18n('browser.storagetitle');
				if (reference.storagename != null) {
					title = i18n('browser.directorytitle');
				}
				if (element_items_total != null) {
					if (element_items_total === 0) {
						title += " " + i18n('browser.emptydir');
					} else if (element_items_total == 1) {
						title += ' - ' + i18n('browser.oneelement');
					} else {
						title += ' - ' + i18n('browser.Nelements', element_items_total);
					}
				}
				td_element_attributes = (
					React.createElement("td", null, 
						React.createElement("span", {className: "label label-success"}, 
							title
						)
					)
				);
			} else {
				td_element_attributes = (
					React.createElement("td", null, 
						React.createElement(mydmam.async.pathindex.reactFileSize, {size: element.size, style: {marginLeft: 0}})
					)
				);
			}

			if (reference.storagename != null) {
				td_element_date = (React.createElement("td", null, React.createElement(mydmam.async.pathindex.reactDate, {date: element.date})));
			}

			var external_pos = null;
			if (element.directory === false) {
				external_pos = (React.createElement(mydmam.async.pathindex.reactExternalPosition, {pathindexkey: elementkey, externalpos: this.props.externalpos}));
			}

			tbody.push(
				React.createElement("tr", {key: elementkey}, 
					td_element_name, 
					td_element_attributes, 
					td_element_date, 
					React.createElement("td", null, mydmam.async.pathindex.mtdTypeofElement(dircontent[pos].mtdsummary)), 
					React.createElement("td", null, external_pos)
				)
			);
		}

		return (
			React.createElement("table", {className: "table table-hover table-condensed"}, 
				thead, 
				React.createElement("tbody", null, tbody)
			)
		);
	}
});
})(window.mydmam.async.navigate);
// Generated by hd3gtv.mydmam.web.JSProcessor for the module internal
// Source hash: 5d70e8f4a3badcf4d9be536bbbb8a93b
