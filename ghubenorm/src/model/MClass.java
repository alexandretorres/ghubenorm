package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

import common.ReflectiveVisitor;
import common.Util;
import common.Visitable;
@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
public class MClass implements Visitable {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private String name;
	@Column(length=1024)
	private String packageName;
	private boolean isAbstract=false;
	@ManyToOne(optional=false)
	@JsonBackReference
	private Repo repo;
	@Embedded
	private MPersistent persistence=new MPersistent();
	@ManyToOne(cascade=CascadeType.PERSIST)
	private MClass superClass;
	@Column(length=1024)
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
	@Column(length=2048)
	private String filePath;
	@Enumerated(EnumType.ORDINAL)
	private ClassifierType type;
	@OneToMany(mappedBy="clazz",cascade=CascadeType.ALL,orphanRemoval=true)
	private List<MMethod> methods = new ArrayList<>();
	@OneToMany(mappedBy="toInterface",cascade=CascadeType.ALL,orphanRemoval=true)
	@OrderBy
	private List<MImplement> implementInterfaces;
	public static MClass newMClass(String path,Repo repo) {
		return new MClass(path,repo);
	}
	protected MClass(String path,Repo repo) {
		this.filePath=Util.capSize(path,2048);
		this.repo=repo;
		this.type=ClassifierType.ClassType;
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
	public String getUMLName() {
		String name = getFullName();
		if (name!=null)
			name = name.replaceAll("\\.", "::");
		return name;
	}
	public String getFullName() {
		if (packageName==null)
			return name;
		return packageName+"."+name;
	}
	public MClass setName(String name) {
		name = Util.capSize(name, 255);
		this.name = name;
		return this;
	}
	public String getPackageName() {
		return packageName;
	}
	public MClass setPackageName(String packageName) {
		name = Util.capSize(name, 1024);
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
		return newTableSource(name,true);
	}
	public MTable newTableSource(String name,boolean persistent) {
		MTable t= MTable.newMTable(repo,name);
		persistence = getPersistence().setPersistent(persistent);		
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
		if (superClass!=null /*&& superClass.isPersistent()*/)
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
		if (this.equals(superClass))
			throw new RuntimeException("A class could not extend itself");
		this.superClass = superClass;
		if (superClass!=null)
			superClass.getSpecializations().add(this);
		return this;
	}
	
	public String getSuperClassName() {
		return superClassName;
	}
	public void setSuperClassName(String superClassName) {
		superClassName = Util.capSize(superClassName, 1024);
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
					if (jc.getColumn()!=null && jc.getColumn().getName()!=null && jc.getColumn().getName().equals(colName)) {
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
	public List<MMethod> getMethods() {
		return methods;
	}
	protected void setMethods(List<MMethod> methods) {
		this.methods = methods;
	}
	public ClassifierType getType() {
		return type;
	}
	public MClass setType(ClassifierType type) {
		this.type = type;
		return this;
	}
	public List<MImplement> getImplementInterfaces() {
		if (implementInterfaces==null)
			implementInterfaces = new ArrayList<>();
		return implementInterfaces;
	}
	protected void setImplementInterfaces(List<MImplement> implementInterfaces) {
		this.implementInterfaces = implementInterfaces;
	}
	public MClass addImplements(String interfaceName,MClass toInterface) {
		MImplement imp = new MImplement();
		imp.setToInterface(toInterface);
		imp.setFromClass(this);
		imp.setName(interfaceName);
		getImplementInterfaces().add(imp);
		return this;
	}
	
}
