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
	private Dir root;
	/**
	 * Superclass-ClassCode: subclasses waiting for a class definition 
	 */
	Map<String,List<ClassNode>> subclasses = new HashMap<String,List<ClassNode>>();
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
		//TODO: decode :: to package
		
		Optional<MClass> ret = getClasses().stream().filter(cl->cl.getName().equalsIgnoreCase(name)).findFirst();
		return ret.orElse(null);
	}
	public MClass getClazzFromUnderscore(String underscore_name) {
		Optional<MClass> ret = getClasses().stream().filter(
				cl->JRubyInflector.getInstance().underscore(cl.getName()).equalsIgnoreCase(underscore_name)).
				findFirst();
		return ret.orElse(null);
	}
	public void solveRefs(RubyVisitor rv) {
		boolean keep;
		do {
			HashSet<String> removed = new HashSet<String>(); 
			keep=false;
			for (Iterator<String> it=subclasses.keySet().iterator();it.hasNext();) {
				String name = it.next();
				MClass parent = getClazz(name);
				if (parent!=null) {
					List<ClassNode> lst = subclasses.get(name);	
					for (ClassNode n:lst) {
						rv.createClass(n, parent,parent.isPersistent());
					}
					keep=true;
					removed.add(name);
					//it.remove();				
				}
			}
			for (String s:removed) {
				subclasses.remove(s);
			}
		} while (keep && !subclasses.isEmpty());
		
		for (String name:subclasses.keySet()) {			
			for (ClassNode n:subclasses.get(name)) {
				rv.createClass(n, null,false);
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
