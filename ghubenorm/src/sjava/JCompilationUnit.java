package sjava;

import static gitget.Log.LOG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import common.Util;
import dao.ConfigDAO;
import dao.DAOInterface;
import model.MClass;
import model.MProperty;
import model.MTable;

public class JCompilationUnit {
	String url;
	String packageName="";
	String base;
	Set<MClass> classes= new HashSet<MClass>();
	Set<Import> imports = new HashSet<Import>();

	JavaRepo jrepo;
	static DAOInterface<MClass> daoMClass = ConfigDAO.getDAO(MClass.class);
	static DAOInterface<MTable> daoMTable = ConfigDAO.getDAO(MTable.class);
	
	public boolean propertyAccess=false;
	public boolean hasFieldAnnotations=false;
	public boolean hasMethodAnnotations=false;
	
	public JCompilationUnit(JavaRepo jrepo, String url) {
		this.url=url;
		this.jrepo=jrepo;
	}
	public String stripGenericType(String typeName) {
		int idx = typeName.indexOf('<');
		if (idx<0) {
			return typeName;
		}
		String prefix = typeName.substring(0,idx);
		
		int end = typeName.indexOf('>');
		if (end<0 || end<idx)
			return prefix;
		if (typeName.length()>end) {
			String tail = typeName.substring(end+1);
			return prefix + stripGenericType(tail);
		} else
		return prefix;
	}
	public MClass createClass(String name) {		
		MClass c = daoMClass.persist(MClass.newMClass(this.url,jrepo.getRepo()).setName(name));
	
		//new MClass(comp,ctx.Identifier().getText());
		c.setPackageName(packageName);
		classes.add(c);
		jrepo.getClasses().add(c);
		return c;
	}
	/*public JCompilationUnit(String url) {
		this.url=url;
	}*/
	public boolean importsTag(JPATags tag) {
		for (Import imp:imports) {
			if (tag.isImport(imp))
				return true;			
		}
		return false;
	}
	public String toString() {
		String ret="";
		for (Import i:imports) {
			if (i.from.equals("javax.persistence.*")) {
				ret+=("imports all persistence package\n");
			} else if (i.from.equals("javax.persistence.Entity")) {
				ret+=("imports Entity persistence package\n");
			}
		}
		for (MClass c:classes) {
			ret+=c.toString()+"\n";
		}
		return ret;
	}
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	/*
		SecondaryTable:
			String name();
			String catalog() default "";
			String schema() default "";
			PrimaryKeyJoinColumn[] pkJoinColumns() default {};
			ForeignKey foreignKey() default @ForeignKey(PROVIDER_DEFAULT);
			UniqueConstraint[] uniqueConstraints() default {};
			Index[] indexes() default {};
		Table
			public @interface Table {
			String name() default "";
			String catalog() default "";
			String schema() default "";
			UniqueConstraint[] uniqueConstraints() default {};
			Index[] indexes() default {};
	 */
	//--
	public MTable toTable(MClass c,String entityName) {
		String name = entityName==null ? c.getName() : entityName;		
		MTable tab = daoMTable.persist(c.newTableSource(name));		
		jrepo.getDataSources().add(tab);
		return tab;
	}
	public MTable toTable(MClass c,Annotation atab) {
		String name = atab==null ? c.getName() : atab.getValue("name", c.getName());		
		MTable tab = daoMTable.persist(c.newTableSource(name));
		if (atab!=null) {
			tab.setCatalog(atab.getValueAsString("catalog"));
			tab.setSchema(atab.getValueAsString("schema"));
		}
		jrepo.getDataSources().add(tab);
		return tab;
	}
	/**
	 * return true if ALL references to classes with this name are solved
	 * @param newClass
	 * @return
	 */
	public boolean checkPendingRefs(MClass newClass,boolean acceptAnyBase) {
		int isClass=0;
		boolean acceptBase = acceptAnyBase || isSameBase(newClass) ;
		for (MClass cl:classes) {
			if (cl.getSuperClass()==null && cl.getSuperClassName()!=null) {
				String superName = cl.getSuperClassName();
				if (superName.equals(newClass.getFullName())) {
					if (acceptBase) {
						cl.setSuperClass(newClass);
						isClass=isClass==0?isClass=1:isClass;
					} else {
						isClass=2;
					}
				/*
				String pakName = null;
				int point=superName.indexOf(".");
				if (point>=0) {
					pakName = superName.substring(0,point);
					superName = superName.substring(point+1);
					if (pakName.equals(cl.getPackageName()) && superName.equals(newClass.getName())) {
						cl.setSuperClass(newClass);
						isClass=isClass==0?isClass=1:isClass;
					}*/
				} else if (superName.equals(newClass.getName()) ){
					if (importClass(newClass) && acceptBase) {
						cl.setSuperClass(newClass);
						isClass=isClass==0?isClass=1:isClass;
					} else {
						isClass=2;
					}
				}				
			}
			for (MProperty prop:cl.getProperties()) {
				if (prop.getTypeClass()==null || prop.getType()!=null) {
					String typeName = prop.getType();
					if (typeName.equals(newClass.getFullName())) {
						if (acceptBase) {
							prop.setTypeClass(newClass);
							isClass=isClass==0?isClass=1:isClass;
						} else {
							isClass=2;
						}
					} else if (typeName.equals(newClass.getName())){
						if (importClass(newClass) && acceptBase) {
							prop.setTypeClass(newClass);
							isClass=isClass==0?isClass=1:isClass;
						} else {
							isClass=2;
						}
					}	
				}
			}
		}
		return isClass==1;
	}
	public boolean isSameBase(MClass cl) {
		if (this.base==null) {
			if (packageName==null)
				this.base = url;
			else {
				String pak =  this.packageName.replace('.', '/');
				int pos = url.indexOf(pak);
				if (pos>0)
					this.base = url.substring(0,pos);
				else
					base="";
			}
		}
		return (cl.getFilePath().startsWith(base));
	}
	/**
	 * removes all classes that have a distinct BASE url from the set, and returns ONE of them, if it exists
	 * @param set
	 * @return
	 */
	private MClass filterByBaseUrl(Set<MClass> set) {
		MClass otherBased=null;
		for (Iterator<MClass> it=set.iterator();it.hasNext();) {
			MClass cnd = it.next();
			if (!isSameBase(cnd)) {
				otherBased = cnd; //ANY other Based class 
				it.remove();
			}
		}
		return otherBased;
	}
	public MClass getClazz(String name) {		
		//TODO:extract package name
		Set<MClass> set1 = jrepo.getClasses().stream().
				filter(cl->(cl.getName().equals(name) && Util.equals(cl.getPackageName(),packageName)) || cl.getFullName().equals(name)).collect(Collectors.toSet());
		MClass otherBased=filterByBaseUrl(set1);
		
		if (set1.isEmpty()) {
			set1 = jrepo.getClasses().stream().
					filter(cl->cl.getName().equals(name) && this.importClass(cl)).collect(Collectors.toSet());
			MClass otherBased2=filterByBaseUrl(set1);
			if (otherBased==null)
				otherBased = otherBased2;
		}
		return set1.isEmpty() ? otherBased : set1.iterator().next();
	}
	public boolean importClass(MClass cl) {
		if (Util.equals(packageName, cl.getPackageName()))
				return true;
		for (Import i:imports) {
			if (i.isImport(cl.getPackageName(), cl.getName()))
				return true;
		}
		return false;
	}
}
class Import {
	String from;
	String base;
	public static Import newImport(String from) {
		Import ret = new Import();
		ret.from=from;
		if (from.endsWith(".*")) {
			ret.base = from.substring(0,from.length()-2);
		}
		return ret;
	}
	private Import() {
		
	}
	public String getFrom() {
		return from;
	}
	
	public boolean isImport(String pak,String name) {		
		if (from.equals(pak+"."+name)) {
			return true;
		}
		if (base!=null) {			
			return base.equals(pak);
		}
		return false;
	}
}
class ElementValue {
	Annotation annotation;
	List<ElementValue> values;
	private Object value;
	public ElementValue(Expression val) {
		if (val instanceof AnnotationExpr) {
			this.annotation = Annotation.newAnnotation((AnnotationExpr)val);
		} else if (val instanceof ArrayInitializerExpr) {
			ArrayInitializerExpr array = (ArrayInitializerExpr)val;
			values = new ArrayList<ElementValue>();
			for (Expression item:array.getValues()) {
				values.add(new ElementValue(item));
			}
		} else if (val instanceof FieldAccessExpr) {
			FieldAccessExpr fex = (FieldAccessExpr) val;
			this.value = fex.toString();
		} else /*if (val.conditionalExpression()!=null)*/ {
			//TokenStream tokens = SJavaListnerImpl.parser.getTokenStream();			
			//Interval interval = val.conditionalExpression().getSourceInterval();			
			//this.value = ExprEval.evaluate( tokens.getText(interval));
			this.value = ExprEval.evaluate( val.toString());
		}
	}
	//TODO: inner subclasses for each type
	public Object getValue() {
		if (annotation!=null)
			return annotation;
		if (values!=null)
			return values;
		if (value!=null)
			return value;
		return null;
	}
}
class Annotation {
	String type;
	private static final String DEFAULT_KEY="value";
	Map<String, ElementValue> values = new HashMap<String, ElementValue>();
	private Annotation() {	}
	public List<ElementValue> extractListValue(String aname) {
		List<ElementValue> values = this.getListValue(aname);		
		if (values==null) {
			values = new ArrayList<ElementValue>();
			ElementValue ev = this.getElementValue(aname);
			if (ev!=null && ev.annotation!=null) {
				values.add(ev);								
			}
		}
		return values;
	}
	public Object getValue(String name) {
		if (values.containsKey(name)) {
			return values.get(name).getValue();
		}
		return null;
	}
	public String getValueAsString(String name) {
		if (values.containsKey(name)) {
			return (String)values.get(name).getValue();
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public <T> T getValue(String name,T def_value,Class<T> type) {
		if (values.containsKey(name)) {
			T value = (T) values.get(name).getValue();
			if (type.isInstance(value))
				return value;
			else {
				LOG.warning("Annotation<"+this.type+">.getValue("+name+"): Could not assign "+value+" using type "+type);
				return def_value;
			}
		}
		return def_value;
	}
	public boolean hasValue(String name) {
		return values.containsKey(name);
	}
	public String getValue(String name,String def_value) {
		if (values.containsKey(name)) {
			return (String) values.get(name).getValue();
		}
		return def_value;
	}
	public boolean getValue(String name,boolean def_value) {
		if (values.containsKey(name)) {
			return (boolean) values.get(name).getValue();
		}
		return def_value;
	}
	public Object getSingleValue() {
		if (values.containsKey(DEFAULT_KEY))
			return values.get(DEFAULT_KEY).getValue();
		return null;
	}
	public String getSingleValue(String def_value) {
		if (values.containsKey(DEFAULT_KEY)) {
			return (String) values.get(DEFAULT_KEY).getValue();
		}
		return def_value;
	}
	public ElementValue getElementValue() {
		if (values.containsKey(DEFAULT_KEY)) {
			return values.get(DEFAULT_KEY);
		}
		return null;
	}
	public List<ElementValue> getListValue() {
		return getListValue(DEFAULT_KEY);
	}
	public ElementValue getElementValue(String name) {
		if (values.containsKey(name)) {
			return values.get(name);
		}
		return null;
	}
	public List<ElementValue> getListValue(String name) {
		if (values.containsKey(name)) {
			return values.get(name).values;
		}
		return Collections.EMPTY_LIST;
	}
	
	static private VoidVisitorAdapter<Annotation> adapt = new VoidVisitorAdapter<Annotation>() {

		@Override
		public void visit(MarkerAnnotationExpr n, Annotation arg) {
			arg.load(n);			
		}

		@Override
		public void visit(NormalAnnotationExpr n, Annotation arg) {
			arg.load(n);			
		}

		@Override
		public void visit(SingleMemberAnnotationExpr n, Annotation arg) {
			arg.load(n);			
		}
		
	};
	static Annotation newAnnotation(AnnotationExpr a) {
		Annotation ret = new Annotation();
		a.accept(adapt,ret);
		return ret;
	}
	private void load(MarkerAnnotationExpr m) {
		type = m.getName().toString();
	}
	private void load(NormalAnnotationExpr na) {
		type = na.getName().toString();			
		for (MemberValuePair pair:na.getPairs()) {
			String id = pair.getName();
			Expression value = pair.getValue();
			values.put(id,new ElementValue(value));
		}
	}
	private void load(SingleMemberAnnotationExpr m) {
		type = m.getName().toString();
		
		Expression value = m.getMemberValue();
		values.put(DEFAULT_KEY,new ElementValue(value));
	}
	
}
class ExprEval {
	public static String getConstant(String constant) {
		if (constant==null)
			return null;
		String[] types = constant.split("\\.");
		return types[types.length-1];
	}
	private static ScriptEngineManager mgr = new ScriptEngineManager();
	private static ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
	public static Object evaluate(String expr)  {
		Prof.open("ExprEval.eval");
		try {		
			Object ret = jsEngine.eval(expr);
			if (ret!=null && !(ret instanceof String) && !(ret instanceof Number) && !(ret instanceof Boolean) && !(ret instanceof Class)) {
				if (ret.getClass().getSimpleName().equals("ScriptObjectMirror"))
					return expr;
				System.out.println("strange evaluation for"+expr+":"+ret);
			}
			return ret;
		} catch (ScriptException e) {
			LOG.fine("Java expression was not evaluated:"+expr);
			// TODO Auto-generated catch block
			LOG.log(Level.FINE, e.getMessage(),e);
			
		} finally {
			Prof.close("ExprEval.eval");
		}
		return expr;
		
	}
}