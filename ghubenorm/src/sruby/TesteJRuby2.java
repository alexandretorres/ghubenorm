package sruby;


import dao.ConfigDAO;
import dao.DAOInterface;
import db.daos.RepoDAO;

import db.jpa.JPA_DAO;
import gitget.Auth;
import gitget.Log;
import model.Language;
import model.Repo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import org.jruby.ast.Node;
import org.jruby.lexer.yacc.SyntaxException;

public class TesteJRuby2 { 

	static int fileCnt=0;
	static RubyRepoLoader loader = new RubyRepoLoader();
	
	
	public static void main(String[] args)  {
		Node n=null;
		try {
			ConfigDAO.config(JPA_DAO.instance);
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			
			dao.beginTransaction();
			//testeDB();	
			RubyRepo repo =	loader.setRepo(new Repo(Language.RUBY));
			dao.persit(repo.getRepo());
	        //first warm up the parser
			FileInputStream in = new FileInputStream("warmup.rb");
			n= loader.parse(in);			
			// 			
			//ps-deathstar-master
			//in = new FileInputStream("repos/chroma32-master/db/schema.rb");
			in = new FileInputStream("repos/ps-deathstar-master/db/schema.rb");
			//in = new FileInputStream("repos/promoweb-master/db/schema.rb");
			//in = new FileInputStream("repos/gitlabhq-master/db/schema.rb");
			
			fileCnt++;
			n = loader.parse(in);
	     
	        //
	        //File baseFile = new File("repos/chroma32-master/app/models/");//
	        File baseFile = new File("repos/ps-deathstar-master/app/models/");//
	        //File baseFile = new File("repos/promoweb-master/app/models/");//
	        //File baseFile = new File("repos/gitlabhq-master/app/models/");//
	       
	   
	        for (File f:baseFile.listFiles()) {
	        	if (f.isFile()) {
	        		try {
	        			read(f);
	        		} catch (SyntaxException ex) {
	        			Log.LOG.warning("Syntax exception on file "+f.getName()+" position "+ex.getPosition());
	        	
	        			ex.printStackTrace();
	        		}
	        		fileCnt++;
	        	}
	        }		     
			loader.solveRefs();
			System.out.println("files:"+fileCnt);
			repo.print();
			
			
			dao.commitAndCloseTransaction();		
			
		} catch (Exception e) {
			e.printStackTrace();
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
		loader.visitFile(new FileInputStream(f));		
	}
	private static void testeDB() {	
		ConfigDAO.config(JPA_DAO.instance);
		RepoDAO dao = ConfigDAO.getDAO(Repo.class);
		//DAOInterface<Repo> dao = ConfigDAO.getDAO(Repo.class);
		try  {
			dao.beginTransaction();
			Repo repo = new Repo(Language.RUBY);
            repo.setName("abc123");
            repo.setUrl("http");
            dao.persit(repo);
            dao.find(repo.getId());
            System.out.println(dao.findByURL("http"));
			dao.commitAndCloseTransaction();
		} catch (Exception e) {
			e.printStackTrace();
			dao.rollbackAndCloseTransaction();	    
	   
	    }
		
	}
	

}
