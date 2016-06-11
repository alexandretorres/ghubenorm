package gitget;

import static gitget.Log.LOG;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import dao.ConfigDAO;
import dao.DAOInterface;
import model.Language;
import model.Repo;
import sruby.RubyRepo;
import sruby.RubyRepoLoader;

class RubyCrawler  {
	
	private static RubyRepoLoader loader = new RubyRepoLoader();
	private static GitHubCaller gh = GitHubCaller.instance;
	private DAOInterface<Repo> daoRepo;
	public void processRepo(JsonObject repoJson,String fullName)  {
		try {
			if (daoRepo==null)
				daoRepo = ConfigDAO.getDAO(Repo.class);
			URL url = new URL("https://api.github.com/repos/"+fullName+"/contents/db/schema.rb"
					+ "?access_token="+gh.oauth);
			try (JsonReader rdr = gh.callApi(url,false)) {			
				daoRepo.beginTransaction();
				Repo repo = new Repo(Language.RUBY);
				repo.setLanguage(Language.RUBY);
				repo.setName(fullName);
				repo.setUrl(repoJson.getString("html_url"));			
				daoRepo.persit(repo);
				if (rdr==null) { //File not Found
					LOG.info(" no suitable dbPath for "+fullName);
					//return;
				} else {
					repo.setConfigPath("db/schema.rb");	
					LOG.info(" dbPath:"+fullName+"/raw/master/"+repo.getConfigPath());
					loadRepo(repo);
				}
			}
			daoRepo.commitAndCloseTransaction();
			
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,"Repository "+fullName+":"+ex.getMessage(),ex);	
		}
	}
	// get html_url, replace /blob/ with /raw/ ?
	// var=path
	//puro:https://github.com/iluwatar/java-design-patterns/blob/master/service-layer/src/main/java/com/iluwatar/servicelayer/spell/Spell.java
	//raw: https://github.com/iluwatar/java-design-patterns/raw/master/service-layer/src/main/java/com/iluwatar/servicelayer/spell/Spell.java
	public void printCode_(JsonObject repoJson,String fullName)  {
		try {
			int cnt=0,total=0,p=1;		
			//https://api.github.com/repositories/2500088/contents/db
			//filename:schema.rb create_table in:file
			URL url = new URL("https://api.github.com/search/code?page="+p+"&per_page=100"
					+ "&q=filename:schema.rb+create_table+in:file+language:ruby+repo:"+ fullName
					+ "&access_token="+gh.oauth);
			//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(url,true)) {
				JsonObject obj = rdr.readObject();
				total=obj.getInt("total_count");
				JsonArray results = obj.getJsonArray("items");
				if (total>0) {
					daoRepo.beginTransaction();
					Repo repo = new Repo(Language.RUBY);
					repo.setLanguage(Language.RUBY);
					repo.setName(fullName);
					repo.setUrl(repoJson.getString("html_url"));
					
					for (JsonObject result : results.getValuesAs(JsonObject.class)) {
						String dbpath = result.getString("path");
						String fname = result.getString("name");
						if (dbpath.contains("db/"+fname)) {
							if (repo.getConfigPath()!=null) {
								LOG.warning("duplicate path:"+repo.getConfigPath()+" x "+dbpath);							
							}
							repo.setConfigPath(dbpath);							
						} else 
							LOG.info(" Rejected:"+fullName+"/raw/master/"+dbpath);						
					}
					daoRepo.persit(repo);
					if (repo.getConfigPath()!=null) {
						LOG.info(" dbPath:"+fullName+"/raw/master/"+repo.getConfigPath());
						loadRepo(repo);
					} else {
						LOG.warning(" no suitable dbPath for "+fullName);
					}
					daoRepo.commitAndCloseTransaction();
				}
			}		
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,"Repository "+fullName+":"+ex.getMessage(),ex);	
		}
	}

	public RubyRepo loadRepo(Repo repo) throws MalformedURLException {			
		//  /repos/:owner/:repo/contents/:path
		/*
		URL url = new URL("https://api.github.com/repos/"+repo.getName()+"/contents/app/models"
				+ "?access_token="+oauth);
		JsonReader rdr = gh.callApi(url);
		if (rdr==null) { //File not Found
			return null;
		}
		RubyRepo rrepo = loader.setRepo(repo);			
		loader.visitSchema(new URL("https://github.com/"+repo.getName()+ "/raw/master/"+repo.getConfigPath()));	
				
		JsonArray results = rdr.readArray();*/
		RubyRepo rrepo=readModelEntry(null,repo,"app/models");
		/*
		for (JsonObject result : results.getValuesAs(JsonObject.class)) {
			
			
			String type = result.getString("type");
			if (type.equals("file")) {
				String fpath = result.getString("download_url");
				if (fpath.endsWith(".rb")) {
					URL furl = new URL(fpath);						
					loader.visitFile(furl);						
				}
			} else if (type.equals("dir")) {
				String path=result.getString("path");
			}
		}*/
		if (rrepo!=null) {
			loader.solveRefs();
			rrepo.print();
		}
		return rrepo;
	}
	private RubyRepo readModelEntry(RubyRepo rrepo,Repo repo,String path) throws MalformedURLException {
		URL url = new URL("https://api.github.com/repos/"+repo.getName()+"/contents/"+path
				+ "?access_token="+gh.oauth);
		JsonReader rdr = gh.callApi(url,false);
		if (rdr==null) { //File not Found
			return null;
		}
		if (rrepo==null) {
			rrepo = loader.setRepo(repo);	
			loader.visitSchema(new URL("https://github.com/"+repo.getName()+ "/raw/master/"+repo.getConfigPath()));	
		}		
		JsonArray results = rdr.readArray();		
		for (JsonObject result : results.getValuesAs(JsonObject.class)) {			
			String type = result.getString("type");
			if (type.equals("file")) {
				String fpath = result.getString("download_url");
				if (fpath.endsWith(".rb")) {
					URL furl = new URL(fpath);						
					loader.visitFile(furl);						
				}
			} else if (type.equals("dir")) {
				String dpath=result.getString("path");
				readModelEntry(rrepo,repo,dpath);
			}
		}
		return rrepo;
	}
}