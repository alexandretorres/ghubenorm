package dao;

import java.util.Collection;

public interface DAOInterface<C> {	
	public void beginTransaction() ;
	public void commitAndCloseTransaction();
	public void rollbackAndCloseTransaction();
	public <T> T first(Collection<T> col) ;
	public C find(Object pk) ;
	public void persit(C obj);
}
