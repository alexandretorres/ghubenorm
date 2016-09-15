package model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import static gitget.Log.LOG;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
	
	static Comparator<MProperty> mPropComp = new Comparator<MProperty>() {
		/*Compares its two arguments for order. Returns a negative integer,
		 *  zero, or a positive integer as the first argument is less than, equal to, or greater than the second.*/
		@Override
		public int compare(MProperty p1, MProperty p2) {
			int ret = p1.getName().compareTo(p2.getName());
			if (p1.isPk() && p2.isPk()) {
				return ret;
			}else if (p1.isPk() && !p2.isPk()) {
				return -1;
			}else if (!p1.isPk() && p2.isPk()) {
				return 1;
			}else {
				return ret;
			}
			
		}
	};
	
	static Comparator<MClass> mClassComp = new Comparator<MClass>() {
		/*Compares its two arguments for order. Returns a negative integer,
		 *  zero, or a positive integer as the first argument is less than, equal to, or greater than the second.*/
		@Override
		public int compare(MClass o1, MClass o2) {
			int ret =0;
			//o1<o2 => negative // return "o1-o2"
			String pak1 = o1.getPackageName();
			String pak2 = o1.getPackageName();
			if (pak1!=null) {
				if (pak2==null)
					return 1;
				ret = pak1.compareTo(pak2);
				if (ret!=0)
					return ret;
			}
			String n1 = o1.getName();
			String n2 = o2.getName();
			ret = n1.compareTo(n2);
			return ret;
			
		}
	};
	public void print() {
		//TODO: rethink this print to better reuse. 
		StringWriter sw =new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		LOG.info("{Repo:Start}{name:"+this.getName()+"}----------------");
		try {
			List<MClass> classList = new ArrayList<MClass>(getClasses());
			if (classList.isEmpty()) {
				LOG.info("no classes");
				return;
			}
			Collections.sort(classList, mClassComp);
			//PrintWriter pw =new PrintWriter(System.out);
			pw.println("{Classes:}");
			for (MClass cl:classList) {
				pw.println("=====================================");
				pw.print((cl.getPackageName()==null ? "" : cl.getPackageName()+".")+cl.getName());
				if (cl.isAbstract())
					pw.print("[Abstract]");
				if (cl.getSuperClass()!=null) {
					if (!cl.getGeneralization().isEmpty()) {
						MGeneralization gen = cl.getGeneralization().iterator().next();
						if (gen instanceof MHorizontal) {
							pw.print(" ▤");
						} else if (gen instanceof MVertical) {
							MVertical vert = (MVertical) gen;
							pw.print(" ▥");
							if (vert.getDiscriminatorValue()!=null) {
								pw.print("("+vert.getDiscriminatorValue()+")");
							}
							for (MJoinColumn jc:vert.getJoinCols()) {
								pw.print("[");
								MColumnDefinition col = jc.getColumn();
								if (col.getTable()!=null)
									pw.print(col.getTable().getName()+".");
								pw.print(col.getName());
								col=jc.getInverse();
								if (col!=null) {
									if (col.getTable()!=null)
										pw.print(col.getTable().getName()+".");									
									pw.print(col.getName());									
								}
								pw.print("]");
							}
						} else if (gen instanceof MFlat) {
							MFlat flat = (MFlat) gen;							
							pw.print(" ▣");
							if (flat.getDiscriminatorValue()!=null) {
								pw.print("("+flat.getDiscriminatorValue()+")");
							}
						}
					} else if (cl.isPersistent()) {
						pw.print(" △");
					}
					pw.print(" extends " + cl.getSuperClass().getName());
				}
				MTable mainTab=null;
				if (cl.isPersistent()) {
					pw.print(" | ");
					if (cl.getPersistence().getSource() instanceof MTable) {
						mainTab = (MTable) cl.getPersistence().getSource() ;
						if (mainTab.getName()!=null)
							pw.print(mainTab.getName());
					} else if (cl.getPersistence().getSource() instanceof MJoinedSource) {
						MJoinedSource source = (MJoinedSource) cl.getPersistence().getSource();
						for (MTable stab:source.getDefines()) {
							if (mainTab==null)
								mainTab=stab;
							else
								pw.print(", ");
							pw.print(stab.getName());
						}
					}
				}
				
				pw.println("\n________________________________");
				List<MProperty> propList = new ArrayList<MProperty>(cl.getProperties());
				Collections.sort(propList, mPropComp);
				
				for (MProperty p:propList) {
					if (p.isPk())
						pw.print("<PK>");
					if (p.isTransient())
						pw.print("θ ");
					pw.print(p.getName()+"["+p.getMin()+".."+(p.getMax()<0 ? "*": p.getMax())+"]:"+Optional.ofNullable(p.getType()).orElse("<<unknow>>"));
					//TODO:check Java not persitent can have decl.
									
					if (p.isEmbedded()) {
						pw.print(" <Embbeded> ");
						
					}	
					if (p.getColumnMapping()!=null) {
						MColumnDefinition col = p.getColumnMapping().getColumnDefinition();
						String colTab = mainTab==null || col.getTable()==null || col.getTable().getName().equals(mainTab.getName())? "" : col.getTable().getName()+".";
						pw.print(" | "+colTab+Optional.ofNullable(col.getName()).orElse(""));
						if (col.getColummnDefinition()!=null && col.getColummnDefinition().length()>0)
							pw.print(":"+col.getColummnDefinition());
						pw.print( col.getLength()==null ? "" : "("+col.getLength()+")");
					}
						
					pw.print( printAssociationDef(cl,p));
					MAssociation assoc = p.getAssociation();
					if (assoc==null)
						assoc = p.getToAssociation();
					
					
					if (assoc!=null) {						
						MProperty inv = assoc.getInverse(p);
						
						if (p.isTransient())
							pw.print("--(transient)---");
						else
							pw.print("---------------");
						try {							
							pw.print((p.getTypeClass()==null ? p.getType() : p.getTypeClass().getName())+(inv==null ? "" : "."+ inv.getName()+"["+inv.getMin()+".."+(inv.getMax()<0 ? "*": inv.getMax())+"]"));
						} catch (Exception ex) {
							LOG.log(Level.SEVERE,ex.getMessage(),ex);		 						
						}
					}  
					
					pw.println();
				}
				if (!cl.getOverrides().isEmpty()) {
					String header = "____________\n";
					header += "Overrides:\n";
					String tx="";
					for (MOverride ov: cl.getOverrides()) {
						if (ov instanceof MAttributeOverride) {
							MAttributeOverride ao = (MAttributeOverride) ov;
							if (ao.checkOverride()) {
								Stream<String> st1 = ao.getProperties().stream().map(MProperty::getName);
								tx+=String.join(".",  st1.toArray(String[]::new));		
								tx+=" to column "+ao.getColumn().getName()+"\n";
							}
						} else {
							
						}
					}
					if (tx.length()>0)
						pw.print(header+tx);
				}
				pw.flush();
			}
		} catch (Exception ex) {			
			LOG.log(Level.SEVERE, ex.getMessage(), ex);
		} finally {
			pw.flush();
			LOG.log(Level.INFO, sw.toString());
			LOG.info("{Repo:End}----------------");
		}
			//System.out.println(sw.getBuffer());
	}
	String printAssociationDef(MClass cl,MProperty p) {
		StringWriter sw =new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		if (p.getAssociationMapping()!=null) {			
			int f=0;
			MAssociationDef adef = p.getAssociationMapping().getValue();
			String dsName=null;
			if (adef.getDataSource()!=null && (dsName=adef.getDataSource().printName())!=null) {
				pw.print("joinTable="+dsName);
				f=1;
			}
			for (MJoinColumn jc:adef.getJoinColumns()) {
				//TODO:Check with ruby this change. This will never work with Java and looks fishy
				MColumnDefinition colDef = jc.getColumn(); //jc.getColumnForProperty(p); 
				MColumnDefinition invColDef = jc.getInverse(); //jc.getColumnForProperty(inv);				
				if (f>0)
					pw.print(", ");
				if (f<2)
					pw.print("joinColumn(s)=");
				f=2;
				if (colDef!=null)
					if (colDef.getTable()!=null && !colDef.getTable().equals(cl.getPersistence().getMainTable())) {
						pw.print(colDef.getTable().getName());
						pw.print(".");
					}
					pw.print(colDef.getName());
				if (invColDef!=null) {	
					//TODO: this way will print the table name, the class, and the field.
					String tabName = Optional.ofNullable(invColDef.getTable()).map(t->t.getName()).orElse(null);
					pw.print("=");
					if (invColDef.getName()==null ) {
						String name = printInverseJoinColumn(invColDef,p.getParent(),tabName);
						if (name==null && p.getTypeClass()!=null) {
							 name = printInverseJoinColumn(invColDef,p.getTypeClass(),tabName);
						}
						pw.print(name);									
					} else {
						if (tabName!=null)
							pw.print(tabName+".");
						pw.print(invColDef.getName());
					}
				}
				
				
			}
								
		}
		pw.flush();
		String ret = sw.toString();
		if (ret.length()>0)
			ret= "("+ret+")";
		return ret;
	}
	String printInverseJoinColumn(MColumnDefinition invColDef,MClass clazz,String tabName) {
		//TODO: MainTable instead of class name
		String name = null;
		if (tabName==null && clazz.getPersistence().hasTableSource(invColDef.getTable())) {
			return "<"+clazz.getName()+">.<id>";
		}
		for (MProperty pk:clazz.getPK()) {
			if (invColDef.equals(pk.getColumnDef())) {
				if (tabName==null)
					tabName = "<"+clazz.getName()+">";
				name = pk.getName();
				break;
			}
		}
		if (name==null) {
			for (MGeneralization gen:clazz.getGeneralization()) {
				if (gen instanceof MVertical) {
					MVertical vert = (MVertical) gen;
					for (MJoinColumn gjc: vert.getJoinCols()) {
						if (invColDef.equals(gjc.getColumn())) {
							String idName = "<id>";
							for (MProperty pk:clazz.findPK()) {
								if (pk.getColumnDef()!=null)
									idName = pk.getColumnDef().getName();
								if (idName.equals("<id>")) {
									idName = pk.getName();
								}
								break;
							}
							if (tabName==null)
								tabName = "<"+clazz.getName()+">";
							name = idName;
							break;
						}
					}
				}
			}
		}
		if (name==null) {
			for (MOverride over:clazz.getOverrides()) {
				if (over instanceof MAttributeOverride) {
					MAttributeOverride aover = (MAttributeOverride) over;
					if (invColDef.equals(aover.getColumn())) {
						String idName = "<id>";
						for (MProperty pk:clazz.findPK()) {
							if (pk.getColumnDef()!=null)
								idName = pk.getColumnDef().getName();
							if (idName.equals("<id>")) {
								idName = pk.getName();
							}
							break;
						}
						if (tabName==null)
							tabName ="<"+clazz.getName()+">";
						name = idName;
						break;
					}
				}
			}
		}
		if (name!=null)
			name = (tabName==null ? "" : tabName+"." )+name;
		return name;
	}
}
