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
	}

	@Test
	public void test() {
		String repo = 				
				"towski/mator";
				//"vegantech/sims";
				//"fernandomachado/crm";
				//"ferblape/query_memcached";
				//"fabioespindula/crm";
				//"gustin/lovd-by-less";
				//"francois/acctsoft";
		processRepo(gh.getRepoInfo(repo) ,repo);
		
	}

}
