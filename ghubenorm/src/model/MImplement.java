package model;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;
@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
public class MImplement {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private String name;
	@ManyToOne
	private MClass toInterface;
	@ManyToOne(optional=false)
	private MClass fromClass;
	
	
	protected MImplement() {
		
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
	public MClass getToInterface() {
		return toInterface;
	}
	public void setToInterface(MClass toInterface) {
		this.toInterface = toInterface;
	}
	public MClass getFromClass() {
		return fromClass;
	}
	public void setFromClass(MClass fromClass) {
		this.fromClass = fromClass;
	}
	
	
}
