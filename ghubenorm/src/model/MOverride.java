package model;

import java.util.ArrayList;
import java.util.List;

public abstract class MOverride {

	private List<MProperty> properties = new ArrayList<MProperty>();
	/**
	 * Ordered List of connected properties x.y.z
	 */
	public List<MProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<MProperty> properties) {
		this.properties = properties;
	}
	
}
