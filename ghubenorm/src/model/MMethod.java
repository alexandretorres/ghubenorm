package model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
public class MMethod {	
	private String name;
	private List<String> params;
	private String type;
	private MClass clazz;
	private boolean isAbstract=false;
	@Enumerated(EnumType.ORDINAL)
	private Visibility visibility=Visibility.PACKAGE;

	protected MMethod() {}
	public static MMethod newMethod(MClass cl,String name) {
		MMethod ret = new MMethod();		
		ret.name=name;
		ret.params=  new ArrayList<String>() ;
		ret.clazz=cl;
		cl.getMethods().add(ret);
		return ret;
	}
	public String getName() {
		return name;
	}
	public MMethod setName(String name) {
		this.name = name;
		return this;
	}
	public List<String> getParams() {
		return params;
	}
	
	protected void setParams(List<String> params) {
		this.params = params;
	}
	public MClass getClazz() {
		return clazz;
	}
	protected void setClazz(MClass clazz) {
		this.clazz = clazz;
	}
	public String getType() {
		return type;
	}
	public MMethod setType(String returns) {
		this.type = returns;
		return this;
	}
	public Visibility getVisibility() {
		return visibility;
	}
	public MMethod setVisibility(Visibility visibility) {
		this.visibility = visibility;
		return this;
	}
	public boolean isAbstract() {
		return isAbstract;		
	}
	public MMethod setAbstract(boolean b) {
		isAbstract=b;
		return this;
	}
}
