package model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class MColumnDefinition {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	
	public int getId() { return id;	}
	public void setId(int id) {	this.id = id;	}
	public abstract String getName();

	public abstract boolean isNullable();

	public abstract boolean isInsertable();

	public abstract boolean isUpdatable();

	public abstract String getColummnDefinition() ;

	public abstract Integer getLength();

	public abstract int getPrecision();

	public abstract int getScale();

	public abstract boolean isUnique();
	
	public abstract MTable getTable();
}
