package model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@JsonIdentityInfo(generator=JSOGGenerator.class)
public abstract class MColumnDefinition {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	
	public int getId() { return id;	}
	public void setId(int id) {	this.id = id;	}
	public abstract String getName();

	public abstract boolean isNullableDef();

	public abstract boolean isInsertableDef();

	public abstract boolean isUpdatableDef();

	public abstract String getColumnDefinition() ;

	public abstract int getLengthDef();

	public abstract int getPrecisionDef();

	public abstract int getScaleDef();

	public abstract boolean isUniqueDef();
	
	public abstract MTable getTable();
	
	public abstract MColumn getColumn();
	public String get_Type() {
		return this.getClass().getSimpleName();
	}
}
