package model;

import static javax.persistence.InheritanceType.SINGLE_TABLE;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@Entity
@Inheritance(strategy=SINGLE_TABLE)
@JsonIdentityInfo(generator=JSOGGenerator.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
public abstract class MGeneralization {
	protected MGeneralization() {
		
	}
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	public String get_Type() {
		return this.getClass().getSimpleName();
	}
}
