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
		//ConfigDAO.config(JPA_DAO.instance);
	}

	@Test
	public void test() {	
		String repos[]= new String[] {
				//"johnragan/johnragan_org"
				
				"randi2/randi2",
				"pancutan/ConcesionariaDB",
			//
				"SnapImpact/ivolunteer",
				"khaya/acm","vusa/Placd","rangalo/Seam2Examples","felipero/examples","ifischer/getabike",
				"tjcampos/TPW","rafabene/XSeam2","johnragan/johnragan_org","myabc/opendls",
				"sirgwain/CraigStars-",	"martijnblankestijn/javaee6-demo",	"OpenAMEE/amee.platform.domain",
				"dshaish/Delegator_base","jraduget/kaleido-repository",	"cgreenhalgh/lobbyservice",
				"dtrott/mdtracker",	"KhurtinDN/AdmissionDepartment","jsmadja/fluxx",
				"pedrotoliveira/smartproject","guruzu/Hibernate-recipes-JPA","ihilt/echarts",
				"RaviH/MavenSpringHibernateMockito","kgbu/boothmgr","emacadie/James-Admin-Web-App",				
				"adelinojr/BookStoreClienteEJB","myabc/appfuse",/**/
				//----x----
				//	"EthanWint/PDFFilter";
				//		"android/platform_packages_apps_phone";
		// "facebook/react-native";
		// "kmahaley/MSD_File_Sharing";
		// "travis/cosmo";
		//  "apache/felix";
		//  "apache/camel";
		};
		try {
			for (String repo:repos) {
				new JavaCrawler().processRepo(GitHubCaller.instance.getRepoInfo(repo) ,repo);	
			}
			Prof.print();
		} finally {
			ConfigDAO.finish();
		}
		
	}

}
