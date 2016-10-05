package common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
/**
 * Reflective Visitor implementations must have a method dealing with each visit type, in the form 
 * visit<ClassName>(object). Each Visitable is expected to implement an accept method that calls accepts to the
 * components before calling visit. The callaccept operations checks if the object implements the visitable interface,
 * if not, it just calls the visit operation.
 *  
 * @author torres
 *
 */
public abstract class ReflectiveVisitor {
	public void visit(Object obj) {		
	    Method method = getMethod(getClass(),obj.getClass());	   
		try {
			invoke(method,new Object[] {obj});
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}		
	}
	public abstract void invoke(Method method,Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
	/**
	 * Calls the accept operation if the object is Visitable, otherwise it calls the plain visit
	 * @param obj
	 */
	public void callAccept(Object obj) {
		if (obj==null)
			return;
		if (obj instanceof Collection) {
			Collection col = ((Collection)obj);
			for (Object o:col.toArray()) {
				callAccept(o);
			}
			return;
		}
		if (obj instanceof Visitable) {
			((Visitable)obj).accept(this);
		} else
			visit(obj);
	}
	/**
	 * Default visitor 
	 */
	public abstract void visitObject(Object obj);
	
	
	private Method getMethod(Class thisClass,Class c) {
	   Class newc = c;
	   Method m = null;
	   // Try the superclasses
	   while (m == null && newc != Object.class) {
	      String method = newc.getName();
	      method = "visit" + method.substring(method.lastIndexOf('.') + 1);
	      try {
	         m = thisClass.getMethod(method, new Class[] {newc});
	      } catch (NoSuchMethodException e) {
	         newc = newc.getSuperclass();
	      }
	   }
	   // Try the interfaces.  If necessary, you
	   // can sort them first to define 'visitable' interface wins
	   // in case an object implements more than one.
	   if (newc == Object.class) {
	      Class[] interfaces = c.getInterfaces();
	      for (int i = 0; i < interfaces.length; i++) {
	         String method = interfaces[i].getName();
	         method = "visit" + method.substring(method.lastIndexOf('.') + 1);
	         try {
	            m = thisClass.getMethod(method, new Class[] {interfaces[i]});
	         } catch (NoSuchMethodException e) {}
	      }
	   }
	   if (m == null) {
	      try {
	         m = thisClass.getMethod("visitObject", new Class[] {Object.class});
	      } catch (Exception e) {
	          // Can't happen
	      }
	   }
	   return m;
	}
}