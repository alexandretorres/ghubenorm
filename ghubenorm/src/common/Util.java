package common;

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
}
