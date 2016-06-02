package model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class MOverride {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	@ManyToOne(optional=false)
	private MClass clazz;
	@ManyToMany
	private List<MProperty> properties = new ArrayList<MProperty>();
	
	protected MOverride() {}	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * Ordered List of connected properties x.y.z
	 */
	public List<MProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<MProperty> properties) {
		this.properties = properties;
	}
	protected MClass getClazz() {
		return clazz;
	}
	protected void setClazz(MClass clazz) {
		this.clazz = clazz;
	}
	
}
