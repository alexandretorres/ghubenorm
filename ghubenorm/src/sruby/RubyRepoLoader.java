package sruby;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.Node;
import org.jrubyparser.parser.ParserConfiguration;

import static gitget.Log.LOG;
import model.Repo;

public class RubyRepoLoader {
	private RubyVisitor fileVisitor;
	private RubyRepo rrepo;
	private Parser rubyParser;
	private ParserConfiguration config;
	private SchemaVisitor schemaVisitor;
	
	public RubyRepoLoader() {
		rubyParser = new Parser();
		CompatVersion version = CompatVersion.RUBY2_0;
        config = new ParserConfiguration(0, version);       
		
	}
	public RubyRepo setRepo(Repo repo) {
		this.rrepo = new RubyRepo(repo);
		schemaVisitor = new SchemaVisitor(rrepo);
		fileVisitor = new RubyVisitor(rrepo);
		return rrepo;
		
	}
	public Node visitSchema(URL url) {
		try (BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()))) {		
			Node n = rubyParser.parse("", in, config);		
	        n.accept(schemaVisitor); 
	        return n;
		} catch (Exception ex) {		
			LOG.warning("could not visit file "+url);
			ex.printStackTrace();
			return null;		
		}
	}
	public Node visitFile(URL url) {
		try (BufferedReader in = new BufferedReader(
		        new InputStreamReader(url.openStream()))) {			
			Node n = rubyParser.parse("", in, config);		
	        n.accept(fileVisitor); 
	        return n;
		} catch (Exception ex) {	
			LOG.warning("could not visit file "+url);
			ex.printStackTrace();
			
			return null;		
		}
	    
	}
	public void solveRefs() {
		rrepo.solveRefs(fileVisitor);		
	}
}