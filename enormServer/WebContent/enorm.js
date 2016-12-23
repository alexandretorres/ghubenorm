//--- basic functions
String.prototype.hashCode = function() {
  var hash = 0, i, chr, len;
  if (this.length === 0) return hash;
  for (i = 0, len = this.length; i < len; i++) {
    chr   = this.charCodeAt(i);
    hash  = ((hash << 5) - hash) + chr;
    hash |= 0; // Convert to 32bit integer
  }
  return hash;
};
function MyMap() {	
	this.map={};
	this.lid=1;
}
MyMap.prototype.put= function(key,obj) {
	var id;	
	if (key.getKey) {
		id = key.getKey();		
	} else if (key.id)
		id=key.id;
	else 
		key.id = id = this.lid++;
	
	this.map[id]=obj;
	
};
MyMap.prototype.get= function(key) {
	var id;	
	if (key.getKey) {
		id = key.getKey();		
	} else if (key.id)
		id=key.id;
	else 
		return;	
	return this.map[id];
};

function extend(base, sub) {
  var origProto = sub.prototype;
  sub.prototype = Object.create(base.prototype);
  for (var key in origProto)  {
     sub.prototype[key] = origProto[key];
  }
  // The constructor property was set wrong, let's fix it
  Object.defineProperty(sub.prototype, 'constructor', { 
    enumerable: false, 
    value: sub 
  });
}
function getCookie(name) {
	  var value = "; " + document.cookie;
	  var parts = value.split("; " + name + "=");
	  if (parts.length == 2) return parts.pop().split(";").shift();
	}
function createCookie(name,value,days) {
  if (days) {
      var date = new Date();
      date.setTime(date.getTime()+(days*24*60*60*1000));
      var expires = "; expires="+date.toGMTString();
  }
  else var expires = "";
  document.cookie = name+"="+value+expires+"; path=/";
}
function getStorage(name) {
	if (typeof(Storage) !== "undefined") {
		return localStorage.getItem(name);
	} else
		return null;
}
function setStorage(name,value) {
	if (typeof(Storage) !== "undefined") {
		localStorage.setItem(name, value);
	}
}
function coalesce() {
    var len = arguments.length;
    for (var i=0; i<len; i++) {
        if (arguments[i] !== null && arguments[i] !== undefined) {
            return arguments[i];
        }
    }
    return null;
}
function D3MyData(fdata) {
	if ( typeof fdata==='function')
		this.fdata=fdata;
	else
		this.const_data=fdata;
}
D3MyData.prototype.getData = function() {
	var d3data=this;
	return function(d,i) {
		if (d3data.const_data) 
			d3data.data = d3data.const_data;			
		 else 	
			d3data.data = d3data.fdata.call(this,d,i)
	
		return d3data.data ;
	};
}
D3MyData.prototype.keyData = function() {
	var d3data=this;
	return function (d,i,group) {
		var idx = d3data.data.indexOf(d);
		if (idx<0)
			return null;
		return idx;
	};	
}
//-- entities
function MClass() {
	
}
MClass.prototype.getKey = function() {
	return this.fullName+this.filepath;
}
function MProperty() {
	
}
MProperty.prototype.getKey = function() {
	return this.parent.getKey()+"."+this.name;
}
//-- visaulization classes
function Model(repo) {
	this.repo=repo;
	this.diags=new MyMap();
	this.diagram=null;
}
Model.prototype.showDiag = function(clazz) {
	var diag = this.diags.get(clazz);
	if (diag==undefined || diag==null) {
		diag = new Diagram(clazz);		
		diag.newDiagram();
		this.diags.put(clazz,diag);
	}
	this.diagram=diag;
		
};

function Diagram(baseClazz) {
	this.clazz = baseClazz;
	this.nodes=[];
	this.links=[];
	this.reposition = true;
}
Diagram.prototype.indexOfClass = function(clazz) {	
	for (var i=0;i<this.nodes.length;i++) {
		if (this.nodes[i].clazz==clazz)
			return i;
	}
	return -1;
}
Diagram.prototype.nodeOf = function(clazz) {	
	for (var i=0;i<this.nodes.length;i++) {
		if (this.nodes[i].clazz==clazz)
			return this.nodes[i];
	}
	return;
}
Diagram.prototype.newDiagram = function() {	

	var cls = [];
	cls.push(this.clazz);
	var plinks = this.clazz.properties.filter(function(d) {return d.typeClass!=null && getAssociation(d)!=null && !d.embedded});
	plinks.forEach(function(d) {
		if (d.typeClass!=null && !cls.includes(d.typeClass)) 
			cls.push(d.typeClass)
		});
	
			
	this.links = plinks.map(function(d) {
			return {type:"A",source:cls.indexOf(d.parent),target:cls.indexOf(d.typeClass),prop:d}
		});
	//embed
	plinks = this.clazz.properties.filter(function(d) {return d.typeClass!=null && d.embedded});
	plinks.forEach(function(d) {
		if (!cls.includes(d.typeClass)) 
			cls.push(d.typeClass)
		});	
	
	this.links = this.links.concat(plinks.map(function(d) {
			return {type:"E",source:cls.indexOf(d.parent),target:cls.indexOf(d.typeClass),prop:d}
		}));
	//extends
	var cur = this.clazz;
	while(cur.superClass!=null) {
		if (!cls.includes(cur.superClass)) 
			cls.push(cur.superClass)
		
		var gen = cur.generalization.length>0 ? cur.generalization[0] : null;
		var exts = [{type:"G",source:cls.indexOf(cur),target:cls.indexOf(cur.superClass),prop:gen}];
		this.links = this.links.concat(exts);
		cur=cur.superClass;
	}	
	var nodes = this.nodes;
	cls.forEach(function (cl) {
		nodes.push(new Node(cl));
	});	
	return cls;

};
function Node(clazz) {
	this.clazz = clazz;
	this.x=null;
	this.y=null;
}
Node.prototype.getFontStype = function () {
	if (this.clazz.abstract)
		  return "italic"
		return "";
	
}
function Link(source,target,type,prop) {
	this.source = source;
	this.target = target;
	this.type=type;
	this.prop=prop;
	this.coords = {x1:0,y1:0,x2:0,y2:0};
	//{type:"A",source:cls.indexOf(d.parent),target:cls.indexOf(d.typeClass),prop:d}
}
