package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import common.ReflectiveVisitor;
import common.Visitable;
@Entity
public class MClass implements Visitable {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private String name;
	private String packageName;
	private boolean isAbstract=false;
	@ManyToOne(optional=false)
	private Repo repo;
	@Embedded
	private MPersistent persistence=new MPersistent();
	@ManyToOne(cascade=CascadeType.PERSIST)
	private MClass superClass;
	private String superClassName;
	@OneToMany(mappedBy="parent",cascade=CascadeType.PERSIST)
	@OrderBy
	private List<MProperty> properties = new ArrayList<MProperty>();
	@OneToMany(mappedBy="clazz",cascade=CascadeType.ALL)
	private Set<MOverride> overrides = new HashSet<MOverride>();
	@OneToMany(mappedBy="superClass")
	private Set<MClass> specializations=new HashSet<MClass>();
	@OneToMany(cascade=CascadeType.ALL,orphanRemoval=true)
	private Set<MGeneralization> generalization= new HashSet<MGeneralization>();
	@Embedded
	private MDiscriminator discriminatorColumn;
	@Basic(optional=false)
	private String filePath;
	public static MClass newMClass(String path,Repo repo) {
		return new MClass(path,repo);
	}
	protected MClass(String path,Repo repo) {
		this.filePath=path;
		this.repo=repo;
	}	
	protected MClass() {
	}
	public int getId() { return id;	}
	public void setId(int id) {	this.id = id;	}
	public Repo getRepo() {
		return repo;
	}
	protected void setRepo(Repo repo) {
		this.repo = repo;
	}
	protected void setOverrides(Set<MOverride> overrides) {
		this.overrides = overrides;
	}
	public String getName() {
		return name;
	}
	public String getFullName() {
		if (packageName==null)
			return name;
		return packageName+"."+name;
	}
	public MClass setName(String name) {
		this.name = name;
		return this;
	}
	public String getPackageName() {
		return packageName;
	}
	public MClass setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}
	public boolean isAbstract() {
		return isAbstract;
	}
	public MClass setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
		return this;
	}
	/**
	 * creates a new MTable. If the datasource is set to JoinedSource, adds this new table to the source.
	 * If not, sets this MTable as the new source. 
	 * @param name
	 * @return
	 */
	public MTable newTableSource(String name) {
		MTable t= MTable.newMTable(repo,name);
		persistence = setPersistent();		
		persistence.setDataSource(t);		
		return t;
	}
	public MJoinedSource setJoinedSource() {
		persistence = setPersistent();
		if (persistence.getSource() instanceof MJoinedSource)
			return (MJoinedSource)persistence.getSource();
		MJoinedSource s = new MJoinedSource();
		s.setRepo(repo);
		persistence.setPersistent(true).setDataSource(s);		
		return s;
	}
	public MPersistent setPersistent() {
		if (persistence==null)
			persistence = new MPersistent();
		persistence.setPersistent(true);
		return persistence;
	}
	public MClass unsetPersistent() {
		persistence.setDataSource(null);
		persistence.setPersistent(false);
		return this;
	}
	/**
	 * Find datasource in this class or in the superclasses
	 * @return
	 */
	public MDataSource findDataSource() {
		MDataSource ret = persistence.getSource();
		if (ret!=null)
			return ret;
		if (superClass!=null && superClass.isPersistent())
			return superClass.findDataSource();
		return null;
		
	}
	public MPersistent getPersistence() {
		return persistence;
	}
	void setPersistence(MPersistent persistence) {
		this.persistence = persistence;
	}
	public MClass getSuperClass() {
		return superClass;
	}
	public MClass setSuperClass(MClass superClass) {
		this.superClass = superClass;
		if (superClass!=null)
			superClass.getSpecializations().add(this);
		return this;
	}
	
	public String getSuperClassName() {
		return superClassName;
	}
	public void setSuperClassName(String superClassName) {
		this.superClassName = superClassName;
	}
	public MProperty newProperty() {
		MProperty prop = MProperty.newMProperty(this);
		properties.add(prop);
		return prop;
	}
	public MProperty newPKProperty() {
		MProperty prop = MProperty.newMProperty(this);
		prop.setPk(true);
		properties.add(0,prop);
		return prop;
	}
	public List<MProperty> getProperties() {
		return properties;
	}
	public void setProperties(List<MProperty> properties) {
		this.properties = properties;
	}
	public MClass override(MOverride override) {
		this.overrides.add(override);
		override.setClazz(this);
		return this;
	}
	public Set<MOverride> getOverrides() {
		return overrides;
	}
	public MColumn findOverridenColumn(String colName) {
		for (MOverride ov:getOverrides()) {
			if (ov instanceof MAttributeOverride) {
				MColumn col = ((MAttributeOverride) ov).getColumn().getColumn();				
				if (colName.equals(col.getName())) {
					return col;
				}
			}			
		}
		if (superClass!=null)
			return superClass.findOverridenColumn(colName);
		return null;
	}
	public MColumn findColumnByName(String colName) {
		MColumn ret = findOverridenColumn(colName);
		if (ret!=null)
			return ret;
		
		List<MProperty> allProps = getAllProperties();
		for (MProperty cp:allProps) {
			if (cp.getColumnDef()!=null) {
				String cdName = cp.getColumnDef().getName();
				
				if (colName.equals(cdName)) {
					return cp.getColumnDef().getColumn();					
				} else if (cdName==null && colName.equalsIgnoreCase(cp.getName())){
					return cp.getColumnDef().getColumn();
				}
			}
			if (cp.isEmbedded() && cp.getTypeClass()!=null) {
				ret = cp.getTypeClass().findColumnByName(colName);
				if (ret!=null)
					return ret;
			}
			if (cp.getAssociationDef()!=null) {
				for ( MJoinColumn jc:cp.getAssociationDef().getJoinColumns()) {
					if (jc.getColumn()!=null && jc.getColumn().getName().equals(colName)) {
						return jc.getColumn().getColumn();//does it depends? inverse sometimes is in the source
					}
					
				}
			}
		}
		return null;
		
	}
	public Set<MClass> getSpecializations() {
		return specializations;
	}
	public void setSpecializations(Set<MClass> specializations) {
		this.specializations = specializations;
	}
	
	public Set<MGeneralization> getGeneralization() {
		return generalization;
	}
	protected void setGeneralization(Set<MGeneralization> generalization) {
		this.generalization = generalization;
	}
	
	public MDiscriminator getDiscriminatorColumn() {
		return discriminatorColumn;
	}
	public void setDiscriminatorColumn(MDiscriminator discriminatorColumn) {
		this.discriminatorColumn = discriminatorColumn;
	}
	public <T extends MGeneralization> T addGeneralization(Class<T> type) {
		T ret;
		try {
			ret = type.newInstance();
		} catch (Exception e) {		
			e.printStackTrace();		
			return null;
		}
		
		generalization.add(ret);
		return ret;
	}
	/**
	 * SE superclasse for abstrata e for persistente, filho tem tabela
	 * SE superclasse n�o for abstrata for persistente, filho n�o tem tabela
	 * SE superclasse n�o for persistente, e filho persistente, tem tabela -so que n�o, 
	 *   pq tem que extender active:record!
	 * @return
	 */
	public boolean isFirstConcretePersistent() {
		if (this.superClass==null) {
			return isPersistent() && !isAbstract;
		}
		if (superClass.isPersistent()) {
			return !superClass.isFirstConcretePersistent();
			
		}
		return false;
	}
	public boolean isPersistent() {
		return persistence.isPersistent();
	}
	public List<MProperty> findPK() {
		List<MProperty> ret = getPK();
		if (ret.isEmpty() && superClass!=null)
			return superClass.findPK();
		else
			return ret;
	}
	//TODO:ordered PK?
	public List<MProperty> getPK() {
		return getProperties().stream().filter(p->p.isPk()).collect(Collectors.toList());
	}
	public String toString() {
		return (repo!=null ? "Repo:"+repo.getName()+" - " : "" ) + this.getFullName();
	}
	public MProperty findProperty(String name) {
		return getProperties().stream().filter(p->p.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	public MProperty findInheritedProperty(String name) {
		MProperty res= getProperties().stream().filter(p->p.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
		if (res==null && getSuperClass()!=null) {
			res = superClass.findInheritedProperty(name);
		}
		return res;
	}
	public List<MProperty> getAllProperties() {
		List<MProperty> ret=null;
		if (superClass!=null) {
			ret=superClass.getAllProperties();
		} else {
			ret = new ArrayList<MProperty>();
		}
		ret.addAll(0,properties); //Add to the start!
		return ret;			
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	@Override
	public void accept(ReflectiveVisitor visitor) {
		
		visitor.callAccept(properties);
		
		visitor.callAccept(generalization);
		
		visitor.callAccept(overrides);		
		
		visitor.visit(this);
	}
	
}
