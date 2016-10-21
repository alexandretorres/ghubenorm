package gitget;

import static gitget.Log.LOG;

import java.net.URL;
import java.util.logging.Level;

import model.Language;
import model.Repo;
import sruby.RubyRepo;
import sruby.RubyRepoLoader;

public class LoadRubyRepo {

	public static void main(String[] args) {
		//String url_name="https://github.com/gitlabhq/gitlabhq/raw/85495c8c85533e2d4156231fd2535270afffef5a/db/schema.rb";
		String url_name="https://github.com/gitlabhq/gitlabhq/raw/master/db/schema.rb";
		try {
			URL url = new URL(url_name);
			
			//InputStream is = url.openStream();
			
			RubyRepoLoader loader = RubyRepoLoader.getInstance();
			
			RubyRepo repo =loader.setRepo(new Repo(Language.RUBY));
			loader.visitSchema(url);
			repo.listTables();
			
			
			String model_url = url_name.substring(0,url_name.lastIndexOf("/"));
			model_url = model_url+"/../app/models/";
			///repos/:owner/:repo/contents/:path
		
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
			
		}
	}

}
