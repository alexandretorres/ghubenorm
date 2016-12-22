package servlet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.ConfigDAO;
import db.daos.MyConfigNop;
import gitget.Auth;
import gitget.GitHubRepoLoader;
import gitget.Options;

/**
 * Servlet implementation class Auth
 */
@WebServlet("/Auth")
public class AuthSrv extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	@Override
	public void init(ServletConfig config) throws ServletException {
		WebUtil.init(config);	
		super.init(config);
	}
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AuthSrv() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String start = request.getParameter("start");
		String cliId = Auth.getProperty("client_id_web");
		String protocol = Auth.getProperty("http");
		if (start!=null) {
			int rnd = (int)(Math.random()*100000);
			request.getSession().setAttribute("rnd", rnd);
			//"http://179.219.65.204:8080/enorm/Auth
			String url  = request.getRequestURL().toString();
			if (protocol.equals("https"))
				url = url.replace("http:","https:");
			String r = response.encodeRedirectURL(
					"https://github.com/login/oauth/authorize?client_id="+cliId+"&redirect_uri="
					+url+"&state="+rnd); 
			
			response.sendRedirect(r);
			return;
			
		}
		Object ob = request.getSession().getAttribute("rnd");
		request.getSession().removeAttribute("rnd");
		if (ob==null) {
			response.sendError(400,"absent state: invalid session?");
			return;
		}
			
		String expectedState = ob.toString();

		String code = request.getParameter("code");
		String state = request.getParameter("state");
		if (state==null || !state.equals(expectedState)) {
			response.sendError(400,"state does not correspond to expected");
			return;
		}
			
		//System.out.println(code+","+state);
		
		HttpsURLConnection con=null;
		try {
			URL obj = new URL("https://github.com/login/oauth/access_token");
			con = (HttpsURLConnection) obj.openConnection();
			con.setRequestMethod("POST");			
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setRequestProperty("Accept","application/json");
			String urlParameters = "client_id="+cliId+"&client_secret="+gitget.Auth.getProperty("oauth_web")
				+"&code="+code;
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			
			InputStream is = con.getInputStream();
			String token=null;
			try (JsonReader rdr= Json.createReader(is)) {
				JsonObject result = rdr.readObject();
				token = result.getString("access_token");
				//System.out.println(token);
				Cookie cookie = new Cookie("access_token",token);				
				cookie.setMaxAge(60*60*24*30); //1 month
				response.addCookie(cookie);
						
			}
			// get data...
			if (token!=null) {
				//https://api.github.com/user?access_token=...
				obj = new URL("https://api.github.com/user?access_token="+token);
				con = (HttpsURLConnection) obj.openConnection();
				con.setRequestMethod("GET");				
				con.setRequestProperty("Accept","application/json");				
				is = con.getInputStream();
				try (JsonReader rdr= Json.createReader(is)) {
					JsonObject result = rdr.readObject();
					String login = result.getString("login");
					Cookie cookie = new Cookie("login",login);				
					cookie.setMaxAge(60*60*24*30); //1 month
					response.addCookie(cookie);
					System.err.println(result);
				}
					
			}
			response.sendRedirect(".");	
			return;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//response.getWriter().append("Served at: ").append(request.getContextPath());
	}
	private void processResponse(HttpsURLConnection con,HttpServletResponse response) throws IOException {		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer resp2 = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			resp2.append(inputLine);
		}
		in.close();
		response.getWriter().append("result: ").append(resp2.toString()).append("\n");
	}
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(400);
	}

}
