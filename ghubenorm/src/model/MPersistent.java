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
	public MTable getMainTable() {
		if (source instanceof MJoinedSource) {
			MJoinedSource jds =(MJoinedSource) source;
			if (jds.getDefines().isEmpty())
				return null;
			else
				return jds.getDefines().get(0);
		} else if (source instanceof MTableRef) {
			return ((MTableRef)source).getTable();
		} else
			return (MTable) source;
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
