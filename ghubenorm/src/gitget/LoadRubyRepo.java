package gitget;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import sruby.RubyRepo;
import sruby.TesteJRuby2;

public class LoadRubyRepo {

	public static void main(String[] args) {
		String url_name="https://github.com/gitlabhq/gitlabhq/raw/85495c8c85533e2d4156231fd2535270afffef5a/db/schema.rb";
		
		try {
			URL url = new URL(url_name);
			
			//InputStream is = url.openStream();
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(url.openStream()));
			RubyRepo repo = TesteJRuby2.parseSchema(in);
			repo.listTables();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
