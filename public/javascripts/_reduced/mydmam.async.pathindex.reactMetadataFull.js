(function(a){a.reactMetadataFull=React.createClass({displayName:"reactMetadataFull",render:function(){if(!this.props.mtdsummary|!this.props.reference){return null;
}var e=this.props.mtdsummary;var d=this.props.reference;var b=md5(d.storagename+":"+d.path);
var h="";var k=null;if(e.master_as_preview){h=e.mimetype.substring(0,e.mimetype.indexOf("/"));
var c=d.path.substring(d.path.lastIndexOf("."),d.path.length);k=a.metadatas.getFileURL(b,"master_as_preview","default"+c);
}var f=null;if(e.previews){var g=e.previews;if((g.video_lq_pvw!=null)|(g.video_sd_pvw!=null)|(g.video_hd_pvw!=null)|(h=="video")){f=(React.createElement(a.metadatas.Video,{file_hash:b,mtdsummary:e,reference:d,master_as_preview_url:k}));
}else{if((g.audio_pvw!=null)|(h=="audio")){f=(React.createElement(a.metadatas.Audio,{file_hash:b,mtdsummary:e,reference:d,master_as_preview_url:k}));
}else{if((g.full_size_thumbnail!=null)|(g.cartridge_thumbnail!=null)|(g.icon_thumbnail!=null)){f=(React.createElement(a.metadatas.Image,{file_hash:b,previews:g}));
}}}}else{if(h=="video"){f=(React.createElement(a.metadatas.Video,{file_hash:b,mtdsummary:e,reference:d,master_as_preview_url:k}));
}else{if(h=="audio"){f=(React.createElement(a.metadatas.Audio,{file_hash:b,mtdsummary:e,reference:d,master_as_preview_url:k}));
}}}var j=[];if(e.summaries){for(var i in e.summaries){j.push(React.createElement("blockquote",{key:i,style:{marginTop:"1em"}},React.createElement("p",null,e.summaries[i]),React.createElement("small",null,i)));
}}return(React.createElement("div",null,f,j));}});})(window.mydmam.async.pathindex);