package gitget;

import static gitget.Log.LOG;

import java.io.FileWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonReader;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module.Feature;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Repo;

public class RepoToJSON {
	final int LOAD_ID=45;//1278;
	@Test	
	public void testOne() {
		try {
			
			
			//3
			ConfigDAO.config(JPA_DAO.instance);
			RepoDAO dao = ConfigDAO.getDAO(Repo.class);
			dao.beginTransaction();
			Repo repo = dao.find(LOAD_ID);
			//RubyRepo rrepo = new RubyRepo(repo);
			toJson(repo,"code.txt");
			
			
			//mapper.writeValue(System.out, repo.getClasses().iterator().next());
			repo.print();
			dao.rollbackAndCloseTransaction();
			ConfigDAO.finish();
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}
	public static void toJson(Repo repo,String fileName) {
		try{ 
			//StringWriter sw = new StringWriter();
			FileWriter fw = new FileWriter(fileName);
			ObjectMapper mapper = new ObjectMapper();	
			// or, for Hibernate 5.x
			Hibernate5Module mod = new Hibernate5Module();
			mod.configure(Feature.FORCE_LAZY_LOADING, true);
			mod.configure(Feature.USE_TRANSIENT_ANNOTATION, false);			
			mapper.registerModule(mod);
			//
			mapper.writeValue(fw, repo);
			//fw.flush();
			fw.close();
			
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
			
		}
	}

}
