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

	public abstract boolean isNullableDef();

	public abstract boolean isInsertableDef();

	public abstract boolean isUpdatableDef();

	public abstract String getColummnDefinition() ;

	public abstract int getLengthDef();

	public abstract int getPrecisionDef();

	public abstract int getScaleDef();

	public abstract boolean isUniqueDef();
	
	public abstract MTable getTable();
	
	public abstract MColumn getColumn();
}
