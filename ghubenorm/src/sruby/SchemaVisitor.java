package sruby;

import static gitget.Log.LOG;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

import org.jruby.ast.CallNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.FCallNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.Node;
import org.jruby.ast.TrueNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.AbstractNodeVisitor;

import dao.ConfigDAO;
import dao.DAOInterface;
import gitget.Log;
import model.MColumn;
import model.MDefinition;
import model.MDefinitionType;
import model.MTable;


public class SchemaVisitor extends AbstractNodeVisitor<Object> {
	/**
	 * Ruby pluralize:
	 * gem install activesupport
	 * or
	 * jruby -S gem install activesupport
	 * irb
	 * or
	 * jirb 
	 * require 'active_support/inflector'
	 * "<string>".pluralize(3)
	 * @author torres
	 *
	 */
	private RubyRepo repo;
	
	static String[] dbtypes = new String[] 
			{"string","integer","datetime","timestamp","date","time","boolean","decimal","float",
					"binary","text","primary_key"};
	Stack<Object> stack = new Stack<Object>();
	static DAOInterface<MTable> daoMTable = ConfigDAO.getDAO(MTable.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	public SchemaVisitor(RubyRepo repo){
		this.repo=repo;
	}
	@Override
	protected Object defaultVisit(Node node) {		
		visitChildren(node);
		return null;
	}
	private String decodeName(INameNode n) {
		String name = n.getName().asJavaString();
		if (n instanceof Colon2Node) {
			Colon2Node c2 = (Colon2Node) n;
			if (c2.getLeftNode() instanceof INameNode) {
				name = decodeName((INameNode)c2.getLeftNode())+"::"+name;
			}
		}
		return name;
	}
	
	/**
	 * Faz sentido isso?
	 */
	@Override
	public Object visitClassNode(ClassNode n) {		
		return null;
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
				table = daoMTable.persist(MTable.newMTable(repo.getRepo(),tabname));		
				repo.getTables().add(table);
			}
			if (table!=null)
				stack.push(table);
			
		} else if (n.getName().equals("add_index")) {
			Iterator<Node> it = n.getArgsNode().childNodes().iterator();
			Node nod = it.next();
			String tabname = Helper.getValue(nod);
			MTable tab = repo.getTable(tabname);
			if (tab!=null) {
				nod = it.next();
				String tmp = Helper.getValue(nod);
				if (tmp != null) { // we´ve found NIL at one project... can´t figure out why ruby acepts NIL here
					String[] colNames = tmp.split(",");
					MColumn[] cols = new MColumn[colNames.length];
					for (int i=0;i<colNames.length;i++) {
						cols[i] = tab.findColumn(colNames[i]);
					}
					if (it.hasNext())
						nod = it.next();
					else 
						nod=null;
					addIndex(nod, tab, cols);
				}
				
			}
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
		return createColumn(tab,type,null,args);
	}
	private MColumn createColumn(MTable tab,String type,String name,List<Node> args){
		Iterator<Node> it= args.iterator();
		if (tab==null) {
			if (it.hasNext()) {
				String tabName = Helper.getValue(it.next());
				//tab= find tab by name
			} else
				return null;
		}
		MColumn ret=null;
		if (name==null && it.hasNext()) {
			name = Helper.getValue(it.next());
		}
		if (name!=null) {
			ret= daoColumn.persist(tab.addColumn().setName(name));
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
					Log.log(RubyRepoLoader.getCurrentRepo(),Level.INFO,tab.getName()+"."+ret.getName()+":"+ ex.getMessage(),ex);	
				}
				//String index = Helper.getHashArgument(hn.getPairs(), "index");
				Node nidx = Helper.getHashValue(hn.getPairs(), "index");
				if (nidx!=null) {
					addIndex(nidx,tab, ret);
				}
				ret.setDefaultValue(Helper.getHashArgument(hn.getPairs(), "default"));
				try {
					ret.setPrecision(Helper.getHashArgument(hn.getPairs(), "precision",Integer.class));
				} catch (Exception ex) {}
				try {
					ret.setScale(Helper.getHashArgument(hn.getPairs(), "scale",Integer.class));
				} catch (Exception ex) {}
				boolean polymorphic = "true".equals(Helper.getHashArgument(hn.getPairs(), "polymorphic"));	
				if (polymorphic) {
					daoColumn.persist(tab.addColumn().setName(JRubyInflector.instance.polymorphicTypeName(ret.getName()) ));
				}
			} else {				
				type = Helper.getValue(n);
				
			}
		}
		ret.setColumnDefinition(type);
		return ret;
	}
	private MDefinition addIndex(Node nidx,MTable tab,MColumn... cols) {		
		MDefinition def=null;
		if (nidx==null || nidx instanceof TrueNode) {
			return tab.newIndex(cols);
		} else if (nidx instanceof HashNode){
			//KeyValuePair<Node, Node> pair = hn.getPairs().stream().filter(p->Helper.getName(p.getKey()).equals("index")).findFirst().orElse(null);
			//Node indexNode = pair.getValue();
			if (nidx instanceof HashNode) {
				HashNode ihn = (HashNode) nidx;
				String name = Helper.getHashArgument(ihn.getPairs(),"name");
				String unique = Helper.getHashArgument(ihn.getPairs(),"unique");
				def = tab.newIndex(cols).setName(name);
				if ("true".equals(unique))
					def.setType(MDefinitionType.UNIQUE);
				
				return def;
			}
		} else {
			LOG.warning("Unknown index node type:"+nidx.getClass());
		}	
		return def;
	}
	/**
	 * call to method on something
	 */
	@Override
	public Object visitCallNode(CallNode n) {
		//t.references ou t.belongs_to, da no mesmo
		//System.out.println("call node:"+n.getName());
		String name = n.getName().asJavaString();
		Object top=null;
		if (!stack.isEmpty()) {
			top = stack.peek();
		}
		Object ret=null;
		if (top instanceof MTable) {
			MTable tab = (MTable) top;
			if (name.equals("references") || name.equals("add_reference") || name.equals("add_belongs_to") || name.equals("belongs_to")) {
				MColumn c=createColumn(tab,"integer",n.getArgsNode().childNodes());
				c.setName(JRubyInflector.instance.foreignKey(c.getName()));  /*+"_id"*/
				ret=c;
				//TODO:indexes according with http://api.rubyonrails.org/classes/ActiveRecord/ConnectionAdapters/TableDefinition.html#method-i-column
			} else if (name.equals("column") || name.equals("add_column")) {			
				ret=createColumn(tab,name,n.getArgsNode().childNodes());				
			} else if (name.equals("timestamps") ) {	
				MColumn[] rets = new MColumn[2];
				rets[0]=createColumn(tab,"timestamp","created_at",Helper.safeGetChilds(n.getArgsNode()));	
				rets[1]=createColumn(tab,"timestamp","updated_at",Helper.safeGetChilds(n.getArgsNode()));
				ret=rets;
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
