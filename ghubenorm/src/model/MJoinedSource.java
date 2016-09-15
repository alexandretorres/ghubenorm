package model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Entity
public class MJoinedSource extends MDataSource{
	
	@OrderColumn(name="pos")
	@OneToMany(cascade=CascadeType.PERSIST)
	private List<MTable> defines=new ArrayList<MTable>() ;
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

	@Override
	public String printName() {
		String ret="";
		for (MTable tab:defines) {
			String tname = tab.getName();
			if (ret.length()>0)
				ret+=",";
			ret+=tname==null ? "<Unnamed>" : tname;
		}
		return ret;
	}


	
}
