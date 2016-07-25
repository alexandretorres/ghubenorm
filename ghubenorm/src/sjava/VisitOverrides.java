package sjava;

import static gitget.Log.LOG;

import java.util.Arrays;

import common.LateVisitor;
import model.MAttributeOverride;
import model.MClass;
import model.MColumn;
import model.MProperty;

public class VisitOverrides implements LateVisitor<MProperty> {
	MClass clazz;
	MProperty prop;
	Annotation override;
	Annotation overrides;
	JCompilationUnit unit;

	
	public VisitOverrides(MClass clazz, JCompilationUnit unit, Annotation override, Annotation overrides) {
		super();
		this.clazz = clazz;
		this.override = override;
		this.overrides = overrides;
		this.unit = unit;
	}
	public VisitOverrides(MProperty prop,JCompilationUnit unit, Annotation override, Annotation overrides) {
		super();
		this.prop = prop;
		this.override = override;
		this.overrides = overrides;
		this.unit = unit;
		this.clazz = prop.getParent();
	}
	public MProperty[] findPropPath(String path[],MProperty target) {		
		MProperty[] propPath = new MProperty[path.length+1]; // if MAP, length-1
		propPath[0]=target;
		for (int i=0;i<path.length;i++) {
			target = target.getParent().findProperty(path[i]);
			if (target==null)
				return null;
			propPath[i]=target;
		}
		return propPath;
	}
	public void loadOverrides(Annotation override,Annotation overrides) {
		if (overrides!=null) {
			for (ElementValue v:overrides.getListValue()) {
				loadOverrides(v.annotation,null);
			}			
		}
		if (override!=null) {
			String aname = override.getValueAsString("name");
			Annotation acol = override.getValue("column", null, Annotation.class);
			String[] path = aname.split("\\.");
			MProperty prop = this.prop;
			if (prop==null) {
				//clazz.findInheritedProperty(name)
				prop = clazz.findInheritedProperty(path[0]);
				if (path.length>1)
					path = Arrays.copyOfRange(path,1, path.length);
				else
					path=new String[0];
			}
			
			
			MProperty[] propPath = findPropPath(path, prop);
			if (propPath==null) {				
				LOG.info("Could not find Override path "+aname+" for property "+prop.getName()+" in class "+prop.getParent());
				return;
			}
			MColumn col = JavaVisitor.createMColumn(clazz,acol);
			MAttributeOverride over = MAttributeOverride.newMAttributeOverride(col, propPath);
			clazz.override(over);
		}
	}

	@Override
	public MProperty exec() {
		this.loadOverrides(override, overrides);
		
		return prop;
	}

}
