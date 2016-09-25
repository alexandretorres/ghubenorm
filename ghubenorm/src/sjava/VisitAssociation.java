package sjava;

import java.util.List;
import java.util.Set;

import javax.persistence.OneToMany;

import common.LateVisitor;
import gitget.Log;
import model.MAssociation;
import model.MAssociationDef;
import model.MAttributeOverride;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MColumnMapping;
import model.MGeneralization;
import model.MJoinColumn;
import model.MOverride;
import model.MProperty;
import model.MTable;
import model.MVertical;

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
					if (!BaseType.isBaseType(typeName))
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
						massoc=invProp.getAssociation().setTo(prop).setNavigableTo(true);//why not set navigation to? 						
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
				int max = ManyToMany.isType(assoc,unit) ||  ManyToOne.isType(assoc,unit) ? -1 : 1;
				MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false).setMax(max);
			}
		}
		//---
		MClass fromClass = prop.getParent();  // Where the Foreign Key points, if Join Column exists
		MClass toClass = typeClass;
		if (typeClass==null && !(ElementCollection.isType(assoc,unit)))
			Log.LOG.warning("Null destination type ("+prop.getType()+") for association "+prop.getName()+" at "+prop.getParent());
		if ((ManyToOne.isType(assoc,unit) || OneToOne.isType(assoc,unit)) && typeClass!=null) {
			toClass = fromClass;
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
				//TODO: missing inverseJoinColumns of JoinTable. The referencedColumns are named inverse, while the inverse are just normal JoinCols
				adef.setDataSource(tab);
				List<ElementValue> jcs = an.getListValue("joinColumns");
				if (jcs!=null)
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,tab,adef,ajc,true);
						
					}
				jcs = an.getListValue("inverseJoinColumns");
				if (jcs!=null)
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						if (toClass!=null)
							createJoinColumn(unit.jrepo,toClass,tab,adef,ajc,true);
						else
							Log.LOG.warning("Join Column refers to unknown class");
						
					}
			} else if (JoinColumns.isType(an,unit)) {
				MAssociationDef adef = prop.getOrInitAssociationDef();
				List<ElementValue> jcs = an.getListValue();
				if (jcs!=null)
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,null,adef,ajc,false);
						
					}
			} else if (JoinColumn.isType(an,unit)) {
				MAssociationDef adef = prop.getOrInitAssociationDef();
				MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,null,adef,an,false);
				
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
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,tab,adef,ev.annotation,false);						
					}
				} else
					for (ElementValue ev:jcs) {
						Annotation ajc = ev.annotation;
						MJoinColumn jc= createJoinColumn(unit.jrepo,fromClass,tab,adef,ajc,false);
						
					}
			}
		}
		return true;
	}
	public static MJoinColumn createJoinColumn(JavaRepo repo,MClass clazz,MTable toTable,MAssociationDef adef,Annotation ajoin,boolean junctionTable) {
		MColumn col = MColumn.newMColumn();					
		JavaVisitor.daoMCol.persit(col);
		MJoinColumn jc = MJoinColumn.newMJoinColumn(adef, col);
		adef.getJoinColumns().add(jc);
		JavaVisitor.daoJoinCol.persit(jc);
		loadJoinColumn(repo,clazz,toTable,jc, ajoin,junctionTable);
		
		return jc;
	}
	public static void loadJoinColumn(JavaRepo repo,MClass fromClazz,MTable toTable,MJoinColumn jcol,Annotation ajoin,boolean junctionTable) {
		//TODO: If it is a JoinTable, the inverse must NOT BE NOT NULL. It must refer to a column, that refers to a table in order to differentiate inverseJoinColumns
		MColumn col = jcol.getColumn().getColumn();
		col.setName(ajoin.getValue("name", null));		/*""*/
		//
		col.setUnique(ajoin.getValue("unique",null,Boolean.class));
		col.setNullable(ajoin.getValue("nullable",null,Boolean.class));
		col.setInsertable(ajoin.getValue("insertable",null,Boolean.class));
		col.setUpdatable(ajoin.getValue("updatable",null,Boolean.class));
		col.setColummnDefinition(ajoin.getValue("columnDefinition", null)); 
		//TODO: foreignKey
		String tabName=ajoin.getValueAsString("table");
		String refColName = ajoin.getValue("referencedColumnName", null); 
		//
		col.setTable(toTable);
		//--------------------------
		if (junctionTable || refColName!=null) { //TODO: create main table if absent
			MTable tab=fromClazz.getPersistence().getTableSource(tabName);
			MColumn refCol=null;
			if (tab!=null && refColName!=null) {
				refCol = tab.findColumn(refColName);				
			}			
			if (refCol!=null)
				jcol.setInverse(refCol);	
			else
				repo.visitors.add(new VisitColumnRef(repo,refColName,fromClazz,jcol));
						
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
	JavaRepo repo;
	
	public VisitColumnRef(JavaRepo repo,String refColName, MClass fromClazz, MJoinColumn jcol) {
		super();
		this.refColName = refColName;
		this.fromClazz = fromClazz;
		this.jcol = jcol;
		this.repo=repo;
	}


	@Override
	public int getOrder() {		
		return 2;
	}


	@Override
	public boolean exec() {
		MColumn refCol=null;
		try {			
			if (refColName==null || refColName.equals("")) {
				//refColName=""; if it is null, MUST BE NULL
				List<MProperty> pk = fromClazz.getPK();
				if (pk.size()==1) {
					MProperty ppk = pk.get(0);
					MColumnDefinition cdef = ppk.getColumnDef();
					if (cdef!=null) {
						refCol=cdef.getColumn();						
					} else {
						MTable tab = fromClazz.getPersistence().getMainTable();
						refCol = JavaVisitor.daoMCol.persit(MColumn.newMColumn().setTable(tab));
						ppk.setColumnMapping(MColumnMapping.newMColumnMapping(refCol));
						//TODO:This fix cannot be used for inherited PK because we have to know what is the destination class in the hierarqy
					
					}
				} else {
					//TODO: FIRST look for PrimaryKeyJoinColumn on the generalizations, recursively, until find a PK
					//IF not...
					for (MClass cl = fromClazz;cl.getSuperClass()!=null && !cl.getGeneralization().isEmpty();cl=cl.getSuperClass()) {
					//while() {
						for (MGeneralization gen:cl.getGeneralization()) {
							if (gen instanceof MVertical) {
								Set<MJoinColumn> jcs = (((MVertical) gen).getJoinCols());
								for (MJoinColumn pjc:jcs) {
									refCol = pjc.getColumn().getColumn();
									break;
								}
							}
						}
				//		cl=cl.getSuperClass();
					}
					if (refCol==null && fromClazz.getSuperClass()!=null) {
						pk = fromClazz.findPK();
						if (!pk.isEmpty()) {
							MProperty ppk = pk.get(0);
							//
							for (MOverride ov:fromClazz.getOverrides()) {								
								if (ov instanceof MAttributeOverride) {
									if (ov.getProperties().contains(ppk)) {
										refCol = ((MAttributeOverride)ov).getColumn().getColumn();
										break;
									}
								}
							}
							if (refCol==null) {
								refCol = JavaVisitor.daoMCol.persit(MColumn.newMColumn());
								//TODO: Overrive is a "difference" column, do we need the previous override history?
								MAttributeOverride over = MAttributeOverride.newMAttributeOverride(refCol, ppk);
								fromClazz.override(over);
								VisitOverrides.daoMAttrOverride.persit(over);
							}
						}
					}
					if (refCol==null) {
						//Create a column with a "reserved" name
						MTable tab = fromClazz.getPersistence().getMainTable();
						if (tab==null) {
							tab = JCompilationUnit.daoMTable.persit(MTable.newMTable(repo.getRepo(),(String) null));
							fromClazz.getPersistence().setDataSource(tab);
						} else
							refCol = tab.findColumn(MColumn.ID_COLUMN_NAME);
						if (refCol==null)
							refCol = JavaVisitor.daoMCol.persit(MColumn.newMColumn().setTable(tab).setName(MColumn.ID_COLUMN_NAME));	
					}
				}
			} else
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
								refCol = JavaVisitor.daoMCol.persit(MColumn.newMColumn().setName(refColName).setTable(tab));
								//embp.setColumnMapping(MColumnMapping.newMColumnMapping(refCol));								
								break;
							}
						}
					if (refCol!=null)
						break;
				} else {					
					if (cp.getName().equalsIgnoreCase(refColName)) {
						refCol = JavaVisitor.daoMCol.persit(MColumn.newMColumn().setName(refColName).setTable(tab));
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
