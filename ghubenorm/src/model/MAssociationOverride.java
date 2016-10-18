package model;

import javax.persistence.ManyToOne;

public class MAssociationOverride extends MOverride {
	@ManyToOne
	private MAssociationDef def;

	public MAssociationDef getDef() {
		return def;
	}

	public MAssociationOverride setDef(MAssociationDef def) {
		this.def = def;
		return this;
	}
	
	
}
