package gitget;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import model.Language;
import model.Repo;
import sjava.JavaRepo;

public class JavaCrawlerTest extends JavaCrawler {
	String[][] cases = new String[][] {{
			"/src1/main/java/org/that/java1.java",
			"/src1/main/java/org/that/java2.java",
			"/src1/main/java/org/other/java3.java",
			"/src1/branch/org/that/java4.java",
			"/src2/main/java/org/what/java5.java",
			"/src2/main/java/org/other/java6.java",
			
	},{
		"/java1.java",
		"/java2.java"
	},{
		"/src1/org/test/javaError.java",
		"/src1/org/test/javaOk.java",
		
	}, {
		"/org/that/java1.java",
		"/org/that/java2.java",
		"/org/other/java3.java",
		"/org/that/java4.java",
		"/org/what/java5.java",
		"/com/other/java6.java",
	}
		};
	String[][] packages = new String[][] {{
		"org.that",
		"org.that",
		"org.other",
		"org.that",
		"org.what",
		"org.other"
		
	},
	{"",""},
	{
		null,
		"org.test"
	},
	{
		"org.that",
		"org.that",
		"org.other",
		"org.that",
		"org.what",
		"com.other"
	}
	};
	
	String[][] expectedResults = new String[][] {{
		"/src1/main/java",
		"/src1/branch",
		"/src2/main/java"
	
	},{
		""
	},{
		"/src1"
	},{
		""
		}};
	int currentCase=0;
	@Override
	protected String findPackage(JavaRepo jrepo, String leafPath) throws MalformedURLException {
		for (int i=0;i<cases[currentCase].length;i++) {
			if (leafPath.equals(cases[currentCase][i])) {				
				String pak = packages[currentCase][i];
				if (pak!=null)
					pak=pak.replace('.', '/');
				return pak;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testFindBasePaths() {
		Repo repo = new Repo(Language.JAVA);
		repo.setName("Dummy/Dummy");
		JavaRepo jrepo = new JavaRepo(repo);
		
		for (currentCase=0;currentCase<cases.length;currentCase++) {
			try {
				Dir root = Dir.newRoot();
				jrepo.setRoot(root);
				List<String>results = new ArrayList<String>();
				Collections.addAll(results, expectedResults[currentCase]);
			    //results.addAll(Arrays.asList(expectedResults[currentCase]));
				String[] ccase = cases[currentCase];
				for (String p:ccase) {
					root.register(p);
				}
				findBasePaths(jrepo);
				for (Dir bdir:jrepo.getBasePaths()) {
					String bp = bdir.getPath();
					assertTrue("Case "+currentCase+" Path '"+bp+"' not expected",results.contains(bp)); 
					results.remove(bp);
				}
				for (String rem:results) {
					fail("Case "+currentCase+" Excpected path '"+rem+"' not in the base paths list");
				}
				List<Dir> all =jrepo.getRoot().toLeafList(); 
				results = new ArrayList<String>();
				Collections.addAll(results, cases[currentCase]);
				for (Dir d:all) {
					String p = d.getPath();
					if (results.contains(p)) {
						results.remove(p);
					} else {
						fail("Case "+currentCase+": Root dir is not the same:'"+p+"' was not in the original root");
					}
				}
				List<String> badFiles = jrepo.getBadFiles().toLeafList().stream().map(d->d.getPath()).collect(Collectors.toCollection(ArrayList::new));
				for (String rem:results) {
					if (!badFiles.contains(rem))
						fail("Case "+currentCase+": Root path '"+rem+"' not in the new root");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				fail("Case "+currentCase+" "+ex.getMessage());
				
			}
		}
		
	}	
}
