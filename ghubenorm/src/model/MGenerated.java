package model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Enumerated;

@Embeddable
public class MGenerated {
	private boolean generated=false;
	private String generator;
	@Enumerated
	@Column(name="gen_strategy")
	private MGeneratorType type;
	
	public boolean isGenerated() {
		return generated;
	}
	public void setGenerated(boolean generated) {
		this.generated = generated;
	}
	public String getGenerator() {
		return generator;
	}
	public void setGenerator(String generator) {
		this.generator = generator;
	}
	public MGeneratorType getType() {
		return type;
	}
	public void setType(MGeneratorType type) {
		this.type = type;
	}
	
}
