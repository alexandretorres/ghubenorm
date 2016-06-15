package sjava;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import model.MClass;
import model.MJoinedSource;
import model.MProperty;
import model.MTable;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import sjava.SJavaParser.AnnotationContext;
import sjava.SJavaParser.ClassBodyContext;
import sjava.SJavaParser.ClassDeclarationContext;
import sjava.SJavaParser.ClassModifierContext;
import sjava.SJavaParser.CompilationUnitContext;
import sjava.SJavaParser.FieldDeclarationContext;
import sjava.SJavaParser.FieldModifierContext;
import sjava.SJavaParser.MarkerAnnotationContext;
import sjava.SJavaParser.MethodDeclarationContext;
import sjava.SJavaParser.NormalClassDeclarationContext;
import sjava.SJavaParser.PackageDeclarationContext;
import sjava.SJavaParser.SingleStaticImportDeclarationContext;
import sjava.SJavaParser.SingleTypeImportDeclarationContext;
import sjava.SJavaParser.StaticImportOnDemandDeclarationContext;
import sjava.SJavaParser.TypeImportOnDemandDeclarationContext;
import sjava.SJavaParser.UnannReferenceTypeContext;
import sjava.SJavaParser.VariableDeclaratorContext;

import static sjava.JPATags.*;

public class SJavaListnerImpl extends SJavaBaseListener {
	private JCompilationUnit comp;
	private Stack<MClass> classStack = new Stack<MClass>();
	
	static SJavaParser parser;
	public SJavaListnerImpl(JCompilationUnit comp) {
		parser = comp.parser;
		this.comp = comp;
	}
	@Override
	public void enterCompilationUnit(CompilationUnitContext ctx) {
		classStack.removeAll(classStack);
		System.out.println("text CU:"+ctx.getText());
	}

	@Override
	public void enterSingleTypeImportDeclaration(SingleTypeImportDeclarationContext ctx) {
		TokenStream tokens = parser.getTokenStream();  //ctx.typeName().packageOrTypeName().Identifier() ...	
		String from = tokens.getText(ctx.typeName().getSourceInterval());		
		comp.imports.add(Import.newImport(from));
		// TODO Auto-generated method stub
	}

	@Override
	public void enterStaticImportOnDemandDeclaration(StaticImportOnDemandDeclarationContext ctx) {
		// TODO Auto-generated method stub
		super.enterStaticImportOnDemandDeclaration(ctx);
	}

	@Override
	public void enterSingleStaticImportDeclaration(SingleStaticImportDeclarationContext ctx) {
		// TODO Auto-generated method stub
		super.enterSingleStaticImportDeclaration(ctx);
	}

	@Override
	public void enterTypeImportOnDemandDeclaration(TypeImportOnDemandDeclarationContext ctx) {
		TokenStream tokens = parser.getTokenStream();
		String from = tokens.getText(ctx.packageOrTypeName().getSourceInterval());
		Import imp = Import.newImport(from+".*");
		comp.imports.add(imp);
		// TODO Auto-generated method stub
		super.enterTypeImportOnDemandDeclaration(ctx);
	}


	@Override
	public void enterAnnotation(AnnotationContext ctx) {
		// ALL annotations
		super.enterAnnotation(ctx);
		
	}
	
	@Override
	public void enterMarkerAnnotation(MarkerAnnotationContext ctx) {
		// TODO Auto-generated method stub
		super.enterMarkerAnnotation(ctx);
	}
	
	@Override
	public void enterNormalClassDeclaration(NormalClassDeclarationContext ctx) {	
		TokenStream tokens = parser.getTokenStream();
		MClass c = comp.createClass().setName(ctx.Identifier().getText());//new MClass(comp,ctx.Identifier().getText());
		classStack.push(c);
		List<Annotation> annots = new ArrayList<Annotation>();
		for (ClassModifierContext mod:ctx.classModifier()) {
			if (mod.annotation()!=null) {
				Annotation anot = Annotation.newAnnotation(mod.annotation());
				annots.add(anot);
				System.out.println("class has annotation "+tokens.getText(mod.annotation().getSourceInterval()));
				if (Entity.isType(anot) && comp.importsTag(Entity)) {
					c.setPersistent();					
				}
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
	
	@Override
	public void exitNormalClassDeclaration(NormalClassDeclarationContext ctx) {
		classStack.pop();		
	}
	@Override
	public void enterClassBody(ClassBodyContext ctx) {		
		System.out.println("the body is={"+ctx.getText()+"}");
	}
	@Override
	public void enterPackageDeclaration(PackageDeclarationContext ctx) {
		if (ctx.Identifier().isEmpty())
			return;
		String name=null;
		for (TerminalNode id:ctx.Identifier()) {
			if (name==null)
				name=id.getText();
			else
				name +="."+id.getText();
		}
		comp.packageName=name;
		super.enterPackageDeclaration(ctx);
	}
	@Override
	//fieldModifier* unannType variableDeclaratorList ';'
	public void enterFieldDeclaration(FieldDeclarationContext ctx) {
		MClass clazz = classStack.peek();
		List<Annotation> annots = new ArrayList<Annotation>();
		boolean isStatic=false;
		for (FieldModifierContext mod:ctx.fieldModifier()) {
			if (mod.annotation()!=null) { 
				annots.add(Annotation.newAnnotation(mod.annotation()));
			} else if (mod.getText().equals("static")) {
				isStatic=true;
			}			
		}
		Annotation assoc = annots.stream().filter(a->OneToMany.isType(a) || ManyToMany.isType(a) || ManyToOne.isType(a) || OneToOne.isType(a)).findFirst().orElse(null);
		Annotation embed = annots.stream().filter(a->Embedded.isType(a)).findFirst().orElse(null);
		Annotation column = annots.stream().filter(a->Column.isType(a)).findFirst().orElse(null);
		String typeName=null;
		if (ctx.unannType().unannPrimitiveType()!=null) {
			typeName = ctx.unannType().unannPrimitiveType().getText();
		} else if (ctx.unannType().unannReferenceType()!=null) {
			UnannReferenceTypeContext utype = ctx.unannType().unannReferenceType();  
			//ctx.unannType().unannReferenceType().unannClassOrInterfaceType().unannClassType_lfno_unannClassOrInterfaceType().typeArguments().getText()
			typeName = utype.getText();
		}
		for (VariableDeclaratorContext var:ctx.variableDeclaratorList().variableDeclarator()) {
			if (!isStatic) {
				
				MProperty prop = clazz.newProperty().setName(var.variableDeclaratorId().Identifier().getText()).setType(typeName);					
				if (var.variableDeclaratorId().dims()!=null) {
					prop.setMax(-1);
					prop.setTransient(true);
				} else if (assoc!=null) {
					comp.jrepo.visitors.add(new VisitAssociation(prop, comp, assoc, annots));
				}
				
				
			}
		}
		super.enterFieldDeclaration(ctx);
	}
	@Override
	public void enterMethodDeclaration(MethodDeclarationContext ctx) {
		// TODO Auto-generated method stub
		super.enterMethodDeclaration(ctx);
	}
	
}
