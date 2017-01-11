package sjava;

import static gitget.Log.LOG;
import static sjava.JPATags.Access;
import static sjava.JPATags.AssociationOverride;
import static sjava.JPATags.AssociationOverrides;
import static sjava.JPATags.AttributeOverride;
import static sjava.JPATags.AttributeOverrides;
import static sjava.JPATags.CollectionTable;
import static sjava.JPATags.Column;
import static sjava.JPATags.DiscriminatorColumn;
import static sjava.JPATags.DiscriminatorValue;
import static sjava.JPATags.ElementCollection;
import static sjava.JPATags.Embedded;
import static sjava.JPATags.EmbeddedId;
import static sjava.JPATags.Entity;
import static sjava.JPATags.GeneratedValue;
import static sjava.JPATags.Id;
import static sjava.JPATags.IdClass;
import static sjava.JPATags.Inheritance;
import static sjava.JPATags.JoinTable;
import static sjava.JPATags.ManyToMany;
import static sjava.JPATags.ManyToOne;
import static sjava.JPATags.MappedSuperclass;
import static sjava.JPATags.OneToMany;
import static sjava.JPATags.OneToOne;
import static sjava.JPATags.PrimaryKeyJoinColumn;
import static sjava.JPATags.PrimaryKeyJoinColumns;
import static sjava.JPATags.SecondaryTable;
import static sjava.JPATags.SecondaryTables;
import static sjava.JPATags.Table;

import java.beans.Beans;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.AnnotableNode;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;
import model.ClassifierType;
import model.MAttributeOverride;
import model.MClass;
import model.MColumn;
import model.MColumnMapping;
import model.MDiscriminator;
import model.MDiscrminableGeneralization;
import model.MFlat;
import model.MGeneralization;
import model.MGeneratorType;
import model.MHorizontal;
import model.MJoinColumn;
import model.MJoinedSource;
import model.MMethod;
import model.MOverride;
import model.MProperty;
import model.MTable;
import model.MVertical;
import model.Visibility;


public class JavaVisitor extends VoidVisitorAdapter<Object>  {
	
	static DAOInterface<MProperty> daoMProp = ConfigDAO.getDAO(MProperty.class);	
	static DAOInterface<MColumn> daoMCol = ConfigDAO.getDAO(MColumn.class);
	static DAOInterface<MJoinColumn> daoJoinCol = ConfigDAO.getDAO(MJoinColumn.class);

	private JCompilationUnit comp;
	private Stack<MClass> classStack = new Stack<MClass>();
	public void setComp(JCompilationUnit comp) {
		this.comp=comp;
	}
	Map<MClass,ClassInfo> classInfo = new HashMap<MClass,ClassInfo>();
	@Override
	public void visit(CompilationUnit cu, Object arg1) {
		classStack.removeAll(classStack);	
		classInfo.clear();
		if (cu.getPackage()==null) {
			comp.packageName=null;
		} else {
			comp.packageName=cu.getPackage().getName().toString();
		}
		super.visit(cu, arg1);
	}
	@Override
	public void visit(ImportDeclaration n, Object arg) {
		if (!n.isEmptyImportDeclaration()) {
			String from = n.getName().toString();
			if (n.isAsterisk())
				from=from+".*";
			if (!n.isStatic())
				comp.imports.add(Import.newImport(from));
		
			// TODO Auto-generated method stub
			super.visit(n, arg);
		}
	}
	
	@Override
	public void visit(EnumDeclaration cd, Object arg1) {
		MClass c = comp.createClass(cd.getName());
		c.setType(ClassifierType.EnumType);
		//super.visit(arg0, arg1);
	}
	@Override
	public void visit(ClassOrInterfaceDeclaration cd, Object arg1) {
		MClass c =null;
		ClassInfo info=null;
	/*	if (cd.isInterface()) {
			MClass ci = comp.createClass(cd.getName());
			ci.setType(ClassifierType.InterfaceType);
		} else  {	*/
			c = comp.createClass(cd.getName());
			if (cd.isInterface()) {
				c.setType(ClassifierType.InterfaceType);
			}
			classStack.push(c);
			if (cd.getImplements()!=null && !cd.getImplements().isEmpty())				
				comp.jrepo.visitors.add(new VisitImplements(c, cd.getImplements(),comp));	
				
			info = new ClassInfo();
			classInfo.put(c, info);
			List<Annotation> annots = new ArrayList<Annotation>();
			c.setAbstract(ModifierSet.isAbstract(cd.getModifiers()));	
			if (c.getType()==ClassifierType.InterfaceType && !cd.getExtends().isEmpty())
				comp.jrepo.visitors.add(new VisitImplements(c, cd.getExtends(),comp));
			else
				for (ClassOrInterfaceType scls:cd.getExtends()) {
					String superName = comp.getFullName(scls);				
					comp.jrepo.visitors.add(new VisitInheritance(c, comp));	
					
					comp.jrepo.addLateSubclass(superName, c,comp);
					/*
					MClass superClass = comp.getClazz(superName);
					if (superClass!=null) {
						c.setSuperClass(superClass);					
					} else {
						comp.jrepo.addLateSubclass(superName, c,comp);
					}*/
				}
			Annotation entity=null;
		
			
			for (AnnotationExpr mod:cd.getAnnotations()) {			
				Annotation anot = Annotation.newAnnotation(mod);
				annots.add(anot);
				//System.out.println("class has annotation "+tokens.getText(mod.annotation().getSourceInterval()));
				if (Entity.isType(anot,comp)) {
					entity=anot;
					c.setPersistent();					
				} else if (MappedSuperclass.isType(anot,comp)) {
					comp.jrepo.mappedSuperClasses.add(c);
				} else if (AttributeOverride.isType(anot,comp)) {
					comp.jrepo.visitors.add(new VisitOverrides(c, comp, Collections.singletonList(anot), null,true));				
				} else if (AttributeOverrides.isType(anot,comp)) {					
					comp.jrepo.visitors.add(new VisitOverrides(c, comp, null,anot,true));					
				} else if (AssociationOverride.isType(anot,comp)) {
					comp.jrepo.visitors.add(new VisitOverrides(c, comp, Collections.singletonList(anot), null,false));				
				} else if (AssociationOverrides.isType(anot,comp)) {
					comp.jrepo.visitors.add(new VisitOverrides(c, comp, null,anot,false));	
				} else if (Access.isType(anot, comp)) {	
					if (!anot.getSingleValue("").contains("FIELD"))
						comp.propertyAccess=true;
				}
				//DiscriminatorColumn
							
			}
			comp.jrepo.classAnnot.put(c, annots);
			Annotation idClass = annots.stream().filter(a->IdClass.isType(a,comp)).findFirst().orElse(null);
			
		
			if (c.isPersistent()) {			
				Annotation atab = annots.stream().filter(a->Table.isType(a,comp)).findFirst().orElse(null);
				List<Annotation> asecTabs = annots.stream().
						filter(a->SecondaryTable.isType(a,comp)).collect(Collectors.toList());
				annots.stream().filter(a->SecondaryTables.isType(a,comp)).findFirst().map(s->s.getListValue()).orElse(Collections.EMPTY_LIST)
						.stream().map(v->v.annotation).forEachOrdered(asecTabs::add);//toArray(Annotation[]::new);
				
				if (!asecTabs.isEmpty()) {	
					DAOInterface<MJoinedSource> DAOJoined = ConfigDAO.getDAO(MJoinedSource.class);
					DAOJoined.persist(c.setJoinedSource());
					String entityName = entity.getValueAsString("name");
					if (atab!=null)
						comp.toTable(c, atab);
					else if (!asecTabs.isEmpty()) {						
						//TODO: what about inheritance of a table??						
						comp.toTable(c, entityName); //create a "default" table for the class
					}					
					for (Annotation a:asecTabs) {
						comp.toTable(c, a);
					}				
				} else if (atab!=null) {				
					comp.toTable(c, atab);
				}			
			}	
		//}		
		super.visit(cd, arg1);
		if (c!=null) {
			//process properties
			procClassInfo(c, info);
			for (PropInfo pinf:info.propInfo) {
				if (!pinf.skip)
					procFieldOrMethod(c,pinf,pinf.ctx,pinf.annots, pinf.type, pinf.modifiers);
			}
			for (Iterator<MMethod> it=c.getMethods().iterator();it.hasNext();) {
				MMethod met=it.next();		
				String name = met.getName();
				if (name.length()>3 && name.startsWith("set")) {
					String sufix = met.getName().substring(3);
					for (MProperty p:c.getProperties()) {
						if (p.getName().equalsIgnoreCase(sufix)) {
							it.remove();						
							break;
						}
					}					
				}
				
			}
		
			//pending refs
		
			for (MProperty p:c.getProperties()) {
				if (p.getTypeClass()==null) {
					String name = p.getType();
					comp.jrepo.addPendingRef(name, c, comp);
					/*MClass clazz = comp.getClazz(name);
					
					if (clazz==null) {
						comp.jrepo.addPendingRef(name, c, comp);
					} else
						p.setTypeClass(clazz);*/
				}
			}
		}
		List<JCompilationUnit> pending = comp.jrepo.pendingRefs.get(cd.getName());
		if (pending!=null && c!=null)
			for (Iterator<JCompilationUnit> it =pending.iterator();it.hasNext();) {
				JCompilationUnit comp = it.next();
				if (comp.checkPendingRefs(c,false)) {
					it.remove();
				}
				//TODO: comp.solve refs (superclasses, embedds...)
			}
		if (c!=null)
			classStack.pop(); //??
	}
	private List<Annotation> procAnnotations(AnnotableNode ctx) {
		List<Annotation> annots = new ArrayList<Annotation>();
		for (AnnotationExpr mod:ctx.getAnnotations()) {
			Annotation a = Annotation.newAnnotation(mod);
			for (JPATags t:JPATags.values()) {
				if (t.isType(a, comp)) {
					annots.add(a);
					break;
				}
			}								
		}
		return annots;
	}
	private String getBeanName(String methodName)
	{
		if (methodName==null)
			return null;
		String ret = null;
		if (methodName.startsWith("is")) 
			ret = methodName.substring(2);
		else if (methodName.startsWith("get")) 
			ret = methodName.substring(3);
		else 
			return null;
	    // Assume the method starts with either get or is.
	    return Introspector.decapitalize(ret);
	}
	private void procClassInfo(MClass clazz,ClassInfo info) {
		//TODO: hasFieldAnnotations should be inherited... please kill me! Who invented JPA?
		comp.propertyAccess = comp.propertyAccess || (!comp.hasFieldAnnotations && comp.hasMethodAnnotations);
		//default pass: Skip all properties or fields, depending on annotation position (SHOULD BE INHERITED! OH NO!)
		for (PropInfo pinf:info.propInfo) {
			if (comp.propertyAccess) {
				if (pinf.ctx instanceof FieldDeclaration)
					pinf.skip=true;
			} else {
				if (pinf.ctx instanceof MethodDeclaration)
					pinf.skip=true;
			}			
		}// Access fields
		for (PropInfo pinf:info.propInfo) {
			Annotation access = pinf.annots.stream().filter(a->Access.isType(a,comp)).findFirst().orElse(null);
			if (access!=null) {	
				//if (!access.getSingleValue(null).contains("FIELD")) {
				info.setProperty(pinf);				
			}		
		}
		
	}
	private void procFieldOrMethod(MClass clazz,PropInfo pinf,BodyDeclaration ctx, List<Annotation> annots,Type type, int modifiers) {
		boolean trans = ModifierSet.isTransient(modifiers);
		boolean isStatic=false;
		isStatic = ModifierSet.isStatic(modifiers);
		
		Annotation transAnot = annots.stream().filter(a->sjava.JPATags.Transient.isType(a,comp)).findFirst().orElse(null);
		trans = trans || transAnot!=null;
		Annotation elementCol = annots.stream().filter(a->ElementCollection.isType(a,comp)).findFirst().orElse(null);
		
		Annotation assoc = annots.stream().
				filter(a->OneToMany.isType(a,comp) || ManyToMany.isType(a,comp) || ManyToOne.isType(a,comp) || OneToOne.isType(a,comp)).
				findFirst().orElse(null);
		Annotation embed = annots.stream().filter(a->Embedded.isType(a,comp)).findFirst().orElse(null);
		
		List<Annotation> attrOver = annots.stream().filter(a->AttributeOverride.isType(a,comp)).collect(Collectors.toList());
		Annotation attrOvers = annots.stream().filter(a->AttributeOverrides.isType(a,comp)).findFirst().orElse(null);
		
		//long teste = annots.stream().filter(a->AssociationOverride.isType(a,comp)).count();
		
		List<Annotation> assocOver = annots.stream().filter(a->AssociationOverride.isType(a,comp)).collect(Collectors.toList());
		Annotation assocOvers = annots.stream().filter(a->AssociationOverrides.isType(a,comp)).findFirst().orElse(null);
		
		Annotation column = annots.stream().filter(a->Column.isType(a,comp)).findFirst().orElse(null);
		Annotation generated = annots.stream().filter(a->GeneratedValue.isType(a,comp)).findFirst().orElse(null);
		//
		Annotation id = annots.stream().filter(a->Id.isType(a,comp)).findFirst().orElse(null);
		Annotation joinTable = annots.stream().filter(a->JoinTable.isType(a,comp)).findFirst().orElse(null);
		Annotation colTable = annots.stream().filter(a->CollectionTable.isType(a,comp)).findFirst().orElse(null);
		
		Annotation embeddedId = annots.stream().filter(a->EmbeddedId.isType(a,comp)).findFirst().orElse(null);
		//------
		
		
		String typeName=null;
		
		if (type instanceof PrimitiveType) {
			typeName = ((PrimitiveType)type).toString();
		} else if (type instanceof ReferenceType) {
			ReferenceType utype = (ReferenceType) type;
			//UnannReferenceTypeContext utype = ctx.unannType().unannReferenceType();  
			//ctx.unannType().unannReferenceType().unannClassOrInterfaceType().unannClassType_lfno_unannClassOrInterfaceType().typeArguments().getText()
			typeName = utype.getType().toString();
		}	
		if (!isStatic) {			
			MProperty prop = daoMProp.persist(clazz.newProperty().setName(pinf.name).setType(typeName));					
			prop.setTransient(trans);
			if ( (pinf.var!=null && pinf.var.getId().getArrayCount()>0) ||
				 (pinf.type instanceof ReferenceType && ((ReferenceType)pinf.type).getArrayCount()>0)	) {
				prop.setMax(-1);
				//prop.setTransient(true);
			} else if (assoc!=null) {
				comp.jrepo.visitors.add(new VisitAssociation(prop, comp, assoc, annots));		
			} else if (elementCol!=null) {
				comp.jrepo.visitors.add(new VisitAssociation(prop, comp, elementCol, annots));
				
			} else if (embed!=null) {
				prop.setEmbedded(true);
			}
			if (id!=null) {
				prop.setPk(true);
			} else if (embeddedId!=null) {
				prop.setPk(true);
			}			
			if (!attrOver.isEmpty() || attrOvers!=null) {
				comp.jrepo.visitors.add(new VisitOverrides(prop, comp, attrOver, attrOvers,true));
			}
			if (!assocOver.isEmpty() || assocOvers!=null) {
				comp.jrepo.visitors.add(new VisitOverrides(prop, comp, assocOver, assocOvers,false));
			}
			if (column!=null) {
				daoMCol.persist(createColumnMapping(prop,column));											
			}	
			if (generated!=null) {
				prop.getGenerated().setGenerated(true);
				prop.getGenerated().setGenerator(generated.getValueAsString("generator"));
				
				String gtype = generated.getValue("strategy",null);				
				prop.getGenerated().setType(findStrategy(gtype));
				
			}
		
		}
	}
	public MGeneratorType findStrategy(String str) {
		if (str==null || str.length()==0)
			return null;
		for (MGeneratorType v:MGeneratorType.values()) {
			if (str.toUpperCase().endsWith(v.name())) {
				return v;
			}
		}
		return null;
	}
	
	@Override
	public void visit(MethodDeclaration ctx, Object arg1) {
		if (!classStack.isEmpty()) {			
			MClass clazz = classStack.peek();	
			ClassInfo info = classInfo.get(clazz);
			//
			int modifiers = ctx.getModifiers();
			Type type = ctx.getType();
			List<Annotation> annots = procAnnotations(ctx);
			if (!annots.isEmpty())
				comp.hasMethodAnnotations=true;
			String bname = getBeanName(ctx.getName()); 
			if (bname!=null && !(type instanceof VoidType) && ctx.getParameters().isEmpty() && clazz.getType()==ClassifierType.ClassType)
				info.propInfo.add(new PropInfo(bname,ctx, annots, type, modifiers, null));
			else {				
				String typeName="";
				if (type instanceof PrimitiveType) {
					typeName = ((PrimitiveType)type).toString();
				} else if (type instanceof ReferenceType) {
					ReferenceType utype = (ReferenceType) type;
					typeName = utype.getType().toString();
				}	else if (type instanceof VoidType) {
					typeName="";
				}
				
				MMethod met = MMethod.newMethod(clazz, ctx.getName()).setType(typeName)
						.setAbstract(ModifierSet.isAbstract(ctx.getModifiers()))
						.setVisibility(Visibility.findVisibility(
								ModifierSet.isPublic(ctx.getModifiers()),
								ModifierSet.isProtected(ctx.getModifiers()), 
								ModifierSet.isPrivate(ctx.getModifiers())));
				for (Parameter p:ctx.getParameters()){
					met.addParam(p.getName());
				}
				//add method
			}
				
			//if (comp.propertyAccess || !comp.hasFieldAnnotations)
			//ONLY IF USING METHOD ANNOTATION
			//visitFieldOrMethod(ctx, type, modifiers, null, arg1);
		}
		super.visit(ctx, arg1);
	}
	@Override
	public void visit(FieldDeclaration ctx, Object arg1) {
		if (!classStack.isEmpty()) {			
			MClass clazz = classStack.peek();	
			ClassInfo info = classInfo.get(clazz);
			
			int modifiers = ctx.getModifiers();
			Type type = ctx.getType();
			List<VariableDeclarator> vars = ctx.getVariables();
			List<Annotation> annots = procAnnotations(ctx);
			if (!annots.isEmpty())
				comp.hasFieldAnnotations=true;
			for (VariableDeclarator var:vars) {
				String bname = var.getId().getName();
				info.propInfo.add(new PropInfo(bname,ctx, annots, type, modifiers, var));
			}						
		}
		super.visit(ctx, arg1);
	}
	public static MColumn createMColumn(MClass clazz,Annotation column) {
		MColumn col = MColumn.newMColumn();
		col.setName(column.getValue("name",null));
		col.setUnique(column.getValue("unique",null,Boolean.class));
		col.setNullable(column.getValue("nullable",null,Boolean.class));
		col.setInsertable(column.getValue("insertable",null,Boolean.class));
		col.setUpdatable(column.getValue("updatable",null,Boolean.class));
		col.setColumnDefinition(column.getValue("columnDefinition",null));
		col.setLength(column.getValue("length",null,Integer.class));
		col.setPrecision(column.getValue("precision",null,Integer.class));
		col.setScale(column.getValue("scale",null,Integer.class));
		//--
		String tabname = column.getValueAsString("table");
		//--
		
		col.setTable(clazz.getPersistence().getTableSource(tabname));
		if (tabname!=null && (col.getTable()!=null && !col.getTable().getName().equalsIgnoreCase(tabname)))
			LOG.info("JPA Column refers to table not declared by the class");
		return col;
	}
	public  MColumn createColumnMapping(MProperty prop,Annotation column) {
		MClass clazz = prop.getParent();
		MColumn col = createMColumn(clazz,column);
		prop.setColumnMapping(MColumnMapping.newMColumnMapping(col));	
		return col;
		//set table... late!?
	}
	
	class VisitImplements implements LateVisitor {
		MClass subClass;
		JCompilationUnit unit;
		List<ClassOrInterfaceType> impl;
		public VisitImplements(MClass subClass,List<ClassOrInterfaceType> impl,JCompilationUnit unit) {
			this.subClass = subClass;
			this.unit = unit;
			this.impl=impl;
		}
		@Override
		public boolean exec() {
			for (ClassOrInterfaceType ci:impl) {
				String name = unit.getFullName(ci);				
				MClass sup = unit.getClazz(name);
				subClass.addImplements(name,sup);
			}
			return true;
		}
	}
	
	class VisitInheritance implements LateVisitor {
		MClass subClass;
		JCompilationUnit unit;
		//Annotation inheritance;
		
		public VisitInheritance(MClass subClass, JCompilationUnit unit) {
			this.subClass = subClass;
			this.unit = unit;
			
		}
		private void loadPKJoin(MClass superClass,MClass subClass,MVertical gen,Annotation pkJoin,Annotation fk) {
			//TODO: create a MDefinition object for FK defs 
			List<MProperty> superPK = superClass.findPK(); //TODO: get defaultnames...			
			String name = pkJoin.getValue("name", null/*""*/); 
			String colDef = pkJoin.getValue("columnDefinition", null); 
			String refColName = pkJoin.getValue("referencedColumnName", null); 
			if (fk==null) {
				fk = (Annotation)pkJoin.getValue("foreignKey");
			}
			
			MColumn col = MColumn.newMColumn();
			col.setName(name);
			col.setColumnDefinition(colDef);
			JavaVisitor.daoMCol.persist(col);
			MTable mainTab = subClass.getPersistence().getMainTable();			
			if (mainTab!=null) {
				col.setTable(mainTab);
			}
			MJoinColumn jc = MJoinColumn.newMJoinColumn(gen, col);
			daoJoinCol.persist(jc);
			mainTab = superClass.getPersistence().getMainTable();
			//TODO: this is a mess! question: can I DECLARE a column by referencing? don´t think so
			//TODO: what is the order? Maybe references should be treated in a third stage, after declarations
			//TODO: There is a similar create join column algor. in the VisitAssociation class
			if (refColName!=null) { //TODO: create main table if absent
				MColumn refCol=null;
				if (mainTab!=null) {
					//find the refcol
					for (MColumn cref:mainTab.getColumns()) {
						if (cref.getName().equals(refColName)) {
							refCol=cref;
							break;
						}
					}
				}
				//TODO: What about overriden attributes??
				
				// first pass is on column mappings
				if (refCol==null) {	// Creates the reference column, if it does not exists				
					for (MProperty cp:superPK) {
						if (cp.isEmbedded()) {//This is unusual for ENORM, but normal for JPA
							for (MOverride ov:cp.getParent().getOverrides()) {
								if (ov instanceof MAttributeOverride) {
									MAttributeOverride mao = (MAttributeOverride) ov;
									if (refColName.equals(mao.getColumn().getName())) {
										refCol = mao.getColumn().getColumn();									
										break;
									}
								}
							}														
						} else if (cp.getColumnMapping()!=null 
								&& cp.getColumnMapping().getColumnDefinition().getName().equals(refColName)) {
							refCol = cp.getColumnMapping().getColumnDefinition().getColumn();
							break;
						}
					}
				}
				if (refCol==null) { //second pass is on property names
					for (MProperty cp:superClass.getProperties()) { //TODO: get "ALL" properties	
						if (cp.isEmbedded() && cp.getTypeClass()!=null) { 							
							for (MProperty embp:cp.getTypeClass().getProperties()) {
								if (embp.getName().equals(refColName)) {
									refCol = MColumn.newMColumn().setName(refColName).setTable(mainTab);;
									//TODO: Add an AttributeOverride?
									//TODO: Could this recurse by using "."?								
									break;
								}
							}
						} else if (cp.getName().equals(refColName)) {
							refCol = MColumn.newMColumn().setName(refColName).setTable(mainTab);;
							cp.setColumnMapping(MColumnMapping.newMColumnMapping(refCol));
							break;							
						}
					}
				}
				// Now we have "created"??? the reference column
				jc.setInverse(refCol);				
				gen.getJoinCols().add(jc);
			}
		}
		public MClass findPersistentRoot(MClass c) {
			MClass root = c.getSuperClass();
			if (root==null)
				return null;
			{
				MClass step = root;
				while (step.getSuperClass()!=null) {
					step = step.getSuperClass();
					if (step.isPersistent())
						root = step;
				}
			}
			return root;
		}
		@Override
		public boolean exec() {
			MClass superClass = subClass.getSuperClass();
			if (superClass==null)
				return true;
		/*	if (!subClass.isPersistent()) {
				if (unit.jrepo.mappedSuperClasses.contains(superClass)) {
					subClass.addGeneralization(MHorizontal.class);
				}
				return true;
			}*/
			MClass root = findPersistentRoot(subClass);
			
			boolean mappedSup = unit.jrepo.mappedSuperClasses.contains(superClass) && !superClass.isPersistent();
			//--
			MGeneralization gen=null;
			List<Annotation> sannots = unit.jrepo.classAnnot.get(superClass);
			JCompilationUnit sunit = unit.jrepo.getParsed().get(superClass.getFilePath());
			List<Annotation> annots = unit.jrepo.classAnnot.get(subClass);
			Annotation inheritance =  Inheritance.findAnnotation(sannots, sunit);
			String type="";
			if (!mappedSup) {
				if (inheritance!=null) {
					type = ExprEval.getConstant(inheritance.getValue("strategy",""));
				}
				if (type.equals("") && root!=superClass && subClass.isPersistent()) {
					sannots = unit.jrepo.classAnnot.get(root);
					sunit = unit.jrepo.getParsed().get(root.getFilePath());
					inheritance =  Inheritance.findAnnotation(sannots, sunit);
					if (inheritance!=null)
						type = ExprEval.getConstant(inheritance.getValue("strategy",""));
				}
			}
			switch (type){
				case "JOINED":
					gen = subClass.addGeneralization(MVertical.class);
					Annotation pkJoins = PrimaryKeyJoinColumns.findAnnotation(annots, unit);					
										
					if (pkJoins==null) {
						List<Annotation> pkJoinLst=PrimaryKeyJoinColumn.findAnnotations(annots, unit);
						if (!pkJoinLst.isEmpty()) {
							for (Annotation pkJoin:pkJoinLst) 
								loadPKJoin(superClass,subClass,(MVertical)gen,pkJoin,null);
						}
					} else {
						List<ElementValue> lst = pkJoins.getListValue("value");
						Annotation fk = (Annotation)pkJoins.getValue("foreignKey");
						
						for (ElementValue pkj:lst) {
							loadPKJoin(superClass,subClass,(MVertical)gen,(Annotation)pkj.getValue(),fk);
						}
					}
					break;
				case "TABLE_PER_CLASS":
					gen = subClass.addGeneralization(MHorizontal.class);
					break;
				case "SINGLE_TABLE":
					gen = subClass.addGeneralization(MFlat.class);
					break;
				default:
					if (mappedSup)
						gen = subClass.addGeneralization(MHorizontal.class);
					else if (root.isPersistent() && subClass.isPersistent())
						gen = subClass.addGeneralization(MFlat.class);
					
			}	
			Annotation discrCol = DiscriminatorColumn.findAnnotation(sannots, sunit);
			Annotation sdiscrVal = DiscriminatorValue.findAnnotation(sannots, sunit);
			if (discrCol!=null || sdiscrVal!=null) {
				MDiscriminator dcol = superClass.getDiscriminatorColumn();
				if (dcol==null) {
					dcol = new MDiscriminator();
					superClass.setDiscriminatorColumn(dcol);
				}
				if (sdiscrVal!=null)
					dcol.setValue(sdiscrVal.getSingleValue(""));
				if (discrCol!=null && dcol.getColumn()==null) {					
					String dtype = ExprEval.getConstant(discrCol.getValue("discriminatorType",null));
					//Integer length = discrCol.getValue("length",null,Integer.class);
					//Annotation column = discrCol.getValue("columnDefinition",null,Annotation.class);
					MColumn col=null;
					
					col = createMColumn(superClass,discrCol);
					if (col.getColumnDefinition()==null) {					
						String coldef = discrCol.getValue("columnDefinition",null);
						if (coldef==null)
							col.setColumnDefinition(dtype);
						else
							col.setColumnDefinition(coldef);
							
					}
					
					JavaVisitor.daoMCol.persist(col);
					dcol.setColumn(col);
					/*} else if (dtype!=null || length!=null) {
						col = MColumn.newMColumn();
						col.setColumnDefinition(dtype);
						if (length!=null)
							col.setLength(length);
						JavaVisitor.daoMCol.persist(col);
						dcol.setColumn(col);
					}*/
				}
			}
			if (gen instanceof MDiscrminableGeneralization) {
				Annotation discrVal = DiscriminatorValue.findAnnotation(annots, unit);
				if (discrVal!=null) {
					String discr = discrVal.getSingleValue(null);
					((MDiscrminableGeneralization) gen).setDiscriminatorValue(discr);
				}
			}
			
			
			
			return true;
		}
		
	}
	private class ClassInfo {
		List<PropInfo> propInfo = new ArrayList<PropInfo>();
		public void setProperty(PropInfo pinf) {
			pinf.skip=false;
			for (PropInfo p:propInfo) {
				if (p!=pinf && p.name!=null && p.name.equals(pinf.name))
					p.skip=true;					
			}
		}
	}
	private class PropInfo {
		String name;
		BodyDeclaration ctx;
		List<Annotation> annots;
		Type type; 
		int modifiers;
		VariableDeclarator var;
		boolean skip=false;
		public PropInfo(String name,BodyDeclaration ctx,List<Annotation> annots, Type type, int modifiers, VariableDeclarator var) {
			super();
			this.name=name;
			this.ctx = ctx;
			this.annots=annots;
			this.type = type;
			this.modifiers = modifiers;
			this.var = var;
			
			
		}
		
	}
}

