package model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
/**
 * This entity must be added to entity manager
 * @author torres
 *
 */
@Entity
public class MAssociationDef {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private MCascadeType cascade;
	boolean orphanRemoval;
	boolean enforce=false;
	@OneToMany(mappedBy="associationDef",cascade=CascadeType.ALL)
	private List<MJoinColumn> joinColumns = new ArrayList<MJoinColumn>();
	
	/**
	 * Association table used on many to many
	 * OR
	 * Table used on OneToMany unidirectional
	 * OR
	 * Table used for storing collections of embedded elements 
	 */
	@ManyToOne
	private MDataSource dataSource;
	
	protected MAssociationDef() {}	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
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
		MJoinColumn jc = MJoinColumn.newMJoinColumn(this,coldef);		
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

	public MCascadeType getCascade() {
		return cascade;
	}

	public void setCascade(MCascadeType cascade) {
		this.cascade = cascade;
	}
	
	public void addCascade(MCascadeType cascadeOp) {
		if (cascadeOp!=null)
			this.cascade=cascadeOp.add(this.cascade);
	}
}
