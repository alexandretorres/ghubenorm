package gitget;

import static gitget.Log.LOG;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
/**
 * stores passwords and authcodes in the oauth.properties file. oauth.properties should not be 
 * added to source control. TODO: create a template oauth.properties 
 * @author torres
 *
 */
public class Auth {
	private static Properties prop = new Properties();
	public static String getProperty(String name) {
		return prop.getProperty(name); 
	}
	static {
		
		InputStream input = null;

		try {
			input = new FileInputStream(Options.AUTH_PATH+"oauth.properties");
			// load a properties file
			prop.load(input);
			// get the property value and print it out
			//System.out.println("--->"+prop.getProperty("oauth"));			

		} catch (IOException ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);			
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					LOG.log(Level.SEVERE,e.getMessage(),e);	
					
				}
			}
		}

	  }
	
}
