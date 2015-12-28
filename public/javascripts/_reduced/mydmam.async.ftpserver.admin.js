(function(b){b.hasUserAdminRights=function(){return mydmam.async.isAvaliable("ftpserver","adminoperationuser")&mydmam.async.isAvaliable("ftpserver","groupdomainlists");
};var a=function(){var e="";var c="abcdefghijkmnopqrstuvwxyz23456789";for(var d=0;
d<8;d++){e+=c.charAt(Math.floor(Math.random()*c.length));}return e;};b.AddUser=React.createClass({displayName:"AddUser",getInitialState:function(){return{groups:null,domains:null,display_password_generator:false,generated_password:null,actual_user:null};
},componentWillMount:function(){if(this.props.params.userid==null){mydmam.async.request("ftpserver","groupdomainlists",{},function(c){this.setState({groups:c.groups,domains:c.domains});
}.bind(this));}},onAddUserBtnClick:function(){var d=null;var c=this.props.params.userid;
if(c==null){d={user_name:React.findDOMNode(this.refs.user_name).value,clear_password:React.findDOMNode(this.refs.password).value,group_name:React.findDOMNode(this.refs.group).value,domain:React.findDOMNode(this.refs.domain).value,operation:"CREATE"};
}else{d={user_id:c,clear_password:React.findDOMNode(this.refs.password).value,operation:"CH_PASSWORD"};
}document.body.style.cursor="wait";mydmam.async.request("ftpserver","adminoperationuser",d,function(e){document.body.style.cursor="auto";
if(this.props.params.userid==null){React.findDOMNode(this.refs.user_name).value=e.user_name;
}this.setState({done:e.done});}.bind(this));},toogleBtnDisplayGeneratePasswordForm:function(){var c=a();
this.setState({display_password_generator:!this.state.display_password_generator,generated_password:c});
React.findDOMNode(this.refs.password).value=c;},generatePasswordBtn:function(){var c=a();
this.setState({generated_password:c});React.findDOMNode(this.refs.inputgeneratedpassword).value=c;
React.findDOMNode(this.refs.password).value=c;},render:function(){var j=this.props.params.userid;
var k=mydmam.async.FormControlGroup;var f=null;if(this.state.display_password_generator){f=(React.createElement(k,{label:"Generate password"},React.createElement("div",{className:"input-append"},React.createElement("input",{type:"text",disabled:"disabled",readOnly:"readonly",ref:"inputgeneratedpassword",defaultValue:this.state.generated_password}),React.createElement("button",{className:"btn",type:"button",onClick:this.generatePasswordBtn},React.createElement("i",{className:"icon-repeat"})))));
}var d=classNames({btn:true,active:this.state.display_password_generator});var h=function(m){var n=[];
n.push(React.createElement(k,{key:"1"},React.createElement("button",{type:"submit",className:"btn btn-success"},React.createElement("i",{className:"icon-ok icon-white"})," ",m)));
if(j!=null){n.push(React.createElement(k,{key:"2"},React.createElement("a",{type:"cancel",className:"btn btn-info",href:"#ftpserver"},React.createElement("i",{className:"icon-th-list icon-white"})," ",i18n("ftpserver.adduser.goback"))));
}return n;};var c=function(m,n){if(this.state.done!=null){if(this.state.done){return(React.createElement(mydmam.async.AlertInfoBox,{title:m}));
}else{return(React.createElement(mydmam.async.AlertErrorBox,{title:i18n("ftpserver.adduser.warning")},n));
}}return null;}.bind(this);if(j==null){var e=this.state.groups;var l=this.state.domains;
if(e==null|l==null){return(React.createElement(mydmam.async.PageLoadingProgressBar,null));
}var i=[];for(pos in e){i.push(React.createElement("option",{key:pos,value:e[pos]},e[pos]));
}var g=[];for(pos in l){g.push(React.createElement("option",{key:pos,value:l[pos]},l[pos]));
}return(React.createElement("div",null,c(i18n("ftpserver.adduser.iscreated"),i18n("ftpserver.adduser.cantcreate")),React.createElement("form",{className:"form-horizontal",onSubmit:this.onAddUserBtnClick},React.createElement(k,{label:i18n("ftpserver.adduser.username")},React.createElement("input",{type:"text",placeholder:i18n("ftpserver.adduser.username"),ref:"user_name"})),React.createElement(k,{label:"Password"},React.createElement("div",{className:"input-append"},React.createElement("input",{type:"password",placeholder:i18n("ftpserver.adduser.password"),ref:"password",defaultValue:this.state.generated_password}),React.createElement("button",{className:d,type:"button",onClick:this.toogleBtnDisplayGeneratePasswordForm},React.createElement("i",{className:"icon-arrow-down"})))),f,React.createElement(k,{label:i18n("ftpserver.adduser.group")},React.createElement("select",{ref:"group"},i)),React.createElement(k,{label:i18n("ftpserver.adduser.domain")},React.createElement("select",{ref:"domain"},g)),h(i18n("ftpserver.adduser.create")))));
}else{return(React.createElement(mydmam.async.PageHeaderTitle,{title:i18n("ftpserver.chpassword.title"),fluid:"true"},c(i18n("ftpserver.chpassword.updated"),i18n("ftpserver.chpassword.noupdated")),React.createElement("form",{className:"form-horizontal",onSubmit:this.onAddUserBtnClick},React.createElement(k,{label:i18n("ftpserver.adduser.password")},React.createElement("div",{className:"input-append"},React.createElement("input",{type:"password",placeholder:i18n("ftpserver.adduser.password"),ref:"password",defaultValue:this.state.generated_password}),React.createElement("button",{className:d,type:"button",onClick:this.toogleBtnDisplayGeneratePasswordForm},React.createElement("i",{className:"icon-arrow-down"})))),f,h(i18n("ftpserver.chpassword.update")))));
}}});mydmam.routes.push("ftpserver-editUser","ftpserver/edit/:userid",b.AddUser,[{name:"ftpserver",verb:"adminoperationuser"},{name:"ftpserver",verb:"groupdomainlists"}]);
})(window.mydmam.async.ftpserver);