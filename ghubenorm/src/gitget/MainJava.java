package gitget;

import static gitget.Log.LOG;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class MainJava {
	public static void main(String[] args) {
		new Thread(new JavaCrawler()).start();
	}
}

class JavaCrawler implements Runnable {

	static String oauth = Auth.getProperty("oauth");
	static GitHubCaller gh = GitHubCaller.instance;
	public static final long MAX_REPOS=500;
	// per_page max é de 100 (mais que isso ele considera como 100)

	@Override
	public void run() {
		try {
			URL uauth = new URL("https://api.github.com/?access_token=" + oauth);
			//try (InputStream is = uauth.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(uauth)) {
				JsonObject obj = rdr.readObject();
				System.out.println(obj.toString());
			}
			int cnt = 0, total = 0, p = 1;
			do {
				URL url = new URL("https://api.github.com/search/repositories?page=" + p
						+ "&per_page=100&q=language:java&order=desc&access_token=" + oauth);
				System.out
						.println("********************************************************************************* ");
				System.out.println("page " + p);
				//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
				try (JsonReader rdr = gh.callApi(url)) {
					JsonObject obj = rdr.readObject();
					JsonArray results = obj.getJsonArray("items");
					total = obj.getInt("total_count");
					for (JsonObject result : results.getValuesAs(JsonObject.class)) {
						System.out.println(cnt + " " + result.getString("name") + ":" + result.getString("full_name"));
						System.out.print("owner:" + result.getJsonObject("owner").getString("login"));

						System.out.println("-----------");
						cnt++;
						// ----
						printCode(result.getString("full_name"));

					}
				}
				p++;
			} while (cnt < MAX_REPOS && cnt < total);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
			
		}
	}

	// get html_url, replace /blob/ with /raw/ ?
	// var=path
	// puro:https://github.com/iluwatar/java-design-patterns/blob/master/service-layer/src/main/java/com/iluwatar/servicelayer/spell/Spell.java
	// raw:
	// https://github.com/iluwatar/java-design-patterns/raw/master/service-layer/src/main/java/com/iluwatar/servicelayer/spell/Spell.java
	public static void printCode(String path) throws Exception {
		int cnt = 0, total = 0, p = 1;
		do {
			URL url = new URL("https://api.github.com/search/code?page=" + p + "&per_page=100"
					+ "&q=javax.persistence+in:file+language:java+repo:" + path + "&access_token=" + oauth);
			//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(url)) {
				JsonObject obj = rdr.readObject();
				total = obj.getInt("total_count");
				JsonArray results = obj.getJsonArray("items");

				for (JsonObject result : results.getValuesAs(JsonObject.class)) {
					System.out.println(result);
				}
			}
		} while (false);
	}
}
