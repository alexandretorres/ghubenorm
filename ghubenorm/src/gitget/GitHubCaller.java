package gitget;

import static gitget.Log.LOG;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

public class GitHubCaller {
	public boolean forceTooManyFiles=false;
	public static final int MAX_TRIES=2; 
	private int tries=0;
	private APILimit limits;
	public static final GitHubCaller instance = new GitHubCaller();
	Config config;
	private GitHubCaller() {		
	}	
	public Config getConfig() {
		if (config==null)
			config = Config.getDefault();
		return config;
	}
	public void setConfig(Config config) {
		this.config=config;
	}
	public String getOAuth() {
		return getConfig().oauth;
	}
	public APILimit getLimits() {
		return limits;
	}

	private void setLimits(APILimit limits) {
		this.limits = limits;
	}
	public static String getSha(JsonObject result,String branch) {
		String sha = branch;
		if (result.containsKey("url")) {
			sha = result.getString("url");
			sha = sha.substring(sha.lastIndexOf("?ref=")+5);
			//sha = result.getString("sha");
		}
		return sha;
	}
	public URL newURL(String host,String path,String query) throws MalformedURLException, URISyntaxException {
		return (new URI("https",host,path,query,null)).normalize().toURL();			
		
	}
	protected JsonObject getRepoInfo(String path)  {
		try {
			URL url = newURL("api.github.com","/repos/"+path,"access_token="+getOAuth());		
			try (JsonReader rdr = callApi(url,false)) {
				if (rdr==null) {
					LOG.info("could not retrieve repo info for "+path+".");
					return null;
				}
				JsonObject obj =rdr.readObject();	
				return obj;
			}
		} catch (Exception ex) {
			LOG.log(Level.WARNING, ex.getMessage(), ex);
			return null;
		}
		
	}
	public JsonObject listFileTree(String path,String branch) throws IOException, URISyntaxException {
		URL url =newURL("api.github.com","/repos/"+path+"/git/trees/"+branch,"recursive=1&access_token="+getOAuth());
		//URL url = new URL("https://api.github.com/repos/"+path+"/git/trees/"+branch+"?recursive=1&access_token="+oauth);
		
		try (JsonReader rdr = callApi(url,false)) {///Json.createReader(url.openStream())) {
			if (rdr==null)
				return null;
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
			setAuthOnConnection(connection);
			InputStream is = connection.getInputStream();
			JsonReader rdr = Json.createReader(is);
			tries=0;
			return rdr;
		} catch (FileNotFoundException fex) {
			LOG.info("File not found:"+url);
			return null;
		} catch (IOException iex) {					
			IoError streamError=IoError.OTHER;
			
			try {
				//APILimit curLimits = limits;
				LOG.warning("Current Limits:" +limits);
				limits = retrieveLimits();
				LOG.warning("True Limits:   "+limits);
			} catch (Exception e) {	}			
			
			if (connection!=null) {
				String msg="";
				try {
					msg = getErrorStream(connection);					
					streamError = IoError.find(msg);					
				} catch (Exception e) {	}
				if (streamError==IoError.OTHER && msg!=null)
					LOG.warning("Error stream:" +msg);
				try {
					String st = "Headers:";
					for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
					    st+=(header.getKey() + "=" + header.getValue())+"\n";
					}
					LOG.fine(st);
				} catch (Exception e) {	}
				
				try {
					LOG.warning("HTTP response:"+connection.getResponseMessage());
				} catch (IOException e) {	}	
			
				
			}
			if (streamError==IoError.OTHER) {
				LOG.log(Level.WARNING,iex.getMessage(),iex);				
			} else if (streamError!=IoError.API_LIMIT_REACHED)
				return null;
			if (limits.core<=1 || limits.search<=1 || streamError==IoError.API_LIMIT_REACHED) {
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
	public String getErrorStream(URLConnection conn) {
		try {
			if (conn!=null && conn instanceof HttpURLConnection) {
				HttpURLConnection connection = (HttpURLConnection) conn;
				
				InputStream error = connection.getErrorStream();
				try (java.util.Scanner s = new java.util.Scanner(error)) {
					s.useDelimiter("\\A");
				    if (s.hasNext()) {
				    	String msg = s.next();
				    	return msg;		    	
				    }
			    }
			}
		} catch (Exception ex) {}
		return null;
	}
	public InputStream openAuthStream(URL url) throws IOException {		
		HttpURLConnection con = (HttpURLConnection) url.openConnection();			
		setAuthOnConnection(con);
		return con.getInputStream();
	}
	public void setAuthOnConnection(HttpURLConnection con) {
		con.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
		con.setRequestProperty("Accept","application/json");	
		con.setRequestProperty("Authorization","Bearer "+getOAuth());
	}
	public APILimit retrieveLimits() {
		APILimit ret = null;
		try {
			URL url = new URL("https://api.github.com/rate_limit?access_token="+getOAuth());
			try (JsonReader rdr = Json.createReader(openAuthStream(url))) {
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
/**
 * Server errors and default action
 * @author torres
 *
 */
enum IoError {
	REPO_BLOCKED("Repository access blocked"),
	API_LIMIT_REACHED("API rate limit exceeded for"),
	EMPTY_REPO("Git Repository is empty"),
	FILE_NOT_FOUND("<title>Page not found &middot; GitHub</title>"),
	OTHER("");
	
	public final String fragment;
	
	IoError(String fragment) {
		this.fragment=fragment;
	}
	public static IoError find(String msg) {
		if (msg==null)
			return OTHER;
		for (IoError io:IoError.values()) {
			if (msg.contains(io.fragment)) {
				return io;
			}
		}
		return OTHER;
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
