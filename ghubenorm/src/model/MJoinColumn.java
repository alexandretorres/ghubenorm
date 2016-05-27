package model;

public class MJoinColumn {
	
	private MColumnDefinition column;
	private MColumnDefinition inverse;
	
	public static MJoinColumn newMJoinColumn(MColumnDefinition column) {
		return new MJoinColumn(column);
	}
	private MJoinColumn(MColumnDefinition column) {
		this.column = column;
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
