package dao;

public abstract class ConfigDAO {
	private static ConfigDAO config;
	public static ConfigDAO getConfig() {
		return config;
	}
	public static boolean isConfigured() {
		return config!=null;
	}
	public static void config(ConfigDAO configDAO) {
		config = configDAO;
		configDAO.setup();
		
	}
	public static <X extends Object,T extends DAOInterface<X>> T getDAO(Class<X> cl) {
		return config.findDAOInstance(cl);
	}
	public abstract <X extends Object,T extends DAOInterface<X>> T findDAOInstance(Class<X> cl);
	/**
	 * Release resources specific of the implementation, such as EntityManagers
	 */
	protected abstract void doFinish();
	public abstract void setup();
	/**
	 * Release resources specific of the implementation, such as EntityManagers,
	 * by calling doFinish at the config
	 */
	public static void finish() {
		if (config!=null)
			config.doFinish();
	}
	
}
