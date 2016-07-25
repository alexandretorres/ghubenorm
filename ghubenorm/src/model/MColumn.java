package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
public class MColumn extends MColumnDefinition{
	//TODO: Use nullable types for length, precision and so forth. 
//	@Transient public static final int DEFAULT_LENGTH = 255;	
	@Transient public static final int DEFAULT_SCALE = 0;	
	private String name;
	private boolean nullable=true;
	private boolean insertable=true;
	private boolean updatable=true;
	private String colummnDefinition;
	private Integer length;
	private int precision;
	private int scale;
	@Column(name="isUnique")
	private boolean unique;
	@ManyToOne(optional=true)
	private MTable table;
	/**
	 * This property was not included on the original ENORM
	 */
	private String defaulValue;
	
	
	public static MColumn newMColumn() {
		return new MColumn();
	}
	protected MColumn() {}
	
	public String getName() {
		return name;
	}
	public MColumn setName(String name) {
		this.name = name;
		return this;
	}
	public boolean isNullable() {
		return nullable;
	}
	public MColumn setNullable(boolean nullable) {
		this.nullable = nullable;
		return this;
	}
	public boolean isInsertable() {
		return insertable;
	}
	public MColumn setInsertable(boolean insertable) {
		this.insertable = insertable;
		return this;
	}
	public boolean isUpdatable() {
		return updatable;
	}
	public MColumn setUpdatable(boolean updatable) {
		this.updatable = updatable;
		return this;
	}
	public String getColummnDefinition() {
		return colummnDefinition;
	}
	public MColumn setColummnDefinition(String colummnDefinition) {
		this.colummnDefinition = colummnDefinition;
		return this;
	}
	public Integer getLength() {
		return length;
	}
	public MColumn setLength(Integer length) {
		/*if (length==null)
			this.length = DEFAULT_LENGTH;
		else*/
		this.length = length;
		return this;
	}
	public int getPrecision() {
		return precision;
	}
	public MColumn setPrecision(Integer precision) {
		if (precision==null)
			this.precision = DEFAULT_SCALE;
		else
			this.precision = precision;
		return this;
	}
	public int getScale() {
		return scale;
	}
	public MColumn setScale(Integer scale) {
		if (scale==null)
			this.scale = DEFAULT_SCALE;
		else
			this.scale = scale;
		return this;
	}
	public boolean isUnique() {
		return unique;
	}
	public MColumn setUnique(boolean unique) {
		this.unique = unique;
		return this;
	}
	public String getDefaulValue() {
		return defaulValue;
	}
	public void setDefaulValue(String defaulValue) {
		this.defaulValue = defaulValue;
	}
	public MTable getTable() {
		return table;
	}
	public MColumn setTable(MTable table) {
		this.table = table;
		return this;
	}
	@Override
	public MColumn getColumn() {		
		return this;
	}
	
	
}
