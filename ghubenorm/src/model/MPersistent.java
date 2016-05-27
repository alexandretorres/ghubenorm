package model;

public class MPersistent {
	private MDataSource source;
	MPersistent() {
		
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
