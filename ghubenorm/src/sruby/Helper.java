package sruby;
import static gitget.Log.LOG;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.DNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.RationalNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.types.INameNode;
import org.jruby.util.KeyValuePair;

import com.google.common.base.CharMatcher;

import gitget.Log;

public class Helper {
	
	static public boolean in(Object a[],Object o) {
		for (Object i:a){
			if (i.equals(o))
				return true;
		}
		return false;
	}
	static public String getName(Node node) {		
		if (node instanceof INameNode) {
			return ((INameNode)node).getName();
		}
		return null;
	}
	static String getValue(Node node) {	
		
		if (node instanceof SymbolNode) {
			return ((SymbolNode)node).getName();	
		} else if (node instanceof NilNode) {
			return null;
		} else if (node instanceof KeywordArgNode) {
			return ((KeywordArgNode)node).toString();	
		} else if (node instanceof StrNode) {			
			String s = ((StrNode) node).getValue().toString();
			CharMatcher matcher = CharMatcher.is('"');
			s = matcher.trimFrom(s);
			return s;
		} else if (node instanceof FixnumNode) {
			return ""+((FixnumNode)node).getValue();
		} else if (node instanceof ArrayNode || node instanceof ZArrayNode) { //TODO: Maybe it should be ArrayNode OR ZArrayNode. ListNode is too broad
			ListNode lst = (ListNode) node;
			if (lst.children().length==1)
				return getValue(lst.children()[0]);
			else {
				//visitor keeps " and [...
				//return visit(node);
				
				try {
					List<Node> children = Arrays.asList(lst.children());
					String ret = children.stream().
							map(n->getValue(n)).
							reduce(null,(a, b) -> (a==null ? b : a+","+ b) );
					return ret;
				} catch (Exception ex) {
					Log.log(RubyRepoLoader.getCurrentRepo(),Level.SEVERE,ex.getMessage(),ex);						
				}
				return "";
			}	
		} else if (node instanceof DNode) {
			DNode dnod = (DNode) node;
			try {
				List<Node> children = Arrays.asList(dnod.children());
				String ret = children.stream().
						map(n->getValue(n)).
						reduce(null,(a, b) -> (a==null ? b : a+ b) );
				return ret;
			} catch (Exception ex) {
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.SEVERE,ex.getMessage(),ex);						
			}
			return "";
		} else if (node instanceof CallNode) {
			CallNode cn = (CallNode) node;
			return getValue(cn.getReceiverNode())+"."+cn.getName();
		
		} else if (node instanceof EvStrNode) {			//This is a guess
			return "#"+getValue(((EvStrNode)node).getBody()); 
		} else if (node instanceof NewlineNode) {
			NewlineNode nn = (NewlineNode) node;			
			return "{"+getValue( nn.getNextNode())+"}"; 
		} else if (node instanceof LiteralNode) { 
			LiteralNode nn = (LiteralNode) node;
			return nn.getName();  
		} else if (node instanceof INameNode) {
			return ((INameNode) node).getName();
		} else if (node instanceof HashNode) {// this is not correct. GetName strips a lot of info. 
			HashNode hn = (HashNode) node; 
			StringBuffer ret = new StringBuffer();
			if (hn.getPairs().size()>1)
				ret.append("{");
			boolean first=true;
			for (KeyValuePair<Node, Node> pair:hn.getPairs()) {	
				if (first) {
					first=false;
				} else {
					ret.append(",");
				}
				ret.append(getValue(pair.getKey()));  //This does not GENERATE code...
				ret.append("=>");
				ret.append(getValue(pair.getValue()));
				
			}
			if (hn.getPairs().size()>1)
				ret.append("}");
			return ret.toString();
		} else if (node instanceof FloatNode) {
			return Double.toString(((FloatNode)node).getValue());
		} else if (node instanceof RationalNode) {
			RationalNode rn = (RationalNode) node;
			StringBuffer buf = new StringBuffer();
			buf.append(rn.getNumerator());
			buf.append("/");
			buf.append(rn.getDenominator());	
			return buf.toString();		
		} else {
			LOG.warning("could not get value for ruby node of type "+node.getClass());
			return "";  //visit(node);
			
		}		
		//return visit(node);
	}
	static public Node getHashValue(List<KeyValuePair<Node, Node>> list,String name) {
		 return list.stream().filter(p->Helper.getName(p.getKey()).equals(name)).findFirst().map(p->p.getValue()).orElse(null);
	}
	static public <T> T getHashArgument(List<KeyValuePair<Node, Node>> list,String name,Class<T> cl) {
		String res = getHashArgument(list, name);
		if (res==null)
			return null;
		
		if (cl==Integer.class)
			return (T) Integer.decode(res);
		if (cl==Boolean.class)
			return (T)(Boolean) res.equals("true");
		return null;
	}
	static public String getHashArgument(List<KeyValuePair<Node, Node>> list,String name) {
		for (KeyValuePair<Node, Node> pair:list) {
			String key = getName(pair.getKey());
			if (name.equals(key)) {
				return getValue(pair.getValue());
			}
		}
		
		return null;
	}
	static boolean keepScope(Node c) {
		return NewlineNode.class.isInstance(c) ; //or other classes?
	}
	static public AttrAssignNode getAttrAssignNode(Node n,String varname) {
		if (n instanceof AttrAssignNode) {
			AttrAssignNode an = (AttrAssignNode) n;
			String aname = an.getName();
			CharMatcher matcher = CharMatcher.is('=');
			aname = matcher.trimFrom(aname);
			if (varname.equals(aname)) {
				return an;
			}
		} else if (keepScope(n)) {
			AttrAssignNode an = findAttrAssignNode(n, varname);
			if (an!=null)
				return an;
		}
		return null;
	}
	static public AttrAssignNode findAttrAssignNode(Node root,String varname) {
		if (root==null)
			return null;
		AttrAssignNode ret = getAttrAssignNode(root,varname);
		if (ret!=null)
			return ret;
		for (Node n:root.childNodes()) {
			ret = getAttrAssignNode(n,varname);
			if (ret!=null)
				return ret;
			/*if (n instanceof AttrAssignNode) {
				AttrAssignNode an = (AttrAssignNode) n;
				String aname = an.getName();
				CharMatcher matcher = CharMatcher.is('=');
				aname = matcher.trimFrom(aname);
				if (varname.equals(aname)) {
					return an;
				}
			} else if (keepScope(n)) {
				AttrAssignNode an = findAttrAssignNode(n, varname);
				if (an!=null)
					return an;
			}*/
		}
		
		return null;
	}
	public static List<Node> safeGetChilds(Node n) {
		List<Node> ret=null;
		if (n!=null) {
			ret = n.childNodes();
			
		}
		if (ret==null) {
			ret = Collections.emptyList();
		}
		return ret;
		
	}
}
