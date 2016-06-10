package sruby;

import static gitget.Log.LOG;

import java.util.List;
import java.util.logging.Level;

import dao.ConfigDAO;
import dao.jpa.ConfigJPA;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Repo;

public class TestLoadRepo {
	public static void main(String[] args) {
		try {
			//3
			ConfigDAO.config(JPA_DAO.instance);
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			dao.beginTransaction();
			List<Repo> all = dao.findAll();
			for (Repo repo:all) {			
				//Repo repo = dao.find(3);
				LOG.log(Level.INFO,repo.getId()+" "+repo.getUrl());
				RubyRepo rrepo = new RubyRepo(repo);
				rrepo.print();
			}
			dao.commitAndCloseTransaction();
				
			ConfigDAO.finish();
			
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}
	public static void main_one(String[] args) {
		try {
			//3
			ConfigDAO.config(JPA_DAO.instance);
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			dao.beginTransaction();
			Repo repo = dao.find(3);
			RubyRepo rrepo = new RubyRepo(repo);
			rrepo.print();
			dao.commitAndCloseTransaction();
			ConfigDAO.finish();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}

}
