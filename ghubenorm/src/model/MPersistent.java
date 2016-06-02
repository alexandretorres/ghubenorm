package model;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Embeddable
public class MPersistent {
	
	@OneToOne
	private MDataSource source;
	
	protected MPersistent() {
		
	}
	
	public MPersistent setDataSource(MDataSource ds) {
		this.source=ds;
		return this;
	}
	
	public MTable newTableSource(String name) {
		MTable t= MTable.newMTable(name);
		source=t;
		return t;
	}
	public MDataSource getSource() {
		return source;
	}
	
}
