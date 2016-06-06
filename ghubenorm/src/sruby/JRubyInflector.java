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

public class JRubyInflector {
	public static final JRubyInflector instance = new JRubyInflector();
	ScriptContext context;
	ScriptEngine rubyEngine;
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
	
	public static void main(String[] args) {
		
		try {
			
			System.out.println(instance.tableize("photo"));
			System.out.println(instance.tableize("person"));
			System.out.println(instance.underscore("FirstPerson"));
			System.out.println(instance.singularize("people"));
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
