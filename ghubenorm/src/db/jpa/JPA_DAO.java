package db.jpa;

import java.util.Date;
import java.util.List;

import javax.persistence.Query;
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
	final static String FindByPublicId = "Repo.FindByPublicId";
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
	public List<Repo> findByPublicId(int pid) {
		return getEm().createNamedQuery(FindByPublicId,Repo.class).setParameter("pid", pid).getResultList();
	}
	public void cleanRepo(int publicId) {
		//getEm().createNamedStoredProcedureQuery("CleanRepo").setParameter(1, publicId).execute();
		getEm().createNativeQuery("select count(*) from \"CleanRepo\"("+publicId+")").getResultList();	
	}
	void setDate(int publicId,Date dt,int lastPid) {		
		Query q = getEm().createNativeQuery("update Repo set dt_change=? where publicId=?");	
		q.setParameter(1, dt);
		q.setParameter(2, publicId);
		q.executeUpdate();
		//
		if (lastPid>=0 && publicId>lastPid) {
			q = getEm().createNativeQuery("update Repo set dt_change=? "
					+ "where publicId<? and publicId>? and dt_change is null");	
			q.setParameter(1, dt);
			q.setParameter(2, publicId);
			q.setParameter(3, lastPid);
			q.executeUpdate();
		}
	}
	void setDate(String name,Date dt) {		
		Query q = getEm().createNativeQuery("select publicid from Repo where name=?");		
		q.setParameter(1, name);
		Integer pid = (Integer) q.getSingleResult();
		setDate(pid,dt,-1);
	}
	
}
