package dao.nop;

import java.util.Collection;

import dao.DAOInterface;

public class DAONop<C> implements DAOInterface<C> {

	@Override
	public void beginTransaction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commitAndCloseTransaction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rollbackAndCloseTransaction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T first(Collection<T> col) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public C find(Object pk) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public C persit(C obj) {
		// TODO Auto-generated method stub
		return obj;
	}

	@Override
	public void remove(C obj) {
		// TODO Auto-generated method stub
		
	}

	

}
