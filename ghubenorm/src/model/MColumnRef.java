package model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class MColumnRef extends MColumnDefinition { 
	@ManyToOne
	private MColumn column;
	
	protected MColumnRef() {}
	public static MColumnRef newMColumnRef(MColumn column) {
		return new MColumnRef(column);
	}

	private MColumnRef(MColumn column) {
		super();
		this.column = column;
	}

	public MColumn getColumn() {
		return column;
	}

	public void setColumn(MColumn column) {
		this.column = column;
	}
	//delegates
	public String getName() {
		return column.getName();
	}

	public boolean isNullable() {
		return column.isNullable();
	}

	public boolean isInsertable() {
		return column.isInsertable();
	}

	public boolean isUpdatable() {
		return column.isUpdatable();
	}

	public String getColummnDefinition() {
		return column.getColummnDefinition();
	}

	public int getLength() {
		return column.getLength();
	}

	public int getPrecision() {
		return column.getPrecision();
	}

	public int getScale() {
		return column.getScale();
	}

	public boolean isUnique() {
		return column.isUnique();
	}

	@Override
	public MTable getTable() {
		
		return column.getTable();
	}
	
}
