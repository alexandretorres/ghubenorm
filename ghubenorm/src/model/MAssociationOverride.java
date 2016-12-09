package model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;
@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
public class MAssociationOverride extends MOverride {
	@ManyToOne(cascade=CascadeType.ALL)
	private MAssociationDef def;
	public static MAssociationOverride newMAssociationOverride(MClass clazz) {
		return new MAssociationOverride( clazz);
	}
	public static MAssociationOverride newMAssociationOverride(MClass clazz,MProperty... props) {
		MAssociationOverride o = new MAssociationOverride(clazz);
		for (MProperty p:props) {
			if (p!=null)
				o.getProperties().add(p);
		}
		return o;
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
	public MAssociationDef getOrInitAssociationDef() {
		if (def==null)
			def=new MAssociationDef();
		return def;
	}
	
}
