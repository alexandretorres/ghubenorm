package sjava;

import static sjava.JPATags.Column;
import static sjava.JPATags.Embedded;
import static sjava.JPATags.Entity;
import static sjava.JPATags.ManyToMany;
import static sjava.JPATags.ManyToOne;
import static sjava.JPATags.OneToMany;
import static sjava.JPATags.OneToOne;
import static sjava.JPATags.SecondaryTable;
import static sjava.JPATags.SecondaryTables;
import static sjava.JPATags.Table;

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

import dao.ConfigDAO;
import dao.DAOInterface;

import static gitget.Log.LOG;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MColumnMapping;
import model.MDataSource;
import model.MJoinedSource;
import model.MProperty;
import model.MTable;
import model.MTableRef;
import model.Repo;


public class JavaVisitor extends VoidVisitorAdapter<Object>  {
	
	static DAOInterface<MProperty> daoMProp = ConfigDAO.getDAO(MProperty.class);	
	static DAOInterface<MColumn> daoMCol = ConfigDAO.getDAO(MColumn.class);
	
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
				if (Entity.isType(anot) && comp.importsTag(Entity)) {
					c.setPersistent();					
				}
			}
			if (c.isPersistent()) {			
				Annotation atab = annots.stream().filter(a->Table.isType(a)).findFirst().orElse(null);
				Annotation asecTab = annots.stream().filter(a->SecondaryTable.isType(a)).findFirst().orElse(null);
				Annotation[] asecTabs = 
						annots.stream().filter(a->SecondaryTables.isType(a)).findFirst().map(s->s.getListValue()).orElse(Collections.EMPTY_LIST)
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
			Annotation assoc = annots.stream().filter(a->OneToMany.isType(a) || ManyToMany.isType(a) || ManyToOne.isType(a) || OneToOne.isType(a)).findFirst().orElse(null);
			Annotation embed = annots.stream().filter(a->Embedded.isType(a)).findFirst().orElse(null);
			Annotation column = annots.stream().filter(a->Column.isType(a)).findFirst().orElse(null);
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
					if (column!=null) {
						daoMCol.persit(createMColumn(prop,column));											
					}				
				}
			}
		}
		super.visit(ctx, arg1);
	}
	public MColumn createMColumn(MProperty prop,Annotation column) {
		MClass clazz = prop.getParent();
		MColumn col = MColumn.newMColumn();
		col.setName(column.getValue("name",null));
		col.setUnique(column.getValue("unique",Boolean.FALSE));
		col.setNullable(column.getValue("nullable",Boolean.TRUE));
		col.setColummnDefinition(column.getValue("columnDefinition",null));
		col.setLength(column.getValue("length",null,Integer.class));
		col.setPrecision(column.getValue("precision",null,Integer.class));
		col.setScale(column.getValue("scale",null,Integer.class));
		prop.setColumnMapping(MColumnMapping.newMColumnMapping(col));	
		String tabname = column.getValueAsString("table");
		
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
		//set table... late!?
	}
	@Override
	public void visit(MethodDeclaration arg0, Object arg1) {
		// TODO Auto-generated method stub
		super.visit(arg0, arg1);
	}
	
	
}
