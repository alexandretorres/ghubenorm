package sjava;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.MappedSuperclass;

//IdClass=Type
public enum JPATags {
	Entity,Id,IdClass,EmbeddedId,
	Column,Table,SecondaryTable,SecondaryTables,PrimaryKeyJoinColumn,PrimaryKeyJoinColumns,
	OneToMany,ManyToOne,OneToOne,ManyToMany,Embedded,
	Inheritance,MappedSuperclass,DiscriminatorValue,DiscriminatorColumn,
	//parei aqui de olhar
	AssociationOverride,AssociationOverrides,AttributeOverride,AttributeOverrides,CollectionTable,
	JoinColumn,JoinColumns,JoinTable,
	ElementCollection,Embeddable,Temporal,Basic,Transient,
	ForeignKey,Index,OrderBy,OrderColumn,UniqueConstraint,
	GeneratedValue,SequenceGenerator,TableGenerator,
	MapKey,MapKeyClass,MapKeyColumn,MapKeyEnumerated,MapKeyJoinColumn,MapKeyJoinColumns,MapKeyTemporal,MapsId,
	Access,
	;
	
	//And the BASIC that has fetch for properties?
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
	public List<Annotation> findAnnotations(Collection<Annotation> sannots,JCompilationUnit unit) {
		return sannots.stream().filter(a->this.isType(a, unit)).collect(Collectors.toList());	
	}
	
}
