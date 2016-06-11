package sjava;

import static gitget.Log.LOG;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jruby.lexer.yacc.SyntaxException;

import model.Repo;
import sjava.SJavaParser.CompilationUnitContext;

public class JavaLoader {
	public CompilationUnit load(URL url) {
		try (InputStream in =  url.openStream()) {	
			ANTLRInputStream stfile = new ANTLRInputStream(in);
			SJavaLexer lexer = new SJavaLexer(stfile);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			SJavaParser parser = new SJavaParser(tokenStream);
			
			CompilationUnitContext context = parser.compilationUnit();
			// Walk it and attach our listener
		    ParseTreeWalker walker = new ParseTreeWalker();//context.packageDeclaration()...
		    SJavaListnerImpl listner = new SJavaListnerImpl(parser);
		    walker.walk(listner, context); 
		    System.out.println("compilation unit:\n"+listner.comp);
		    return listner.comp;
		} catch (SyntaxException sex) {
			LOG.warning("Syntax exception on file "+url.toString()+" position "+sex.getLine());			
			LOG.log(Level.SEVERE,sex.getMessage(),sex);	
			
			return null;
		} catch (Exception ex) {	
			LOG.warning("could not visit file "+url);
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
			
			return null;		
		}
		
		
	}
}
