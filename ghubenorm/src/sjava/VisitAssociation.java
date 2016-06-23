package sjava;

import java.util.List;

import javax.persistence.OneToMany;

import common.LateVisitor;
import model.MAssociation;
import model.MClass;
import model.MProperty;
import static sjava.JPATags.*;

public class VisitAssociation implements LateVisitor<MProperty> {
	MProperty prop;
	JCompilationUnit unit;
	Annotation assoc;
	List<Annotation> annotations;
	
	public VisitAssociation(MProperty prop, JCompilationUnit unit, Annotation assoc, List<Annotation> annotations) {
		super();
		this.prop = prop;
		this.unit = unit;
		this.assoc = assoc;
		this.annotations = annotations;
	}

	@Override
	public MProperty exec() {
		String inverse = assoc.getValueAsString("mappedBy");
		boolean optional = !(assoc.getValue("optional")==Boolean.FALSE); //NULL,TRUE=>False otherwise True
		if (!optional)
			prop.setMin(1);
		MClass typeClass = prop.getTypeClass();
		if (OneToMany.isType(assoc,unit) || ManyToMany.isType(assoc,unit)) {
			prop.setMax(-1);
			int[] interval = new int[] {prop.getType().indexOf("<"),prop.getType().indexOf(">")};
			if (interval[0]>=0 && interval[1]>=0 && interval[0]<interval[1]) {
				String typeName = prop.getType().substring(interval[0]+1,interval[1]);
				typeClass = unit.getClazz(typeName);
				if (typeClass!=null) {
					prop.setTypeClass(typeClass);
				}
			}
		} else {
			prop.setMax(1);
			String typeName = prop.getType();
			int pos = typeName.indexOf("<");
			if (pos>0) {
				typeName = typeName.substring(0, pos);
			}
			typeClass = unit.getClazz(typeName);
			if (typeClass!=null) {
				prop.setTypeClass(typeClass);
			}
		}
		if (typeClass==null) {
			String targetEntity = assoc.getValueAsString("targetEntity");
			if (targetEntity!=null) {
				typeClass = unit.getClazz(targetEntity);
				if (typeClass!=null) {
					prop.setTypeClass(typeClass);
				}
			}
		}
		MAssociation massoc=prop.getAssociation();
		massoc = massoc==null ? prop.getToAssociation() : massoc;
		if (inverse!=null && inverse.length()>0 && typeClass!=null) {			
			MProperty invProp = typeClass.getProperties().stream().filter(p->p.getName().equals(inverse)).
					findFirst().orElse(null);
			if (invProp!=null) {
				if (OneToMany.isType(assoc,unit) || ManyToMany.isType(assoc,unit)) {
					if (invProp.getAssociation()==null) {
						massoc=MAssociation.newMAssociation(invProp,prop).
						setNavigableFrom(true).
						setNavigableTo(true);						
					} else if (invProp.getAssociation().getTo()==null) {
						massoc=invProp.getAssociation().setTo(prop);					
					}
				} else {				
					if (invProp.getAssociation()==null) {
						massoc=MAssociation.newMAssociation(prop,invProp).
						setNavigableFrom(true).
						setNavigableTo(true);						
					} else if (invProp.getAssociation().getTo()==null) {
						massoc=invProp.getAssociation().setTo(prop).swap();
						prop.getAssociation().setNavigableTo(true);						
					}					
				}
			}
		}
		if (massoc==null)
			MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false);
		//TODO: all other association parameters
		return null;
	}

}
