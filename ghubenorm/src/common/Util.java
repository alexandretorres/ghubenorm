package common;

import java.util.Collection;

public class Util {
	public static boolean equals(Object v1,Object v2) {
		if (v1==null || v2==null) {
			return v1==v2;
		}
		return v1.equals(v2);
	}
	public static boolean isNullOrEmpty(String s) {
		return (s==null || s.length()==0);
			
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void removeAll(Collection col) {
		col.removeAll(col);
	}
	public static String capSize(String str,int size) {
		if (str!=null && str.length()>size)
			str = str.substring(0,size);
		return str;
		
	}
}
