package dao.nop;

import dao.ConfigDAO;
import dao.DAOInterface;

public class ConfigNop extends ConfigDAO {
	DAONop dao = new DAONop();
	@Override
	public <X, T extends DAOInterface<X>> T findDAOInstance(Class<X> cl) {
		return (T) dao;
	}

	@Override
	protected void doFinish() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setup() {
		// TODO Auto-generated method stub

	}

}
