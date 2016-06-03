package model;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Embeddable
public class MColumnMapping {
	
	@OneToOne
	MColumnDefinition columnDefinition;
	
	protected MColumnMapping() {}
	public static MColumnMapping newMColumnMapping(MColumnDefinition columnDefinition) {
		return new MColumnMapping( columnDefinition);
	}
	private MColumnMapping( MColumnDefinition columnDefinition) {
		super();
	
		this.columnDefinition = columnDefinition;
	}
	
	public MColumnDefinition getColumnDefinition() {
		return columnDefinition;
	}
	public void setColumnDefinition(MColumnDefinition columnDefinition) {
		this.columnDefinition = columnDefinition;
	}
	
	
}
