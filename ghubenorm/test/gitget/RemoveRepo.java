package gitget;

import javax.activation.DataSource;

import org.junit.Before;
import org.junit.Test;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.MDataSource;
import model.Repo;
import static gitget.Log.LOG;
public class RemoveRepo {
	@Before
	public void setUp() throws Exception {
	//	ConfigDAO.config(new ConfigNop());
		ConfigDAO.config(JPA_DAO.instance);
	}
	@Test
	public void test() {
		String repos[] = {	
			//	"nikhilgupte/shk-fas",				
			//	"activescaffold/active_scaffold",
				"djcp/cohort",
			//	"holtonma/markholton-old",
		};
		for (String repo:repos) 
			doRemove(repo);
		
	}
	private void doRemove(String repoName) {
		boolean f=false;
		RepoDAO dao = ConfigDAO.getDAO(Repo.class);
		dao.beginTransaction();
		for (Repo r:dao.findByName(repoName)) {
			dao.removeCascade(r);
			LOG.info("repo removed:"+r.getId()+" name "+repoName);
			f=true;
		}
		if (!f)
			LOG.warning("could not find repo "+repoName);
		dao.commitAndCloseTransaction();
	}
}
