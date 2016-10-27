package sruby;

import static gitget.Log.LOG;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.SyntaxException;

import gitget.GitHubCaller;
import gitget.Log;
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
	private static RubyRepoLoader instance;
	private Stack<RubyVisitor> visitors = new Stack<RubyVisitor>();
	private RubyVisitor fileVisitor;
	private RubyRepo rrepo;
	//private Parser rubyParser;
	//private ParserConfiguration config;
	private SchemaVisitor schemaVisitor;
	private Ruby runtime ;
	private Set<URL> visitList ;
	
	private RubyRepoLoader() {		
		runtime = Ruby.newInstance();	  
		runtime.setDefaultInternalEncoding(UTF8Encoding.INSTANCE);

		
	}
	public static RubyRepoLoader getInstance() {
		if (instance==null)
			instance = new RubyRepoLoader();
		return instance;
	}
	public RubyRepo setRepo(Repo repo) {
		this.rrepo = new RubyRepo(repo);
		schemaVisitor = new SchemaVisitor(rrepo);
		fileVisitor = new RubyVisitor(rrepo);
		this.visitList= new HashSet<URL>();
		return rrepo;
		
	}
	public static Repo getCurrentRepo() {
		if (instance!=null && instance.rrepo!=null)
			return instance.rrepo.getRepo();
		return null;
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
			} catch (SyntaxException sex) {
				LOG.warning("Syntax exception on file "+url.toString()+" position "+sex.getLine());			
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.INFO,sex.getMessage(),sex);	
				return null;
			} catch (org.jruby.exceptions.RaiseException rex) {
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.INFO,rex.getMessage(),rex);
				return null;
			}
			
		} catch (Exception ex) {	
			String msg = GitHubCaller.instance.getErrorStream(connection);			
			LOG.warning("could not visit file "+url);
			if (msg!=null)
				LOG.warning("Error stream:" +msg);
			if (ex instanceof FileNotFoundException)
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.WARNING,ex.getMessage(),ex);
			else
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.SEVERE,ex.getMessage(),ex);	
							
			return null;		
		}
	}
	public Node visitFile(String url,InputStream in) {			
		Node n = parse(in);
		//rubyParser.parse("", in, config);		
		fileVisitor.reset(url);
        n.accept(fileVisitor); 
        return n;
	    
	}
	//TODO: avoid repeated loading
	public Node visitFile(URL url) {
		URLConnection connection=null;
		try {
			if (visitList.contains(url))
				return null;
			visitList.add(url);
			connection=url.openConnection();
			try (InputStream in =  connection.getInputStream()) {	
				return visitFile(url.toString(),in);			
			} catch (SyntaxException sex) {
				LOG.warning("Syntax exception on file "+url.toString()+" position "+sex.getLine());			
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.INFO,sex.getMessage(),sex);	
			} catch (org.jruby.exceptions.RaiseException rex) {
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.INFO,rex.getMessage(),rex);
			}
			return null;
		} catch (Exception ex) {	
			String msg = GitHubCaller.instance.getErrorStream(connection);			
			LOG.warning("could not visit file "+url);
			if (msg!=null)
				LOG.warning("Error stream:" +msg);
			if (ex instanceof FileNotFoundException)
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.WARNING,ex.getMessage(),ex);
			else
				Log.log(RubyRepoLoader.getCurrentRepo(),Level.SEVERE,ex.getMessage(),ex);	
			
			return null;		
		}
	    
	}
	public void solveRefs() {
		rrepo.solveRefs(fileVisitor);		
	}
	public void pushVisitor() {
		this.visitors.push(fileVisitor);
		fileVisitor = new RubyVisitor(rrepo);
	}
	public void popVisitor() {
		fileVisitor =visitors.pop();
	}
}