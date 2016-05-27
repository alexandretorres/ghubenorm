package model;

public class MProperty {
	
	private String name;
	private String type;
	private MClass typeClass;
	private MClass parent;
	
	private int max=1;
	private int min=0;
	
	private MColumnMapping columnMapping;
	private MAssociationMapping associationMapping;
	private boolean pk;
	private boolean embedded;
	private boolean ptransient;
	
	private MAssociation association;
	
	private MProperty(MClass parent) {
		this.parent=parent;
	}
	public static MProperty newMProperty(MClass parent) {
		return new MProperty(parent);
	}
	public String getName() {
		return name;
	}
	public MProperty setName(String name) {
		this.name = name;
		return this;
	}
	public String getType() {
		if (typeClass!=null)
			return typeClass.getName();
		return type;
	}
	public MProperty setType(String type) {
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
	public MProperty setColumnMapping(MColumnMapping columnMapping) {
		if (columnMapping!=null)
			columnMapping.setProperty(null);
		this.columnMapping = columnMapping;
		if (columnMapping!=null)
			columnMapping.setProperty(this);
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
	public MAssociationDef getOrInitAssociationDef() {
		if (associationMapping==null)
			associationMapping=MAssociationMapping.newMAssociationMapping(new MAssociationDef());
		return associationMapping.getValue();
	}
	public MAssociationMapping getAssociationMapping() {
		return associationMapping;
	}
	public MProperty setAssociationMapping(MAssociationMapping associationMapping) {
		if (associationMapping!=null)
			associationMapping.setProperty(null);
		this.associationMapping = associationMapping;
		if (associationMapping!=null)
			associationMapping.setProperty(this);
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
	
}
