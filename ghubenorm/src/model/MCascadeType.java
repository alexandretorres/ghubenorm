package model;

public enum MCascadeType {
	ALL,PERSIST,MERGE,REMOVE,REFRESH,DETACH;
	public MCascadeType add(MCascadeType cas) {
		if (cas==null)
			return this;
		if (cas==ALL || this==ALL)
			return ALL;
		if ((cas==PERSIST && this==REMOVE) || (this==PERSIST && cas==REMOVE))
			return ALL;
		if (cas==PERSIST || this==PERSIST)
			return PERSIST;
		if (cas==REMOVE || this==REMOVE)
			return REMOVE;
		return this;
		//TODO: Incomplete
			
	}
}
