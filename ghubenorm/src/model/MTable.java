package model;

import java.util.ArrayList;
import java.util.List;

public class MTable extends MDataSource {
	
	private String name;
	private String catalog;
	private String schema;
	private List<MColumn> columns=new ArrayList<MColumn>();
	
	public static MTable newMTable(String name) {
		return new MTable( name);
	}
	private MTable(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}
	public MTable setName(String name) {
		this.name = name;
		return this;
	}
	public String getCatalog() {
		return catalog;
	}
	public MTable setCatalog(String catalog) {
		this.catalog = catalog;
		return this;
	}
	public String getSchema() {
		return schema;
	}
	public MTable setSchema(String schema) {
		this.schema = schema;
		return this;
	}
	public List<MColumn> getColumns() {
		return columns;
	}
	public MTable setColumns(List<MColumn> columns) {
		this.columns = columns;
		return this;
	}
	public MColumn addColumn() {
		if (columns==null)
			columns = new ArrayList<MColumn>();
		MColumn c = MColumn.newMColumn();
		c.setTable(this);
		columns.add(c);
		return c;
	}
	public MColumn findColumn(String name) {
		for (MColumn c:columns) {
			if (c.getName().equalsIgnoreCase(name))
				return c;
		}
		return null;
	}
	
}
