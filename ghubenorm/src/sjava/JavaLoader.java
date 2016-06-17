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
	protected void doLoad(JCompilationUnit comp,URL url) throws IOException {
		SJavaParser parser=null;
		
		try (InputStream in =  url.openStream()) {	
			
			ANTLRInputStream stfile = new ANTLRInputStream(in);
			SJavaLexer lexer = new SJavaLexer(stfile);
			TokenStream tokenStream = new CommonTokenStream(lexer);			
			
			Prof.open("JavaLoader.parser");
			parser = new SJavaParser(tokenStream);	
			Prof.close("JavaLoader.parser");
		}
		Prof.open("JavaLoader.doLoad.test");
		CompilationUnitContext context = parser.compilationUnit();
		Prof.close("JavaLoader.doLoad.test");
		// Walk it and attach our listener
	    //context.packageDeclaration()...
	    comp.parser=parser;
	    Prof.open("JavaLoader.walk");
	    SJavaListnerImpl listner = new SJavaListnerImpl(comp);
	    walker.walk(listner, context); 
	    Prof.close("JavaLoader.walk");
	 //   System.out.println("compilation unit:\n"+comp);
	    
	    return;
		
	}

	public JavaRepo getJrepo() {
		return jrepo;
	}
	public void setJrepo(JavaRepo jrepo) {
		this.jrepo = jrepo;
	}
	
}
