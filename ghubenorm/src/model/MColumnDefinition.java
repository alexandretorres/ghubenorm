package model;

public abstract class MColumnDefinition {
	
	public abstract String getName();

	public abstract boolean isNullable();

	public abstract boolean isInsertable();

	public abstract boolean isUpdatable();

	public abstract String getColummnDefinition() ;

	public abstract int getLength();

	public abstract int getPrecision();

	public abstract int getScale();

	public abstract boolean isUnique();
	
	public abstract MTable getTable();
}
