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
import javax.print.attribute.standard.MediaSize.Other;

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
	public static final long MAX_REPOS=100000;	
	
	
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
					
					
					boolean fork = result.getBoolean("fork");
					
					
					boolean priv = result.getBoolean("private");
					id = result.getInt("id");
					LOG.info(cnt+" "+":"+fullName+
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

