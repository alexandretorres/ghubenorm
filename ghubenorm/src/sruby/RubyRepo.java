package sruby;

import static gitget.Log.LOG;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.jruby.ast.ClassNode;

import common.LateVisitor;
import common.Util;
import gitget.Dir;
import gitget.Log;
import model.Language;
import model.MAssociation;
import model.MAttributeOverride;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MJoinColumn;
import model.MOverride;
import model.MProperty;
import model.MTable;
import model.Repo;

public class RubyRepo {
	private Repo repo;
	Stack<LateVisitor> visitors = new Stack<LateVisitor>() ;
	Map<MClass,ClassNode> incomplete = new HashMap<MClass,ClassNode>();
	private Dir root;
	/**
	 * Superclass-ClassCode: subclasses waiting for a class definition 
	 */
	Map<String,List<MClass>> subclasses = new HashMap<String,List<MClass>>();
	public RubyRepo(Repo repo) {
		this.repo=repo;
	}
	public RubyRepo() {
		repo = new Repo(Language.RUBY);
	}
	
	public Set<MClass> getClasses() {
		return repo.getClasses();
	}
	@SuppressWarnings("unchecked")
	public Set<MTable> getTables() {
		return (Set) repo.getDataSources();
	}
	public MTable getTable(String name) {
		Optional<MTable> ret = getTables().stream().filter(tab->tab.getName().equalsIgnoreCase(name)).findFirst();
		return ret.orElse(null);
	}
	public MClass getClazz(String name) {
		final String cname;
		if (name.contains("::")) {
			name = name.replace("::",".");
			int idx = name.lastIndexOf(".");
			String path = name.substring(0, idx);
			cname = name.substring(idx+1);
			
			Optional<MClass> ret = getClasses().stream().filter(cl->cl.getName().equalsIgnoreCase(cname) && path.equals(cl.getPackageName())).findFirst();
			if (ret.isPresent())
				return ret.get();			
		} else
			cname =name;
		Optional<MClass> ret = getClasses().stream().filter(cl->cl.getName().equalsIgnoreCase(cname)).findFirst();
		return ret.orElse(null);
		
	}
	public MClass getClazzFromUnderscore(String underscore_name) {
		Optional<MClass> ret = getClasses().stream().filter(
				cl->JRubyInflector.getInstance().underscore(cl.getName()).equalsIgnoreCase(underscore_name)).
				findFirst();
		return ret.orElse(null);
	}
	private void completeClasses(RubyVisitor rv) {
		Map<MClass, ClassNode> curList = incomplete;
		for (MClass cl:curList.keySet()) {
			if (cl.getSuperClassName()!=null) {
				MClass parent = getClazz(cl.getSuperClassName());
				
				ClassNode node = incomplete.get(cl);
				if (node!=null)
					rv.visitClass(cl, node, null, false);
			}
		}
		
	}
	/**
	 * visit pending classes on the sublass map
	 * preconditions: all classes created at this point
	 * @param rv
	 */
	public void solveRefs(RubyVisitor rv) {
		boolean keep; 
		do {
			HashSet<String> removed = new HashSet<String>(); 
			keep=false;
			for (Iterator<String> it=subclasses.keySet().iterator();it.hasNext();) {
				String supername = it.next();
				MClass parent = getClazz(supername); 
				boolean wait = parent!=null && !Util.isNullOrEmpty(parent.getSuperClassName()) && incomplete.containsKey(parent);
				if (!wait) {
					List<MClass> lst = subclasses.get(supername);	
					for (MClass c:lst) {
						ClassNode n = incomplete.get(c);
						if (n!=null)
							rv.visitClass(c,n, parent,parent==null ? false : parent.isPersistent());
					}
					keep=true;
					removed.add(supername);
					//it.remove();		
				}				
			}
			for (String s:removed) {
				subclasses.remove(s);
			}
		} while (keep && !subclasses.isEmpty());
		
		for (String name:subclasses.keySet()) {			
			for (MClass c:subclasses.get(name)) {
				ClassNode n = incomplete.get(c);
				if (n!=null)
					rv.visitClass(c,n, null,false);
			}
		}
		subclasses.clear();
		for (LateVisitor v:visitors) {
			v.exec();
		}		
	}
	public void listTables() {
		for (MTable t:getTables()) {
			System.out.println("Table "+t.getName());
			for (MColumn c:t.getColumns()) {
				System.out.println("	"+c.getName()+":"+c.getColummnDefinition());
			}
		}
	}

	public Repo getRepo() {
		return repo;
	}
	public void setRepo(Repo repo) {
		this.repo = repo;
	}
	public Dir getRoot() {
		return root;
	}
	public void setRoot(Dir root) {
		this.root = root;
	}
	
}
