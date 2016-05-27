package sjava;

public class Clazz {
	String name;
	boolean entity=false;
	CompilationUnit cunit;
	public Clazz(CompilationUnit cunit,String name) {
		this.cunit=cunit;
		this.name = name;
	}
	protected void x() {
		
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isEntity() {
		return entity;
	}
	public void setEntity(boolean entity) {
		this.entity = entity;
	}
	public String toString() {
		String ret = name;
		if (entity)
			ret=ret+"(entity)";
		return ret;
	}
	
}
class Method {
	
}
class Property {
	
}