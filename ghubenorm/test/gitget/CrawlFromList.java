package gitget;

import static gitget.Log.LOG;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import dao.ConfigDAO;
import dao.jpa.CascadeDeleteVisitor;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;
import model.SkipReason;
import sjava.Prof;

public class CrawlFromList extends GitHubCrawler {
	public static boolean RELOAD_INFO=true;
	public static void main(String[] args) {	
		ConfigDAO.config(JPA_DAO.instance);
		new Thread(new CrawlFromList()).start();
	}
	private List<Integer> publicIds = new ArrayList<>();	
	@Override
	public void run() {	
		//this.gh.forceTooManyFiles=true;
		try {
			try (Scanner sc = new Scanner(new File("reload.txt"))) {				
		        while (sc.hasNextLine()) {		        				        
		            String line = sc.nextLine();
		            publicIds.add(new Integer(line));
		        }
			}
			
			URL uauth = new URL("https://api.github.com/?access_token="+gh.getOAuth());
			//try (InputStream is = uauth.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(uauth,false)) {
				JsonObject obj = rdr.readObject();
				LOG.info(obj.toString());				
			}
			//select...
			RepoDAO dao = repoDao;			
			//remove(dao);
			reloadFromList(dao);
		} catch (Throwable ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);
		} finally {
			Prof.print();
			ConfigDAO.finish();
		}
	}
	public void remove(RepoDAO dao) {		
		for (int pid:this.publicIds) {
			dao.beginTransaction();		
			Repo repo = dao.findByPublicId(pid).get(0);		
			CascadeDeleteVisitor del = new CleanRepoVisitor(repo);			
			//dao.removeCascade(r);
			LOG.info("visitor executed, now issuing commit for:"+repo.getId()+" name "+repo.getName());
			dao.commitAndCloseTransaction();
		}		
	}
	public SkipReason reloadInfo(Repo repo) {
		String fullName = repo.getName();
		JsonObject result = gh.getRepoInfo(repo.getName());
		if (result==null) {			
			return SkipReason.NULL_INFO;
		}
	
		JsonValue parent = result.get("parent");
		if (parent==JsonValue.NULL) {
			LOG.severe("repo "+fullName+" has a parent but is not FORKED. Skipping");
			return SkipReason.HAS_PARENT;
			
		}					
		JsonValue lang_obj = result.get("language");
		Language lang =Language.UNKNOWN;
		if (lang_obj.getValueType()==ValueType.STRING) {
			lang = Language.getLanguage(((JsonString) lang_obj).getString());
		} else if (lang_obj.getValueType()==ValueType.ARRAY) {
			JsonArray array = (JsonArray)lang_obj;
			lang = Language.getLanguage(array.getString(0));
		} else if (lang_obj.getValueType()==ValueType.NULL) {
			return SkipReason.NO_LANGUAGE;
									
		} else {
			return SkipReason.NO_LANGUAGE;
		}
		repo.setLanguage(lang);
		if (lang!=Language.JAVA && lang!=Language.RUBY) {
			return SkipReason.OTHER_LANGUAGE;
		}
		
		
		repo.setSkipReason(null);
		return null;
	}
	public void reloadFromList(RepoDAO dao) {		
		
		for (int pid:this.publicIds) {
			/*
			dao.beginTransaction();		
			Repo repo = dao.findByPublicId(pid).get(0);		
			CascadeDeleteVisitor del = new CleanRepoVisitor(repo);			
			//dao.removeCascade(r);
			LOG.info("visitor executed, now issuing commit for:"+repo.getId()+" name "+repo.getName());
			dao.commitAndCloseTransaction();*/
			dao.beginTransaction();	
			dao.cleanRepo(pid);
			dao.commitAndCloseTransaction();
			dao.beginTransaction();		
			Repo repo = dao.findByPublicId(pid).get(0);			
			dao.commitAndCloseTransaction();
			if (!repo.getClasses().isEmpty() || !repo.getDataSources().isEmpty()) {
				throw new RuntimeException("reload repos cannot process repositories that are already loaded");
			}
			SkipReason skip=null;
			if (RELOAD_INFO) {
				skip = reloadInfo(repo);
				if (skip!=null) {
					Repo old = repo;
					dao.beginTransaction();	
					repo=dao.findByPublicId(pid).get(0);	
					repo.setSkipReason(skip);
					repo.setLanguage(old.getLanguage());
					dao.commitAndCloseTransaction();
					LOG.warning(repo.getName()+"("+repo.getPublicId()+") SKIPED! "+skip);
					continue;
				}
				
			}
			
			if (repo.getLanguage()==Language.JAVA) {
				repo.overrideErrorLevel(null);
				repo.setSkipReason(SkipReason.NONE);
				repo.setLoadDate(new Date());
				skip = java.processRepo(repo);
			} else if (repo.getLanguage()==Language.RUBY) {
				repo.overrideErrorLevel(null);
				repo.setSkipReason(SkipReason.NONE);
				repo.setLoadDate(new Date());
				skip = ruby.processRepo(repo);
			}
			if (skip!=null && skip!=SkipReason.NONE)
				LOG.warning("SKIPED! "+skip);
			LOG.info("repo reload finished:"+repo.getId()+" name "+repo.getName()+" public id "+repo.getPublicId());
		}
		
			
	}
}
