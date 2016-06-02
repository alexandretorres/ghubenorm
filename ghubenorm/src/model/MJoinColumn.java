package model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class MJoinColumn {
	@Id
	private int id;
	@ManyToOne(optional=false)
	private MAssociationDef associationDef;
	@ManyToOne(optional=false)
	private MColumnDefinition column;
	@ManyToOne
	private MColumnDefinition inverse;
	
	protected MJoinColumn() {}
	public static MJoinColumn newMJoinColumn(MColumnDefinition column) {
		return new MJoinColumn(column);
	}
	private MJoinColumn(MColumnDefinition column) {
		this.column = column;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public MColumnDefinition getColumn() {
		return column;
	}
	public void setColumn(MColumnDefinition column) {
		this.column = column;
	}
	public MColumnDefinition getInverse() {
		return inverse;
	}
	public void setInverse(MColumnDefinition inverse) {
		this.inverse = inverse;
	}
	public MColumnDefinition getColumnForProperty(MProperty p) {
		if (p==null)
			return null;
		if (p.getAssociation()!=null) {
			if (p.getAssociation().getFrom().equals(p)) {
				return column;
			} else if (p.getAssociation().getTo().equals(p)) {
				return inverse;
			}
		}
		return null;
	}
}
