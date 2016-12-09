package gitget;

import static gitget.Log.LOG;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import model.Language;
import model.Repo;
import model.SkipReason;

public class GitHubRepoLoader extends GitHubCrawler {
	public Repo load(String name) {
		Repo repo=null;
		JsonObject result = gh.getRepoInfo(name);
		
		JsonValue lang_obj = result.get("language");
		Language lang =Language.UNKNOWN;
		if (lang_obj.getValueType()==ValueType.STRING) {
			lang = Language.getLanguage(((JsonString) lang_obj).getString());
		} else if (lang_obj.getValueType()==ValueType.ARRAY) {
			JsonArray array = (JsonArray)lang_obj;
			lang = Language.getLanguage(array.getString(0));
		} else if (lang_obj.getValueType()==ValueType.NULL) {
			return null;						
		} else {
			return null;
		}
		//pick a repo
		if (lang==Language.RUBY) {
			repo = ruby.createRepo(result,name);
			SkipReason skip =  ruby.processRepo(repo);
			
		} else if (lang==Language.JAVA) {	
			repo = java.createRepo(result, name);
			SkipReason skip = java.processRepo(repo);
			
		}
		
		return repo;
	}
}
