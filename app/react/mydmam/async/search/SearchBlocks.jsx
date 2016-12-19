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

search.found = function(results, dom_target) {
	if (!dom_target) {
		return;
	}

	/**
	 * Create React uniq key.
	 */
	var createReactKey = function(result_list) {
		for (var pos in result_list) {
			result_list[pos].reactkey = result_list[pos].index + ":" + result_list[pos].type + ":" + result_list[pos].key;
		};
	}

	createReactKey(results.results);

	React.render(
		<search.SearchResultPage results={results} />,
		dom_target
	);
};

search.SearchForm = React.createClass({
	componentDidMount: function() {
		React.findDOMNode(this.refs.q).focus();
		React.findDOMNode(this.refs.q).value = this.getDefaultText();
	},
	componentDidUpdate: function() {
		React.findDOMNode(this.refs.q).focus();
		React.findDOMNode(this.refs.q).value = this.getDefaultText();
	},
	handleSubmit: function(e) {
		e.preventDefault();
		var q = React.findDOMNode(this.refs.q).value.trim();
	    if (!q) {
			return;
		}
		this.props.onSearchFormSubmit({q: q});
	},
	getDefaultText: function() {
		var value = "";
		if (this.props.results) {
			value = this.props.results.q;
			if (!value) {
				value = "";
			}
		}
		return value;
	},
	render: function() {
	    return (
	    	<form className="search-query form-search" onSubmit={this.handleSubmit}>
				<div className="input-append">
					<input type="text" ref="q" placeholder={i18n("maingrid.search")} className="search-query span10" />
					<button className="btn btn-info" type="submit">{i18n("maingrid.search")}</button>
				</div>
			</form>
		);
	}
});

search.SearchResults = React.createClass({
	render: function() {
		var stat = this.props.stat;
		var external_storages_location = this.props.external_storages_location;

		var resultList = this.props.results.results.map(function (result) {
			var ViewHander = mydmam.module.f.processViewSearchResult(result);
			if (!ViewHander) {
				console.error("Can't handle search result", result, stat[result.key]);
				return (
					<div style={{marginBottom: "1em"}} key={result.reactkey}>
						<div>Error</div>
					</div>
				);
			} else {
				var can_resolve_external_location = (external_storages_location.indexOf(result.content.storagename) > -1);
				return (
					<div style={{marginBottom: "1em"}} key={result.reactkey}>
						<ViewHander result={result} stat={stat[result.key]} can_resolve_external_location={can_resolve_external_location} />
					</div>
				);
			}
		});
	    return (
	    	<div>
		        {resultList}
			</div>
		);
	}
});

search.SearchResultsHeader = React.createClass({
	render: function() {
		if (this.props.results.results.length == 0) {
			return (
				<div>
					<h4>{i18n("search.noresults")}</h4>
					<em>{i18n("search.noresultsfor")}</em> <strong>{this.props.results.q}</strong><br />
					<small className="muted">({this.props.results.duration / 1000} {i18n("search.seconds")})</small>.
				</div>
			);
		} else {
			var pageon = (<span>{i18n("search.oneresult")}</span>);
			var pageadd = null;
			if (this.props.results.total_items_count > 1) {
				pageon = (<span>{this.props.results.total_items_count} {i18n("search.results")}</span>);
				if (this.props.results.pagecount > 1) {
					pageadd = (<span>{i18n("search.pageXonY", this.props.results.from, this.props.results.pagecount)}</span>);
				}
			}

			return (
				<p><small className="muted">
					{pageon} {pageadd} ({this.props.results.duration / 1000} {i18n("search.seconds")})
				</small><br />
				<small>
					{i18n("search.method." + this.props.results.mode.toLowerCase())}
				</small>
				</p>
			);
		}
	}
});