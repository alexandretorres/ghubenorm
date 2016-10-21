package gitget;

import static gitget.Log.LOG;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;
import sjava.Prof;

/**
 * MUST use this: https://developer.github.com/v3/repos/#list-all-public-repositories
 * https://api.github.com/repositories?since=364
 * @author torres
 *
 */
public class GitHubCrawler implements Runnable {
	/**
	 * In order to issue RELOAD_REPOS, first (re)compute hasClasses, then drop all tables except repo.
	 * after that, reload_repos will reload only the repositories where hasClasses is true, until the last publicid.
	 * After that it works just as the normal githubcrawler.
	 */
	public static final boolean RELOAD_REPOS=true;
	public static final long MAX_REPOS=1000000;	 
	public static final long MAX_ERRORS=10;	
	static {
		ConfigDAO.config(JPA_DAO.instance);		
	}
	RubyCrawler ruby = new RubyCrawler();
	JavaCrawler java = new JavaCrawler();
	static GitHubCaller gh = GitHubCaller.instance;

	
	
	public static void main(String[] args) {		
		new Thread(new GitHubCrawler()).start();
	}	
	@Override
	public void run() {			
		try {				
			URL uauth = new URL("https://api.github.com/?access_token="+gh.oauth);
			//try (InputStream is = uauth.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(uauth,false)) {
				JsonObject obj = rdr.readObject();
				LOG.info(obj.toString());				
			}
			//select...
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			dao.beginTransaction();
			int lastId = 0;
			try {
				lastId = dao.findMaxPublicId();
			} catch (Exception ex) {
				ex.printStackTrace();
			}		
			dao.rollbackAndCloseTransaction();
			if (RELOAD_REPOS) {
				lastId = reloadRepos(dao,lastId);
			}
			LOG.info("Starting at public id "+lastId);
			readByRepo(lastId);
		} catch (Throwable ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);
		} finally {
			Prof.print();
			ConfigDAO.finish();
		}
	}
	private int reloadRepos(RepoDAO dao, int lastId) {
		int id=0;
		int start=0;
		
		while (id<lastId) {
			dao.beginTransaction();
			List<Repo> results = dao.findPage(start,100);
			dao.commitAndCloseTransaction();
			for (Repo repo:results) {
				start++;
				// if has classes is part of the query, id will never reach max publicid
				if(repo.getHasClasses() && repo.getConfigPath()!=null && repo.getConfigPath().length()>0) {
					if (!repo.getClasses().isEmpty()) {
						throw new RuntimeException("reload repos cannot process repositories that are already loaded");
					}
					if (repo.getLanguage()==Language.JAVA) {
						java.processRepo(repo);
					} else if (repo.getLanguage()==Language.RUBY) {
						ruby.processRepo(repo);
					}
				}
				if (repo.getPublicId()>id)
					id = repo.getPublicId();
			}
		}
		return id;
	}
	//TODO: get ALL repo info instead! there is the language
	private Language mainLanguage_(String path) throws MalformedURLException {
		URL url = new URL("https://api.github.com/repos/"+path+"/languages?access_token="+gh.oauth);
		Language lang = null;
		int cnt=0;
		try (JsonReader rdr = gh.callApi(url,false)) {
			JsonObject obj =rdr.readObject();		
				
			for (String key:obj.keySet()) {
				int val = obj.getInt(key);
				if (val>cnt) {
					cnt = val;
					lang = Language.getLanguage(key);
				}
			}			
		} catch (Exception ex) {
			return Language.UNKNOWN;
		}
		Log.LOG.info("Main language:"+lang);
		if (lang==null)
			return Language.UNKNOWN;
		else
			return lang;			
		
	}
	/**
	 * first id, usually zero.
	 * @param id
	 * @throws MalformedURLException
	 */
	private void readByRepo(long id) throws MalformedURLException {
		long cnt=0,p=1;
		long errorCount=0;
		//long id=0;
		do {
			//GitHubCaller.instance.limits = gh.retrieveLimits();
			URL url = new URL("https://api.github.com/repositories?since="+id+"&access_token="+gh.oauth);
			LOG.info("********************************************************************************* ");
			LOG.info("page "+p);
			//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			
			try (JsonReader rdr = gh.callApi(url,false)) {				
				JsonArray results = rdr.readArray();				
				if (results.isEmpty()) {
					LOG.warning("empty results. Error? "+rdr.read());
				}
				String fullName=null;
				for (JsonObject result : results.getValuesAs(JsonObject.class)) {
					try {
						fullName = result.getString("full_name");					
						boolean fork = result.containsKey("fork") ?  result.getBoolean("fork") : false;					
						boolean priv = result.getBoolean("private");
						id = result.getInt("id");
						LOG.info(cnt+" (ID:"+id+")"+":"+fullName+
								" owner:"+result.getJsonObject("owner").getString("login"));
						
						cnt++;
						if (priv) {
							LOG.info("<Private>");
							continue;
						}
						if (fork) {
							LOG.info(fullName+" is a FORK repo. Skipping");
							continue;
						}
						LOG.info("-----------"+gh.getLimits());					
						//----
						//Language lang = mainLanguage(fullName);
						//
						
						//
						result = gh.getRepoInfo(fullName);
						if (result==null)
							continue;
						JsonValue parent = result.get("parent");
						if (parent==JsonValue.NULL) {
							LOG.severe("repo "+fullName+" has a parent but is not FORKED. Skipping");
							continue;
						}					
						JsonValue lang_obj = result.get("language");
						Language lang =Language.UNKNOWN;
						if (lang_obj.getValueType()==ValueType.STRING) {
							lang = Language.getLanguage(((JsonString) lang_obj).getString());
						} else if (lang_obj.getValueType()==ValueType.ARRAY) {
							JsonArray array = (JsonArray)lang_obj;
							lang = Language.getLanguage(array.getString(0));
						} else if (lang_obj.getValueType()==ValueType.NULL) {
							// do nothing
						} else {
							LOG.info("unexpected language value for repo "+fullName);
						}
						if (lang==Language.RUBY) {
							ruby.processRepo(ruby.createRepo(result,fullName));
						} else if (lang==Language.JAVA) {
							java.processRepo(java.createRepo(result, fullName));
						}
						fullName=null;
					} catch (Exception ex) {
						errorCount++;
						LOG.severe("Exception reading repo list ("+errorCount+"), repo '"+fullName+"'");
						LOG.log(Level.SEVERE,ex.getMessage(),ex);
						LOG.info(result.toString());						
						if (errorCount>MAX_ERRORS) {
							LOG.severe("Error count exceeded MAX. Exiting");
							return;
						}
					}
				}
			}
			p++;
			
		} while(cnt<MAX_REPOS);
		
	}
	@Deprecated
	private void readByAPI() throws MalformedURLException {
		long cnt=0,total=0,p=1;
		do {
			//PROBLEM: ONLY 1000 RESULTS PER SEARCH! 
			URL url = new URL("https://api.github.com/search/repositories?page="+p+"&per_page=100&q=language:ruby&order=desc&access_token="+gh.oauth);
			LOG.info("********************************************************************************* ");
			LOG.info("page "+p);
			//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(url,true)) {
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
					ruby.processRepo(ruby.createRepo(result,result.getString("full_name")));
					
				}
			}
			p++;
			
		} while(cnt<MAX_REPOS && cnt<total);
		
	}

}

