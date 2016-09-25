package model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import common.Util;
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
	
	public boolean checkOverride() {
		return !column.getColumn().isDummy();
		/*
		
		MColumn defCol = null;		
		MColumn cc ;
		for (MProperty p:this.getProperties()) {
			if (p.getColumnDef()!=null && (cc = p.getColumnDef().getColumn())!=null) {
				defCol = cc;
				break;
			}
			//TODO: check an override for this property at the context class			
		}
		if (defCol==null)
			defCol=MColumn.DEFAULT_COLUMN;
		if (!Util.equals(column.getName(),defCol.getName()))
			return true;
		if (column.isInsertable()!=defCol.isInsertable())
			return true;
		if (column.isNullable()!=defCol.isNullable())
			return true;		
		if (column.isUnique()!=defCol.isUnique())
			return true;
		if (column.isUpdatable()!=defCol.isUpdatable())
			return true;
		if (!Util.equals(column.getColummnDefinition(),defCol.getColummnDefinition()))
			return true;
		if (!Util.equals(column.getLength(),defCol.getLength()))
			return true;
		if (!Util.equals(column.getPrecision(),defCol.getPrecision()))
			return true;
		if (!Util.equals(column.getScale(),defCol.getScale()))
			return true;
		if (!Util.equals(column.getTable(),defCol.getTable())) {
			if (column.getTable()!=null && column.getTable().getName()!=null)
				return true;
		}
	
		
		return false;*/
	}
}
