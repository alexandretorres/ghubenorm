package sjava;

import java.util.Collection;

import javax.persistence.MappedSuperclass;

//IdClass=Type
public enum JPATags {
	Entity,Id,IdClass,EmbeddedId,
	Column,Table,SecondaryTable,SecondaryTables,PrimaryKeyJoinColumn,PrimaryKeyJoinColumns,
	OneToMany,ManyToOne,OneToOne,ManyToMany,Embedded,
	Inheritance,MappedSuperclass,DiscriminatorValue,DiscriminatorColumn;
	String path;
	JPATags() {
		path="javax.persistence";
	}
	JPATags(String path) {
		this.path=path;
	}
	public boolean isImport(String s) {
		if (s==null)
			return false;
		if (s.equals(path+"."+this.name())) {
			return true;
		}
		if (s.endsWith(".*")) {
			s = s.substring(0,s.length()-2);
			return s.equals(path);
		}
		return false;
	}
	public boolean isImport(Import imp) {
		return isImport(imp.getFrom());
	}
	private boolean isType(Annotation a) {
		return (a.type!=null && a.type.equals(this.name()));			
		
	}
	public boolean isType(Annotation a,JCompilationUnit comp) {
		if (isType(a)) {
			return comp.importsTag(this);
		} else
			return false;
	}
	public Annotation findAnnotation(Collection<Annotation> sannots,JCompilationUnit unit) {
		return sannots.stream().filter(a->this.isType(a, unit)).findFirst().orElse(null);		
	}
	
}
