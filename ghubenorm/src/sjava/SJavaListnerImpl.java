package sjava;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.antlr.v4.runtime.TokenStream;

import sjava.SJavaParser.AnnotationContext;
import sjava.SJavaParser.ClassBodyContext;
import sjava.SJavaParser.ClassDeclarationContext;
import sjava.SJavaParser.ClassModifierContext;
import sjava.SJavaParser.CompilationUnitContext;
import sjava.SJavaParser.NormalClassDeclarationContext;
import sjava.SJavaParser.SingleStaticImportDeclarationContext;
import sjava.SJavaParser.SingleTypeImportDeclarationContext;
import sjava.SJavaParser.StaticImportOnDemandDeclarationContext;
import sjava.SJavaParser.TypeImportOnDemandDeclarationContext;
import static sjava.JPATags.*;

public class SJavaListnerImpl extends SJavaBaseListener {
	CompilationUnit comp = new CompilationUnit();
	SJavaParser parser;
	public SJavaListnerImpl(SJavaParser parser) {
		this.parser = parser;
	}
	@Override
	public void enterCompilationUnit(CompilationUnitContext ctx) {
		
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
		
	}
	@Override
	public void enterNormalClassDeclaration(NormalClassDeclarationContext ctx) {
		TokenStream tokens = parser.getTokenStream();
		Clazz c = new Clazz(comp,ctx.Identifier().getText());
		comp.classes.add(c);
		for (ClassModifierContext mod:ctx.classModifier()) {
			if (mod.annotation()!=null) {
				
				System.out.println("class has annotation "+tokens.getText(mod.annotation().getSourceInterval()));
				if (mod.annotation().markerAnnotation()!=null) {
					String type =mod.annotation().markerAnnotation().typeName().Identifier().getText();
					c.setEntity(Entity.name().equals(type) && comp.importsTag(Entity));
				}
			}
			
		}
		
		
	}
	@Override
	public void enterClassBody(ClassBodyContext ctx) {
		
		
		System.out.println("the body is={"+ctx.getText()+"}");
	}
	
}
