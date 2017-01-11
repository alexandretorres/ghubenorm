package sjava;

import static gitget.Log.LOG;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.junit.Test;

import dao.ConfigDAO;
import dao.DAOInterface;
import dao.nop.ConfigNop;
import db.daos.MyConfigNop;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import gitget.RepoToJSON;
import model.Language;
import model.Repo;


public class JavaLoaderTest {
	static int REPO_ID=0;//3182599;
	
	static {
		//ConfigDAO.config(JPA_DAO.instance);	
		ConfigDAO.config(new MyConfigNop());	
	}
	private JavaLoader loader = new JavaLoader();
	JavaRepo jrepo=new JavaRepo(new Repo(Language.JAVA));
	private void loadDir(File baseFile) throws IOException {
		if (baseFile.isFile())
			read(baseFile);
		else
			for (File f:baseFile.listFiles()) {
	        	if (f.isFile()) {	        		
	    			read(f);        		
	        	} else {
	        		loadDir(f);
	        	}
			}	
	}
	private void read(File baseFile) throws IOException {
		if (!baseFile.getName().endsWith(".java"))
			return;
		Prof.open("loader.load");
		JCompilationUnit comp = loader.load(baseFile.toURI().toURL());
		Prof.close("loader.load");
	}
	@Test
	public void testLoad() {
		try {
			
			RepoDAO daoRepo = ConfigDAO.getDAO(Repo.class);	
			jrepo.getRepo().setName("TEST");
			if (REPO_ID>0) {
				try {
				daoRepo.beginTransaction();				
				Repo repo = daoRepo.find(REPO_ID);
				daoRepo.commitAndCloseTransaction();
				jrepo.setRepo(repo);
				} catch (Exception ex) {
					System.err.println("could not load the repo number "+REPO_ID+", continuing loading...");
					ex.printStackTrace();
				}
			
			}
			daoRepo.beginTransaction();
			jrepo.setRepo(daoRepo.reattachOrSave(jrepo.getRepo()));
			//daoRepo.persist(jrepo.getRepo());
			//--
			loader.setJrepo(jrepo);
			//File baseFile = new File("trash/SmallTest.java");
			//File baseFile = new File("repos/MSD_File_Sharing-e1d5650d8cf477355ebe69b52f507c85c12b2ba6/WHAM project war/WHAM/src");
			//File baseFile = new File("repos/cosmo-master");
			File baseFile = new File(
					"C:\\eclipse\\eclipse_mars2_64\\workspace\\eclipselink.jpa.test\\src"
					//"C:\\java\\eclipselink\\eclipselink_test_src"
					//"C:\\dev\\eclipselink_test_src"
					//"C:\\repos\\PDFFilter-master" 
					//"C:\\Users\\torres\\Downloads\\javarepos\\PDFFilter-master\\PDFFilter-master"
					//"src/"
					);
			Prof.open("loadDir");
			loadDir(baseFile);
			Prof.close("loadDir");
			//--
			
			Prof.open("solverefs");
		    jrepo.solveRefs();
		    Prof.close("solverefs");
		    //System.out.println("compilation unit:\n"+listner.comp);
		    daoRepo.commitAndCloseTransaction();
		    jrepo.getRepo().print();
		    RepoToJSON.toJson(jrepo.getRepo(),"code.txt");
			
		    Prof.print();
		    
		    
			/*ParseTree tree = parser.compilationUnit(); 
			System.out.println(tree.toStringTree(parser));*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			ConfigDAO.finish();
		}
	}
	

}
