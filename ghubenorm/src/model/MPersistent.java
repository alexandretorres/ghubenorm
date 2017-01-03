package model;

import javax.persistence.Embeddable;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Embeddable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
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
	/**
	 * Return the tableSource that refers to the tabname hint. It will return the main table if there is a single table
	 * or if the tabname parameter is null. If the tabname is not null, the source is joined, and there is no such table,
	 * it returns null. It also returns null if there is no source.
	 * @param tabname
	 * @return
	 */
	public MTable getTableSource(String tabname) {		
		if (source instanceof MTableRef)
			return ((MTableRef)source).getTable();
		if (source instanceof MJoinedSource) {
			for (MTable tab:((MJoinedSource)source).getDefines()) {
				if (tabname==null || tab.getName().equalsIgnoreCase(tabname)) {
					return tab;
					
				}
			}
		} else if (source instanceof MTable) {
			//TODO: if tablename does not match this would be an error.			
			return ((MTable) source);			
			
		}
		return null;
	}
	public boolean hasTableSource(MTable tab) {	
		if (tab==null)
			return false;
		if (source instanceof MTableRef)
			return tab.equals( ((MTableRef)source).getTable());
		if (source instanceof MJoinedSource) {
			for (MTable jtab:((MJoinedSource)source).getDefines()) {
				if (tab.equals(jtab))
					return true;
			}
			return false;
		} else if (source instanceof MTable) {
			return tab.equals(source)	;	
			
		}
		return false;
	}
}
