package gitget;

import static gitget.Log.LOG;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.json.JsonObject;

import dao.ConfigDAO;
import dao.DAOInterface;
import dao.nop.ConfigNop;
import model.Language;
import model.Repo;
import sjava.CompilationUnit;
import sjava.JavaLoader;
import sjava.JavaRepo;

public class JavaCrawler {
	public static void main(String[] args) {
		ConfigDAO.config(new ConfigNop());
		new JavaCrawler().processRepo(null,"facebook/react-native");
		
	}
	private static GitHubCaller gh = GitHubCaller.instance;
	private DAOInterface<Repo> daoRepo;
	private JavaLoader loader = new JavaLoader();
	public void processRepo(JsonObject repoJson,String fullName)  {
		try {
			if (daoRepo==null)
				daoRepo = ConfigDAO.getDAO(Repo.class);	
			Repo repo = new Repo(Language.JAVA);
			repo.setName(fullName);			
			JavaRepo jrepo = new JavaRepo(repo);			
			Dir root = Dir.newRoot();
			jrepo.setRoot(root);
			JsonObject result = gh.listFileTree(fullName);
			for (JsonObject res: result.getJsonArray("tree").getValuesAs(JsonObject.class)) {
				String path  =res.getString("path");
				if (path.endsWith("persistence.xml"))  {
					LOG.info("path "+path+" has a persistence.xml file");
					repo.setConfigPath(path);
				}
				if (path.endsWith(".java")) {
					root.register(path);
				}
			}
			findBasePaths(jrepo);			
			jrepo.getRoot().print();
			//para cada path principal, pega um java, deduz o path real a partir do package
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,"Repository "+fullName+":"+ex.getMessage(),ex);	
		}
	}
	protected void findBasePaths(JavaRepo jrepo) throws MalformedURLException {
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
	protected String findPackage(JavaRepo jrepo,String leafPath) throws MalformedURLException {
		URL url = new URL("https://github.com/"+jrepo.getRepo().getName()+ "/raw/master"+leafPath);
		CompilationUnit unit= loader.load(url);	
		if (unit==null) {
			return null;
		}
		return unit.getPackageName().replace('.', '/');
	}
	
}

