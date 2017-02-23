package db.daos;

import java.util.List;

import dao.DAOInterface;
import model.Repo;

public interface RepoDAO extends DAOInterface<Repo> {	
	public List<Repo> findByURL(String url);
	public List<Repo> findByName(String name);
	public List<Repo> findByPublicId(int pid);
	public List<Repo> findAll();
	public List<Repo> findPage(int start,int max);
	public int findMaxPublicId();
	public Repo merge(Repo repo);
	public Repo reattachOrSave(Repo repo);
	public void cleanRepo(int publicId);
	public int deleteFromToLast(int publicId);
}


