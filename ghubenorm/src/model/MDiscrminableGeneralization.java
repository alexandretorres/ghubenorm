package model;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
