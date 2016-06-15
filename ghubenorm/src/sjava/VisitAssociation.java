package sjava;

import java.util.List;

import javax.persistence.OneToMany;

import common.LateVisitor;
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
		if (OneToMany.isType(assoc) || ManyToMany.isType(assoc)) {
			int[] interval = new int[] {prop.getType().indexOf("<"),prop.getType().indexOf(">")};
			if (interval[0]>=0 && interval[1]>=0 && interval[0]<interval[1]) {
				String type = prop.getType().substring(interval[0],interval[1]);
				MClass typeClass = unit.getClazz(type);
				if (typeClass!=null) {
					prop.setTypeClass(typeClass);
				}
			}
			
		}
		return null;
	}

}
