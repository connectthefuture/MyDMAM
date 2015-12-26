/** This file is automatically generated! Do not edit. */ (function(search) { /*
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

search.SearchResultsHeader = React.createClass({displayName: "SearchResultsHeader",
	render: function() {
		if (this.props.results.results.length == 0) {
			return (
				React.createElement("div", {className: "alert alert-info"}, 
					React.createElement("h4", null, i18n("search.noresults")), 
					i18n("search.noresultsfor"), " ", React.createElement("strong", null, this.props.results.q), React.createElement("br", null), 
					React.createElement("small", null, "(", this.props.results.duration / 1000, " ", i18n("search.seconds"), ")"), "."
				)
			);
		} else {
			var pageon = (React.createElement("span", null, i18n("search.oneresult")));
			var pageadd = null;
			if (this.props.results.total_items_count > 1) {
				pageon = (React.createElement("span", null, this.props.results.total_items_count, " ", i18n("search.results")));
				if (this.props.results.pagecount > 1) {
					pageadd = (React.createElement("span", null, i18n("search.pageXonY", this.props.results.from, this.props.results.pagecount)));
				}
			}

			return (
				React.createElement("p", null, React.createElement("small", {className: "muted"}, 
					pageon, " ", pageadd, " (", this.props.results.duration / 1000, " ", i18n("search.seconds"), ")"
				), React.createElement("br", null), 
				React.createElement("small", null, 
					i18n("search.method." + this.props.results.mode.toLowerCase())
				)
				)
			);
		}
	}
});
})(window.mydmam.async.search);
// Generated by hd3gtv.mydmam.web.JSProcessor the Sat Dec 26 03:36:04 CET 2015 with MyDMAM git version: dvl 6a57be20
