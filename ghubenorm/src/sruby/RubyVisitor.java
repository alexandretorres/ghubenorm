package sruby;

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
import org.jruby.ast.FCallNode;
import org.jruby.ast.Node;
import org.jruby.ast.SelfNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.AbstractNodeVisitor;

import dao.ConfigDAO;
import dao.DAOInterface;
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
	
	static DAOInterface<MClass> daoMClass = ConfigDAO.getDAO(MClass.class);
	static DAOInterface<MTable> daoMTable = ConfigDAO.getDAO(MTable.class);
	static DAOInterface<MProperty> daoMProp = ConfigDAO.getDAO(MProperty.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	String currentURL;
	public void reset(String url) {
		this.currentURL=url;
		stack.removeAll(stack);
	}
	
	public RubyVisitor(RubyRepo repo){
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
	
	public MClass createClass(ClassNode n,MClass superclazz,boolean isPersistent) {
		
		
		String name = n.getCPath().getName();
		MClass clazz = daoMClass.persit(MClass.newMClass(currentURL,repo.getRepo()).setName(name));
	
		stack.push(clazz);
		repo.getClasses().add(clazz);
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
			if (clazz.isFirstConcretePersistent()) {
				
				AttrAssignNode an = Helper.findAttrAssignNode(n.getBodyNode(), "table_name");		
				String tabname = JRubyInflector.getInstance().tableize(name);
				if (an!=null && an.getReceiverNode() instanceof SelfNode) {				
					tabname=Helper.getValue(an.getArgsNode());
				}
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
		}
		
		//---
		super.visitClassNode(n);
		if (clazz!=null) {
			if (clazz.isPersistent() && !clazz.getProperties().stream().anyMatch(p->p.isPk())) {
				//TODO: not for subclasses!!
				//MProperty idProp = clazz.newPKProperty().setName("<id>").setType("integer"); 				
			}			
			stack.pop();			
			List<ClassNode> subs = repo.subclasses.get(name);
			/*
			if (subs!=null) {
				for (ClassNode sc:subs) {
					MClass sub = createClass(sc, isPersistent);
					sub.setSuperClass(clazz);
				}
			}*/
			//repo.subclasses.remove(name);
		}
		return clazz;
	}
	@Override
	public Object visitClassNode(ClassNode n) {
		Object ret=null;
		String name = n.getCPath().getName();
		String sname="";
		
		if (n.getSuperNode() instanceof INameNode) {
			INameNode sup = (INameNode) n.getSuperNode();
			//String lexname = sup.getLexicalName();
			sname = decodeName(sup); 			
		}
		if (sname.equals("") || sname.equals("ActiveRecord::Base")) {
			ret = createClass(n,null,sname.equals("ActiveRecord::Base"));			
			// Inner classes are not added in resove ref stage. TODO: separated method to read before the resolve
		} else if (stack.isEmpty()){
			List<ClassNode> subs = repo.subclasses.get(sname);
			
			if (subs==null) {
				subs = new ArrayList<ClassNode>();
				repo.subclasses.put(sname, subs); //TODO: remove the :: or process this correctly
			}
			subs.add(n);
		}
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
			}
			
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
		if (!stack.isEmpty() && stack.peek() instanceof MClass && n.getReceiverNode() instanceof SelfNode) {
			MClass clazz = (MClass) stack.peek();
			switch (n.getName()) {
			case "primary_key":
			case "primary_keys":				
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
					}
				}
				
				break;
			}
		}
		return super.visitCallNode(n);
	}
	
	
}
