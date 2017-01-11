package model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;
@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
public class MMethod {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private String name;
	@Transient
	private String[] params_;
	@Column(length=2048)
	private String params_lst;
	private String type;
	@ManyToOne(optional=false)
	private MClass clazz;
	private boolean isAbstract=false;
	@Enumerated(EnumType.ORDINAL)
	private Visibility visibility=Visibility.PACKAGE;

	protected MMethod() {}
	public static MMethod newMethod(MClass cl,String name) {
		MMethod ret = new MMethod();		
		ret.name=name;
		//ret.params=  new ArrayList<String>() ;
		ret.clazz=cl;
		cl.getMethods().add(ret);
		return ret;
	}
	
	public int getId() {
		return id;
	}
	protected void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public MMethod setName(String name) {
		this.name = name;
		return this;
	}
	public String[] getParams() {
		if (params_==null)
			if (params_lst==null)
				params_="".split(",");
			else
				params_ = params_lst.split(",");
		return params_;
	}
	
	public void addParam(String param) {
		if (params_lst==null || params_lst.length()==0)			
			params_lst=param;
		else
			params_lst=params_lst+","+param;
		params_=null;		
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
	protected String getParams_lst() {
		return params_lst;
	}
	protected void setParams_lst(String params_lst) {
		this.params_lst = params_lst;
	}
	
}
