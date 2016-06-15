package model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Entity
public class MJoinedSource extends MDataSource{
	@OneToMany(cascade=CascadeType.PERSIST)
	@OrderColumn(name="pos")
	private List<MTable> defines ;
	protected MJoinedSource() {
		
	}
	public List<MTable> getDefines() {
		return defines;
	}

	protected void setDefines(List<MTable> defines) {
		this.defines = defines;
	}
	public MJoinedSource addTable(MTable tab) {
		if (defines==null)
			defines= new ArrayList<MTable>();
		defines.add(tab);
		return this;
	}


	
}
