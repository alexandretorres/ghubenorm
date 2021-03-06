package sjava;

import static gitget.Log.LOG;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.TokenMgrException;
import com.github.javaparser.ast.CompilationUnit;

import gitget.GitHubCaller;
import gitget.Log;



public class JavaLoader {
	JavaRepo jrepo;	
	JavaVisitor visitor = new JavaVisitor();
	public JCompilationUnit load(URL url) {
		Prof.open("LOAD");
		try {
			String surl = url.toString();
			JCompilationUnit comp = jrepo.getParsed().get(surl);			
			if (comp==null) {	
				comp = new JCompilationUnit(jrepo,surl);
				doLoad(comp,url);
				jrepo.getParsed().put(surl, comp);
			}
			return comp;				
		// JavaParser MAY throw an TokenMgrError. This is a BAD design for JavaParser, since it should be a plain exception! I�m too lazy to see how many "errors" this lib is throwing
		// So we are catching all ERRORS and EXCEPTIONS here. In the future, list all Errors 
		} catch (Throwable ex) {			
			LOG.warning("could not visit file "+url);
			Log.log(jrepo.getRepo(),Level.SEVERE,ex.getMessage(),ex);	
			if (!JCompilationUnit.daoMClass.checkTransactionState(ex))
				throw new RuntimeException("Transaction marked for rollback");			
			return null;		
		} finally {	
			Prof.close("LOAD");
		}
	}
	protected void doLoad(JCompilationUnit comp,URL url) throws IOException {		    
		URLConnection connection=null;
		try {
			connection=url.openConnection();
		
			try (InputStream in =  connection.getInputStream()) {	
				CompilationUnit cu = JavaParser.parse(in);
		        visitor.setComp(comp);
		        cu.accept(visitor, null);		        
			} catch (ParseException|TokenMgrException pe) {
				LOG.warning("could not parse "+url);
				Log.log(jrepo.getRepo(),Level.WARNING,pe.getMessage(),pe);
			}	
		} catch (IOException iex) {
			String msg = GitHubCaller.instance.getErrorStream(connection);
			if (msg!=null)
				LOG.warning("Error stream:" +msg);
			throw new IOException(iex);
			
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
