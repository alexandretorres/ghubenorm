package model;

public class MColumn extends MColumnDefinition{
	public static final int DEFAULT_LENGTH = 255;
	public static final int DEFAULT_SCALE = 0;
	private String name;
	private boolean nullable;
	private boolean insertable;
	private boolean updatable;
	private String colummnDefinition;
	private int length;
	private int precision;
	private int scale;
	private boolean unique;
	private MTable table;
	/**
	 * This property was not included on the original ENORM
	 */
	private String defaulValue;
	
	public static MColumn newMColumn() {
		return new MColumn();
	}
	private MColumn() {
		
	}
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
	public int getLength() {
		return length;
	}
	public MColumn setLength(Integer length) {
		if (length==null)
			this.length = DEFAULT_LENGTH;
		else
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
	public void setTable(MTable table) {
		this.table = table;
	}
	
	
}
