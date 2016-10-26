package gitget;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import dao.ConfigDAO;
import db.daos.MyConfigNop;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;

public class RubyCrawlerTest1 extends RubyCrawler {
	/**
	 * Use this option to reload a repo from github. All classes and tables of this repo must be removed before this, if they exist.
	 */
	public static boolean RETRIEVE_AGAIN=false;
	GitHubCaller gh = GitHubCaller.instance;
	@Before
	public void setUp() throws Exception {
		ConfigDAO.config(new MyConfigNop());
		//ConfigDAO.config(JPA_DAO.instance);
	}

	@Test
	public void test() {
		String repos[] = {		
				"mikeymckay/formtastic.us"
				//"alexs/salva-old"
				//"scharfie/gabby"
				//"james/freefall"
				//"wesabe/pfc"
				//"sylvani/sylrplm"
				//"odambrine/pfolio"
				//"fatfreecrm/fat_free_crm"
				//"tslocke/hobo"
				//"trevorturk/eldorado",
				//---
			//"activescaffold/active_scaffold",
		//	"djcp/cohort",
		//	"holtonma/markholton-old",
		//	"nikhilgupte/shk-fas",				
				//--
				//"adrianodennanni/PokeLibrary",
				  // has_one primary_key that makes no sense 
		/*		"andromedai/andromedai",
				"alexhasapis/after_school",
				"bkielbasa/redmine-msproject-importer",
				"bonjias/newapp",*/
			/*	"towski/mator",				
				"tih-ra/shoto",
				"danieloliveira/financeiro",
				"jdwyah/hippo-on-rails",
				"macbury/iSklep",
				"phddoom/garvindocs",
				"wendbandeira/kendell",
				"thiagoaos/myllet",
				"lukapiske/ren",*/

				/*
				"vegantech/sims",
				"fernandomachado/crm",
				"ferblape/query_memcached",
				"fabioespindula/crm",
				"gustin/lovd-by-less",
				"francois/acctsoft",*/
				
		};
		if (RETRIEVE_AGAIN) {
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			for (String name:repos) {
				dao.beginTransaction();
				List<Repo> res = dao.findByName(name);
				if (res.isEmpty())
					continue;
				Repo repo = res.iterator().next();				
				dao.commitAndCloseTransaction();
				if (repo.getClasses().isEmpty() && repo.getConfigPath()!=null ) {
					repo.overrideErrorLevel(null);
					processRepo(repo);
				}
			}			
		} else {
			for (String repo:repos) 
				processRepo(createRepo(gh.getRepoInfo(repo) ,repo));
		}
	}

}
