package model;

import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

import common.ReflectiveVisitor;
import common.Util;
import common.Visitable;

@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
public class MProperty implements Visitable {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private String name;
	@Column(length=1024)
	private String type;	
	private int max=1;
	private int min=0;
	private boolean pk;
	private boolean embedded;
	private boolean ptransient;
	@Embedded
	private MColumnMapping columnMapping;
	@Embedded
	private MAssociationMapping associationMapping;
	@ManyToOne
	private MClass typeClass;
	@ManyToOne(optional=false)
	private MClass parent;	
	@OneToOne(fetch=FetchType.LAZY,cascade=CascadeType.ALL)
	private MAssociation association;
	//Lazy Fetch prevents duplicate loading due to outer join
	@OneToOne(fetch=FetchType.LAZY,mappedBy="to",cascade=CascadeType.PERSIST)
	private MAssociation toAssociation;
	
	private boolean derived=false;
	@Embedded	
	private MGenerated generated=new MGenerated();
	
	/**
	 * Used for polymorphic associations when a type column specifies the type of the association
	 */
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="value",column=@Column(name="polymorphic_value"))		
	})
	@AssociationOverride(name="column",joinColumns=@JoinColumn(name="polymorphic_discr"))
	private MDiscriminator discriminatorColumn;
	
	protected MProperty() {
		
	}
	private MProperty(MClass parent) {
		this.parent=parent;
	}
	public static MProperty newMProperty(MClass parent) {
		return new MProperty(parent);
	}	
	public int getId() { return id;	}
	public void setId(int id) {	this.id = id;	}
	public String getName() {
		return name;
	}
	public MProperty setName(String name) {
		name= Util.capSize(name,255);
		this.name = name;
		return this;
	}
	public String getType() {
		if (typeClass!=null)
			return typeClass.getName();
		return type;
	}
	public MProperty setType(String type) {
		type= Util.capSize(type,1024);
		this.type = type;
		return this;
	}
	public int getMax() {
		return max;
	}
	public MProperty setMax(int max) {
		this.max = max;
		return this;
	}
	public int getMin() {
		return min;
	}
	public MProperty setMin(int min) {
		this.min = min;
		return this;
	}
	public MClass getTypeClass() {
		return typeClass;
	}
	
	public MProperty setTypeClass(MClass typeClass) {
		this.typeClass = typeClass;
		return this;
	}
	public MColumnMapping getColumnMapping() {
		return columnMapping;
	}
	public MColumnDefinition getColumnDef() {
		if (columnMapping!=null && columnMapping.columnDefinition!=null)
			return columnMapping.columnDefinition;
		return null;
	}
	public MProperty setColumnMapping(MColumnMapping columnMapping) {
		//if (columnMapping!=null)
		//	columnMapping.setProperty(null);
		this.columnMapping = columnMapping;
		//if (columnMapping!=null)
		//	columnMapping.setProperty(this);
		return this;
	}
	
	public boolean isPk() {
		return pk;
	}
	public MProperty setPk(boolean pk) {
		this.pk = pk;
		return this;
	}
	
	public boolean isEmbedded() {
		return embedded;
	}
	public MProperty setEmbedded(boolean embedded) {
		this.embedded = embedded;
		return this;
	}
	public MAssociationDef getAssociationDef() {
		if (associationMapping!=null)
			return associationMapping.getValue();
		return null;
	}
	public MAssociationDef getOrInitAssociationDef() {
		if (associationMapping==null)
			associationMapping=MAssociationMapping.newMAssociationMapping(new MAssociationDef());
		return associationMapping.getValue();
	}
	public MAssociationMapping getAssociationMapping() {
		return associationMapping;
	}
	public MProperty setAssociationMapping(MAssociationMapping associationMapping) {
		//if (associationMapping!=null)
		//	associationMapping.setProperty(null);
		this.associationMapping = associationMapping;
		//if (associationMapping!=null)
		//	associationMapping.setProperty(this);
		return this;
	}
	public MClass getParent() {
		return parent;
	}
	public MAssociation getAssociation() {
		return association;
	}
	void setAssociation(MAssociation association) {
		this.association = association;
	}
	public boolean isTransient() {
		return ptransient;
	}
	public MProperty setTransient(boolean t) {
		this.ptransient=t;
		return this;
	}
	public MAssociation getToAssociation() {
		return toAssociation;
	}
	public void setToAssociation(MAssociation toAssociation) {
		this.toAssociation = toAssociation;
	}
	
	public MDiscriminator getDiscriminatorColumn() {
		if (discriminatorColumn==null)
			discriminatorColumn = new MDiscriminator();
		return discriminatorColumn;
	}
	protected void setDiscriminatorColumn(MDiscriminator discriminatorColumn) {
		this.discriminatorColumn = discriminatorColumn;
	}
	
	public boolean isDerived() {
		return derived;
	}
	public MProperty setDerived(boolean derived) {
		this.derived = derived;
		return this;
	}
	

	public MGenerated getGenerated() {
		
		return generated;
	}
	protected void setGenerated(MGenerated generated) {
		this.generated = generated;
	}
	public MProperty setGenerated() {
		generated.setGenerated(true);
		return this;
	}
	@Override	
	public void accept(ReflectiveVisitor visitor) {
		visitor.callAccept(getAssociationDef());
		visitor.callAccept(association);
		visitor.visit(this);
	}
	public String toString() {
		return (parent==null ? "" : parent.getName()+".")+ name+" "+super.toString();
	}
}
