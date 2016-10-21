package model;

import static javax.persistence.InheritanceType.SINGLE_TABLE;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;

@Entity
@Inheritance(strategy=SINGLE_TABLE)
public abstract class MGeneralization {
	protected MGeneralization() {
		
	}
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
}
