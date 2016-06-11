package gitget;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;

import dao.ConfigDAO;
import db.jpa.JPA_DAO;
import model.Language;
import sruby.TesteJRuby2;

import static gitget.Log.LOG;

/**
 * MUST use this: https://developer.github.com/v3/repos/#list-all-public-repositories
 * https://api.github.com/repositories?since=364
 * @author torres
 *
 */
public class GitHubCrawler implements Runnable {
	RubyCrawler ruby = new RubyCrawler();
	JavaCrawler java = new JavaCrawler();
	static GitHubCaller gh = GitHubCaller.instance;
	public static final long MAX_REPOS=50000;	
	
	public static void main(String[] args) {		
		new Thread(new GitHubCrawler()).start();
	}	
	@Override
	public void run() {			
		try {
			ConfigDAO.config(JPA_DAO.instance);			
			URL uauth = new URL("https://api.github.com/?access_token="+gh.oauth);
			//try (InputStream is = uauth.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(uauth,false)) {
				JsonObject obj = rdr.readObject();
				LOG.info(obj.toString());				
			}
			readByRepo();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);
		} finally {
			ConfigDAO.finish();
		}
	}

	
	private Language mainLanguage(String path) throws MalformedURLException {
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
			return Language.OTHER;
		}
		Log.LOG.info("Main language:"+lang);
		if (lang==null)
			return Language.OTHER;
		else
			return lang;			
		
	}
	private void readByRepo() throws MalformedURLException {
		long cnt=0,p=1;
		long id=0;
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
				
				for (JsonObject result : results.getValuesAs(JsonObject.class)) {
					
					String fullName = result.getString("full_name");
					String name = result.getString("name");
					boolean priv = result.getBoolean("private");
					id = result.getInt("id");
					LOG.info(cnt+" "+name+":"+fullName+
							"owner:"+result.getJsonObject("owner").getString("login"));
					if (priv) {
						LOG.info("<Private>");
						continue;
					}
					
					LOG.info("-----------"+gh.limits);
					cnt++;
					//----
					Language lang = mainLanguage(fullName);
					if (lang==Language.RUBY) {
						ruby.processRepo(result,fullName);
					} else if (lang==Language.JAVA) {
						java.processRepo(result, fullName);
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
					ruby.processRepo(result,result.getString("full_name"));
					
				}
			}
			p++;
			
		} while(cnt<MAX_REPOS && cnt<total);
		
	}

}
class GitHubCaller {
	static String oauth = Auth.getProperty("oauth");
	public static final int MAX_TRIES=2; 
	private int tries=0;
	APILimit limits;
	public static final GitHubCaller instance = new GitHubCaller();
	private GitHubCaller() {		
	}	
	public JsonObject listFileTree(String path) throws IOException {
		URL url = new URL("https://api.github.com/repos/"+path+"/git/trees/master?recursive=1&access_token="+oauth);
		
		try (JsonReader rdr = Json.createReader(url.openStream())) {
			JsonObject res = rdr.readObject();
			return res;
		}
		
	}
	/**
	 * TODO: 
	 * -one minute for search limit reached
	 * -ten minutes for general limt.
	 * the reset time is on UTC seconds.
	 * @param url
	 * @param isSearch
	 * @return
	 */
	public JsonReader callApi(URL url,boolean isSearch) {	
		try {
			if (limits==null)
				limits = retrieveLimits();
			if (isSearch) {				
				while (limits.search<=0) {
					long time = System.currentTimeMillis()/1000;
					time = limits.searchReset-time;
					if (time>0) {
						time++;
						LOG.info("Sleeping searches for "+time+" seconds...");
						Thread.sleep(time*1000);
					}
					limits = retrieveLimits();
				}
				limits.search--;
			} else {				
				while (limits.core<=0) {
					long time = System.currentTimeMillis()/1000;
					time = limits.coreReset-time;
					if (time>0) {
						time++;
						LOG.info("Sleeping core for "+time+" seconds...");
						Thread.sleep(time*1000);
					}
					limits = retrieveLimits();
					
				}
				limits.core--;
			}
			InputStream is = url.openStream();
			JsonReader rdr = Json.createReader(is);
			tries=0;
			return rdr;
		} catch (FileNotFoundException fex) {
			LOG.info("File not found:"+url);
			return null;
		} catch (IOException iex) {
			/*
			tries++;
			try {
				LOG.info("Sleeping for half a minute..."+tries);
				Thread.sleep(30000);
				if (tries<=MAX_TRIES)
					return callApi(url,isSearch);
			} catch (Exception tex) {
				LOG.log(Level.SEVERE,tex.getMessage(),tex);					
			}*/
			LOG.log(Level.WARNING,iex.getMessage(),iex);
			return null;
			//throw new RuntimeException(iex);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	public APILimit retrieveLimits() {
		APILimit ret = null;
		try {
			URL url = new URL("https://api.github.com/rate_limit?access_token="+oauth);
			try (JsonReader rdr = Json.createReader(url.openStream())) {
				ret = new APILimit(rdr);
				//JsonObject obj = rdr.readObject();
				//JsonObject res = obj.getJsonObject("resources");
				LOG.info(ret.toString());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.log(Level.WARNING,e.getMessage(),e);
		}
		return ret;
	}
}
class APILimit {
	long time;
	int core;
	int coreLimit;
	long coreReset;
	int search;
	int searchLimit;	
	long searchReset;
	public APILimit(JsonReader rdr) {
		time = System.currentTimeMillis()/1000;
		JsonObject obj = rdr.readObject();
		JsonObject res = obj.getJsonObject("resources");
		JsonObject jcore = res.getJsonObject("core");
		this.core = jcore.getInt("remaining");
		this.coreLimit = jcore.getInt("limit");
		this.coreReset = jcore.getInt("reset");
		JsonObject s = res.getJsonObject("search");
		this.search = s.getInt("remaining");
		this.searchLimit = s.getInt("limit");
		this.searchReset = s.getInt("reset");
		
	}
	@Override
	public String toString() {
		return "APILimit [time=" + time + ", core=" + core + ", coreLimit=" + coreLimit + ", coreReset=" + coreReset
				+ ", search=" + search + ", searchLimit=" + searchLimit + ", searchReset=" + searchReset + "]";
	}
	
}
