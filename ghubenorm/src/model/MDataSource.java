package model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class MDataSource {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	//associationdefs are not navigable from here

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
