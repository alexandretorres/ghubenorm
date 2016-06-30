package gitget;

import static gitget.Log.LOG;
import static org.junit.Assert.*;

import java.net.URL;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Test;

public class GitCrawlerTest extends GitHubCrawler {

	@Test
	public void test() throws Exception {
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
					LOG.info("-----------"+gh.getLimits());
					LOG.info("-----------");
					cnt++;				
				}
			}
			p++;
			
		} while(cnt<MAX_REPOS && cnt<total);
		//gh.callApi(url, isSearch)
	}

}
