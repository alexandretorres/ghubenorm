package gitget;

import static gitget.Log.LOG;

import java.util.List;
import java.util.logging.Level;

import org.junit.Test;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Repo;

public class TestLoadAllRepos {
	@Test
	public void testAll() {
		try {
			//3
			ConfigDAO.config(JPA_DAO.instance);
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			dao.beginTransaction();
			List<Repo> all = dao.findAll();
			for (Repo repo:all) {				
				if (repo.getClasses().isEmpty())
					continue;
				//	LOG.log(Level.INFO,repo.getId()+" "+repo.getUrl());
				repo.print();
			}
			dao.rollbackAndCloseTransaction();
				
			ConfigDAO.finish();
			
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}
}
