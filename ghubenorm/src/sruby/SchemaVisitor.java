package sruby;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.ClassNode;
import org.jrubyparser.ast.Colon2Node;
import org.jrubyparser.ast.FCallNode;
import org.jrubyparser.ast.HashNode;
import org.jrubyparser.ast.INameNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.SymbolNode;
import org.jrubyparser.util.NoopVisitor;

import model.MClass;
import model.MColumn;
import model.MTable;
import sjava.CompilationUnit;

public class SchemaVisitor extends NoopVisitor {
	private RubyRepo repo;
	static String[] dbtypes = new String[] 
			{"string","integer","datetime","boolean","decimal","binary"};
	Stack stack = new Stack();
	
	public SchemaVisitor(RubyRepo repo){
		this.repo=repo;
	}
	@Override
	protected Object visit(Node parent) {		
		return super.visit(parent);
	}
	private String decodeName(INameNode n) {
		String name = n.getName();
		if (n instanceof Colon2Node) {
			Colon2Node c2 = (Colon2Node) n;
			if (c2.getLeftNode() instanceof INameNode) {
				name = decodeName((INameNode)c2.getLeftNode())+"::"+name;
			}
		}
		return name;
	}
	@Override
	public Object visitClassNode(ClassNode n) {
		Object ret=null;
		String name = n.getCPath().getName();
		String sname="";
		
		if (n.getSuper() instanceof INameNode) {
			INameNode sup = (INameNode) n.getSuper();
			//String lexname = sup.getLexicalName();
			sname = decodeName(sup); 
		}
		
		MClass clazz = MClass.newMClass().setName(name);
		ret = clazz;
	
		
		
		super.visitClassNode(n);
		return ret;
	}
	@Override
	/**
	 * call to method on this
	 */
	public Object visitFCallNode(FCallNode n) {
		MTable table=null;
		//System.out.println("call F node:"+n.getName());
		if (n.getName().equals("create_table")) {
			
			
			Iterator<Node> it = n.getArgs().childNodes().iterator();
			Node nod = it.next();
			String tabname = Helper.getValue(nod);
			if (tabname!=null && tabname.length()>0) {							
				table = MTable.newMTable(tabname);		
				repo.tables.add(table);
			}
			if (table!=null)
				stack.push(table);
			
		}
		super.visitFCallNode(n);
		if (table!=null) {
			//TODO:primary key...
			stack.pop();
		}
		//System.out.println("after  F node:"+n.getName());
		return table;
	}
	/*
	private Map<String, String> toHash(HashNode n) {
		HashMap<String,String> ret = new HashMap<String,String>();
		Iterator<Node> it = n.getListNode().childNodes().iterator();
		while (it.hasNext()) {
			Node name = it.next();
			Node value = it.next();
					
		}
			
		
		
		
		return ret;
	}*/
	private MColumn createColumn(MTable tab,String type,List<Node> args){
		Iterator<Node> it= args.iterator();
		if (tab==null) {
			if (it.hasNext()) {
				String tabName = Helper.getValue(it.next());
				//tab= find tab by name
			} else
				return null;
		}
		MColumn ret=null;
		if (it.hasNext()) {
			ret=tab.addColumn().setName(Helper.getValue(it.next()));
		} else {
			return null;
		}
		if (type==null) 
			type="VARCHAR";
		while(it.hasNext()) {
			Node n = it.next();
			if (n instanceof HashNode) {
				HashNode hn = (HashNode) n;
				ret.setNullable("true".equals(Helper.getHashArgument(hn.getListNode(), "null")));	
				try {
					ret.setLength(Helper.getHashArgument(hn.getListNode(), "limit",Integer.class));	
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				ret.setDefaulValue(Helper.getHashArgument(hn.getListNode(), "default"));
				ret.setPrecision(Helper.getHashArgument(hn.getListNode(), "precision",Integer.class));
				ret.setScale(Helper.getHashArgument(hn.getListNode(), "scale",Integer.class));
			} else {				
				type = Helper.getValue(n);
				
			}
		}
		ret.setColummnDefinition(type);
		return ret;
	}
	/**
	 * call to method on something
	 */
	@Override
	public Object visitCallNode(CallNode n) {
		//t.references ou t.belongs_to, da no mesmo
		//System.out.println("call node:"+n.getName());
		String name = n.getName();
		Object top=null;
		if (!stack.isEmpty()) {
			top = stack.peek();
		}
		MColumn ret=null;
		if (top instanceof MTable) {
			MTable tab = (MTable) top;
			if (name.equals("references") || name.equals("add_reference")) {
				ret=createColumn(tab,"integer",n.getArgs().childNodes());
				ret.setName(ret.getName()+"_id");
				//TODO:indexes according with http://api.rubyonrails.org/classes/ActiveRecord/ConnectionAdapters/TableDefinition.html#method-i-column
			}else if (name.equals("column") || name.equals("add_column")) {			
				ret=createColumn(tab,null,n.getArgs().childNodes());
					
			} else if (Helper.in(dbtypes,name) && top instanceof MTable) {		
				ret=createColumn(tab,name,n.getArgs().childNodes());
				/*
				Iterator<Node> it = n.getArgs().childNodes().iterator();
				Node nod = it.next();
				MColumn col = tab.addColumn().setColummnDefinition("Varchar");
				if (nod instanceof SymbolNode) {
					SymbolNode sn = (SymbolNode) nod;
					col.setName(sn.getName());
				}*/
				
			}
		}
		return super.visitCallNode(n);
	}
	

}
