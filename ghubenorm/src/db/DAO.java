package db;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import gitget.Auth;

public class DAO<C> {
	private static transient EntityManagerFactory emf;// = Persistence.createEntityManagerFactory("primary");
	private static transient EntityManager em;
	private Class<C> clazz;
	private static HashMap<Class,DAO> map = new HashMap<Class,DAO>();
	static {
		Map<String,String> props = new HashMap<String,String>();
		props.put("hibernate.connection.password", Auth.getProperty("hibernate.connection.password"));
		emf = Persistence.createEntityManagerFactory("gitenorm",props);
	}
	@SuppressWarnings("unchecked")
	public static <X extends Object,T extends DAO<X>> T getInstance(Class<X> cl) {
		if (!map.containsKey(cl)) {
			DAO<X> ret = new DAO<X>(cl);
			map.put(cl, ret);
			return (T) ret;
		} else {
			return (T) map.get(cl);
		}
		
	}
	protected DAO(Class<C> cl) {
		clazz = cl;
	}
	/*
	@SuppressWarnings("unchecked")
	protected Class<C> getTypeClass() {
		if (clazz==null)
			clazz = (Class<C>) ((ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0];
		return clazz;
	}*/
	public static EntityManager getEm() {
		return em;
	}
	public static void setEm(EntityManager pem) {
		em = pem;
	}
	
	public void beginTransaction() {
		try{
			em = emf.createEntityManager();			 
			em.getTransaction().begin();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	public void commitAndCloseTransaction() {
		if (em.getTransaction().isActive())
			em.getTransaction().commit();
		em.close();
		em=null;
	}
	public void rollbackAndCloseTransaction() {
		try {
			if (em.getTransaction().isActive())
				em.getTransaction().rollback();
		} catch (Exception ex) {
			
		}
		
		em.close();
		em=null;
	}
	public static <T> T first(Collection<T> col) {
		if (col.isEmpty())
			return null;
		return col.iterator().next();
	}
	public C find(Object pk) {
		return em.find(clazz, pk);
	}
	public void persit(C obj) {
		em.persist(obj);
	}
}
