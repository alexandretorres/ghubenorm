package gitget;

import static gitget.Log.LOG;

import java.util.List;
import java.util.logging.Level;

import org.junit.Test;

import dao.ConfigDAO;
import dao.jpa.ConfigJPA;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Repo;

public class TestLoadRepo {
	final int LOAD_ID=1;//1278;
	@Test	
	public void testOne() {
		try {
			//3
			ConfigDAO.config(JPA_DAO.instance);
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			dao.beginTransaction();
			Repo repo = dao.find(LOAD_ID);
			//RubyRepo rrepo = new RubyRepo(repo);
			repo.print();
			dao.rollbackAndCloseTransaction();
			ConfigDAO.finish();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}

}
