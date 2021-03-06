package gitget;

import org.junit.Before;
import org.junit.Test;

import dao.ConfigDAO;
import dao.nop.ConfigNop;
import db.daos.MyConfigNop;
import db.jpa.JPA_DAO;
import model.Repo;
import model.SkipReason;
import sjava.Prof;

public class JavaCrawlerOneTest {

	@Before
	public void setUp() throws Exception {
		ConfigDAO.config(new MyConfigNop()); 
		//ConfigDAO.config(JPA_DAO.instance);
	}

	@Test
	public void test() {	
		String repos[]= new String[] {
						"EricDuminil/picolena"
			//	"alcirBarros/Projetos-Base"
			//	"kazuar/ExamDesign"
			//	"eclipse/eclipselink.runtime"
				//"jaxio/generated-projects"
				//	"hibernate/hibernate-ogm"
				//	"liferay/liferay-portal"
		//	"identityxx/velo1"
				//	"daveangulo/ivolunteer_ori"
				//"apache/openjpa" //TOO MANY FILES
				//"magnusart/ConsumerMaster"
				//"johnragan/johnragan_org"
			/*	
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
				JavaCrawler jc = new JavaCrawler(); 
				Repo mrepo=jc.createRepo(GitHubCaller.instance.getRepoInfo(repo),repo);
				SkipReason result = jc.processRepo(mrepo);	
				System.err.println(repo+" RESULT:"+result+" or "+mrepo.getSkipReason());
			}
			Prof.print();
		} finally {
			ConfigDAO.finish();
		}
		
	}

}
