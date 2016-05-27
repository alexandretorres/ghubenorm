package model;

public class MAssociationMapping {
	private MAssociationDef value;
	private MProperty property;
	
	public static MAssociationMapping newMAssociationMapping(MAssociationDef def) {
		return new MAssociationMapping(def);
	}
	private MAssociationMapping(MAssociationDef def) {
		value=def;
	}
	public MAssociationDef getValue() {
		return value;
	}

	public void setValue(MAssociationDef value) {
		this.value = value;
	}
	public MProperty getProperty() {
		return property;
	}
	public void setProperty(MProperty property) {
		this.property = property;
	}
	
}
