package sjava;

import static gitget.Log.LOG;

import java.util.Arrays;
import java.util.List;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;
import model.MAssociationDef;
import model.MAssociationOverride;
import model.MAttributeOverride;
import model.MClass;
import model.MColumn;
import model.MProperty;
//Overrides after Associations?
public class VisitOverrides implements LateVisitor {
	MClass clazz;
	MProperty prop;
	List<Annotation> overrideLst;
	Annotation overrides;
	JCompilationUnit unit;
	boolean attribute=true;

	static DAOInterface<MAttributeOverride> daoMAttrOverride = ConfigDAO.getDAO(MAttributeOverride.class);
	
	public VisitOverrides(MClass clazz, JCompilationUnit unit,List<Annotation> overrideLst, Annotation overrides,boolean attribute) {
		super();
		this.clazz = clazz;
		this.overrideLst = overrideLst;
		this.overrides = overrides;
		this.unit = unit;
		this.attribute = attribute;
	}
	public VisitOverrides(MProperty prop,JCompilationUnit unit, List<Annotation> overrideLst, Annotation overrides,boolean attribute) {
		super();
		this.prop = prop;
		this.overrideLst = overrideLst;
		this.overrides = overrides;
		this.unit = unit;
		this.clazz = prop.getParent();
		this.attribute = attribute;
	}
	public MProperty[] findPropPath(String path[],MProperty target) {		
		MProperty[] propPath = new MProperty[path.length+1]; // if MAP, length-1
		propPath[0]=target;
		for (int i=0;i<path.length;i++) {
			if ( target.getTypeClass()==null)
				return null;
			target = target.getTypeClass().findProperty(path[i]);
			if (target==null)
				return null;
			propPath[i+1]=target;
		}
		return propPath;
	}
	public void loadOverrides(Annotation override) {		
		if (override!=null) {
			String aname = override.getValueAsString("name");
			
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
			if (attribute) {
				Annotation acol = override.getValue("column", null, Annotation.class);
				MColumn col = JavaVisitor.daoMCol.persist(JavaVisitor.createMColumn(clazz,acol));
				MAttributeOverride over = MAttributeOverride.newMAttributeOverride(col, propPath);
				clazz.override(over);
				daoMAttrOverride.persist(over);
			} else {
				MAssociationOverride over = MAssociationOverride.newMAssociationOverride(clazz,propPath);
				if (prop==null) {
					LOG.warning("Could not find Association Override path "+aname+" with null property in class "+clazz);
					return;
				}
					
				MClass fromClass = prop.getParent(); 
				MClass toClass = propPath[propPath.length-1].getTypeClass();//prop.getTypeClass();
				List<ElementValue> jcs = override.extractListValue("joinColumns");
				if (jcs!=null && !jcs.isEmpty()) {
					MAssociationDef adef = over.getOrInitAssociationDef();
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						VisitAssociation.createJoinColumn(unit.jrepo,fromClass,null,adef,ajc,false);							
					}
				}
				
				ElementValue joinTable = override.getElementValue("joinTable");
				if (joinTable!=null) {					
					MAssociationDef adef = over.getOrInitAssociationDef();
					VisitAssociation.createJoinTable(unit, joinTable.annotation, adef, fromClass, toClass);
					
				
				}
			}
		}
	}

	@Override
	public boolean exec() {
		if (overrides!=null) {
			List<ElementValue> ovr = overrides.getListValue();
			if (ovr==null)
				ovr =overrides.extractListValue("value");
			for (ElementValue v:ovr) {
				loadOverrides(v.annotation);
			}			
		}
		if (overrideLst!=null)
			for (Annotation o:overrideLst)
				this.loadOverrides(o);
			
		return true;
	}
	@Override
	public int getOrder() {
	
		return 1;
	}

}
