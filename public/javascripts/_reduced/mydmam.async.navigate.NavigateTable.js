(function(a){a.NavigateTable=React.createClass({displayName:"NavigateTable",getInitialState:function(){return{sorted_col:null,sorted_order:null};
},handleChangeSort:function(d,c){var b=null;if(c==null){b="desc";}else{if(c==="desc"){b="asc";
}}this.setState({sorted_col:d,sorted_order:b});this.props.changeOrderSort(d,b);},render:function(){var o=mydmam.metadatas.url.navigate_react;
var p=this.props.stat.items;if(!p){return null;}if(p.length===0){return null;}var i=this.props.stat.reference;
var c=[];for(var v in p){var d=p[v];d.key=v;c.push(d);}var t=mydmam.async.ButtonSort;
var u=null;if(i.storagename){var m=(this.state.sorted_col==="path"?this.state.sorted_order:null);
var k=(this.state.sorted_col==="size"?this.state.sorted_order:null);var q=(this.state.sorted_col==="date"?this.state.sorted_order:null);
u=(React.createElement("thead",null,React.createElement("tr",null,React.createElement("td",null,React.createElement(t,{onChangeState:this.handleChangeSort,colname:"path",order:m})),React.createElement("td",{className:"pathindex-col-size"},React.createElement(t,{onChangeState:this.handleChangeSort,colname:"size",order:k})),React.createElement("td",{className:"pathindex-col-date"},React.createElement(t,{onChangeState:this.handleChangeSort,colname:"date",order:q})),React.createElement("td",null," "),React.createElement("td",null," "))));
}var b=[];for(var h=0;h<c.length;h++){var g=c[h].key;var f=c[h].reference;var n=c[h].items_total;
var w=null;var l=null;var s=(React.createElement("td",null));if(f.directory){var y=null;
if(i.storagename){y=(React.createElement("a",{className:"tlbdirlistitem",href:o+"#"+f.storagename+":"+f.path,onClick:this.props.navigate,"data-navigatetarget":f.storagename+":"+f.path},f.path.substring(f.path.lastIndexOf("/")+1)));
}else{y=(React.createElement("a",{className:"tlbdirlistitem",href:o+"#"+f.storagename+":/",onClick:this.props.navigate,"data-navigatetarget":f.storagename+":/"},f.storagename));
}var r=null;if(n===0){r=(React.createElement("span",{className:"badge badge-success",style:{marginLeft:5}},i18n("browser.emptydir")));
}w=(React.createElement("th",null,React.createElement(mydmam.async.pathindex.reactBasketButton,{pathindexkey:g}),y,r));
}else{var j=null;if(f.id){j=(React.createElement("span",{className:"label label-info",style:{marginLeft:5,marginRight:5}},f.id));
}w=(React.createElement("td",null,React.createElement(mydmam.async.pathindex.reactBasketButton,{pathindexkey:g}),React.createElement("a",{className:"tlbdirlistitem",href:o+"#"+f.storagename+":"+f.path,onClick:this.props.navigate,"data-navigatetarget":f.storagename+":"+f.path},j,f.path.substring(f.path.lastIndexOf("/")+1))));
}if(f.directory){var x=i18n("browser.storagetitle");if(i.storagename!=null){x=i18n("browser.directorytitle");
}if(n!=null){if(n===0){x+=" "+i18n("browser.emptydir");}else{if(n==1){x+=" - "+i18n("browser.oneelement");
}else{x+=" - "+i18n("browser.Nelements",n);}}}l=(React.createElement("td",null,React.createElement("span",{className:"label label-success"},x)));
}else{l=(React.createElement("td",null,React.createElement(mydmam.async.pathindex.reactFileSize,{size:f.size,style:{marginLeft:0}})));
}if(i.storagename!=null){s=(React.createElement("td",null,React.createElement(mydmam.async.pathindex.reactDate,{date:f.date})));
}var e=null;if(f.directory===false){e=(React.createElement(mydmam.async.pathindex.reactExternalPosition,{pathindexkey:g,externalpos:this.props.externalpos}));
}b.push(React.createElement("tr",{key:g},w,l,s,React.createElement("td",null,mydmam.async.pathindex.mtdTypeofElement(c[h].mtdsummary)),React.createElement("td",null,e)));
}return(React.createElement("table",{className:"table table-hover table-condensed"},u,React.createElement("tbody",null,b)));
}});})(window.mydmam.async.navigate);