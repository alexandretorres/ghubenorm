package servlet;

import javax.servlet.ServletConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import dao.ConfigDAO;
import db.daos.MyConfigNop;
import gitget.Options;

public class WebUtil {
	
	public static void init(ServletConfig config) {
		Options.WEB=true;
		String path = config.getServletContext().getRealPath(".");
		Options.AUTH_PATH = path+"/META-INF/";		
		
	}
	public static String getCookie(HttpServletRequest request,String name) {
	
		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
		 for (Cookie cookie : cookies) {
		   if (cookie.getName().equals(name)) {
		     return cookie.getValue();
		     
		    }
		  }
		}
		return null;
	}
}
