package gitget;

import static gitget.Log.LOG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import dao.ConfigDAO;
import dao.DAOInterface;
import dao.nop.ConfigNop;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;
import sjava.JCompilationUnit;
import sjava.JavaLoader;
import sjava.JavaRepo;
import sjava.Prof;

public class JavaCrawler {
	public static String[] ORM_ARTIFACTS = {
			"<artifactId>hibernate-entitymanager</artifactId>",
			"<artifactId>spring-orm</artifactId>","<artifactId>hibernate-core</artifactId>",
			"<artifactId>hibernate</artifactId>","<artifactId>javax.persistence</artifactId>",
			"<artifactId>openjpa</artifactId>",
			"<artifactId>eclipselink</artifactId>",
			"<artifactId>org.eclipse.persistence.jpa</artifactId>",
			"<artifactId>datanucleus-jpa</artifactId>"};
	public static String[] JARS = {
			"hibernate-core","spring-orm","hibernate-entitymanager","org.eclipse.persistence.jpa",
			"eclipselink","openjpa","datanucleus-jpa","javax.persistence"
	};
	public static void main(String[] args) {
		ConfigDAO.config(JPA_DAO.instance);	
		
		//String repo ="facebook/react-native";
		//String repo ="kmahaley/MSD_File_Sharing";
		//String repo ="travis/cosmo";
		//String repo = "apache/felix";
		//String repo ="apache/camel";
		String repo = "mheath/adbcj"; //503 error?
		//ConfigDAO.config(new ConfigNop());
		//https://github.com/rocioemera/SubscriptionSystem
	
		new JavaCrawler().processRepo(gh.getRepoInfo(repo) ,repo);		
		ConfigDAO.finish();
		Prof.print();
		//new JavaCrawler().processRepo(null,"BorisIvanov/com-iqbuzz-tickets");
		
	}
	private static GitHubCaller gh = GitHubCaller.instance;
	private DAOInterface<Repo> daoRepo;
	private JavaLoader loader = new JavaLoader();
	/**
	 * Persistence.xml may not exist! Spring uses application.yaml or who knows!
	 * @param repoJson
	 * @param fullName
	 */
	public void processRepo(JsonObject repoJson,String fullName)  {
		try {			
			Prof.open("checkIfPersistent");
			String persistenceXML = null;			
			if (daoRepo==null)
				daoRepo = ConfigDAO.getDAO(Repo.class);	
			Repo repo = new Repo(Language.JAVA);			
			repo.setBranch(repoJson.getString("default_branch"));
			repo.setName(fullName);			
			repo.setUrl(repoJson.getString("html_url"));
			repo.setPublicId(repoJson.getInt("id"));
			JavaRepo jrepo = new JavaRepo(repo);			
			Dir root = Dir.newRoot();
			jrepo.setRoot(root);
			loader.setJrepo(jrepo);
			//--
			Prof.open("listFileTree");
			JsonObject result = gh.listFileTree(fullName,repo.getBranchGit());
			Prof.close("listFileTree");
			boolean truncated = result.getBoolean("truncated");
			if (truncated)
				LOG.warning("truncated tree file for repository "+fullName);
			for (JsonObject res: result.getJsonArray("tree").getValuesAs(JsonObject.class)) {
				String path  =res.getString("path");
				if (path.endsWith("persistence.xml"))  {
					LOG.info("path "+path+" has a persistence.xml file");
					repo.setConfigPath(path);
					persistenceXML=path;
				} else if (path.endsWith("pom.xml") && jrepo.JPAArtifacts.isEmpty()) {
					Prof.open("findPOMArtifacts");
					findPOMArtifacts(jrepo,path);
					Prof.close("findPOMArtifacts");
					if (!jrepo.JPAArtifacts.isEmpty()) {
						LOG.info("path "+path+" has a pom.xml file with ORM artifact");
						repo.setConfigPath(path);
					}
				} else if (path.endsWith(".jar")) {
					for (String jname:JARS) {
						if (path.contains(jname)) {
							jrepo.JPAJars.add(jname);
							repo.setConfigPath(path);
							break;
						}
					}
				} else if (path.endsWith(".java")) {
					root.register(path);
				}
			}
			daoRepo.beginTransaction();
			daoRepo.persit(repo);
			Prof.close("checkIfPersistent");
			if (persistenceXML!=null || !jrepo.JPAArtifacts.isEmpty() || !jrepo.JPAJars.isEmpty()) {
				/*
				Prof.open("findBasePaths");
				findBasePaths(jrepo);
				Prof.close("findBasePaths");
				*/
				Prof.open("findJavaPersistenceRefs");
				findJavaPersistenceRefs(jrepo);
				Prof.close("findJavaPersistenceRefs");
				//readAllJavaFiles(jrepo); or...		
				
				jrepo.getRepo().print();
				
			}
			daoRepo.commitAndCloseTransaction();
			//para cada path principal, pega um java, deduz o path real a partir do package
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,"Repository "+fullName+":"+ex.getMessage(),ex);	
		}
	}
	protected void readAllJavaFiles(JavaRepo jrepo) throws MalformedURLException, URISyntaxException {
		for (Dir f:jrepo.getRoot().toLeafList()) {
			if (f.children==null || f.children.isEmpty()) {
				URL url = gh.newURL("github.com","/"+jrepo.getRepo().getName()+ "/raw/"+jrepo.getRepo().getBranchGit()+f.getPath(),null);			
				loader.load(url);
				LOG.info("loaded "+f.getPath());
			}
		}
	}
	protected void findJavaPersistenceRefs(JavaRepo jrepo) throws MalformedURLException, URISyntaxException {
		int p=1;
		int total=0;
		do {
			URL url = gh.newURL("api.github.com","/search/code", "page=" + p + "&per_page=100"
					+ "&q=javax.persistence+in:file+language:java+repo:" + jrepo.getRepo().getName() + "&access_token=" + gh.oauth);
			/*
			URL url = new URL("https://api.github.com/search/code?page=" + p + "&per_page=100"
					+ "&q=javax.persistence+in:file+language:java+repo:" + jrepo.getRepo().getName() + "&access_token=" + gh.oauth);
			*/
			//try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(url,true)) {
				JsonObject obj = rdr.readObject();
				if (total==0)
					total = obj.getInt("total_count");
				JsonArray results = obj.getJsonArray("items");
	
				for (JsonObject result : results.getValuesAs(JsonObject.class)) {
					String path = result.getString("path");
					URL furl = gh.newURL("github.com","/"+jrepo.getRepo().getName()+ "/raw/"+jrepo.getRepo().getBranchGit()+"/"+path,null);			
					loader.load(furl);
					//TODO: load on demand (?) BUT is it needed? all classes import javax.persistence
					/**
					 * Composite PKs must be Embedded OR have all fields listed on the entity class.
					 * ..and Must be public.
					 */
					//System.out.println(result);
					total--;
				}
			}
		} while (total>0);
		jrepo.solveRefs();
		
		
	}
	protected void findPOMArtifacts(JavaRepo jrepo,String path) {
		URLConnection connection = null;
		try { //https://raw.githubusercontent.com/kmahaley/MSD_File_Sharing/master/WHAM%20project%20war/WHAM/pom.xml
			/*String fil=Request.Get("https://raw.githubusercontent.com/kmahaley/MSD_File_Sharing/master/WHAM%20project%20war/WHAM/pom.xml")
	        .connectTimeout(1000)
	        .socketTimeout(1000)
	        .execute().returnContent().asString();
			*/
			//
			
			URL url = gh.newURL("github.com","/"+jrepo.getRepo().getName()+ "/raw/"+jrepo.getRepo().getBranchGit()+"/"+path,null);			
			connection = url.openConnection();
			try (BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()))) {	
				
				for (String st=in.readLine();st!=null;st=in.readLine()) {				
					for (String art:ORM_ARTIFACTS) {
						if (st.contains(art)) {
							jrepo.JPAArtifacts.push(art);
							break;
						}
					}
				}
			} 
			
		} catch (Exception e) {
			String msg = gh.getErrorStream(connection);
			if (msg!=null)
				LOG.warning("Error stream:" +msg);
			// TODO Auto-generated catch block
			e.printStackTrace();
	
		}
	}
	@Deprecated
	protected void findBasePaths(JavaRepo jrepo) throws MalformedURLException, URISyntaxException {
		Dir root = jrepo.getRoot();
		root.removeTestFolders();
		List<Dir> basePaths = new ArrayList<Dir>();
		Dir root2 = Dir.newRoot();
		Dir badFiles = jrepo.getBadFiles();
		for (Dir leaf=root.getFirstLeaf();leaf!=root;leaf = root.getFirstLeaf()) {
			String leafPath = leaf.getPath();
			String packPath = findPackage(jrepo, leafPath);
			if (packPath==null) {
				leaf = leaf.rebase(root, badFiles);
				continue; //remove leaf... ignore this path
			}
			Dir base = leaf.parent;
			
			if (!packPath.equals("")) {		
				int idx = leafPath.indexOf(packPath);
				if (idx<0) {
					leaf = leaf.rebase(root, badFiles);
					continue;
				}
				String basePath = leafPath.substring(0,idx);
				base = root.find(basePath);				
			} 
			
			if (base==root) {
				basePaths.add(base);
				root2.children.addAll(root.children);
				root.children.removeAll(root.children);
			} else {
				base=base.rebase(root, root2);		
				basePaths.add(base);
			}
		}
		jrepo.setBasePaths(basePaths);
		jrepo.setRoot(root2);
	}
	protected String findPackage(JavaRepo jrepo,String leafPath) throws MalformedURLException, URISyntaxException {
		URL url = gh.newURL("github.com","/"+jrepo.getRepo().getName()+ "/raw/"+jrepo.getRepo().getBranchGit()+leafPath,null);
				
		//URL url = new URL("https://github.com/"+jrepo.getRepo().getName()+ "/raw/master"+leafPath);
		JCompilationUnit unit= loader.load(url);	
		if (unit==null) {
			return null;
		}
		if (unit.getPackageName()==null)
			return "";
		
		return unit.getPackageName().replace('.', '/');
	}
	
}
/*
 * The Search API has a custom rate limit. For requests using Basic Authentication, OAuth, or client ID and secret, 
 * you can make up to 30 requests per minute. For unauthenticated requests, the rate limit allows you to make up to 
 * 10 requests per minute. (30/min=> 2 seconds)
 * */
