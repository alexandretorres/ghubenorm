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

	public boolean isNullableDef() {
		return column.isNullableDef();
	}

	public boolean isInsertableDef() {
		return column.isInsertableDef();
	}

	public boolean isUpdatableDef() {
		return column.isUpdatableDef();
	}

	public String getColumnDefinition() {
		return column.getColumnDefinition();
	}

	public int getLengthDef() {
		return column.getLengthDef();
	}

	public int getPrecisionDef() {
		return column.getPrecisionDef();
	}

	public int getScaleDef() {
		return column.getScaleDef();
	}

	public boolean isUniqueDef() {
		return column.isUniqueDef();
	}

	@Override
	public MTable getTable() {
		
		return column.getTable();
	}
	
}
