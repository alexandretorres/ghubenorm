package model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

import common.Visitable;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@JsonIdentityInfo(generator=JSOGGenerator.class)
public abstract class MDataSource implements Visitable {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	//associationdefs are not navigable from here
	@ManyToOne(optional=false)
	@JsonBackReference
	private Repo repo;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	protected Repo getRepo() {
		return repo;
	}
	protected void setRepo(Repo repo) {
		this.repo = repo;
	}
	
	public abstract String printName();
	

}
