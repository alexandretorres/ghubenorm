package sruby;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.stream.Stream;

import org.jrubyparser.ast.*;
import org.jrubyparser.rewriter.DefaultFormatHelper;
import org.jrubyparser.rewriter.ReWriteVisitor;
import org.jrubyparser.rewriter.utils.ReWriterContext;

import com.google.common.base.CharMatcher;
public class Helper {
	private static ReWriteVisitor visitor;
	
	/**
	 * return the visitor, with lazy instantiation, but without setting the output. Use getConfig.setOutput
	 * @return
	 */
	private static ReWriteVisitor getVisitor() {		
		if (visitor==null) {			
			visitor = new ReWriteVisitor( new ReWriterContext((PrintWriter)null, "", new DefaultFormatHelper()));			
		}
		return visitor;
	}
	private static String visit(Node n) {
		StringWriter sw = new StringWriter();
		ReWriteVisitor v=getVisitor();
		v.getConfig().setOutput(new PrintWriter(sw));
		n.accept(v);
		return sw.toString();
	}
	static public boolean in(Object a[],Object o) {
		for (Object i:a){
			if (i.equals(o))
				return true;
		}
		return false;
	}
	static public String getName(Node node) {		
		if (node instanceof SymbolNode) {
			return ((SymbolNode)node).getName();		
		} 
		return null;
	}
	static String getValue(Node node) {	
		
		if (node instanceof SymbolNode) {
			return ((SymbolNode)node).getName();	
		} else if (node instanceof NilNode) {
			return null;
		} else if (node instanceof BareKeywordNode) {
			return ((BareKeywordNode)node).getName();	
		} else if (node instanceof StrNode) {			
			String s = ((StrNode) node).getValue();
			CharMatcher matcher = CharMatcher.is('"');
			s = matcher.trimFrom(s);
			return s;
		} else if (node instanceof FixnumNode) {
			return ""+((FixnumNode)node).getValue();
		} else if (node instanceof ListNode) {
			ListNode lst = (ListNode) node;
			if (lst.childNodes().size()==1)
				return getValue(lst.childNodes().get(0));
			else {
				//visitor keeps " and [...
				//return visit(node);
				
				try {
					String ret = lst.childNodes().stream().
							map(n->getValue(n)).
							reduce(null,(a, b) -> (a==null ? b : a+","+ b) );
					return ret;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return "";
			}		
		} else if (node instanceof CallNode) {
			CallNode cn = (CallNode) node;
			return getValue(cn.getReceiver())+"."+cn.getName();
		
		} else if (node instanceof EvStrNode) {			//This is a guess
			return "#"+getValue(((EvStrNode)node).getBody()); 
		} else if (node instanceof NewlineNode) {
			NewlineNode nn = (NewlineNode) node;			
			return "{"+getValue( nn.getNextNode())+"}"; 
		} else if (node instanceof NamedNode) {
			NamedNode nn = (NamedNode) node;
			return nn.getLexicalName();
		} else {//if (node instanceof FixnumNode) {
			return visit(node);
			//System.out.println("baaaa");
		}		
		//return visit(node);
	}
	static public <T> T getHashArgument(ListNode args,String name,Class<T> cl) {
		String res = getHashArgument(args, name);
		if (res==null)
			return null;
		
		if (cl==Integer.class)
			return (T) Integer.decode(res);
		if (cl==Boolean.class)
			return (T)(Boolean) res.equals("true");
		return null;
	}
	static public String getHashArgument(ListNode args,String name) {
		
		Iterator<Node> it = args.childNodes().iterator();
		while (it.hasNext()) {
			String key = getName(it.next());
			Node value = it.next();
			if (name.equals(key)) {
				return getValue(value);
			}
			
					
		}
		/*
		for (Node n:args.childNodes()) {
			
		}*/
		return null;
	}
	static boolean keepScope(Node c) {
		return NewlineNode.class.isInstance(c) ; //or other classes?
	}
	
	static public AttrAssignNode findAttrAssignNode(Node root,String varname) {
		if (root==null)
			return null;
		for (Node n:root.childNodes()) {
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
		}
		/*
		Stream<Node> x = root.childNodes().stream().
			filter(c->Helper.keepScope(c)).flatMap(c->c.childNodes().stream());*/
		return null;
	}
}
