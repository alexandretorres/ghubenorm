package db.jpa;

import java.util.List;

import javax.persistence.TypedQuery;

import dao.DAOInterface;
import dao.jpa.ConfigJPA;
import dao.jpa.DAO;
import db.daos.RepoDAO;
import model.MDataSource;
import model.Repo;

public class JPA_DAO extends ConfigJPA {
	public static final JPA_DAO instance = new JPA_DAO();
	private JPA_DAO() {
		
	}	
	@Override
	public void setup() {
		new RepoDaoImpl();
		new MDataSourceDaoImpl();
	}
	
}
class MDataSourceDaoImpl extends DAO<MDataSource> implements DAOInterface<MDataSource> {
	public MDataSourceDaoImpl() {		
		super(MDataSource.class);		
	}
}
class RepoDaoImpl extends DAO<Repo> implements RepoDAO {
	final static String FindByURL = "Repo.FindByURL";
	final static String FindByName = "Repo.FindByName";
	public RepoDaoImpl() {
		super(Repo.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Repo> findByURL(String url) {
		return getEm().createNamedQuery(FindByURL,Repo.class).setParameter("url", url).getResultList();
	}
	@Override
	public List<Repo> findByName(String name) {
		return getEm().createNamedQuery(FindByName,Repo.class).setParameter("name", name).getResultList();
	}
	public List<Repo> findAll() {
		return getEm().createQuery("SELECT r FROM Repo r order by id",Repo.class).getResultList();
	}
	public int findMaxPublicId() {
		return (int) getEm().createQuery("SELECT max(r.publicId) FROM Repo r").getSingleResult();
	}

	@Override
	public Repo merge(Repo repo) {
		return getEm().merge(repo);
		
	}
	public Repo reattachOrSave(Repo repo) {		
		if (repo.getId()>0)
			return getEm().merge(repo);
		else
			super.persist(repo);
		return repo;
	}

	public List<Repo> findPage(int start,int max) {
		TypedQuery<Repo> q =  getEm().createQuery("SELECT r FROM Repo r order by publicId",Repo.class);
		q.setFirstResult(start).setMaxResults(max);
		return q.getResultList();
	}
}
