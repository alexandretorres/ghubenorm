package dao;

public abstract class ConfigDAO {
	private static ConfigDAO config;
	public static ConfigDAO getConfig() {
		return config;
	}
	public static void config(ConfigDAO configDAO) {
		config = configDAO;
		configDAO.setup();
		
	}
	public static <X extends Object,T extends DAOInterface<X>> T getDAO(Class<X> cl) {
		return config.findDAOInstance(cl);
	}
	public abstract <X extends Object,T extends DAOInterface<X>> T findDAOInstance(Class<X> cl);
	public abstract void finish();
	public abstract void setup();
	
}
