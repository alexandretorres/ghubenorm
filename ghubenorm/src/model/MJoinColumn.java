package model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class MJoinColumn {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	@ManyToOne
	private MAssociationDef associationDef;
	@ManyToOne(optional=false)
	private MColumnDefinition column;
	@ManyToOne
	private MColumnDefinition inverse;
	@ManyToOne
	private MVertical generalization;
	
	protected MJoinColumn() {}
	public static MJoinColumn newMJoinColumn(MAssociationDef adef,MColumnDefinition column) {
		return new MJoinColumn(adef,column);
	}
	public static MJoinColumn newMJoinColumn(MVertical gen,MColumn column) {
		MJoinColumn ret= new MJoinColumn();
		ret.column=column;
		ret.generalization=gen;
		return ret;
	}
	private MJoinColumn(MAssociationDef adef,MColumnDefinition column) {
		this.associationDef = adef;
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
	
	public MVertical getGeneralization() {
		return generalization;
	}
	protected void setGeneralization(MVertical generalization) {
		this.generalization = generalization;
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
	public MAssociationDef getAssociationDef() {
		return associationDef;
	}
	private void setAssociationDef(MAssociationDef associationDef) {
		this.associationDef = associationDef;
	}
	
}
