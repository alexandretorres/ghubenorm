package model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
@Entity
public class MDefinition {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	@ManyToOne
	private MTable table;	
	@ManyToMany
	@OrderColumn(name="pos")
	private List<MColumn> columns=new ArrayList<MColumn>();
	@Column(nullable=false)
	private MDefinitionType type;
	private String name;
	private String body;
	
	protected MDefinition() {
		
	}
	protected MDefinition(MTable table) {
		this.table=table;
	}
	public int getId() {		return id;	}
	public void setId(int id) {		this.id = id;	}
	
	protected List<MColumn> getColumns() {
		return columns;
	}
	protected void setColumns(List<MColumn> columns) {
		this.columns = columns;
	}
	public MDefinitionType getType() {
		return type;
	}
	public MDefinition setType(MDefinitionType type) {
		this.type = type;
		return this;
	}
	public String getName() {
		return name;
	}
	public MDefinition setName(String name) {
		this.name = name;
		return this;
	}
	public String getBody() {
		return body;
	}
	public MDefinition setBody(String body) {
		this.body = body;
		return this;
	}
	protected MTable getTable() {
		return table;
	}
	protected void setTable(MTable table) {
		this.table = table;
	}
	public MDefinition addColumns(MColumn... cols) {
		for (MColumn col:cols)
			columns.add(col);
		return this;
	}
	
}
