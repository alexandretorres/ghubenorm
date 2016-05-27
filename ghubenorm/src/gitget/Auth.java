package gitget;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Auth {
	private static Properties prop = new Properties();
	public static String getProperty(String name) {
		return prop.getProperty(name); 
	}
	static {
		
		InputStream input = null;

		try {

			input = new FileInputStream("oauth.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			System.out.println("--->"+prop.getProperty("oauth"));
			

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	  }
	
}
