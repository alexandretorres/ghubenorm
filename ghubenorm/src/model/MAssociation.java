package model;

public class MAssociation {
	
	private MProperty from;
	private MProperty to;

	
	private boolean navigableFrom;
	private boolean navigableTo;
	
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
		to.setAssociation(this);
	}
	private MAssociation(MProperty from) {
		super();
		this.from = from;
		
		from.setAssociation(this);		
	}
	public MProperty getFrom() {
		return from;
	}
	public MAssociation setFrom(MProperty from) {
		this.from = from;
		from.setAssociation(this);
		return this;
	}
	public MProperty getTo() {
		return to;
	}
	public MAssociation setTo(MProperty to) {
		this.to = to;
		to.setAssociation(this);
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

public MAssociation swap() {		
		MProperty tmp = to;
		to=from;
		from=tmp;
		return this;
	}
}
