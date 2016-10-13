package gitget;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import dao.ConfigDAO;
import dao.nop.ConfigNop;
import db.jpa.JPA_DAO;

public class RubyCrawlerTest1 extends RubyCrawler {
	GitHubCaller gh = GitHubCaller.instance;
	@Before
	public void setUp() throws Exception {
		ConfigDAO.config(new ConfigNop());
		//ConfigDAO.config(JPA_DAO.instance);
	}

	@Test
	public void test() {
		String repos[] = {
				"tslocke/hobo"
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
		for (String repo:repos) 
			processRepo(gh.getRepoInfo(repo) ,repo);
		
	}

}
