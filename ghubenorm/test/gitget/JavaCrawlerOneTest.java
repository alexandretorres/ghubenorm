package gitget;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import dao.ConfigDAO;
import dao.nop.ConfigNop;
import db.jpa.JPA_DAO;
import sjava.Prof;

public class JavaCrawlerOneTest {

	@Before
	public void setUp() throws Exception {
		ConfigDAO.config(new ConfigNop());
	}

	@Test
	public void test() {
		
		String repo=
				"EthanWint/PDFFilter";
				//"myabc/appfuse";
				//		"android/platform_packages_apps_phone";
		// "facebook/react-native";
		// "kmahaley/MSD_File_Sharing";
		// "travis/cosmo";
		//  "apache/felix";
		//  "apache/camel";
		try {
			new JavaCrawler().processRepo(GitHubCaller.instance.getRepoInfo(repo) ,repo);	
			Prof.print();
		} finally {
			ConfigDAO.finish();
		}
		
	}

}
