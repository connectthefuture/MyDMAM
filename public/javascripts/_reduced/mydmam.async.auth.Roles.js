(function(a){a.roleList=function(c){var b=[];for(pos in c){b.push(React.createElement("li",{key:pos},c[pos]));
}return b;};a.Roles=React.createClass({displayName:"Roles",getInitialState:function(){return{rolelist:{}};
},componentWillMount:function(){mydmam.async.request("auth","rolelist",null,function(b){this.setState({rolelist:b.roles});
}.bind(this));},render:function(){var c=this.state.rolelist;var b=[];for(role_key in c){b.push(React.createElement("tr",{key:role_key},React.createElement("td",null,c[role_key].role_name),React.createElement("td",null,React.createElement("ul",{style:{marginLeft:0,marginBottom:0}},a.roleList(c[role_key].privileges)))));
}return(React.createElement("div",null,React.createElement("table",{className:"table table-bordered table-striped table-condensed"},React.createElement("thead",null,React.createElement("tr",null,React.createElement("th",null,i18n("auth.role")),React.createElement("th",null,i18n("auth.role.privileges")))),React.createElement("tbody",null,b))));
}});})(window.mydmam.async.auth);