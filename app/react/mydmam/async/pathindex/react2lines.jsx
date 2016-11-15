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

pathindex.react2lines = React.createClass({
	shouldComponentUpdate: function(nextProps, nextState) {
		if (nextProps.externalpos.positions) {
			if (nextProps.externalpos.positions[nextProps.result.key]) {
				return true;
			}
		}

		if (nextProps.stat == null) {
			return false;
		}

		return true;
	},
	render: function() {
		var url_navigate = mydmam.metadatas.url.navigate_react;
		
		var result = this.props.result;
		var directory_block = null;
		if (result.content.directory) {
			directory_block = (<span className="label label-success" style={{marginLeft: 5}}>{i18n("search.result.directory")}</span>);
		}

		var path_linked = [];
		var sub_paths = result.content.path.split("/");
		var sub_path;
		var currentpath = "";
		for (var i = 1; i < sub_paths.length; i++) {
			sub_path = sub_paths[i];
			path_linked.push(
				<span key={i}>/
					<a href={url_navigate + "#" + result.content.storagename + ':' + currentpath + "/" + sub_path}>
						{sub_path}
					</a>
				</span>
			);
			currentpath = currentpath + "/" + sub_path;
		};

		//	<pathindex.reactExternalPosition pathindexkey={this.props.result.key} externalpos={this.props.externalpos} />

		return (
			<div className="pathindex">
				<span className="label label-inverse">{i18n("search.result.storage")}</span>
				<span className="label label-info" style={{marginLeft: 5}}>{result.content.id}</span>
				<pathindex.reactDate date={result.content.date} />
				<pathindex.reactFileSize size={result.content.size} />
				{directory_block}
				<pathindex.reactMetadata1Line stat={this.props.stat} />
				<br />
				<span>
					<pathindex.reactBasketButton pathindexkey={this.props.result.key}/>&nbsp;
					<strong className="storagename">
						<a href={url_navigate + "#" + result.content.storagename + ":/"}>
							{result.content.storagename}
						</a>
					</strong>
					&nbsp;::&nbsp;
					<span className="path">
						{path_linked}
					</span>
				</span>
			</div>
		);
	}
});