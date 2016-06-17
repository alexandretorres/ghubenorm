package sjava;

import static gitget.Log.LOG;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;



public class JavaLoader {
	JavaRepo jrepo;	
	JavaVisitor visitor = new JavaVisitor();
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
	
		} catch (Exception ex) {	
			LOG.warning("could not visit file "+url);
			LOG.log(Level.SEVERE,ex.getMessage(),ex);				
			return null;		
		}		
	}
	protected void doLoad(JCompilationUnit comp,URL url) throws IOException {		    
		try (InputStream in =  url.openStream()) {	
			CompilationUnit cu = JavaParser.parse(in);
	        visitor.setComp(comp);
	        cu.accept(visitor, null);
			
		} catch (ParseException pe) {
			LOG.log(Level.WARNING,pe.getMessage(),pe);
		}	
		//LOG.info("compilation unit:\n"+comp);	    
	    return;		
	}

	public JavaRepo getJrepo() {
		return jrepo;
	}
	public void setJrepo(JavaRepo jrepo) {
		this.jrepo = jrepo;
	}
	
}
