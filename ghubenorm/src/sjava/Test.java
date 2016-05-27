package sjava;

import java.io.File;
import java.io.IOException;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

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
			ANTLRFileStream stfile = new ANTLRFileStream("SmallTest.java");
			SJavaLexer lexer = new SJavaLexer(stfile);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			SJavaParser parser = new SJavaParser(tokenStream);
			
			CompilationUnitContext context = parser.compilationUnit();
			// Walk it and attach our listener
		    ParseTreeWalker walker = new ParseTreeWalker();
		    SJavaListnerImpl listner = new SJavaListnerImpl(parser);
		    walker.walk(listner, context); 
		    System.out.println("compilation unit:\n"+listner.comp);
		    
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

}
