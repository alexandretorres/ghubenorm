package sjava;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;


import common.LateVisitor;
import gitget.Dir;
import model.MClass;
import model.MTable;
import model.Repo;

public class JavaRepo {
	private Repo repo;
	private Dir root;
	private List<Dir> basePaths;
	private Dir badFiles;
	Stack<LateVisitor> visitors = new Stack<LateVisitor>() ;
	Map<String,List<JCompilationUnit>> pendingRefs = new HashMap<String,List<JCompilationUnit>>();
	//----
	private HashMap<String, JCompilationUnit> parsed = new HashMap<String, JCompilationUnit>();
	
	public JavaRepo(Repo repo) {
		this.repo = repo;
	}
	public Repo getRepo() {
		return repo;
	}
	public void setRepo(Repo repo) {
		this.repo = repo;
	}
	public Dir getRoot() {
		return root;
	}
	public void setRoot(Dir root) {
		this.root = root;
	}
	public List<Dir> getBasePaths() {
		return basePaths;
	}
	public void setBasePaths(List<Dir> basePaths) {
		this.basePaths = basePaths;
	}
	public Set<MClass> getClasses() {
		return repo.getClasses();
	}
	public Set<MTable> getTables() {
		return repo.getTables();
	}
	public Dir getBadFiles() {
		if (badFiles==null)
			badFiles = Dir.newRoot();
		return badFiles;
	}
	public void setBadFiles(Dir badFiles) {
		this.badFiles = badFiles;
	}
	public HashMap<String, JCompilationUnit> getParsed() {
		return parsed;
	}
	public void setParsed(HashMap<String, JCompilationUnit> parsed) {
		this.parsed = parsed;
	}
	
	public void solveRefs() {
		for (Entry<String, List<JCompilationUnit>> entry:pendingRefs.entrySet()) {
			// do something
		}
		for (LateVisitor v:visitors) {
			v.exec();
		}
	}
	public void addLateSubclass(String supername,MClass clazz,JCompilationUnit comp) {
		clazz.setSuperClassName(supername);
		List<JCompilationUnit> list = pendingRefs.get(supername);
		if (list==null) {
			list = new ArrayList<>();
			pendingRefs.put(supername, list);
		}
		list.add(comp);
	}
	public MTable findTable(String name) {
		MTable ret = getTables().stream().filter(t->t.getName().equals(name)).findFirst().orElse(null);
		return ret;
	}
	
}
