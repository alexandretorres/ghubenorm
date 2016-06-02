package model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
@Entity
public class MAttributeOverride extends MOverride {
	@ManyToOne
	private MColumnDefinition column;
	
	protected MAttributeOverride() {}
	public static MAttributeOverride newMAttributeOverride(MColumnDefinition def,MProperty... props) {
		MAttributeOverride o = new MAttributeOverride(def);
		for (MProperty p:props) {
			if (p!=null)
				o.getProperties().add(p);
		}
		return o;
	}
	private MAttributeOverride(MColumnDefinition def) {
    	this.column = def;
    	
    }
	public MColumnDefinition getColumn() {
		return column;
	}

	public MAttributeOverride setColumn(MColumnDefinition column) {
		this.column = column;
		return this;
	}
	
}
