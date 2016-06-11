package gitget;

import static gitget.Log.LOG;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.json.JsonObject;

import dao.ConfigDAO;
import dao.DAOInterface;
import dao.nop.ConfigNop;

import model.Repo;
import sjava.CompilationUnit;
import sjava.JavaLoader;

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
			JsonObject result = gh.listFileTree(fullName);
			JavaDir root = JavaDir.newRoot();
			for (JsonObject res: result.getJsonArray("tree").getValuesAs(JsonObject.class)) {
				String path  =res.getString("path");
				if (path.endsWith("persistence.xml"))  {
					LOG.info("path "+path+" has a persistence.xml file");
				}
				if (path.endsWith(".java")) {
					root.register(path);
				}
			}
			root.removeTestFolders();
			JavaDir cand = root.getSourceRootCandidate();
			List<JavaDir> basePaths = new ArrayList<JavaDir>();
			if (cand.children!=null) {
				for (JavaDir ch:cand.children) {
					JavaDir leaf = ch.getFirstLeaf();
					String leafPath = leaf.getPath();
					URL url = new URL("https://github.com/"+fullName+ "/raw/master"+leafPath);
					CompilationUnit unit= loader.load(url);	
					if (unit==null)
						continue;
					
					String packPath = unit.getPackageName().replace('.', '/');
					if (packPath.equals("")) {
						basePaths.add(leaf.parent);
					} else {
						String basePath = leafPath.substring(0,leafPath.indexOf(packPath));
						JavaDir dir = root.find(basePath);
						basePaths.add(dir);
					}
				}
			}
			root.print();
			//para cada path principal, pega um java, deduz o path real a partir do package
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,"Repository "+fullName+":"+ex.getMessage(),ex);	
		}
	}
}
class JavaDir {
	//public static final JavaDir root = new JavaDir("");
	String name;
	JavaDir parent;
	List<JavaDir> children;
	public static JavaDir newRoot() {
		return new JavaDir("");
	}
	private JavaDir(String name) {
		this.name = name;
	}
	JavaDir(String name,JavaDir parent) {
		this.name = name;
		this.parent = parent;
		parent.addChild(this);
	}
	void addChild(JavaDir dir) {
		if (children==null)
			children=new ArrayList<JavaDir>();
		children.add(dir);
	}
	JavaDir get(String pname) {
		if (children==null)
			return null;
		return children.stream().filter(c->c.name.equals(pname)).findFirst().orElse(null);
	}
	/**
	 * 
	 * @param path path relative to current branch
	 */
	void register(String path) {
		String way[] = path.split("/");
		JavaDir cur = this;
		for (String step:way) {
			JavaDir child = cur.get(step);
			if (child==null) {
				child = new JavaDir(step, cur);
			}
			cur=child;
		}
	}
	/**
	 * 
	 * @param path path relative to current branch
	 */
	JavaDir find(String path) {		
		String way[] = path.split("/");
		JavaDir cur = this;
		for (String step:way) {
			if (step.equals(""))
				continue;
			JavaDir child = cur.get(step);
			if (child==null) {
				return null;
			}
			cur=child;
		}
		return cur;
	}
	void removeTestFolders() {
		if (this.children==null)
			return;
		for (Iterator<JavaDir> it=this.children.iterator();it.hasNext();) {
			JavaDir d = it.next();
			if (d.name.equalsIgnoreCase("test") || d.name.equalsIgnoreCase("tests")) {
				it.remove();
			}
		}
	}
	/**
	 * Return this if it has more than one children or find first descendant with more than one children
	 * @return 
	 */
	JavaDir getSourceRootCandidate() {
		JavaDir cur = this;		
	//	String path="";
		while(cur.children.size()==1) {
			JavaDir child = cur.children.get(0);
		//	path+=cur.name;
			if (child.children==null)
				return cur;
			else
				cur=child;
		}
		return cur;
	}
	void print() {
		JavaDir cur = this;
		String path="";
		while(cur.children.size()==1) {
			cur = cur.children.get(0);
			path+=cur.name;
			if (cur.children==null)
				return;
		}
		for (JavaDir c:cur.children) {
			System.out.println(path+"/"+c.name);
		}
		System.out.println("-------------------------------------");
		for (JavaDir c:cur.children) {
			c.printChildren(path);
		}
	}
	void printChildren(String path) {
		path +="/"+name;
		System.out.println(path);
		if (children!=null)
			for (JavaDir c:children) {
				c.printChildren(path);
			}
	}
	JavaDir getFirstLeaf() {	
		JavaDir ret = this;
		while (ret.children!=null && !ret.children.isEmpty()) {
			ret=ret.children.get(0);
		}
		return ret;
		
	}
	String getPath() {
		if (parent!=null) 
			return parent.getPath()+"/"+name;
		else
			return name;	
	}
	@Override
	public String toString() {
		try {
			return "JavaDir [" + getPath() + "]";
		} catch (Exception ex) {
			return "JavaDir "+name+" Exception:"+ex.getMessage();
		}
	}
	
}
