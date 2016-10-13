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
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
	private MClass getClazz(String name) {
		return getClazz(null,name);
	}
	public String formatPackage(String pak) {		
		pak = pak==null ? "" : pak; 
		pak = pak.replaceAll("::", ".");
		return pak;
	}
	public MClass getClazz(MClass from,String name) {
		final String cname;
		final String path;
		if (name.contains("::") || name.contains(".")) {
			name = name.replace("::",".");
			int idx = name.lastIndexOf(".");
			path = name.substring(0, idx);
			cname = name.substring(idx+1);
			
			/*Optional<MClass> ret = getClasses().stream().filter(cl->cl.getName().equalsIgnoreCase(cname) && path.equals(cl.getPackageName())).findFirst();
			if (ret.isPresent())
				return ret.get();	*/		
		} else {
			if (from!=null) {
				path = formatPackage(from.getPackageName());
			} else 
				path="";
			cname =name;
			/*Optional<MClass> ret = getClasses().stream().filter(cl->cl.getName().equalsIgnoreCase(cname) && (cl.getPackageName()==null || "".equals(cl.getPackageName()))).findFirst();
			if (ret.isPresent())
				return ret.get();	*/
		}
		
		List<MClass> lst = getClasses().stream().filter(cl->cl.getName().equalsIgnoreCase(cname)).collect(Collectors.toList());
		return pickClassFromList(path, lst);
		/*Optional<MClass> ret = getClasses().stream().filter(cl->cl.getName().equalsIgnoreCase(cname)).findFirst();
		return ret.orElse(null);*/
		
	}
	private MClass pickClassFromList(String pak,List<MClass> lst) {
		MClass ret = null;
		do {
			for (MClass cl:lst) {
				String pakCl = cl.getPackageName();
				pakCl = pakCl==null ? "" :pakCl;
				pakCl = pakCl.replaceAll("::", ".");
				if (pak.equals(pakCl)) {
					return cl;
					
				}
			}
			int idx = pak.lastIndexOf('.');
			if (idx>0)
				pak=pak.substring(0,idx);
			else
				pak=null;
		} while (pak!=null);
		if (ret==null && !lst.isEmpty())
			ret= lst.iterator().next();
		return ret;
	}
	public MClass getClazzFromUnderscore(MClass context,String underscore_name) {
		 List<MClass> lst = getClasses().stream().filter(
				cl->JRubyInflector.getInstance().underscore(cl.getName()).equalsIgnoreCase(underscore_name)).collect(Collectors.toList())
				;
		String pak = context.getPackageName();
		pak = pak==null ? "" : pak; 
		pak = pak.replaceAll("::", ".");
		return pickClassFromList(pak, lst);
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
		int tries=0; //this is a hack
		boolean keep; 
		do {
			HashSet<String> removed = new HashSet<String>(); 
			keep=false;
			for (Iterator<String> it=subclasses.keySet().iterator();it.hasNext();) {
				String supername = it.next();
				//MClass parent = getClazz(supername); 
				// old wait
				List<MClass> lst = subclasses.get(supername);	
				for (Iterator<MClass> it2=lst.iterator();it2.hasNext();) {					
					MClass c = it2.next();					
					MClass parent = getClazz(c.getSuperClassName());
					boolean wait = parent!=null && !Util.isNullOrEmpty(parent.getSuperClassName()) && incomplete.containsKey(parent);
					if (c.equals(parent))
						wait=false;
					if (!wait) {
						tries=0;
						ClassNode n = incomplete.get(c);
						if (n!=null)
							rv.visitClass(c,n, parent,parent==null ? false : parent.isPersistent());
						it2.remove();
					}
					tries++;
				}
				if (tries>1000000)
					throw new RuntimeException("solverefs exceeded limit for finding superclasses:"+subclasses.keySet());
				keep=true;
				if (lst.isEmpty())
					removed.add(supername);
				//it.remove();		
				//old wait ends				
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
		visitors.sort(LateVisitor.comparator);
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
