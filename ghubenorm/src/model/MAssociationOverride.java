package model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
@Entity
public class MAssociationOverride extends MOverride {
	@ManyToOne
	private MAssociationDef def;
	public static MAssociationOverride newMAssociationOverride(MClass clazz) {
		return new MAssociationOverride( clazz);
	}
	protected MAssociationOverride() {}
	protected MAssociationOverride(MClass clazz) {
		setClazz(clazz);
		clazz.getOverrides().add(this);
	}
	public MAssociationDef getDef() {
		return def;
	}

	public MAssociationOverride setDef(MAssociationDef def) {
		this.def = def;
		return this;
	}
	
	
}
