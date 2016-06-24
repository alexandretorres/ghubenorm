package sjava;

import static sjava.JPATags.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;

import static gitget.Log.LOG;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MColumnMapping;
import model.MDataSource;
import model.MDiscriminator;
import model.MDiscrminableGeneralization;
import model.MFlat;
import model.MGeneralization;
import model.MHorizontal;
import model.MJoinColumn;
import model.MJoinedSource;
import model.MProperty;
import model.MTable;
import model.MTableRef;
import model.MVertical;
import model.Repo;


public class JavaVisitor extends VoidVisitorAdapter<Object>  {
	
	static DAOInterface<MProperty> daoMProp = ConfigDAO.getDAO(MProperty.class);	
	static DAOInterface<MColumn> daoMCol = ConfigDAO.getDAO(MColumn.class);
	static DAOInterface<MJoinColumn> daoJoinCol = ConfigDAO.getDAO(MJoinColumn.class);
	private JCompilationUnit comp;
	private Stack<MClass> classStack = new Stack<MClass>();
	public void setComp(JCompilationUnit comp) {
		this.comp=comp;
	}
	@Override
	public void visit(CompilationUnit cu, Object arg1) {
		classStack.removeAll(classStack);	
		if (cu.getPackage()==null) {
			comp.packageName=null;
		} else {
			comp.packageName=cu.getPackage().getName().toString();
		}
		super.visit(cu, arg1);
	}
	@Override
	public void visit(ImportDeclaration n, Object arg) {
		String from = n.getName().toString();
		if (n.isAsterisk())
			from=from+".*";
		if (!n.isStatic())
			comp.imports.add(Import.newImport(from));
		// TODO Auto-generated method stub
		super.visit(n, arg);
	}
	@Override
	public void visit(ClassOrInterfaceDeclaration cd, Object arg1) {
		MClass c =null;
		if (!cd.isInterface()) {
			c = comp.createClass(cd.getName());
			classStack.push(c);
			List<Annotation> annots = new ArrayList<Annotation>();
			c.setAbstract(ModifierSet.isAbstract(cd.getModifiers()));		
			for (ClassOrInterfaceType scls:cd.getExtends()) {
				String superName = scls.getName();
				MClass superClass = comp.getClazz(superName);
				comp.jrepo.visitors.add(new VisitInheritance(c, comp));		
				if (superClass!=null) {
					c.setSuperClass(superClass);					
				} else {
					comp.jrepo.addLateSubclass(superName, c,comp);
				}
			}
			
			for (AnnotationExpr mod:cd.getAnnotations()) {			
				Annotation anot = Annotation.newAnnotation(mod);
				annots.add(anot);
				//System.out.println("class has annotation "+tokens.getText(mod.annotation().getSourceInterval()));
				if (Entity.isType(anot,comp)) {
					c.setPersistent();					
				}
				if (MappedSuperclass.isType(anot,comp)) {
					comp.jrepo.mappedSuperClasses.add(c);
				}				
					//DiscriminatorColumn
							
			}
			comp.jrepo.classAnnot.put(c, annots);
			Annotation idClass = annots.stream().filter(a->IdClass.isType(a,comp)).findFirst().orElse(null);
			if (c.isPersistent()) {			
				Annotation atab = annots.stream().filter(a->Table.isType(a,comp)).findFirst().orElse(null);
				Annotation asecTab = annots.stream().filter(a->SecondaryTable.isType(a,comp)).findFirst().orElse(null);
				Annotation[] asecTabs = 
						annots.stream().filter(a->SecondaryTables.isType(a,comp)).findFirst().map(s->s.getListValue()).orElse(Collections.EMPTY_LIST)
						.stream().map(v->v.annotation).toArray(Annotation[]::new);
				//asecTabs.getSingleValue().
				if (asecTab!=null || asecTabs.length>0) {	
					DAOInterface<MJoinedSource> DAOJoined = ConfigDAO.getDAO(MJoinedSource.class);
					DAOJoined.persit(c.setJoinedSource());
					if (atab!=null)
						comp.toTable(c, atab);
					if (asecTab!=null) {					
						comp.toTable(c, asecTab);
					}
					for (Annotation a:asecTabs) {
						comp.toTable(c, a);
					}				
				} else if (atab!=null) {				
					comp.toTable(c, atab);
				}			
			}	
		}		
		super.visit(cd, arg1);
		List<JCompilationUnit> pending = comp.jrepo.pendingRefs.get(cd.getName());
		if (pending!=null && c!=null)
			for (Iterator<JCompilationUnit> it =pending.iterator();it.hasNext();) {
				JCompilationUnit comp = it.next();
				if (comp.checkPendingRefs(c)) {
					it.remove();
				}
				//TODO: comp.solve refs (superclasses, embedds...)
			}
		if (c!=null)
			classStack.pop(); //??
	}

	
	@Override
	public void visit(FieldDeclaration ctx, Object arg1) {
		if (!classStack.isEmpty()) {			
			MClass clazz = classStack.peek();
			List<Annotation> annots = new ArrayList<Annotation>();
			boolean isStatic=false;
			isStatic = ModifierSet.isStatic(ctx.getModifiers());
			for (AnnotationExpr mod:ctx.getAnnotations()) {		 
				annots.add(Annotation.newAnnotation(mod));					
			}
			Annotation assoc = annots.stream().
					filter(a->OneToMany.isType(a,comp) || ManyToMany.isType(a,comp) || ManyToOne.isType(a,comp) || OneToOne.isType(a,comp)).
					findFirst().orElse(null);
			Annotation embed = annots.stream().filter(a->Embedded.isType(a,comp)).findFirst().orElse(null);
			Annotation column = annots.stream().filter(a->Column.isType(a,comp)).findFirst().orElse(null);
			//
			Annotation id = annots.stream().filter(a->Id.isType(a,comp)).findFirst().orElse(null);
			
			Annotation embeddedId = annots.stream().filter(a->EmbeddedId.isType(a,comp)).findFirst().orElse(null);
			//------
			String typeName=null;
			Type type = ctx.getType();
			if (type instanceof PrimitiveType) {
				typeName = ((PrimitiveType)type).toString();
			} else if (type instanceof ReferenceType) {
				ReferenceType utype = (ReferenceType) type;
				//UnannReferenceTypeContext utype = ctx.unannType().unannReferenceType();  
				//ctx.unannType().unannReferenceType().unannClassOrInterfaceType().unannClassType_lfno_unannClassOrInterfaceType().typeArguments().getText()
				typeName = utype.getType().toString();
			}
			
			for (VariableDeclarator var:ctx.getVariables()) {
				if (!isStatic) {					
					MProperty prop = daoMProp.persit(clazz.newProperty().setName(var.getId().getName()).setType(typeName));					
					if (var.getId().getArrayCount()>0) {
						prop.setMax(-1);
						prop.setTransient(true);
					} else if (assoc!=null) {
						comp.jrepo.visitors.add(new VisitAssociation(prop, comp, assoc, annots));
					} else if (embed!=null) {
						prop.setEmbedded(true);
					}
					if (id!=null) {
						prop.setPk(true);
					} else if (embeddedId!=null) {
						prop.setPk(true);
					}
					if (column!=null) {
						daoMCol.persit(createColumnMapping(prop,column));											
					}				
				}
			}
		}
		super.visit(ctx, arg1);
	}
	public static MColumn createMColumn(MClass clazz,Annotation column) {
		MColumn col = MColumn.newMColumn();
		col.setName(column.getValue("name",null));
		col.setUnique(column.getValue("unique",Boolean.FALSE));
		col.setNullable(column.getValue("nullable",Boolean.TRUE));
		col.setColummnDefinition(column.getValue("columnDefinition",null));
		col.setLength(column.getValue("length",null,Integer.class));
		col.setPrecision(column.getValue("precision",null,Integer.class));
		col.setScale(column.getValue("scale",null,Integer.class));
		//--
		String tabname = column.getValueAsString("table");
		//--
		MDataSource source = clazz.getPersistence().getSource();
		if (source instanceof MTableRef)
			source = ((MTableRef)source).getTable();
		if (source instanceof MJoinedSource) {
			for (MTable tab:((MJoinedSource)source).getDefines()) {
				if (tabname==null || tab.getName().equalsIgnoreCase(tabname)) {
					col.setTable(tab);
					break;
				}
			}
		} else if (source instanceof MTable) {
			//TODO: if tablename does not match this would be an error.			
			col.setTable((MTable) source);			
			if (tabname!=null && !col.getTable().getName().equalsIgnoreCase(tabname))
				LOG.info("JPA Column refers to table not declared by the class");
		}
		return col;
	}
	public  MColumn createColumnMapping(MProperty prop,Annotation column) {
		MClass clazz = prop.getParent();
		MColumn col = createMColumn(clazz,column);
		prop.setColumnMapping(MColumnMapping.newMColumnMapping(col));	
		return col;
		//set table... late!?
	}
	@Override
	public void visit(MethodDeclaration arg0, Object arg1) {
		// TODO Auto-generated method stub
		super.visit(arg0, arg1);
	}
	
	class VisitInheritance implements LateVisitor<MClass> {
		MClass subClass;
		JCompilationUnit unit;
		//Annotation inheritance;
		
		public VisitInheritance(MClass subClass, JCompilationUnit unit) {
			this.subClass = subClass;
			this.unit = unit;
			
		}
		private void loadPKJoin(MClass superClass,MClass subClass,MVertical gen,Annotation pkJoin,Annotation fk) {
			//TODO: create a MDefinition object for FK defs 
			List<MProperty> superPK = subClass.getPK(); //TODO: get defaultnames...			
			String name = pkJoin.getValue("name", ""); 
			String colDef = pkJoin.getValue("columnDefinition", null); 
			String refColName = pkJoin.getValue("referencedColumnName", null); 
			if (fk==null) {
				fk = (Annotation)pkJoin.getValue("foreignKey");
			}
			MColumn col = MColumn.newMColumn();
			col.setName(name);
			col.setColummnDefinition(colDef);
			JavaVisitor.daoMCol.persit(col);
			MTable mainTab = subClass.getPersistence().getMainTable();			
			if (mainTab!=null) {
				col.setTable(mainTab);
			}
			MJoinColumn jc = MJoinColumn.newMJoinColumn(gen, col);
			daoJoinCol.persit(jc);
			mainTab = superClass.getPersistence().getMainTable();
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
				if (refCol==null) {	// Creates the reference column, if it does not exists				
					for (MProperty cp:superPK) {
						if (cp.isEmbedded()) {
							for (MProperty embp:cp.getTypeClass().getProperties()) {
								if (embp.getName().equals(refCol)) {
									refCol = MColumn.newMColumn().setName(refColName).setTable(mainTab);;
									embp.setColumnMapping(MColumnMapping.newMColumnMapping(refCol));								
									break;
								}
							}
							if (refCol!=null)
								break;
						} else {
							if (cp.getName().equals(refCol)) {
								refCol = MColumn.newMColumn().setName(refColName).setTable(mainTab);;
								cp.setColumnMapping(MColumnMapping.newMColumnMapping(refCol));
								break;
							}
							
						}
					}
				}
				// Now we have created the reference column
				jc.setInverse(refCol);				
				gen.getJoinCols().add(jc);
			}
		}
		@Override
		public MClass exec() {
			MClass superClass = subClass.getSuperClass();
			if (superClass==null || !subClass.isPersistent())
				return null;
			//--
			MGeneralization gen=null;
			List<Annotation> sannots = unit.jrepo.classAnnot.get(superClass);
			List<Annotation> annots = unit.jrepo.classAnnot.get(subClass);
			Annotation inheritance =  Inheritance.findAnnotation(sannots, unit);
			String type="";
			if (inheritance!=null) {
				type = ExprEval.getConstant(inheritance.getValue("strategy",""));
			}
			
			switch (type){
				case "JOINED":
					gen = subClass.addGeneralization(MVertical.class);
					Annotation pkJoins = PrimaryKeyJoinColumn.findAnnotation(annots, unit);					
										
					if (pkJoins==null) {
						Annotation pkJoin=PrimaryKeyJoinColumn.findAnnotation(annots, unit);
						if (pkJoin!=null)
							loadPKJoin(superClass,subClass,(MVertical)gen,pkJoin,null);
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
				default:
					gen = subClass.addGeneralization(MFlat.class);
					
			}	
			Annotation discrCol = DiscriminatorColumn.findAnnotation(sannots, unit);
			if (discrCol!=null) {
				MDiscriminator dcol = superClass.getDiscriminatorColumn();
				if (dcol==null) {
					dcol = new MDiscriminator();
					superClass.setDiscriminatorColumn(dcol);
				}
				if (dcol.getValue()==null)
					dcol.setValue(discrCol.getValueAsString("name"));
				if (dcol.getColumn()==null) {					
					String dtype = ExprEval.getConstant(discrCol.getValue("discriminatorType",null));
					Integer length = discrCol.getValue("length",null,Integer.class);
					Annotation column = discrCol.getValue("columnDefinition",null,Annotation.class);
					MColumn col=null;
					if (column!=null) {
						col = createMColumn(superClass,column);
						if (col.getColummnDefinition()==null) {
							col.setColummnDefinition(dtype);
						}
						if (length!=null && col.getLength()==null)
							col.setLength(length);
						JavaVisitor.daoMCol.persit(col);
						dcol.setColumn(col);
					} else if (dtype!=null || length!=null) {
						col = MColumn.newMColumn();
						col.setColummnDefinition(dtype);
						if (length!=null)
							col.setLength(length);
						JavaVisitor.daoMCol.persit(col);
						dcol.setColumn(col);
					}
				}
			}
			if (gen instanceof MDiscrminableGeneralization) {
				Annotation discrVal = DiscriminatorValue.findAnnotation(annots, unit);
				if (discrVal!=null) {
					String discr = discrVal.getSingleValue(null);
					((MDiscrminableGeneralization) gen).setDiscriminatorValue(discr);
				}
			}
			
			
			
			return superClass;
		}
		
	}
}
