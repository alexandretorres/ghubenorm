package sruby;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.jruby.ast.CallNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.FCallNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.Node;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.AbstractNodeVisitor;

import model.MClass;
import model.MColumn;
import model.MTable;


public class SchemaVisitor extends AbstractNodeVisitor<Object> {
	private RubyRepo repo;
	static String[] dbtypes = new String[] 
			{"string","integer","datetime","boolean","decimal","binary"};
	Stack<Object> stack = new Stack<Object>();
	
	public SchemaVisitor(RubyRepo repo){
		this.repo=repo;
	}
	@Override
	protected Object defaultVisit(Node node) {		
		visitChildren(node);
		return null;
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
		/*
		String sname="";
		
		if (n.getSuperNode() instanceof INameNode) {
			INameNode sup = (INameNode) n.getSuperNode();
			//String lexname = sup.getLexicalName();
			sname = decodeName(sup); 
		}*/
		
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
			
			
			Iterator<Node> it = n.getArgsNode().childNodes().iterator();
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
				//TODO: Fix this new hashnode thing
				ret.setNullable("true".equals(Helper.getHashArgument(hn.getPairs(), "null")));	
				try {
					ret.setLength(Helper.getHashArgument(hn.getPairs(), "limit",Integer.class));	
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				ret.setDefaulValue(Helper.getHashArgument(hn.getPairs(), "default"));
				ret.setPrecision(Helper.getHashArgument(hn.getPairs(), "precision",Integer.class));
				ret.setScale(Helper.getHashArgument(hn.getPairs(), "scale",Integer.class));
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
				ret=createColumn(tab,"integer",n.getArgsNode().childNodes());
				ret.setName(ret.getName()+"_id");
				//TODO:indexes according with http://api.rubyonrails.org/classes/ActiveRecord/ConnectionAdapters/TableDefinition.html#method-i-column
			}else if (name.equals("column") || name.equals("add_column")) {			
				ret=createColumn(tab,null,n.getArgsNode().childNodes());
					
			} else if (Helper.in(dbtypes,name) && top instanceof MTable) {		
				ret=createColumn(tab,name,n.getArgsNode().childNodes());
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
