package sjava;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import model.Language;
import model.Repo;
import sjava.SJavaParser.CompilationUnitContext;


public class Test {
	

	public static void main(String[] args) {
		//File f = new File(".");
		try {			
			CharStream stream =		
					new ANTLRInputStream("hello alex");
			HelloLexer lexer = new HelloLexer(stream);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			HelloParser parser = new HelloParser(tokenStream);
			ParseTree tree = parser.r(); 
			System.out.println(tree.toStringTree(parser));
			//
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			ANTLRFileStream stfile = new ANTLRFileStream("trash/SmallTest.java");
			SJavaLexer lexer = new SJavaLexer(stfile);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			SJavaParser parser = new SJavaParser(tokenStream);
			
			CompilationUnitContext context = parser.compilationUnit();
			// Walk it and attach our listener
		    ParseTreeWalker walker = new ParseTreeWalker();
		    JCompilationUnit comp = new JCompilationUnit(new JavaRepo(new Repo(Language.JAVA)),"");
		    comp.parser=parser;
		    SJavaListnerImpl listner = new SJavaListnerImpl(comp);
		    walker.walk(listner, context); 
		    comp.jrepo.solveRefs();
		    //System.out.println("compilation unit:\n"+listner.comp);
		    comp.jrepo.getRepo().print();
			/*ParseTree tree = parser.compilationUnit(); 
			System.out.println(tree.toStringTree(parser));*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			
			/*
			ANTLRFileStream stfile = new ANTLRFileStream("20141121125249_add_card.rb");
			RubyLexer  lexer = new RubyLexer(stfile);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			RubyParser parser = new RubyParser(tokenStream);
			
			ParseTree tree = parser.prog(); 
			System.out.println(tree.toStringTree(parser));*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void listEngines() {
		
	    ScriptEngineManager mgr = new ScriptEngineManager();
	    List<ScriptEngineFactory> factories = mgr.getEngineFactories();
	    for (ScriptEngineFactory factory : factories)
	    {
	        System.out.println("ScriptEngineFactory Info");
	        String engName = factory.getEngineName();
	        String engVersion = factory.getEngineVersion();
	        String langName = factory.getLanguageName();
	        String langVersion = factory.getLanguageVersion();
	        System.out.printf("\tScript Engine: %s (%s)\n", engName, engVersion);
	        List<String> engNames = factory.getNames();
	        for (String name : engNames)
	        {
	            System.out.printf("\tEngine Alias: %s\n", name);
	        }
	        System.out.printf("\tLanguage: %s (%s)\n", langName, langVersion);
	    }
	
	}

}
