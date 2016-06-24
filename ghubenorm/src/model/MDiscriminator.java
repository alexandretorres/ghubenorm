package model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
@Embeddable
public class MDiscriminator {
	@Column(name="discriminator")
	private String value;
	@OneToOne(cascade=CascadeType.ALL,orphanRemoval=true)
	@JoinColumn(name="discr_column")
	private MColumnDefinition column;
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public MColumnDefinition getColumn() {
		return column;
	}
	public void setColumn(MColumnDefinition column) {
		this.column = column;
	}
	
}
