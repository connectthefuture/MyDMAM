(function(a){a.AbstractFoundedFile=React.createClass({displayName:"AbstractFoundedFile",btnDelete:function(b){b.preventDefault();
this.props.onDelete(this.props.abstract_founded_file);},render:function(){var d=this.props.abstract_founded_file;
var b=this.props.jobs;var e=classNames({error:d.status==="ERROR",warning:d.status==="IN_PROCESSING",info:d.status==="DETECTED"});
var h=[];for(job_key in d.map_job_target){if(b[job_key]==null){continue;}var f=d.map_job_target[job_key];
var g=f.substring(f.indexOf(":")+1,f.length);var c=f.substring(0,f.indexOf(":"));
h.push(React.createElement("span",{key:job_key},React.createElement("i",{className:"icon-folder-close"}),c,React.createElement("span",{className:"pull-right"},React.createElement("i",{className:"icon-cog"}),g),React.createElement(mydmam.async.jobs.Minicartridge,{job:b[job_key]})));
}return(React.createElement("tr",{className:e},React.createElement("td",null,React.createElement(mydmam.async.pathindex.reactStoragePathLink,{storagename:d.storage_name,path:d.path,add_link:d.status!=="PROCESSED"})),React.createElement("td",null,React.createElement(mydmam.async.pathindex.reactDate,{date:d.date})),React.createElement("td",null,React.createElement(mydmam.async.pathindex.reactFileSize,{size:d.size})),React.createElement("td",null,React.createElement(mydmam.async.pathindex.reactDate,{date:d.last_checked})),React.createElement("td",null,i18n("manager.watchfolders.status."+d.status)),React.createElement("td",null,h),React.createElement("td",null,React.createElement("button",{className:"btn btn-mini btn-danger pull-right",onClick:this.btnDelete},React.createElement("i",{className:"icon-remove icon-white"})))));
}});})(window.mydmam.async.watchfolders);