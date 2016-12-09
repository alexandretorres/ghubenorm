package servlet;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module.Feature;

import dao.ConfigDAO;
import db.daos.MyConfigNop;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import gitget.GitHubRepoLoader;
import gitget.Options;
import model.Repo;

/**
 * Servlet implementation class RetrieveRepo
 * using compression/sendFile/min size on server.xml.
    <Connector connectionTimeout="20000" port="8080" protocol="HTTP/1.1" 
    compression="on" compressionMinSize="100" useSendfile="false" redirectPort="8443"/>
 
 */
@WebServlet("/ProcessRepo")
public class ProcessRepo extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    @Override
	public void destroy() {
    	try {
    		ConfigDAO.finish();
    	} catch (Exception ex2) {ex2.printStackTrace();}
		super.destroy();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		Options.WEB=true;
		String path = config.getServletContext().getRealPath(".");
		Options.AUTH_PATH = path+"/META-INF/";
		ConfigDAO.config(new MyConfigNop());		
		super.init(config);
	}

	/**
     * @see HttpServlet#HttpServlet()
     */
    public ProcessRepo() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		ServletOutputStream out = null;
		response.reset();
        //ByteArrayInputStream byteArrayInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        String name = req.getParameter("name");
        try {
	       
	        response.setContentType("text/plain"); 
	        GitHubRepoLoader gl = new GitHubRepoLoader();
	        Repo repo=gl.load(name);
	        //-----------
			//RubyRepo rrepo = new RubyRepo(repo);
			//repo.print();
			out = response.getOutputStream();
			//--
			ObjectMapper mapper = new ObjectMapper();	
			Hibernate5Module mod = new Hibernate5Module();
			mod.configure(Feature.FORCE_LAZY_LOADING, true);
			mod.configure(Feature.USE_TRANSIENT_ANNOTATION, false);			
			mapper.registerModule(mod);
			//
			System.out.println("sending data for repo "+name);
		//	GZIPOutputStream gout = new GZIPOutputStream(out);
			mapper.writeValue(out, repo);
			System.out.println("all data sent for repo "+name);
			out.close();
			//--
			
        } catch (Exception ex) {
        	ex.printStackTrace();
        
        	
        }
		//response.getWriter().append("Served at: ").append(req.getContextPath());
	}

}
