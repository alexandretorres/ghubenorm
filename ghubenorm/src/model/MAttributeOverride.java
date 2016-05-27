package model;

import java.util.List;

public class MAttributeOverride extends MOverride {
	private MColumnDefinition column;
	
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
