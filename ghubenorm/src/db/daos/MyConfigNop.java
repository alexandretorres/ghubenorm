package db.daos;

import java.util.List;

import dao.DAOInterface;
import dao.nop.ConfigNop;
import dao.nop.DAONop;
import model.Repo;

public class MyConfigNop extends ConfigNop {
	private RepoDAO repoDAO;
	@SuppressWarnings("unchecked")
	@Override
	public <X, T extends DAOInterface<X>> T findDAOInstance(Class<X> cl) {
		if (cl==Repo.class) {			
			return (T) getRepoDAO();
		}
		return super.findDAOInstance(cl);
	}
	private RepoDAO getRepoDAO() {
		if (repoDAO==null)
			repoDAO = new RepoDAONop();
		return repoDAO;
	}
class RepoDAONop extends DAONop<Repo> implements RepoDAO {
	@Override
	public List<Repo> findByURL(String url) {
		throw new RuntimeException("not implemented in NOP");
	}

	@Override
	public List<Repo> findByName(String name) {
		throw new RuntimeException("not implemented in NOP");	}

	@Override
	public List<Repo> findAll() {
		throw new RuntimeException("not implemented in NOP");
	}

	@Override
	public List<Repo> findPage(int start, int max) {
		throw new RuntimeException("not implemented in NOP");
	}

	@Override
	public int findMaxPublicId() {		
		return 0;
	}

	@Override
	public Repo merge(Repo repo) {
		throw new RuntimeException("not implemented in NOP");
	}

	@Override
	public Repo reattachOrSave(Repo repo) {		
		return repo;
	}
	public List<Repo> findByPublicId(int pid) {
		throw new RuntimeException("not implemented in NOP");
	}

	@Override
	public void cleanRepo(int publicId) {
		throw new RuntimeException("not implemented in NOP");
		
	}
}
}

