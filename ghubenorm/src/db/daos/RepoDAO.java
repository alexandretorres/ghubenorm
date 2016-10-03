package db.daos;

import java.util.List;

import dao.DAOInterface;
import model.Repo;

public interface RepoDAO extends DAOInterface<Repo> {	
	public List<Repo> findByURL(String url);
	public List<Repo> findByName(String name);
	public List<Repo> findAll();
	public int findMaxPublicId();
}


