package model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Embeddable
public class MPersistent {	
	private boolean persistent=false;
	@OneToOne(optional=true)
	private MDataSource source;
	
	protected MPersistent() {
		
	}
	
	public MPersistent setDataSource(MDataSource ds) {
		if (this.source instanceof MJoinedSource && ds instanceof MTable) {
			MTable tab = (MTable)ds;			
			((MJoinedSource) source).addTable(tab);
		} else
			this.source=ds;
		return this;
	}	
	
	public MDataSource getSource() {
		return source;
	}

	public boolean isPersistent() {
		return persistent;
	}

	protected MPersistent setPersistent(boolean persistent) {
		this.persistent = persistent;
		return this;
	}
	
}
