package model;

import javax.persistence.Entity;

@Entity
public abstract class MDiscrminableGeneralization extends MGeneralization {
	private String discriminatorValue;

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}
	
}
