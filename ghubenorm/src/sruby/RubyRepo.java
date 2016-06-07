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
	public Set<MTable> getTables() {
		return repo.getTables();
	}
	public MTable getTable(String name) {
		Optional<MTable> ret = getTables().stream().filter(tab->tab.getName().equalsIgnoreCase(name)).findFirst();
		return ret.orElse(null);
	}
	public MClass getClazz(String name) {
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
					it.remove();				
				}
			}
		} while (keep && !subclasses.isEmpty());
		
		for (String name:subclasses.keySet()) {			
			for (ClassNode n:subclasses.get(name)) {
				rv.createClass(n, null,false);
			}
		}
	
		
		/*
		for (String name:subclasses.keySet()) {
			List<ClassNode> lst = subclasses.get(name);	
			MClass parent = getClazz(name);
			for (ClassNode n:lst) {
				rv.createClass(n, parent,parent.getPersistence()!=null);
			}			
		}*/
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
	public void print() {
		System.out.println("-----------------");
		StringWriter sw =new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		try {
		//PrintWriter pw =new PrintWriter(System.out);
		for (MClass cl:getClasses()) {
			pw.println("=====================================");
			pw.print((cl.getPackageName()==null ? "" : cl.getPackageName()+".")+cl.getName());
			if (cl.isAbstract())
				pw.print("[Abstract]");
			if (cl.getSuperClass()!=null) {
				pw.print(" extends " + cl.getSuperClass().getName());
			}
			MTable tab=null;
			if (cl.isPersistent()) {
				pw.print(" | ");
				if (cl.getPersistence().getSource() instanceof MTable) {
					tab = (MTable) cl.getPersistence().getSource() ;
					pw.print(tab.getName());
				}
			}
			pw.println("\n________________________________");
			for (MProperty p:cl.getProperties()) {
				if (p.isPk())
					pw.print("<PK>");
				pw.print(p.getName()+"["+p.getMin()+".."+(p.getMax()<0 ? "*": p.getMax())+"]:"+Optional.ofNullable(p.getType()).orElse("<<unknow>>"));
				
				if (cl.isPersistent()) {					
					if (p.getColumnMapping()!=null) {
						MColumnDefinition col = p.getColumnMapping().getColumnDefinition();
						pw.print(" | "+Optional.of(col.getName()).orElse(""));
						if (col.getColummnDefinition()!=null && col.getColummnDefinition().length()>0)
							pw.print(":"+col.getColummnDefinition());
						pw.print( col.getLength()==0 ? "" : "("+col.getLength()+")");
					}
					MAssociation assoc = p.getAssociation();
					if (assoc==null)
						assoc = p.getToAssociation();
					if (assoc!=null) {						
						MProperty inv = assoc.getInverse(p);
						if (p.getAssociationMapping()!=null) {
							pw.print("( ");
							boolean f=false;
							for (MJoinColumn jc:p.getAssociationMapping().getValue().getJoinColumns()) {
								MColumnDefinition colDef = jc.getColumnForProperty(p);
								MColumnDefinition invColDef = jc.getColumnForProperty(inv);
								if (f)
									pw.print(",");
								if (colDef!=null)
									pw.print(colDef.getName());
								if (invColDef!=null)									 
									pw.print("="+Optional.of(invColDef.getTable()).map(t->t.getName()+".").orElse("")+invColDef.getName());
								
								f=true;
							}
							pw.print(") ");							
						}
						if (p.isTransient())
							pw.print("--(transient)---");
						else
							pw.print("---------------");
						try {							
							pw.print(p.getTypeClass().getName()+(inv==null ? "" : "."+ inv.getName()+"["+inv.getMin()+".."+(inv.getMax()<0 ? "*": inv.getMax())+"]"));
						} catch (Exception ex) {
							LOG.log(Level.SEVERE,ex.getMessage(),ex);								
						}
					} else if (p.isEmbedded()) {
						pw.print(" <Embbeded> ");
						
					}
				}
				pw.println();
			}
			if (!cl.getOverrides().isEmpty()) {
				pw.println("____________");				
				pw.println("Overrides:");			
				for (MOverride ov: cl.getOverrides()) {
					if (ov instanceof MAttributeOverride) {
						MAttributeOverride ao = (MAttributeOverride) ov;	
						Stream<String> st1 = ao.getProperties().stream().map(MProperty::getName);
						pw.print(String.join(".",  st1.toArray(String[]::new)));					
						pw.println(" to column "+ao.getColumn().getName());
					} else {
						
					}
				}
			}
			pw.flush();
		}
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ex.getMessage(), ex);
		}
		pw.flush();
		LOG.log(Level.INFO, sw.toString());
		//System.out.println(sw.getBuffer());
	}
	public Repo getRepo() {
		return repo;
	}
	public void setRepo(Repo repo) {
		this.repo = repo;
	}
	
}
