package db.jpa;

import java.util.List;

import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import dao.ConfigDAO;
import dao.DAOInterface;
import dao.jpa.ConfigJPA;
import dao.jpa.DAO;
import db.daos.RepoDAO;
import model.Repo;

public class JPA_DAO extends ConfigJPA {
	public static final JPA_DAO instance = new JPA_DAO();
	private JPA_DAO() {
		
	}	
	@Override
	public void setup() {
		new RepoDaoImpl();
	}
	
}

class RepoDaoImpl extends DAO<Repo> implements RepoDAO {
	final static String FindByURL = "Repo.FindByURL";
	public RepoDaoImpl() {
		super(Repo.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Repo> findByURL(String url) {
		return getEm().createNamedQuery(FindByURL,Repo.class).setParameter("url", url).getResultList();
	}
	public List<Repo> findAll() {
		return getEm().createQuery("SELECT r FROM Repo r order by id",Repo.class).getResultList();
	}
	
}