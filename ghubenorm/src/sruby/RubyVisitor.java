package sruby;

import static gitget.Log.LOG;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.FCallNode;
import org.jruby.ast.IArgumentNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.Node;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.AbstractNodeVisitor;

import dao.ConfigDAO;
import dao.DAOInterface;
import gitget.Log;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MColumnMapping;
import model.MProperty;
import model.MTable;

/**
 * Visitor that processes ruby files, identifying active record definitions. Notice that
 * this could be a lot simpler if the aim were just to COUNT occurrences. Think about that.
 * @author torres
 *
 */
public class RubyVisitor extends AbstractNodeVisitor<Object> {
	Stack<Object> stack = new Stack<Object>();
	private RubyRepo repo;
	Stack<String> modules = new Stack<String>();
	static DAOInterface<MClass> daoMClass = ConfigDAO.getDAO(MClass.class);
	static DAOInterface<MTable> daoMTable = ConfigDAO.getDAO(MTable.class);
	static DAOInterface<MProperty> daoMProp = ConfigDAO.getDAO(MProperty.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	String currentURL;
	public void reset(String url) {
		this.currentURL=url;
		stack.removeAll(stack);
		modules.clear();
	}
	
	public RubyVisitor(RubyRepo repo){
		this.repo=repo;
	}
	@Override
	protected Object defaultVisit(Node node) {			
		visitChildren(node); 
		return null;
	}
	@Override
	public Object visitModuleNode(ModuleNode node) {
		String name = decodeName(node.getCPath());
		modules.push(name);
		visitChildren(node);
		modules.pop();
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
	private String decodeName(String context,INameNode n) {
		String name = n.getName();
		if (n instanceof Colon2Node) {
			Colon2Node c2 = (Colon2Node) n;
			if (c2.getLeftNode() instanceof INameNode) {
				name = decodeName((INameNode)c2.getLeftNode())+"::"+name;
			}
		} else if (n instanceof Colon3Node) {
			//Colon3Node c3 = (Colon3Node)n;
			return n.getName();
		} else if (context!=null && context.length()>0) {
			name = context+"::"+name;
		}
		return name;
	}
	
	public MClass createClass(ClassNode n) {
		String dname = decodeName( n.getCPath());
		if (!modules.isEmpty())
			dname = String.join("::", (CharSequence[]) modules.toArray(new CharSequence[] {})) +"::"+dname;
		String path=null; //TODO: in fact you have to see the module declaration
		if (dname.contains("::")) {
			path = dname.replace("::",".");
			path = path.substring(0, path.lastIndexOf("."));			
		}		
		String name = n.getCPath().getName();
		MClass clazz = daoMClass.persit(MClass.newMClass(currentURL,repo.getRepo()).setName(name));
		clazz.setPackageName(path);
		repo.getClasses().add(clazz);
		repo.incomplete.put(clazz,n);
		return clazz;
	}
	/**
	 * SE superclasse for abstrata e for persistente, filho tem tabela
	 * SE superclasse não for abstrata for persistente, filho não tem tabela
	 * SE superclasse não for persistente, e filho persistente, tem tabela -so que não, 
	 *   pq tem que extender active:record!
	 * @return
	 */
	public boolean isFirstConcretePersistent(MClass clazz) {
		MClass superClass = clazz.getSuperClass();
		if (superClass==null) {
			return clazz.isPersistent() && (!clazz.isAbstract() || clazz.getPersistence().getSource()!=null);
		}
		if (superClass.isPersistent()) {
			return !isFirstConcretePersistent(superClass);			
		}
		return false;
	}
	public void visitClass(MClass clazz,ClassNode n,MClass superclazz,boolean isPersistent) {	
		repo.incomplete.remove(clazz);
		stack.push(clazz);
		
		clazz.setSuperClass(superclazz);
		//self.table_name
		if (isPersistent) {			
			AttrAssignNode abstrN = Helper.findAttrAssignNode(n.getBodyNode(), "abstract_class");
			boolean abstr=false;
			if (abstrN!=null && abstrN.getReceiverNode() instanceof SelfNode) {				
				abstr=Helper.getValue(abstrN.getArgsNode()).equals("true");
			}
			clazz.setAbstract(abstr);
			
			//Abstract class on Ruby means Mapped Super class, and Horizontal Inheritance
			clazz.setPersistent();	
			
			String tabname = null;
			
			if (isFirstConcretePersistent(clazz)) {
				tabname = JRubyInflector.getInstance().tableize(clazz.getName());
			}
			AttrAssignNode an = Helper.findAttrAssignNode(n.getBodyNode(), "table_name");
			if (an!=null && an.getReceiverNode() instanceof SelfNode) {				
				tabname=Helper.getValue(an.getArgsNode());
			}
			
			if (tabname!=null) {				
				MTable tab = repo.getTable(tabname);
				if (tab==null)
					tab=daoMTable.persit(clazz.newTableSource(tabname));
				else
					clazz.getPersistence().setDataSource(tab);
				// add properties from the "class"
				for (MColumnDefinition col:tab.getColumns()) {
					daoMProp.persit(
							clazz.newProperty().
							setName(col.getName()).
							setType(col.getColummnDefinition()).
							setMin(col.isNullableDef() ? 0 :1).
							setColumnMapping(MColumnMapping.newMColumnMapping(col)));
					
				}
			
					
			}
			AttrAssignNode pkNode = Helper.findAttrAssignNode(n.getBodyNode(), "primary_key");
			if (pkNode==null)
				 pkNode = Helper.findAttrAssignNode(n.getBodyNode(), "primary_keys");
			if (pkNode!=null)
				createPK(clazz, pkNode);
			
		}
		
		//---
		super.visitClassNode(n);
		if (clazz!=null) {
			if (clazz.isPersistent() && !clazz.getProperties().stream().anyMatch(p->p.isPk())) {
				//TODO: not for subclasses!!
				//MProperty idProp = clazz.newPKProperty().setName("<id>").setType("integer"); 				
			}			
			stack.pop();			
			/*List<ClassNode> subs = repo.subclasses.get(clazz.getName());
			
			if (subs!=null) {
				for (ClassNode sc:subs) {
					MClass sub = createClass(sc, isPersistent);
					sub.setSuperClass(clazz);
				}
			}*/
			//repo.subclasses.remove(name);
		}
		
	}
	@Override
	public Object visitClassNode(ClassNode n) {
		MClass ret=null;
		String name = n.getCPath().getName();
		String sname="";
		ret = createClass(n);
		if (n.getSuperNode() instanceof INameNode) {
			INameNode sup = (INameNode) n.getSuperNode();
			//String lexname = sup.getLexicalName();
			sname = decodeName(ret.getPackageName(),sup); 			
		}
		if (sname.equals("") || sname.equals("ActiveRecord::Base")) {
			visitClass(ret,n,null,sname.equals("ActiveRecord::Base"));
			sname=null;
			// Inner classes are not added in resove ref stage. TODO: separated method to read before the resolve
		} else if (stack.isEmpty()) {
			int idx = sname.lastIndexOf(":");
			idx= idx<0 ? idx=sname.indexOf(".") : idx;
			String superName = idx<0 ? sname : sname.substring(idx+1);
			List<MClass> subs = repo.subclasses.get(superName);
			
			if (subs==null) {
				subs = new ArrayList<MClass>();
				repo.subclasses.put(superName, subs); 
			}
			subs.add(ret);
		}
		ret.setSuperClassName(sname);
		return ret;
	}
	
	@Override
	public Object visitAttrAssignNode(AttrAssignNode iVisited) {
		// TODO Auto-generated method stub
		return super.visitAttrAssignNode(iVisited);
	}
	@Override
	/**
	 * call to method on this
	 */
	public Object visitFCallNode(FCallNode n) {
		//{"belongs_to","has_and_belongs_to_many","has_one","has_many"};
		if (!stack.isEmpty() && stack.peek() instanceof MClass) {
			MClass clazz = (MClass) stack.peek();
			switch (n.getName()) {
				case "has_one":
					repo.visitors.push(new VisitHasOne(repo,clazz,n));	
					break;
				case "belongs_to":
					//TODO: relationship to superclass of another class
					repo.visitors.push(new VisitBelongsTo(repo,clazz,n));					
					break;
				case "has_many":
					repo.visitors.push(new VisitHasMany(repo,clazz,n));
					break;
				case "has_and_belongs_to_many":
					repo.visitors.push(new VisitHasAndBelongsTo(repo,clazz,n));
					break;
				case "composed_of":
					repo.visitors.push(new VisitComposedOf(repo,clazz,n));
					break;
				case "attr_reader": case "attr_writer": case "attr_accessor":
					for (Node cn:n.getArgsNode().childNodes()) {
						String propName = Helper.getValue(cn);
						daoMProp.persit(clazz.newProperty().setName(propName));
					}
					break;
				case "default_scope":
					repo.visitors.push(new VisitDefaultScope(repo,clazz,n));
					
					
					break;
			}
			
		}
		if (n.getName().equals("require_relative") /*|| n.getName().equals("require")*/) {
			String st = currentURL+" has "+n.getName()+": ";
			
			for (Node cn:n.getArgsNode().childNodes()) {
				String propName = Helper.getValue(cn);
				st+=propName+" ";
				try {
					if (propName.lastIndexOf(".")<=propName.lastIndexOf("/")) {
					//if (propName.substring(propName.lastIndexOf("/")).indexOf(".")<0 ) {
						propName = propName+".rb";
					}
					URI uri = new URL(this.currentURL).toURI().resolve(propName);
					//LOG.info(uri.toURL().toString());
					RubyRepoLoader loader = RubyRepoLoader.getInstance();
					loader.pushVisitor();
					try {						
						loader.visitFile(uri.toURL());
					} finally {
						loader.popVisitor();
					}
					
					//TODO: visitFile cannot "reset" our state!
				} catch (MalformedURLException | URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//LOG.info(st);
			//TODO: check if this resource was loaded before continuing
		}
			
		//System.out.println("call F node:"+n.getName());
		return super.visitFCallNode(n);
	}
	/**
	 * call to method on something
	 */
	@Override
	public Object visitCallNode(CallNode n) {
		
		//System.out.println("call node:"+n.getName());
		if (!stack.isEmpty() && stack.peek() instanceof MClass) {
			MClass clazz = (MClass) stack.peek();
			/*switch (n.getName()) {
				case "primary_key":
				case "primary_keys":				
					if (n.getReceiverNode() instanceof SelfNode)
						createPK(clazz,n);
					break;
				}*/
		}
		return super.visitCallNode(n);
	}
	public void createPK(MClass clazz,IArgumentNode n) {
		String pk_value=Helper.getValue(n.getArgsNode());				
		String[] pks = pk_value.split(",");
		for (String pk:pks) {
			MProperty pkProp = null;
			for (MProperty p:clazz.getProperties()) {
				if (p.getName().equalsIgnoreCase(pk)) {
					pkProp = p;
				}
			}
			if (pkProp==null) {
				pkProp = clazz.newPKProperty().setName(pk).setType("integer").setMin(1);						
			} else {
				pkProp.setMin(1).setPk(true);
				if (pkProp.getColumnDef()!=null) {
					if ("primary_key".equals(pkProp.getColumnDef().getColummnDefinition())) {
						pkProp.setType("integer");
						pkProp.getColumnDef().getColumn().setColummnDefinition("integer");
						pkProp.setGenerated();
					}
				}
			}
		}
		
	}
	
	

	
}
