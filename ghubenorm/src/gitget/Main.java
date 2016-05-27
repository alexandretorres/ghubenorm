package gitget;

import java.io.IOException;
/*
import com.google.gson.Gson;
import com.jcabi.github.Content;
import com.jcabi.github.Github;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;
import com.jcabi.github.Search;
*/
public class Main {
/*
	public static void main(String[] args) {
		Gson gson = new Gson();
		Teste obj = new Teste("alex","bla bla!\nblublu!");
		
		// convert java object to JSON format,
		// and returned as JSON formatted string
		String json = gson.toJson(obj);
		System.out.println(json);
		obj= gson.fromJson(json, Teste.class);
		System.out.println(obj.getName()+" says:\n"+obj.getBla());
		// ------
		Github github = new RtGithub(Auth.getProperty("oauth"));
		
		Search s = github.search();
		try {
			Iterable<Repo> repos = s.repos("q=language:java", "", Search.Order.DESC);
			int cnt=0;
			Repo r = repos.iterator().next();
			System.out.println(cnt+":"+r.coordinates().repo()+" http://"+r.coordinates().toString());
			s = github.search();
			Iterable<Content> cts = s.codes("q=javax.persistence+in:file+language:java+repo:"+r.coordinates().toString(), "", Search.Order.DESC);
			Content c = cts.iterator().next();
			System.out.println(c.path());
			//*
			for (Repo r:repos) {
				cnt++;
				System.out.println(cnt+":"+r.coordinates().repo()+" http://"+r.coordinates().toString());
				if (cnt>200)
					break;
			}*/
	/*	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

}
