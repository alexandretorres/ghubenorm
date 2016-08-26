package sjava;

import java.util.List;

import javax.persistence.OneToMany;

import common.LateVisitor;
import gitget.Log;
import model.MAssociation;
import model.MAssociationDef;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MColumnMapping;
import model.MJoinColumn;
import model.MOverride;
import model.MProperty;
import model.MTable;

import static sjava.JPATags.*;

public class VisitAssociation implements LateVisitor {
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
	public boolean exec() {
		String inverse = assoc.getValueAsString("mappedBy");
		
		boolean optional = !(assoc.getValue("optional")==Boolean.FALSE); //NULL,TRUE=>False otherwise True
		String targetEntity=null;
		try {
			targetEntity = assoc.getValueAsString("targetEntity");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (targetEntity!=null) {
			int idx = targetEntity.indexOf(".class");
			if (idx>0) {
				targetEntity = targetEntity.substring(0, idx);
			}
		}
		if (!optional)
			prop.setMin(1);
		MClass typeClass = prop.getTypeClass();
		
		if (OneToMany.isType(assoc,unit) || ManyToMany.isType(assoc,unit) || ElementCollection.isType(assoc,unit)) {
			//TODO: if NO JoinColumn, and unidirectional, there is a JoinTable!
			
			prop.setMax(-1);			
			int[] interval = new int[] {prop.getType().indexOf("<"),prop.getType().lastIndexOf(">")};
			if (interval[0]>=0 && interval[1]>=0 && interval[0]<interval[1]) {
				String typeName = prop.getType().substring(interval[0]+1,interval[1]);
				
				typeName = unit.stripGenericType(typeName);
				int comma = typeName.indexOf(',');
				if (comma>=0) {
					typeName = typeName.substring(comma+1).trim();
				}
				typeClass = unit.getClazz(typeName);
				if (typeClass!=null) {
					prop.setTypeClass(typeClass);
				} else {
					Log.LOG.warning("could not find type '"+typeName+"' on ToMany association " +prop.getParent()+"."+prop.getName());
				}
			}	
			if ( ElementCollection.isType(assoc,unit)) {
				prop.setEmbedded(true);
				MColumnDefinition cdef = prop.getColumnDef();
				if (cdef!=null && cdef.getTable()!=null && cdef.getTable().equals(prop.getParent().getPersistence().getMainTable())) {
					cdef.getColumn().setTable(null); //RESET table def!
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
		if (targetEntity!=null) {
			MClass targetTypeClass = unit.getClazz(targetEntity);
			if (targetTypeClass!=null) {
				typeClass = targetTypeClass;
				if (prop.getTypeClass()==null)
					prop.setTypeClass(typeClass);
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
		if (massoc==null) { 
			if (!(ElementCollection.isType(assoc,unit) && typeClass==null)) {
				MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false);
			}
		}
		//---
		MClass fromClass = prop.getParent();  // Where the Foreign Key points, if Join Column exists
		if (typeClass==null)
			Log.LOG.warning("Null destination type for association "+prop.getName()+" at "+prop.getParent());
		if ((ManyToOne.isType(assoc,unit) || OneToOne.isType(assoc,unit)) && typeClass!=null) {
			fromClass = typeClass;
		}
		//TODO: all other association parameters
		//TODO: all other annotations that affect associations
		for (Annotation an:annotations) {
			if (JoinTable.isType(an,unit)) {
				MAssociationDef adef = prop.getOrInitAssociationDef();
				String name = an.getValueAsString("name");				
				MTable tab = JCompilationUnit.daoMTable.persit(MTable.newMTable(unit.jrepo.getRepo(),name));
				tab.setCatalog(an.getValueAsString("catalog"));
				tab.setSchema(an.getValueAsString("schema"));
				
				adef.setDataSource(tab);
				List<ElementValue> jcs = an.getListValue("joinColumns");
				if (jcs!=null)
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,tab,adef,ajc);
						
					}
			} else if (JoinColumns.isType(an,unit)) {
				MAssociationDef adef = prop.getOrInitAssociationDef();
				List<ElementValue> jcs = an.getListValue();
				if (jcs!=null)
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,null,adef,ajc);
						
					}
			} else if (JoinColumn.isType(an,unit)) {
				MAssociationDef adef = prop.getOrInitAssociationDef();
				MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,null,adef,an);
				
			} else if (CollectionTable.isType(an, unit)) {
				MAssociationDef adef = prop.getOrInitAssociationDef();
				String name = an.getValueAsString("name");				
				MTable tab = JCompilationUnit.daoMTable.persit(MTable.newMTable(unit.jrepo.getRepo(),name));
				tab.setCatalog(an.getValueAsString("catalog"));
				tab.setSchema(an.getValueAsString("schema"));
				
				adef.setDataSource(tab);
				MColumnDefinition cdef = prop.getColumnDef();
				if (cdef!=null) {
					cdef.getColumn().setTable(tab); //@Column used by element collection is at the collection table
				}
				
				List<ElementValue> jcs = an.getListValue("joinColumns");
				if (jcs==null) {
					ElementValue ev = an.getElementValue("joinColumns");
					if (ev!=null && ev.annotation!=null) {
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,tab,adef,ev.annotation);						
					}
				} else
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,tab,adef,ajc);
						
					}
			}
		}
		return true;
	}
	public static MJoinColumn createJoinColumn(JavaRepo repo,MClass clazz,MTable toTable,MAssociationDef adef,Annotation ajoin) {
		MColumn col = MColumn.newMColumn();					
		JavaVisitor.daoMCol.persit(col);
		MJoinColumn jc = MJoinColumn.newMJoinColumn(adef, col);
		adef.getJoinColumns().add(jc);
		JavaVisitor.daoJoinCol.persit(jc);
		loadJoinColumn(repo,clazz,toTable,jc, ajoin);
		
		return jc;
	}
	public static void loadJoinColumn(JavaRepo repo,MClass fromClazz,MTable toTable,MJoinColumn jcol,Annotation ajoin) {
		
		MColumn col = jcol.getColumn().getColumn();
		col.setName(ajoin.getValue("name", ""));		
		//
		col.setUnique(ajoin.getValue("unique",Boolean.FALSE));
		col.setNullable(ajoin.getValue("nullable",Boolean.TRUE));
		col.setInsertable(ajoin.getValue("insertable",Boolean.TRUE));
		col.setUpdatable(ajoin.getValue("updatable",Boolean.TRUE));
		col.setColummnDefinition(ajoin.getValue("columnDefinition", null)); 
		//TODO: foreignKey
		String tabName=ajoin.getValueAsString("table");
		String refColName = ajoin.getValue("referencedColumnName", null); 
		//
		col.setTable(toTable);
		//--------------------------
		if (refColName!=null) { //TODO: create main table if absent
			MTable tab=fromClazz.getPersistence().getTableSource(tabName);
			MColumn refCol=null;
			if (tab!=null) {
				refCol = tab.findColumn(refColName);				
			}			
			if (refCol!=null)
				jcol.setInverse(refCol);	
			else
				repo.visitors.add(new VisitColumnRef(refColName,fromClazz,jcol));
						
		}
		
		
	}
}

class VisitColumnRef implements LateVisitor {
	// for properties, including inherited ones:
		// - The class has a overriden prop with this col. Last override wins
		// - The prop has a column mapping
		// - The prop is embedded and one of the "subprops" has a default column mapping
		// - The prop has an AssociationDef attached: check the joinCols
		// - The column could not be found, but is the default for property
		// - The column could not be found, but is the default for association
		// - The column could not be found, but is the default for embedded prop
	String refColName;
	MClass fromClazz;
	MJoinColumn jcol;
	
	
	public VisitColumnRef(String refColName, MClass fromClazz, MJoinColumn jcol) {
		super();
		this.refColName = refColName;
		this.fromClazz = fromClazz;
		this.jcol = jcol;
	}


	@Override
	public int getOrder() {		
		return 2;
	}


	@Override
	public boolean exec() {
		MColumn refCol=null;
		try {
			refCol = fromClazz.findColumnByName(refColName);
		} catch (Exception ex) {
			ex.printStackTrace();
			
		}
		MTable tab=fromClazz.getPersistence().getMainTable();
		if (refCol==null) {
			List<MProperty> allProps = fromClazz.getAllProperties();
			for (MProperty cp:allProps) {		
				if (cp.isEmbedded()) {
					// This is incorrect. Check pkjoincols. It should create associationdef,
					// The associationDef then refers to joinCols that define the columns mapping the composite key
					if (cp.getTypeClass()!=null)
						for (MProperty embp:cp.getTypeClass().getProperties()) {
							if (embp.getName().equals(refColName)) {
								refCol = MColumn.newMColumn().setName(refColName).setTable(tab);
								//embp.setColumnMapping(MColumnMapping.newMColumnMapping(refCol));								
								break;
							}
						}
					if (refCol!=null)
						break;
				} else {					
					if (cp.getName().equalsIgnoreCase(refColName)) {
						refCol = MColumn.newMColumn().setName(refColName).setTable(tab);
						if (cp.getColumnMapping()==null)
							cp.setColumnMapping(MColumnMapping.newMColumnMapping(refCol));
						break;
					}						
				}
			}
		}
		// Now we have created the reference column
		jcol.setInverse(refCol);
		return true;
	}
	
}
