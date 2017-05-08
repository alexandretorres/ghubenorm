package gitget;

import static gitget.Log.LOG;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Test;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;

public class PickRandom {
	final int MAX_ID=13363980;//21;//1278;
	final static int SIZE_NORM=400;
	final static int SIZE_ORM=100;
	static RepoDAO dao;
	static Random rnd = new Random();
	Set<Repo> java = new HashSet<>();
	Set<Repo> ruby = new HashSet<>();
	
	@Test	
	public void pickTests() {
		try {
			ConfigDAO.config(JPA_DAO.instance);
			dao = ConfigDAO.getDAO(Repo.class);
			//3
			
			pickRandomNoORM();
			
			
			System.out.println("*******RUBY***************");
			for (Repo repo:ruby) {
				System.out.println(/*"repo id:"+repo.getId()+" , pid:"+repo.getPublicId()+", "+*/repo.getName());
			}
			System.out.println("*******JAVA***************");
			for (Repo repo:java) {
				System.out.println(/*"repo id:"+repo.getId()+" , pid:"+repo.getPublicId()+", "+*/repo.getName());
				//git clone https://github.com/mikhail-pn/Map_osm.git
				//repo id:\d* , pid:\d*, 
			}
			//repo.print();
			//RepoToJSON.toJson(repo,"code.txt");
			
			ConfigDAO.finish();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}
	public void pickRandomNoORM() {		
		do {
			try {
				int id = rnd.nextInt(MAX_ID+1);
				dao.beginTransaction();
				Repo repo = dao.find(id);
				Boolean has =  repo.getHasClasses();
				if (has==null)
					has=Boolean.FALSE;				
				
				if (!has && repo.getConfigPath()==null) {						
					if (repo.getLanguage()==Language.JAVA && java.size()<SIZE_NORM && !java.contains(repo) && !repeated(repo.getName())) {
						java.add(repo);
						
					} else if (repo.getLanguage()==Language.RUBY && ruby.size()<SIZE_NORM && !ruby.contains(repo)  && !repeated(repo.getName()) ) {
						ruby.add(repo);
						
					}						
				}
				dao.rollbackAndCloseTransaction();
			} catch (Exception ex) {
				System.err.println("     invalid id, trying another...");
			}
		} while (java.size()<SIZE_NORM || ruby.size()<SIZE_NORM);
	
	
	}
	
	public void pickRandomORM() {		
		do {
			try {
				int id = rnd.nextInt(MAX_ID+1);
				dao.beginTransaction();
				Repo repo = dao.find(id);
				Boolean has =  repo.getHasClasses();
				if (has==null)
					has=Boolean.FALSE;				
				
				if (has) {						
					if (repo.getLanguage()==Language.JAVA && java.size()<SIZE_ORM && !java.contains(repo) && !repeated(repo.getName())) {
						java.add(repo);
						
					} else if (repo.getLanguage()==Language.RUBY && ruby.size()<SIZE_ORM && !ruby.contains(repo)  && !repeated(repo.getName()) ) {
						ruby.add(repo);
						
					}						
				}
				dao.rollbackAndCloseTransaction();
			} catch (Exception ex) {
				System.err.println("     invalid id, trying another...");
			}
		} while (java.size()<SIZE_ORM || ruby.size()<SIZE_ORM);
	
	
	}
	
	public boolean repeated(String name) {
		String[] repos = IssueGitCheckouts.repos;
		for (String r:repos) {
			if (r.equals(name)) 
				return true;
		}
		return false;
	}
}
