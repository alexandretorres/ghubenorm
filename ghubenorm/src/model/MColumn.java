package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

import common.Util;

@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
public class MColumn extends MColumnDefinition{
	//TODO: Use nullable types for length, precision and so forth. 
	@Transient public static final int DEFAULT_LENGTH = 255;	
	@Transient public static final int DEFAULT_SCALE = 0;	
	@Transient public static final MColumn DEFAULT_COLUMN = createDefaultColumn();
	@Transient public static final String ID_COLUMN_NAME = "<id>";
	private String name;
	private Boolean nullable;
	private Boolean insertable;
	private Boolean updatable;
	@Column(length=1024)
	private String columnDefinition;
	private Integer length;
	private Integer precision;
	private Integer scale;
	@Column(name="isUnique")
	private Boolean unique;
	@ManyToOne(optional=true)
	private MTable table;
	/**
	 * This property was not included on the original ENORM
	 */
	@Column(length=2048)
	private String defaultValue;
	
	private static MColumn createDefaultColumn() {
		MColumn c = new MColumn();
		c.setScale(DEFAULT_SCALE);
		c.setLength(DEFAULT_LENGTH);
		c.nullable=true;
		c.insertable=true;
		c.updatable=true;
		c.unique=false;
		c.precision=0;
		return c;
	}
	public static MColumn newMColumn() {
		return new MColumn();
	}
	protected MColumn() {}
	
	public String getName() {
		return name;
	}
	public MColumn setName(String name) {
		name= Util.capSize(name,255);
		this.name = name;
		return this;
	}
	public Boolean isNullable() {
		return nullable;
	}
	public MColumn setNullable(Boolean nullable) {
		this.nullable = nullable;
		return this;
	}
	public Boolean isInsertable() {
		return insertable;
	}
	public MColumn setInsertable(Boolean insertable) {
		this.insertable = insertable;
		return this;
	}
	public Boolean isUpdatable() {
		return updatable;
	}
	public MColumn setUpdatable(Boolean updatable) {
		this.updatable = updatable;
		return this;
	}
	public String getColumnDefinition() {
		return columnDefinition;
	}
	public MColumn setColumnDefinition(String columnDefinition) {
		columnDefinition= Util.capSize(columnDefinition,1024);		
		this.columnDefinition = columnDefinition;
		return this;
	}
	public Integer getLength() {
		return length;
	}
	public MColumn setLength(Integer length) {		
		this.length = length;
		return this;
	}
	public Integer getPrecision() {
		return precision;
	}
	public MColumn setPrecision(Integer precision) {
		this.precision = precision;
		return this;
	}
	public Integer getScale() {
		return scale;
	}
	public MColumn setScale(Integer scale) {
		this.scale = scale;
		return this;
	}
	public Boolean isUnique() {
		return unique;
	}
	public MColumn setUnique(Boolean unique) {
		this.unique = unique;
		return this;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		defaultValue= Util.capSize(defaultValue,2048);		
		this.defaultValue = defaultValue;
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
	@Override
	public boolean isNullableDef() {
		if (nullable==null)
			return DEFAULT_COLUMN.nullable;
		return nullable;
	}
	@Override
	public boolean isInsertableDef() {
		if (insertable==null)
			return DEFAULT_COLUMN.insertable;
		return insertable;
	}
	@Override
	public boolean isUpdatableDef() {
		if (updatable==null)
			return DEFAULT_COLUMN.updatable;
		return updatable;
	}
	
	@Override
	public int getLengthDef() {
		if (length==null)
			return DEFAULT_COLUMN.length;
		return length;
	}
	@Override
	public int getPrecisionDef() {
		if (precision==null)
			return DEFAULT_COLUMN.precision;
		return precision;
	}
	@Override
	public int getScaleDef() {
		if (scale==null)
			return DEFAULT_COLUMN.scale;
		return scale;
	}
	@Override
	public boolean isUniqueDef() {
		if (unique==null)
			return DEFAULT_COLUMN.unique;
		return unique;
	}
	public boolean isDummy() {
		boolean ret = this.insertable==null && this.nullable==null && this.unique==null && this.updatable==null
				&& this.columnDefinition==null && this.defaultValue==null && this.length==null &&
				this.name==null && this.precision==null && this.scale==null;
		ret = ret && (table==null || table.isDummy());
		return ret;
	}
	public String toString() {
		return (table==null ? "" : table.getName()+".")+ name+" "+super.toString();
	}
}
