package model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import common.ReflectiveVisitor;

@Entity
public class MTableRef extends MDataSource {
	@ManyToOne
	private MTable table;

	public MTable getTable() {
		return table;
	}

	public void setTable(MTable table) {
		this.table = table;
	}
	@Override
	public String printName() {
		
		return table.getName();
	}

	@Override
	public void accept(ReflectiveVisitor visitor) {
		visitor.visit(this);
		
	}
}
