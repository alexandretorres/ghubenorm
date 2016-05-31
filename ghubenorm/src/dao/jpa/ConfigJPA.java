package dao.jpa;

import java.util.HashMap;

import dao.ConfigDAO;
import dao.DAOInterface;

public abstract class ConfigJPA extends ConfigDAO {
	private static HashMap<Class,DAO> map = new HashMap<Class,DAO>();
	@Override
	@SuppressWarnings("unchecked")
	public <X extends Object,T extends DAOInterface<X>> T findDAOInstance(Class<X> cl) {
		if (!map.containsKey(cl)) {
			DAO<X> ret = new DAO<X>(cl);			
			return (T) ret;
		} else {
			return (T) map.get(cl);
		}
		
	}
	void addDAO(Class cl,DAO d) {
		if (map.put(cl, d)!=null)
			throw new RuntimeException("Duplicate DAO for the same class "+cl.getName());
	}
	/*public <X extends Object,T extends DAOInterface<X>> T findDAOInstance(Class<X> cl) {
		return DAO.getInstance(cl);
	}*/
	@Override
	public void finish() {
		DAO.finish();		
	}
}
