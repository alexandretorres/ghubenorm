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


import model.Repo;
import sjava.SJavaParser.CompilationUnitContext;

public class JavaLoader {
	JavaRepo jrepo;
	ParseTreeWalker walker = new ParseTreeWalker();
	public JCompilationUnit load(URL url) {
		try {
			String surl = url.toString();
			JCompilationUnit comp = jrepo.getParsed().get(surl);			
			if (comp==null) {	
				comp = new JCompilationUnit(jrepo,surl);
				doLoad(comp,url);
				jrepo.getParsed().put(surl, comp);
			}
			return comp;
				
	/*	} catch (SyntaxException sex) {
			LOG.warning("Syntax exception on file "+url.toString()+" position "+sex.getLine());			
			LOG.log(Level.SEVERE,sex.getMessage(),sex);	
			
			return null;*/
		} catch (Exception ex) {	
			LOG.warning("could not visit file "+url);
			LOG.log(Level.SEVERE,ex.getMessage(),ex);				
			return null;		
		}
		
		
	}
	private void doLoad(JCompilationUnit comp,URL url) throws IOException {
		try (InputStream in =  url.openStream()) {	
			ANTLRInputStream stfile = new ANTLRInputStream(in);
			SJavaLexer lexer = new SJavaLexer(stfile);
			TokenStream tokenStream = new CommonTokenStream(lexer);
			SJavaParser parser = new SJavaParser(tokenStream);		
			
			CompilationUnitContext context = parser.compilationUnit();
			// Walk it and attach our listener
		    //context.packageDeclaration()...
		    comp.parser=parser;
		    SJavaListnerImpl listner = new SJavaListnerImpl(comp);
		    walker.walk(listner, context); 
		    System.out.println("compilation unit:\n"+comp);
		    return;
		}
	}
	public JavaRepo getJrepo() {
		return jrepo;
	}
	public void setJrepo(JavaRepo jrepo) {
		this.jrepo = jrepo;
	}
	
}
