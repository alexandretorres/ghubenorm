package sjava;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import common.LateVisitor;
import gitget.Dir;
import gitget.Log;
import model.MClass;
import model.MDataSource;
import model.MTable;
import model.Repo;

public class JavaRepo {
	private Repo repo;
	private Dir root;
	private List<Dir> basePaths;
	private Dir badFiles;
	public Stack<String> JPAArtifacts = new Stack<String>();
	public Stack<String> JPAJars = new Stack<String>();
	public Set<MClass> mappedSuperClasses = new HashSet<MClass>();
	Stack<LateVisitor> visitors = new Stack<LateVisitor>() ;
	
	Map<String,List<JCompilationUnit>> pendingRefs = new HashMap<String,List<JCompilationUnit>>();
	Map<MClass,List<Annotation>> classAnnot = new HashMap<MClass,List<Annotation>>();
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
	@Deprecated
	public List<Dir> getBasePaths() {
		return basePaths;
	}
	@Deprecated
	public void setBasePaths(List<Dir> basePaths) {
		this.basePaths = basePaths;
	}
	public Set<MClass> getClasses() {
		return repo.getClasses();
	}
	public Set<MDataSource> getDataSources() {
		return repo.getDataSources();
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
		if (!pendingRefs.isEmpty())
			Log.LOG.warning("Pending refs");
		for (Entry<String, List<JCompilationUnit>> entry:pendingRefs.entrySet()) {
			
			// do something
		}
		
		while(!visitors.isEmpty()) {
			Stack<LateVisitor> procList = visitors; 
			visitors= new Stack<LateVisitor>() ;
			for (LateVisitor v:procList) {
				v.exec();
			}
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
	private Set<MTable> getTables() {
		return getDataSources().stream().filter(ds -> ds instanceof MTable).map(t -> (MTable) t).collect(Collectors.toSet());
	}
	public MTable findTable(String name) {
		MTable ret = getDataSources().stream().filter(ds -> ds instanceof MTable).map(t -> (MTable) t).
						filter(t->t.getName().equals(name)).findFirst().orElse(null);		
		//MTable ret = getTables().stream().filter(t->t.getName().equals(name)).findFirst().orElse(null);
		return ret;
	}
	
}
