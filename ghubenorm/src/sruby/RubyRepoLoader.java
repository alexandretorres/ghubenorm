package sruby;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.SyntaxException;

import gitget.GitHubCaller;
import gitget.Log;

import static gitget.Log.LOG;
import model.Repo;
/**
 * Class that encapsulates the Parser use for JRuby. In order to work, the JRUBY_HOME variable must be
 * present in the environment, pointing at a valid and complete JRuby installation. This uses
 * jruby-9.1.2.0.
 * 
 * @author torres
 *
 */
public class RubyRepoLoader {
	private RubyVisitor fileVisitor;
	private RubyRepo rrepo;
	//private Parser rubyParser;
	//private ParserConfiguration config;
	private SchemaVisitor schemaVisitor;
	private Ruby runtime ;
	
	public RubyRepoLoader() {		
		runtime = Ruby.newInstance();		      
		
	}
	
	public RubyRepo setRepo(Repo repo) {
		this.rrepo = new RubyRepo(repo);
		schemaVisitor = new SchemaVisitor(rrepo);
		fileVisitor = new RubyVisitor(rrepo);
		return rrepo;
		
	}
	public Node parse(InputStream in) {
		return runtime.parseFile(in, "", runtime.getCurrentContext().getCurrentScope());
	}
	public Node visitSchema(InputStream in) {
		Node n = parse(in);
		//rubyParser.parse("", in, config);		
        n.accept(schemaVisitor); 
        return n;
	}
	public Node visitSchema(URL url) {
		URLConnection connection=null;
		try {
			connection=url.openConnection();
			try (InputStream in =  connection.getInputStream()) {		
				return visitSchema(in);
			}
		} catch (Exception ex) {	
			String msg = GitHubCaller.instance.getErrorStream(connection);			
			LOG.warning("could not visit file "+url);
			if (msg!=null)
				LOG.warning("Error stream:" +msg);
			LOG.log(Level.SEVERE,ex.getMessage(),ex);				
			return null;		
		}
	}
	public Node visitFile(InputStream in) {			
		Node n = parse(in);
		//rubyParser.parse("", in, config);		
		fileVisitor.reset();
        n.accept(fileVisitor); 
        return n;
	    
	}
	
	public Node visitFile(URL url) {
		URLConnection connection=null;
		try {
			connection=url.openConnection();
			try (InputStream in =  connection.getInputStream()) {	
				return visitFile(in);			
			} catch (SyntaxException sex) {
				LOG.warning("Syntax exception on file "+url.toString()+" position "+sex.getLine());			
				LOG.log(Level.SEVERE,sex.getMessage(),sex);	
			}	
			return null;
		} catch (Exception ex) {	
			String msg = GitHubCaller.instance.getErrorStream(connection);			
			LOG.warning("could not visit file "+url);
			if (msg!=null)
				LOG.warning("Error stream:" +msg);
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
			
			return null;		
		}
	    
	}
	public void solveRefs() {
		rrepo.solveRefs(fileVisitor);		
	}
}