package sjava;

import java.util.List;
import java.util.Set;

import gitget.Dir;
import model.MClass;
import model.MTable;
import model.Repo;

public class JavaRepo {
	private Repo repo;
	private Dir root;
	private List<Dir> basePaths;
	private Dir badFiles;
	
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
	
	
}
