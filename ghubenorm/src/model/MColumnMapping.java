package model;

public class MColumnMapping {

	MProperty property;
	MColumnDefinition columnDefinition;
	public static MColumnMapping newMColumnMapping(MColumnDefinition columnDefinition) {
		return new MColumnMapping( columnDefinition);
	}
	private MColumnMapping( MColumnDefinition columnDefinition) {
		super();
	
		this.columnDefinition = columnDefinition;
	}
	public MProperty getProperty() {
		return property;
	}
	public void setProperty(MProperty property) {
		this.property = property;
	}
	public MColumnDefinition getColumnDefinition() {
		return columnDefinition;
	}
	public void setColumnDefinition(MColumnDefinition columnDefinition) {
		this.columnDefinition = columnDefinition;
	}
	
	
}
