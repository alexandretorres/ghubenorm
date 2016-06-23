package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
@Entity
public class MClass {
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
	private List<MProperty> properties = new ArrayList<MProperty>();
	@OneToMany(mappedBy="clazz",cascade=CascadeType.ALL)
	private Set<MOverride> overrides = new HashSet<MOverride>();
	@OneToMany(mappedBy="superClass")
	private Set<MClass> specializations=new HashSet<MClass>();
	
	public static MClass newMClass(Repo repo) {
		return new MClass(repo);
	}
	protected MClass(Repo repo) {
		this.repo=repo;
	}	
	protected MClass() {
	}
	public int getId() { return id;	}
	public void setId(int id) {	this.id = id;	}
	protected Repo getRepo() {
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
	public Set<MClass> getSpecializations() {
		return specializations;
	}
	public void setSpecializations(Set<MClass> specializations) {
		this.specializations = specializations;
	}
	/**
	 * SE superclasse for abstrata e for persistente, filho tem tabela
	 * SE superclasse não for abstrata for persistente, filho não tem tabela
	 * SE superclasse não for persistente, e filho persistente, tem tabela -so que não, 
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
	public String toString() {
		return (repo!=null ? "Repo:"+repo.getName()+" - " : "" ) + this.getName();
	}
}
