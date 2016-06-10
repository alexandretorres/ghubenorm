package gitget;

import java.net.MalformedURLException;

import dao.ConfigDAO;
import dao.nop.ConfigNop;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;

public class MainLoadOneRepo {
	public static void main(String[] args) {	
		ConfigDAO.config(new ConfigNop());
		RubyCrawler rc = new RubyCrawler();
		Repo repo = new Repo(Language.RUBY);
		//repo.setName("diaspora/diaspora");
		//repo.setName("drhenner/ror_ecommerce");
		//repo.setName("tenex/rails-assets");
	//	repo.setName("churchio/onebody");	
		repo.setName("mephistorb/mephisto");
		
		repo.setConfigPath("db/schema.rb");
		try {
			rc.loadRepo(repo);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
