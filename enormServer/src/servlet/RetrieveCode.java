package servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.codelibs.jhighlight.renderer.Renderer;
import org.codelibs.jhighlight.renderer.XhtmlRendererFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@WebServlet("/source/*")
public class RetrieveCode extends HttpServlet {

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		
		//response.setContentType("text/plain;charset=UTF-8");
		response.setContentType("text/HTML;charset=UTF-8");
        //PrintWriter out = response.getWriter();
        String pathInfo = req.getPathInfo();
        String uri = req.getRequestURI();
        String servletPath = req.getServletPath();
        try {
            String data = "uri:"+ uri+", pathInfo:"+pathInfo+", serv:"+servletPath;
            
            String real = getServletContext().getRealPath("sources/"+pathInfo);
            File file = new File(real);
            if (!file.exists()) {
            	response.getWriter().println("File Not Found<BR>"+data);
            	return;
            }           
            Renderer renderer = XhtmlRendererFactory.getRenderer("java");
            renderer.highlight(pathInfo.substring(pathInfo.lastIndexOf("/")+1),new FileInputStream(file) ,response.getOutputStream(), "UTF-8", false);
            /*
            try (FileReader fr = new FileReader(file)) {  	
                
            	BufferedReader reader = new BufferedReader(fr);            	
            	String line;                
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
