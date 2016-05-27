package model;

import java.util.ArrayList;
import java.util.List;

public class MAssociationDef {
	boolean orphanRemoval;
	boolean enforce=false;
	private List<MJoinColumn> joinColumns = new ArrayList<MJoinColumn>();
	private MDataSource dataSource;
	
	public boolean isOrphanRemoval() {
		return orphanRemoval;
	}
	public void setOrphanRemoval(boolean orphanRemoval) {
		this.orphanRemoval = orphanRemoval;
	}
	public boolean isEnforce() {
		return enforce;
	}
	public void setEnforce(boolean enforce) {
		this.enforce = enforce;
	}
	public List<MJoinColumn> getJoinColumns() {
		return joinColumns;
	}
	public void setJoinColumns(List<MJoinColumn> joinColumns) {
		this.joinColumns = joinColumns;
	}
	
	/**
	 * ManyToMany join table
	 * @return
	 */
	public MDataSource getDataSource() {
		return dataSource;
	}
	public MAssociationDef setDataSource(MDataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}
	public MJoinColumn newJoingColumn(MColumnDefinition coldef) {
		MJoinColumn jc = MJoinColumn.newMJoinColumn(coldef);
		joinColumns.add(jc);
		return jc;
	}
	public MJoinColumn findJoinColumn(String name) {
		for (MJoinColumn j:joinColumns) {
			if (j.getColumn().getName().equalsIgnoreCase(name)) {
				return j;
			}
		}
		return null;
	}
	
	
}
