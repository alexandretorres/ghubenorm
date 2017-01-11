package sjava;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	List<LateVisitor> visitors = new ArrayList<LateVisitor>() ;
	
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
			String ename = entry.getKey();
			if (BaseType.isBaseType(entry.getKey()) || ename.contains("<")){
				entry.setValue(Collections.EMPTY_LIST);
				continue;
			}				
			for (MClass c:repo.getClasses()) {
				
				if (c.getName().equals(entry.getKey())) {
					List<JCompilationUnit> units = entry.getValue();
					for (Iterator<JCompilationUnit> it=units.iterator();it.hasNext();) {
						JCompilationUnit unit = it.next();
						if (unit.checkPendingRefs(c, false))
							it.remove();
					}
				}
			}
			//pass 2
			for (MClass c:repo.getClasses()) {
				if (c.getName().equals(entry.getKey())) {
					List<JCompilationUnit> units = entry.getValue();
					for (Iterator<JCompilationUnit> it=units.iterator();it.hasNext();) {
						JCompilationUnit unit = it.next();
						if (unit.checkPendingRefs(c, true))
							it.remove();
					}
				}
			}
			// do something
		}
		
		while(!visitors.isEmpty()) {
			List<LateVisitor> procList = visitors;
			procList.sort(LateVisitor.comparator);
			visitors= new Stack<LateVisitor>() ;
			for (LateVisitor v:procList) {
				v.exec();
			}
		}
	}
	public void addPendingRef(String name,MClass clazz,JCompilationUnit comp) {
		if (name.indexOf(".")>0)
			name = name.substring(name.lastIndexOf(".")+1);
		List<JCompilationUnit> list = pendingRefs.get(name);
		if (list==null) {
			list = new ArrayList<>();
			pendingRefs.put(name, list);
		}
		list.add(comp);
	}
	public void addLateSubclass(String supername,MClass clazz,JCompilationUnit comp) {
		clazz.setSuperClassName(supername);
		addPendingRef(supername, clazz, comp);
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
