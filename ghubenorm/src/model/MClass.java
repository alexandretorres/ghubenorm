package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MClass {

	private String name;
	private String packageName;
	private boolean isAbstract=false;
	private MPersistent persistence=null;
	private MClass superClass;
	private List<MProperty> properties = new ArrayList<MProperty>();
	private Set<MOverride> overrides = new HashSet<MOverride>();
	private Set<MClass> specializations=new HashSet<MClass>();
	
	public static MClass newMClass() {
		return new MClass();
	}
	private MClass() {
		
	}
	
	public String getName() {
		return name;
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
	public MPersistent setPersistent() {
		if (persistence==null)
			persistence = new MPersistent();
		return persistence;
	}
	public MClass unsetPersistent() {
		persistence=null;
		return this;
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
			return persistence!=null && !isAbstract;
		}
		if (superClass.persistence!=null) {
			return !superClass.isFirstConcretePersistent();
			
		}
		return false;
	}
}
