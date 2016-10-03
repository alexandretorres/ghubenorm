package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
@Entity
public class MTable extends MDataSource {
	
	private String name;
	private String catalog;
	private String schema;

	@OneToMany(mappedBy="table",cascade=CascadeType.PERSIST)
	private Set<MColumn> columns=new HashSet<MColumn>();
	@OneToMany(mappedBy="table",cascade=CascadeType.ALL,orphanRemoval=true)
	private Set<MDefinition> definitons = new HashSet<MDefinition>();
	public static MTable newMTable(Repo repo,String name) {
		return new MTable(repo, name);
	}	
	protected MTable() { }
	protected MTable(Repo repo,String name) {
		setRepo(repo);
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
	public Set<MColumn> getColumns() {
		return columns;
	}
	protected MTable setColumns(Set<MColumn> columns) {
		this.columns = columns;
		return this;
	}
	public MColumn addColumn() {
		if (columns==null)
			columns = new HashSet<MColumn>();
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
	public MDefinition newIndex(MColumn... cols) {
		MDefinition def = new MDefinition(this);
		definitons.add(def);
		def.addColumns(cols).setType(MDefinitionType.INDEX);
		return def;
		
	}
	public Set<MDefinition> getDefinitions() {
		return definitons;
	}
	public String toString() {
		return super.toString()+ (getRepo()!=null ? "Repo:"+getRepo().getName()+" - " : "" ) + this.getName();
	}
	@Override
	public String printName() {
		
		return name;
	}
	public boolean isDummy() {
		return name==null;
	}
	
}
