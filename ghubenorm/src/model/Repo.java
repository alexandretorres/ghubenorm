package model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import static gitget.Log.LOG;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
 

@Entity

public class Repo {
	
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)	
	private int id;
	@Column(unique=true,nullable=false)
	private int publicId;
	private String name;
	private String url;
	private Language language;	
	private String branch;
	/**
	 * This is the internal path to the "base" file that defines the repository. For ruby it is the schema.db file,
	 * for java it may be the presistence.xml. It is used to differentiate what portion of the repository is 
	 * under analysis. Notice that a repository MAY have more than one "database", in distinct source directories. 
	 * Sometimes these will be just "test" databases. 
	 */
	private String configPath;
	
	@OneToMany(fetch=FetchType.EAGER,mappedBy="repo",cascade=CascadeType.ALL)
	Set<MClass> classes = new HashSet<MClass>();
	@OneToMany(fetch=FetchType.EAGER,mappedBy="repo",cascade=CascadeType.PERSIST)
	Set<MDataSource> dataSources = new HashSet<MDataSource>();
	
	protected Repo() {}
	public Repo(Language lang) {
		this.language=lang;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Language getLanguage() {
		return language;
	}
	public void setLanguage(Language language) {
		this.language = language;
	}
	public String getConfigPath() {
		return configPath;
	}
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}
	public Set<MClass> getClasses() {
		return classes;
	}
	protected void setClasses(Set<MClass> classes) {
		this.classes = classes;
	}
	public Set<MDataSource> getDataSources() {
		return dataSources;
	}
	protected void setDataSources(Set<MDataSource> sources) {
		this.dataSources = sources;
	}
	
	public String getBranch() {
		return branch;
	}
	public void setBranch(String branch) {
		this.branch = branch;
	}
	public String getBranchGit() {
		if (branch==null || branch.equals(""))
			return "master";
		return branch;
	}
	
	public int getPublicId() {
		return publicId;
	}
	public void setPublicId(int publicId) {
		this.publicId = publicId;
	}
	/*	public Set<MDataSource> getTables() {
		return sources;
	}
	protected void setTables(Set<MDataSource> sources) {
		this.sources = sources;
	}*/
	//--------------------
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
				//TODO:check Java not persitent can have decl.
				//if (cl.isPersistent()) {					
					if (p.getColumnMapping()!=null) {
						MColumnDefinition col = p.getColumnMapping().getColumnDefinition();
						pw.print(" | "+Optional.ofNullable(col.getName()).orElse(""));
						if (col.getColummnDefinition()!=null && col.getColummnDefinition().length()>0)
							pw.print(":"+col.getColummnDefinition());
						pw.print( col.getLength()==null ? "" : "("+col.getLength()+")");
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
				//}
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
}
