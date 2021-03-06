package sruby;


import static gitget.Log.LOG;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;

import org.jruby.ast.Node;
import org.jruby.lexer.yacc.SyntaxException;
import org.junit.Test;

import dao.ConfigDAO;
import dao.DAOInterface;
import db.daos.MyConfigNop;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import gitget.RepoToJSON;
import model.Language;
import model.Repo;

public class TesteJRuby2 { 
	static int REPO_NUM=4;
	static int fileCnt=0;
	static RubyRepoLoader loader = RubyRepoLoader.getInstance();
	String[][] repos = {			
			{"repos/chroma32-master/db/schema.rb","repos/chroma32-master/app/models/"},			//0
			{"repos/ps-deathstar-master/db/schema.rb","repos/ps-deathstar-master/app/models/"},	//1
			{"repos/promoweb-master/db/schema.rb","repos/promoweb-master/app/models/"},			//2
			{"repos/gitlabhq-master/db/schema.rb","repos/gitlabhq-master/app/models/"},			//3
			{"repos/activerecord_test/schema/schema.rb","repos/activerecord_test/models/"},		//4
			{"repos/boothmgr-master/db/schema.rb","repos/boothmgr-master/app/models/"}				//5
		};
	String[] names = {
			"repos/chroma32-master",
			"repos/ps-deathstar-master",
			"repos/promoweb-master",
			"repos/gitlabhq-master",
			"repos/activerecord_test",
			"repos/boothmgr-master"
			};
	@Test
	public void test()  {
		Node n=null;
		try {
		//	ConfigDAO.config(JPA_DAO.instance);
			ConfigDAO.config(new MyConfigNop());
			DAOInterface<Repo> dao = ConfigDAO.getDAO(Repo.class);
			
			dao.beginTransaction();
			//testeDB();	
			RubyRepo repo =	loader.setRepo(new Repo(Language.RUBY));
			repo.getRepo().setPublicId(REPO_NUM);
			repo.getRepo().setName(names[REPO_NUM]);
			dao.persist(repo.getRepo());
	        //first warm up the parser
			FileInputStream in = new FileInputStream("warmup.rb");
			n= loader.parse(in);			
			
			in = new FileInputStream(repos[REPO_NUM][0]);
			
			
			fileCnt++;
			n = loader.visitSchema(in);
	          
	        File baseFile = new File(repos[REPO_NUM][1]);//
	       
	        for (File f:baseFile.listFiles()) {
	        	if (f.isFile()) {
	        		try {
	        			read(f);
	        		} catch (SyntaxException ex) {
	        			LOG.warning("Syntax exception on file "+f.getName()+" position "+ex.getPosition());
	        	
	        			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
	        		}
	        		fileCnt++;
	        	}
	        }		     
			loader.solveRefs();
			System.out.println("files:"+fileCnt);
			repo.getRepo().print();
			RepoToJSON.toJson(repo.getRepo(),"code.txt");
			
			dao.commitAndCloseTransaction();		
			
		} catch (Exception e) {
			LOG.log(Level.SEVERE,e.getMessage(),e);	
			Throwable t = e;
			Throwable cause = e.getCause();
			while(cause!=null && cause!=t) {
				cause.printStackTrace();
				cause=e.getCause();
			}
			// TODO Auto-generated catch block			
		}		
		ConfigDAO.finish();
	}
	public static void read(File f) throws Exception {	
		loader.visitFile(f.getPath(),new FileInputStream(f));		
	}
	private static void testeDB() {	
		ConfigDAO.config(JPA_DAO.instance);
		RepoDAO dao = ConfigDAO.getDAO(Repo.class);
		//DAOInterface<Repo> dao = ConfigDAO.getDAO(Repo.class);
		try  {
			dao.beginTransaction();
			Repo repo = new Repo(Language.RUBY);
            repo.setName("abc123");
         //   repo.setUrl("http");
            dao.persist(repo);
            dao.find(repo.getId());
            System.out.println(dao.findByURL("http"));
			dao.commitAndCloseTransaction();
		} catch (Exception e) {
			LOG.log(Level.SEVERE,e.getMessage(),e);	
			
			dao.rollbackAndCloseTransaction();	    
	   
	    }
		
	}
	

}
