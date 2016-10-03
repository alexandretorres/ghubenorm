package db.jpa;

import java.util.List;

import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import dao.ConfigDAO;
import dao.DAOInterface;
import dao.jpa.ConfigJPA;
import dao.jpa.DAO;
import db.daos.RepoDAO;
import model.MColumn;
import model.MDataSource;
import model.MDefinition;
import model.MJoinedSource;
import model.MTable;
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
	@Override
	public void remove(MDataSource ds) {
		if (ds instanceof MJoinedSource) {
			MJoinedSource j = (MJoinedSource) ds;
			for (MTable t:j.getDefines()) {
				remove(t);
			}
		} else if (ds instanceof MTable) {
			MTable t = (MTable) ds;	
			DAO<MColumn> coldao = ConfigDAO.getDAO(MColumn.class);
			for (MColumn c:t.getColumns()) {
				coldao.remove(c);
			}
			DAO<MDefinition> defdao = ConfigDAO.getDAO(MDefinition.class);
			for (MDefinition def:t.getDefinitions()) {				
				defdao.remove(def);
			}
		}
		super.remove(ds);
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
	public void remove(Repo r) {
		DAO<MDataSource> dsdao = ConfigDAO.getDAO(MDataSource.class);
		for (MDataSource ds:r.getDataSources()) {
			dsdao.remove(ds);
		}
		super.remove(r);
	}
	
}