package gitget;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import model.Language;
import model.Repo;
import sruby.RubyRepo;
import sruby.RubyRepoLoader;
import sruby.TesteJRuby2;

import static gitget.Log.LOG;


public class MainRuby {
	
	public static void main(String[] args) {
		new Thread(new RubyCrawler()).start();
	}
}
class GitHubCaller {
	public static final int MAX_TRIES=2; 
	private int tries=0;
	public static final GitHubCaller instance = new GitHubCaller();
	private GitHubCaller() {		
	}	
	public JsonReader callApi(URL url) {	
		try {
			InputStream is = url.openStream();
			JsonReader rdr = Json.createReader(is);
			tries=0;
			return rdr;
		} catch (Exception ex) {
			tries++;
			try {
				LOG.info("Sleeping for one minute..."+tries);
				Thread.sleep(60000);
				if (tries<=MAX_TRIES)
					return callApi(url);
			} catch (Exception tex) {
				tex.printStackTrace();
			}
			throw new RuntimeException(ex);
		}
	}
}
class RubyCrawler implements Runnable {
	static String oauth = Auth.getProperty("oauth");
	static GitHubCaller gh = GitHubCaller.instance;
	public static final long MAX_REPOS=500;
	// per_page max é de 100 (mais que isso ele considera como 100)
	static RubyRepoLoader loader = new RubyRepoLoader();
	
	@Override
	public void run() {			
		try {
			URL uauth = new URL("https://api.github.com/?access_token="+oauth);
			//try (InputStream is = uauth.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(uauth)) {
				JsonObject obj = rdr.readObject();
				LOG.info(obj.toString());
				
			}
			long cnt=0,total=0,p=1;
			do {
				URL url = new URL("https://api.github.com/search/repositories?page="+p+"&per_page=100&q=language:ruby&order=desc&access_token="+oauth);
				LOG.info("********************************************************************************* ");
				LOG.info("page "+p);
				//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
				try (JsonReader rdr = gh.callApi(url)) {
					JsonObject obj = rdr.readObject();
					JsonArray results = obj.getJsonArray("items");
					if (results.isEmpty()) {
						LOG.warning("empty results. Error? "+obj);
					}
					total=obj.getInt("total_count");
					for (JsonObject result : results.getValuesAs(JsonObject.class)) {
						String name = result.getString("name");
						LOG.info(cnt+" "+name+":"+result.getString("full_name")+
								"owner:"+result.getJsonObject("owner").getString("login"));
						
						LOG.info("-----------");
						cnt++;
						//----
						printCode(result,result.getString("full_name"));
						
					}
				}
				p++;
			} while(cnt<MAX_REPOS && cnt<total);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	// get html_url, replace /blob/ with /raw/ ?
	// var=path
	//puro:https://github.com/iluwatar/java-design-patterns/blob/master/service-layer/src/main/java/com/iluwatar/servicelayer/spell/Spell.java
	//raw: https://github.com/iluwatar/java-design-patterns/raw/master/service-layer/src/main/java/com/iluwatar/servicelayer/spell/Spell.java
	public static void printCode(JsonObject repoJson,String fullName) throws Exception {
		int cnt=0,total=0,p=1;
		do {
			//https://api.github.com/repositories/2500088/contents/db
			//filename:schema.rb create_table in:file
			URL url = new URL("https://api.github.com/search/code?page="+p+"&per_page=100"
					+ "&q=filename:schema.rb+create_table+in:file+language:ruby+repo:"+ fullName
					+ "&access_token="+oauth);
			//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(url)) {
				JsonObject obj = rdr.readObject();
				total=obj.getInt("total_count");
				JsonArray results = obj.getJsonArray("items");
				if (total>0) {
					Repo repo = new Repo();
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
							
						}else 
							LOG.info(" Rejected:"+fullName+"/raw/master/"+dbpath);
						
						//JsonObject jrepo = result.getJsonObject("repository");
						
						//System.out.println(result);
					}
					if (repo.getConfigPath()!=null) {
						LOG.info(" dbPath:"+fullName+"/raw/master/"+repo.getConfigPath());
						loadRepo(repo);
					} else {
						LOG.warning(" no suitable dbPath for "+fullName);
					}
				}
			}
		} while(false);
	}
	
	private static void loadRepo(Repo repo) {
		try {
			RubyRepo rrepo = loader.setRepo(repo);
			
			loadSchemaDb(repo);
			//  /repos/:owner/:repo/contents/:path
			URL url = new URL("https://api.github.com/repos/"+repo.getName()+"/contents/app/models"
					+ "?access_token="+oauth);
			JsonReader rdr = gh.callApi(url);
			JsonArray results = rdr.readArray();
			for (JsonObject result : results.getValuesAs(JsonObject.class)) {
				String type = result.getString("type");
				if (type.equals("file")) {
					String fpath = result.getString("download_url");
					if (fpath.endsWith(".rb")) {
						URL furl = new URL(fpath);
						
						//TesteJRuby2.parseFile(rrepo, in);
						loader.visitFile(furl);
						//in.close();
						
					}
				}				
			}
			
			loader.solveRefs();
			rrepo.print();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	private static void loadSchemaDb(Repo repo) throws Exception {
		URL url = new URL("https://github.com/"+repo.getName()+ "/raw/master/"+repo.getConfigPath());
		
		//InputStream is = url.openStream();
		
		loader.visitSchema(url);
		//RubyRepo rrepo = TesteJRuby2.parseSchema(repo,in);
		
		
		
				//String model_url = url_name.substring(0,url_name.lastIndexOf("/"));
		//model_url = model_url+"/../app/models/";
	}
}
