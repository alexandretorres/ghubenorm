package model;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Embeddable
public class MAssociationMapping {
	@OneToOne(cascade=CascadeType.ALL)
	private MAssociationDef value;
	
	
	protected MAssociationMapping() {}
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
	
	
}
