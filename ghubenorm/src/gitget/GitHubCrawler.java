package gitget;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import dao.ConfigDAO;
import db.jpa.JPA_DAO;
import model.Language;
import sjava.Prof;
import sruby.TesteJRuby2;

import static gitget.Log.LOG;

/**
 * MUST use this: https://developer.github.com/v3/repos/#list-all-public-repositories
 * https://api.github.com/repositories?since=364
 * @author torres
 *
 */
public class GitHubCrawler implements Runnable {
	static {
		ConfigDAO.config(JPA_DAO.instance);		
	}
	RubyCrawler ruby = new RubyCrawler();
	JavaCrawler java = new JavaCrawler();
	static GitHubCaller gh = GitHubCaller.instance;
	public static final long MAX_REPOS=2000;	
	
	
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
			readByRepo();
		} catch (Throwable ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);
		} finally {
			Prof.print();
			ConfigDAO.finish();
		}
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
							" owner:"+result.getJsonObject("owner").getString("login"));
					if (priv) {
						LOG.info("<Private>");
						continue;
					}
					
					LOG.info("-----------"+gh.limits);
					cnt++;
					//----
					//Language lang = mainLanguage(fullName);
					//
					
					//
					result = gh.getRepoInfo(fullName);
					if (result==null)
						continue;
					
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
	public URL newURL(String host,String path,String query) throws MalformedURLException, URISyntaxException {
		return (new URI("https",host,path,query,null)).toURL();			
		
	}
	protected JsonObject getRepoInfo(String path)  {
		try {
			URL url = newURL("api.github.com","/repos/"+path,"access_token="+oauth);		
			try (JsonReader rdr = callApi(url,false)) {
				JsonObject obj =rdr.readObject();	
				return obj;
			}
		} catch (Exception ex) {
			LOG.log(Level.WARNING, ex.getMessage(), ex);
			return null;
		}
		
	}
	public JsonObject listFileTree(String path,String branch) throws IOException, URISyntaxException {
		URL url =newURL("api.github.com","/repos/"+path+"/git/trees/"+branch,"recursive=1&access_token="+oauth);
		//URL url = new URL("https://api.github.com/repos/"+path+"/git/trees/"+branch+"?recursive=1&access_token="+oauth);
		
		try (JsonReader rdr = callApi(url,false)) {///Json.createReader(url.openStream())) {
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
		HttpURLConnection connection=null;
		try {
			if (limits==null)
				limits = retrieveLimits();
			if (isSearch) {				
				while (limits.search<=0) {
					long time = System.currentTimeMillis()/1000;
					time = limits.searchReset-time;
					if (time>0) {
						time++;
						LOG.warning("Sleeping searches for "+time+" seconds...");
						Thread.sleep(time*1000);
					} else {
						Thread.sleep(1000);
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
						LOG.warning("Sleeping core for "+time+" seconds...");
						Thread.sleep(time*1000);
					} else {
						Thread.sleep(1000);
					}
					limits = retrieveLimits();
					
				}
				limits.core--;
			}
			connection = (HttpURLConnection)url.openConnection();
			
			
			InputStream is = connection.getInputStream();
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
			try {
				//APILimit curLimits = limits;
				LOG.warning("Current Limits:" +limits);
				limits = retrieveLimits();
				LOG.warning("True Limits:   "+limits);
			} catch (Exception e) {	}			
			
			if (connection!=null) {
				try {
					InputStream error = connection.getErrorStream();
					java.util.Scanner s = new java.util.Scanner(error).useDelimiter("\\A");
				    if (s.hasNext())
				    	LOG.warning("Error stream:" +s.next());
				} catch (Exception e) {	}
				
				String st = "Headers:";
				for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
				    st+=(header.getKey() + "=" + header.getValue())+"\n";
				}
				LOG.fine(st);
				try {
					LOG.warning("HTTP response:"+connection.getResponseMessage());
				} catch (IOException e) {	}	
			
				
			}
			if (limits.core<=1 || limits.search<=1) {
				tries++;				
				if (tries<=MAX_TRIES)
					return callApi(url,isSearch);
			}
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
