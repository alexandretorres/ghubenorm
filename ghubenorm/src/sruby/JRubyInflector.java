package sruby;

import static gitget.Log.LOG;

import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
/**
 * to install gem on jruby:
 * jruby -S gem install rails
 * jirb is the command line utility
 * @author user
 *
 */
public class JRubyInflector {
	public static final JRubyInflector instance = new JRubyInflector();
	ScriptContext context;
	ScriptEngine rubyEngine;
	String derive_join_table_name = 
			"def derive_join_table_name(first_table, second_table) \n"
					+ "[first_table.to_s, second_table.to_s].sort.join(\"\0\").gsub(/^(.*_)(.+)\0\1(.+)/, '\1\2_\3').tr(\"\0\", \"_\")\n"
					+ "end";
	public static JRubyInflector getInstance() {
		return instance;
	}
	private JRubyInflector() {
		try {
			ScriptEngineManager m = new ScriptEngineManager();
			rubyEngine = m.getEngineByName("jruby");
			context = rubyEngine.getContext();

			context.setAttribute("label", new Integer(4), ScriptContext.ENGINE_SCOPE);
			
			try{
				rubyEngine.eval("require 'active_support/inflector'",context);
				//rubyEngine.eval("require 'active_record'",context);
				rubyEngine.eval(derive_join_table_name);
			} catch (ScriptException e) {
			    e.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	public String tableize(String className) {
		try {
			return (String) rubyEngine.eval("\""+className+"\".tableize", context);
		} catch (ScriptException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);		   
		    return "";
		}
	}
	public String foreignKey(String value) {
		try {
			return (String) rubyEngine.eval("\""+value+"\".foreign_key", context);
		} catch (ScriptException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);		   
		    return "";
		}
	}
	public String underscore(String value) {
		try {
			return (String) rubyEngine.eval("\""+value+"\".underscore", context);
		} catch (ScriptException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);		   
		    return "";
		}
	}
	public String pluralize(String value) {
		try {
			return (String) rubyEngine.eval("\""+value+"\".pluralize", context);
		} catch (ScriptException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);		   
		    return "";
		}
	}
	public String singularize(String value) {
		try {
			return (String) rubyEngine.eval("\""+value+"\".singularize", context);
		} catch (ScriptException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);		   
		    return "";
		}
	}
	public Object eval(String st) {
		try {
			return (String) rubyEngine.eval(st, context);
		} catch (ScriptException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);		   
		    return "";
		}
		
	}
	public String deriveJoinTable(String tab1,String tab2) {
		try {
			return (String) rubyEngine.eval("derive_join_table_name(\""+tab1+"\",\""+tab2+"\")", context);
		} catch (ScriptException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);		   
		    return "";
		}
	}
	public static void main(String[] args) {
		
		try {
			/*
			System.out.println(instance.tableize("photo"));
			System.out.println(instance.tableize("person"));
			System.out.println(instance.underscore("FirstPerson"));
			System.out.println(instance.singularize("people"));*/
			System.out.println(instance.eval("derive_join_table_name(\"members\", \"clubs\")"));
			/*
			ScriptEngineManager m = new ScriptEngineManager();
			ScriptEngine rubyEngine = m.getEngineByName("jruby");
			ScriptContext context = rubyEngine.getContext();

			context.setAttribute("label", new Integer(4), ScriptContext.ENGINE_SCOPE);
			
			try{
				rubyEngine.eval("require 'active_support/inflector'",context);
				Object x = rubyEngine.eval("\"photo\".tableize", context);
				System.out.println("x="+x);
			} catch (ScriptException e) {
			    e.printStackTrace();
			}*/
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		/*
		BSFManager.registerScriptingEngine("ruby", "org.jruby.javasupport.bsf.JRubyEngine", new String[]{"rb"});
        BSFManager manager = new BSFManager();
        manager.exec("ruby", "call_java.rb", -1, -1, getFileContents("call_java.rb"));
        String expr = "\"photo\".tableize";
        Object x = manager.eval("ruby", "call_java.rb", -1, -1, expr);
        System.out.println("x="+x);*/
	}
	/*
    private static String getFileContents(String filename) throws IOException {
        FileReader in = new FileReader(filename);
        return IOUtils.getStringFromReader(in);
    }*/
}
