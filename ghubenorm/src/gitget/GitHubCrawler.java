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
import model.SkipReason;
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
	public static final boolean RELOAD_REPOS=false;
	public static final long MAX_REPOS=10000000;	 
	public static final long MAX_ERRORS=10;	
	// The maximum number of files retrieved by github query API
	public static final long MAX_RETRIEVE=1000;
	public volatile static boolean stop=false;
	RubyCrawler ruby = new RubyCrawler();
	JavaCrawler java = new JavaCrawler();
	static GitHubCaller gh = GitHubCaller.instance;

	RepoDAO repoDao = ConfigDAO.getDAO(Repo.class);
	
	public static void main(String[] args) {	
		ConfigDAO.config(JPA_DAO.instance);	
		new Thread(new GitHubCrawler()).start();
	}	
	@Override
	public void run() {
		stop=false;
		try {				
			URL uauth = new URL("https://api.github.com/?access_token="+gh.getOAuth());
			//try (InputStream is = uauth.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(uauth,false)) {
				JsonObject obj = rdr.readObject();
				LOG.info(obj.toString());				
			}
			//select...
			RepoDAO dao = repoDao;
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
				if(repo.getHasClasses()!=null && repo.getHasClasses() && repo.getConfigPath()!=null && repo.getConfigPath().length()>0) {
					if (!repo.getClasses().isEmpty()) {
						throw new RuntimeException("reload repos cannot process repositories that are already loaded");
					}
					if (repo.getLanguage()==Language.JAVA) {
						repo.overrideErrorLevel(null);
						java.processRepo(repo);
					} else if (repo.getLanguage()==Language.RUBY) {
						repo.overrideErrorLevel(null);
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
		URL url = new URL("https://api.github.com/repos/"+path+"/languages?access_token="+gh.getOAuth());
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
	private void readByRepo(int id) throws MalformedURLException {
		long cnt=0,p=1;
		long errorCount=0;
		//long id=0;
		do {
			//GitHubCaller.instance.limits = gh.retrieveLimits();
			URL url = new URL("https://api.github.com/repositories?since="+id+"&access_token="+gh.getOAuth());
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
						if (stop) {
							Log.LOG.warning("GitHubCrawler stopping by request.");
							return;
						}
						String html_url = result.getString("html_url");
						fullName = result.getString("full_name");					
						boolean fork = result.containsKey("fork") ?  result.getBoolean("fork") : false;					
						boolean priv = result.getBoolean("private");
						id = result.getInt("id");
						LOG.info(cnt+" (ID:"+id+")"+":"+fullName+
								" owner:"+getOwner(result));
						
						cnt++;
						if (priv) {
							LOG.info("<Private>");
							skipRepo(SkipReason.PRIVATE, Language.UNKNOWN, fullName, html_url, id, null);
							continue;
						}
						if (fork) {
							LOG.info(fullName+" is a FORK repo. Skipping");
							skipRepo(SkipReason.FORK, Language.UNKNOWN, fullName, html_url, id, null);
							continue;
						}
						LOG.info("-----------"+gh.getLimits());					
						//----
						//Language lang = mainLanguage(fullName);
						//
						
						//
						result = gh.getRepoInfo(fullName);
						if (result==null) {
							skipRepo(SkipReason.NULL_INFO, Language.UNKNOWN, fullName, html_url, id, null);
							continue;
						}
						String branch = result.containsKey("default_branch") ? result.getString("default_branch") : null;
						JsonValue parent = result.get("parent");
						if (parent==JsonValue.NULL) {
							LOG.severe("repo "+fullName+" has a parent but is not FORKED. Skipping");
							skipRepo(SkipReason.HAS_PARENT, Language.UNKNOWN, fullName, html_url, id, branch);
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
							skipRepo(SkipReason.NO_LANGUAGE, Language.UNKNOWN, fullName, html_url, id, branch);
							continue;							
						} else {
							LOG.info("unexpected language value for repo "+fullName);
							skipRepo(SkipReason.NO_LANGUAGE, Language.UNKNOWN, fullName, html_url, id, branch);
							continue;
						}
						//pick a repo
						if (lang==Language.RUBY) {
							SkipReason skip =  ruby.processRepo(ruby.createRepo(result,fullName));
							if (skip!=SkipReason.NONE) {
								skipRepo(skip, lang, fullName, html_url, id, branch);
								continue;
							}
						} else if (lang==Language.JAVA) {							
							SkipReason skip = java.processRepo(java.createRepo(result, fullName));
							if (skip!=SkipReason.NONE) {
								skipRepo(skip, lang, fullName, html_url, id, branch);
								continue;
							}
						} else {
							skipRepo(SkipReason.OTHER_LANGUAGE, lang, fullName, html_url, id, branch);
							continue;
						}
						fullName=null;
					} catch (Exception ex) {
						skipRepo(SkipReason.ERROR, Language.UNKNOWN, fullName, null, id, null);
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
	private static String getOwner(JsonObject result) {
		try {			
			String res = result.getJsonObject("owner").getString("login");
			return res;
		} catch (Exception ex) {
			return "<unknown>";
		}
		
	}
	public void skipRepo(SkipReason reason,Language lang,String name,String url,int publicId,String branch) {
		try {			
			repoDao.beginTransaction();
			Repo repo = new Repo(lang);			
			repo.setBranch(branch);
			repo.setName(name);			
			repo.setUrl(url);//repoJson.getString("html_url")
			repo.setPublicId(publicId);//repoJson.getInt("id")
			repo.setSkipReason(reason);
			repoDao.persist(repo);
			repoDao.commitAndCloseTransaction();
		} catch (Exception ex) {
			LOG.log(Level.WARNING, ex.getMessage(), ex);
		}
	}
	@Deprecated
	private void readByAPI() throws MalformedURLException {
		long cnt=0,total=0,p=1;
		do {
			//PROBLEM: ONLY 1000 RESULTS PER SEARCH! 
			URL url = new URL("https://api.github.com/search/repositories?page="+p+"&per_page=100&q=language:ruby&order=desc&access_token="+gh.getOAuth());
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

