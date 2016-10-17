package model;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class MAssociation {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	@OneToOne(mappedBy="association")
	private MProperty from;
	@OneToOne
	private MProperty to;	
	private boolean navigableFrom;
	private boolean navigableTo;
	/**
	 * A polymorphic association is weird thing of Ruby. It is not bidirectional. It is not an association with "more than 2 properties". 
	 * We don´t want to break the "to" one-to-one pointing to bidirectional associations. In current UML, the properties of one association cannot be
	 * shared with other associations. So we just record the "as" clause here.
	 */
	private String polymorphicAs;
	/**
	 * this is the (..)ToOne/Many in case this is an unidirectional association. 1 is one, -1 is many
	 */
	@Basic(optional=false)
	private int max=1;
	
	protected MAssociation() {}
	public static MAssociation newMAssociation(MProperty from) {
		return new MAssociation(from);
	}
	public static MAssociation newMAssociation(MProperty from, MProperty to) {
		return new MAssociation(from, to);
	}
	
	private MAssociation(MProperty from, MProperty to) {
		super();
		this.from = from;
		this.to = to;
		from.setAssociation(this);
		to.setToAssociation(this);
		max = to.getMax();
		//to.setAssociation(this); //This violates ONE TO ONE
	}
	private MAssociation(MProperty from) {
		super();
		this.from = from;
		
		from.setAssociation(this);		
	}
	public int getId() { return id;	}
	public void setId(int id) {	this.id = id;	}
	public MProperty getFrom() {
		return from;
	}
	public MAssociation setFrom(MProperty from) {
		if (from!=null)
			from.setAssociation(null);
		this.from = from;
		from.setAssociation(this);
		return this;
	}
	public MProperty getTo() {
		return to;
	}
	public MAssociation setTo(MProperty to) {
		this.to = to;
		if (to!=null)
			to.setToAssociation(this);
		return this;
	}
	public boolean isNavigableFrom() {
		return navigableFrom;
	}
	public MAssociation setNavigableFrom(boolean navigableFrom) {
		this.navigableFrom = navigableFrom;
		return this;
	}
	public boolean isNavigableTo() {
		return navigableTo;
	}
	public MAssociation setNavigableTo(boolean navigableTo) {
		this.navigableTo = navigableTo;
		return this;
	}
	public MProperty getInverse(MProperty p) {
		if (p.equals(to))
			return from;
		else
			return to;
	}
	public MClass getToClass() {
		return from.getTypeClass();
	}

	public String getPolymorphicAs() {
		return polymorphicAs;
	}
	public MAssociation setPolymorphicAs(String polymorphicAs) {
		this.polymorphicAs = polymorphicAs;
		return this;
	}
	public MAssociation swap() {		
		MProperty tmp = to;
		to=from;
		to.setAssociation(null);
		to.setToAssociation(this);;		
		from=tmp;
		from.setAssociation(this);
		from.setToAssociation(null);
		max = to.getMax();
		return this;
	}
	public int getMax() {
		return max;
	}
	public MAssociation setMax(int max) {
		this.max = max;
		return this;
	}
	
}
