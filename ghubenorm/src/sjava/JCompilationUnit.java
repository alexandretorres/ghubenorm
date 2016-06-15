package sjava;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;

import model.MClass;
import model.MTable;
import sjava.SJavaParser.AnnotationContext;
import sjava.SJavaParser.ElementValueArrayInitializerContext;
import sjava.SJavaParser.ElementValueContext;
import sjava.SJavaParser.ElementValuePairContext;
import sjava.SJavaParser.NormalAnnotationContext;

import static sjava.JPATags.*;

public class JCompilationUnit {
	String url;
	String packageName="";
	Set<MClass> classes= new HashSet<MClass>();
	Set<Import> imports = new HashSet<Import>();
	JavaRepo jrepo;
	transient SJavaParser parser;
	
	public JCompilationUnit(JavaRepo jrepo, String url) {
		this.url=url;
		this.jrepo=jrepo;
	}
	public MClass createClass() {
		MClass c = MClass.newMClass(jrepo.getRepo());//new MClass(comp,ctx.Identifier().getText());
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
	public MTable toTable(MClass c,Annotation atab) {
		MTable tab = c.newTableSource(atab.getValue("name", c.getName()));
		tab.setCatalog(atab.getValueAsString("catalog"));
		tab.setSchema(atab.getValueAsString("schema"));
		jrepo.getTables().add(tab);
		return tab;
	}
	public MClass getClazz(String name) {		
		//TODO:extract package name
		MClass ret = jrepo.getClasses().stream().filter(cl->(cl.getName().equals(name) && cl.getPackageName().equals(packageName)) || cl.getFullName().equals(name)).
				findFirst().orElse(jrepo.getClasses().stream().filter(cl->this.importClass(cl)).findFirst().orElse(null));
		
		return ret;
	}
	public boolean importClass(MClass cl) {
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
	Object value;
	public ElementValue(ElementValueContext val) {
		if (val.annotation()!=null) {
			this.annotation = Annotation.newAnnotation(val.annotation());
		} else if (val.elementValueArrayInitializer()!=null) {
			ElementValueArrayInitializerContext array = val.elementValueArrayInitializer();
			values = new ArrayList<ElementValue>();
			for (ElementValueContext item:array.elementValueList().elementValue()) {
				values.add(new ElementValue(item));
			}
		} else if (val.conditionalExpression()!=null) {
			TokenStream tokens = SJavaListnerImpl.parser.getTokenStream();			
			Interval interval = val.conditionalExpression().getSourceInterval();			
			this.value = ExprEval.evaluate( tokens.getText(interval));
			
		}
	}
}
class Annotation {
	String type;
	private static final String DEFAULT_KEY="value";
	Map<String, ElementValue> values = new HashMap<String, ElementValue>();
	private Annotation() {	}
	public Object getValue(String name) {
		if (values.containsKey(name)) {
			return values.get(name).value;
		}
		return null;
	}
	public String getValueAsString(String name) {
		if (values.containsKey(name)) {
			return (String)values.get(name).value;
		}
		return null;
	}
	public String getValue(String name,String def_value) {
		if (values.containsKey(name)) {
			return (String) values.get(name).value;
		}
		return def_value;
	}
	public Object getSingleValue() {
		if (values.containsKey(DEFAULT_KEY))
			return values.get(DEFAULT_KEY).value;
		return null;
	}
	public String getSingleValue(String def_value) {
		if (values.containsKey(DEFAULT_KEY)) {
			return (String) values.get(DEFAULT_KEY).value;
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
	public List<ElementValue> getListValue(String name) {
		if (values.containsKey(name)) {
			return values.get(name).values;
		}
		return Collections.EMPTY_LIST;
	}
	static Annotation newAnnotation(AnnotationContext a) {
		Annotation ret = new Annotation();
		if (a.markerAnnotation()!=null) {
			ret.type = a.markerAnnotation().typeName().Identifier().getText();
		} else if (a.normalAnnotation()!=null) {
			NormalAnnotationContext ctx = a.normalAnnotation();
			ret.type = ctx.typeName().Identifier().getText();
			for (ElementValuePairContext pair:ctx.elementValuePairList().elementValuePair()) {
				String id = pair.Identifier().getText();
				ElementValueContext value = pair.elementValue();
				ret.values.put(id,new ElementValue(value));
			}
		} else if (a.singleElementAnnotation()!=null) {
			ret.type = a.singleElementAnnotation().typeName().Identifier().getText();
			ElementValueContext value = a.singleElementAnnotation().elementValue();
			ret.values.put(DEFAULT_KEY,new ElementValue(value));
		}
		return ret;
	}
}
class ExprEval {
	private static ScriptEngineManager mgr = new ScriptEngineManager();
	private static ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
	public static Object evaluate(String expr)  {
		try {				   
			return jsEngine.eval(expr);
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return expr;
		
	}
}