package gitget;

import static gitget.Log.LOG;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import model.Language;
import model.Repo;
import model.SkipReason;
import sjava.Prof;
import sruby.RubyRepo;
import sruby.RubyRepoLoader;

class RubyCrawler  {
	
	private static RubyRepoLoader loader = RubyRepoLoader.getInstance();
	private static GitHubCaller gh = GitHubCaller.instance;
	private RepoDAO daoRepo;
	public Repo createRepo(JsonObject repoJson,String fullName) {
		Repo repo = new Repo(Language.RUBY);		
		repo.setPublicId(repoJson.getInt("id"));
		repo.setLanguage(Language.RUBY);
		repo.setName(fullName);
		repo.setUrl(repoJson.getString("html_url"));
		repo.setBranch(repoJson.getString("default_branch"));
		return repo;
	}
	public SkipReason processRepo(Repo repo)  {
		String fullName = repo.getName();
		Prof.open("processRubyRepo");
		try {			
			if (daoRepo==null)
				daoRepo = ConfigDAO.getDAO(Repo.class);
			
			Prof.open("listFileTreeRuby");
			JsonObject result = gh.listFileTree(fullName,repo.getBranchGit());
			Prof.close("listFileTreeRuby");
			if (result==null) {
				LOG.info("no files at the branch "+repo.getBranchGit()+". Skipping repo.");
				return SkipReason.NO_FILES_AT_BRANCH;
			}
			boolean truncated = result.getBoolean("truncated");
			if (truncated)
				LOG.warning("truncated tree file for repository "+fullName);
			Dir root = Dir.newRoot();
			for (JsonObject res: result.getJsonArray("tree").getValuesAs(JsonObject.class)) {
				String path  =res.getString("path");
				if (path.endsWith(".rb")) {
					if (!res.containsKey("size")) {
						LOG.warning("rb file without size? path="+path);
						continue;
					}
					int size = res.getInt("size");
					if (size==0)
						continue;			
					root.register(path).setSha( result.getString("sha")); // must be the TREE sha! not the file SHA
				} else 
					continue;
				if (path.equals("db/schema.rb")) {
					repo.setConfigPath(path);
				} else if (repo.getConfigPath()==null && path.endsWith("db/schema.rb")) {
					repo.setConfigPath(path);
				} else if (path.endsWith("db/schema.rb") && repo.getConfigPath()!=null 
							&& repo.getConfigPath().contains("/test/") && !path.contains("/test/")) {
					repo.setConfigPath(path);
				}
				if (GitHubCrawler.stop)
					throw new RuntimeException("Asked to stop");
			}			
			daoRepo.beginTransaction();	
			Integer level = repo.getErrorLevel();
			repo = daoRepo.reattachOrSave(repo);
			repo.overrideErrorLevel(level);
		
			//daoRepo.persist(repo);
			if (repo.getConfigPath()!=null) {
				RubyRepo rrepo = loader.setRepo(repo);
				rrepo.setRoot(root);
				LOG.info(" dbPath:"+fullName+"/raw/"+repo.getBranchGit()+"/"+repo.getConfigPath());
				Prof.open("loadRubyRepo");
				loadRepo(rrepo);
				Prof.close("loadRubyRepo");
			} else {
				repo.setSkipReason(SkipReason.NO_CONFIG_FOUND);
				LOG.info(" no suitable dbPath for "+fullName);
			}
			
			repo.checkHasClasses();
			daoRepo.commitAndCloseTransaction();
			
		} catch (Exception ex) {
			daoRepo.rollbackAndCloseTransaction();
			LOG.log(Level.SEVERE,"Repository "+fullName+":"+ex.getMessage(),ex);	
			return SkipReason.ERROR;
		}
		Prof.close("processRubyRepo");
		return SkipReason.NONE;
	}
	
	public RubyRepo loadRepo(RubyRepo rrepo) throws MalformedURLException, URISyntaxException {	
		Repo repo = rrepo.getRepo();
		//readModelEntry(null,repo,"app/models");			
		Dir modelDir = rrepo.getRoot().find("app/models");
		if (modelDir==null) {
			List<Dir> candList = rrepo.getRoot().toAllList();
			candList = candList.stream().filter(d->d.children!=null && !d.children.isEmpty() && d.getPath().endsWith("app/models")).collect(Collectors.toList());
			if (!candList.isEmpty()) {
				modelDir = candList.get(0);
				if (candList.size()>1) {
					candList = candList.stream().filter(d->!d.getPath().toLowerCase().contains("test")).collect(Collectors.toList());
					if (!candList.isEmpty())
						modelDir = candList.get(0);
				}
			}
		}
		if (modelDir!=null) {
			URL urlSchema = gh.newURL("github.com", "/"+repo.getName()+ "/raw/"+repo.getBranchGit()+"/"+repo.getConfigPath(), "");
			if (loader.visitSchema(urlSchema)==null)
				return rrepo;	
			List<Dir> all = modelDir.toLeafList();
			for (Dir sourceDir:all) {
				String sha = sourceDir.getSha();
				if (sha==null)
					sha = repo.getBranchGit();
				
				URL furl = gh.newURL("github.com","/"+repo.getName()+ "/raw/"+sha+"/"+sourceDir.getPath(),null);						
				loader.visitFile(furl);
				if (GitHubCrawler.stop)
					throw new RuntimeException("Asked to stop");
			}
			
			loader.solveRefs();
			rrepo.getRepo().print();
		} else {
			LOG.info(" no suitable model path for "+repo.getName());
		}
		return rrepo;
	}

	private RubyRepo readModelEntry(RubyRepo rrepo,Repo repo,String path) throws MalformedURLException {
		URL url = new URL("https://api.github.com/repos/"+repo.getName()+"/contents/"+path
				+ "?access_token="+gh.getOAuth());
		JsonReader rdr = gh.callApi(url,false);
		if (rdr==null) { //File not Found
			return null;
		}
		if (rrepo==null) {
			rrepo = loader.setRepo(repo);	
			loader.visitSchema(new URL("https://github.com/"+repo.getName()+ "/raw/"+repo.getBranchGit()+"/"+repo.getConfigPath()));	
		}		
		JsonArray results = rdr.readArray();		
		for (JsonObject result : results.getValuesAs(JsonObject.class)) {			
			String type = result.getString("type");
			if (type.equals("file")) {
				String fpath = result.getString("download_url");
				if (fpath.endsWith(".rb")) {
					URL furl = new URL(fpath);						
					loader.visitFile(furl);						
				}
			} else if (type.equals("dir")) {
				String dpath=result.getString("path");
				readModelEntry(rrepo,repo,dpath);
			}
		}
		return rrepo;
	}
	
}