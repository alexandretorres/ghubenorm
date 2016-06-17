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
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import model.MClass;
import model.MProperty;


public class JavaVisitor extends VoidVisitorAdapter<Object>  {
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
		if (!cd.isInterface()) {
			MClass c = comp.createClass().setName(cd.getName());
			classStack.push(c);
			List<Annotation> annots = new ArrayList<Annotation>();
			c.setAbstract(ModifierSet.isAbstract(cd.getModifiers()));		
			
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
					c.setJoinedSource();
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
		classStack.pop(); //??
	}

	
	@Override
	public void visit(FieldDeclaration ctx, Object arg1) {
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
				
				MProperty prop = clazz.newProperty().setName(var.getId().getName()).setType(typeName);					
				if (var.getId().getArrayCount()>0) {
					prop.setMax(-1);
					prop.setTransient(true);
				} else if (assoc!=null) {
					comp.jrepo.visitors.add(new VisitAssociation(prop, comp, assoc, annots));
				}
				
				
			}
		}
		super.visit(ctx, arg1);
	}
	@Override
	public void visit(MethodDeclaration arg0, Object arg1) {
		// TODO Auto-generated method stub
		super.visit(arg0, arg1);
	}
	
	
}
