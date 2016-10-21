package sjava;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;





public class Test {
	private static JavaLoader loader = new JavaLoader();

	public static void main(String[] args) {
		
		try {
			//JCompilationUnit comp = new JCompilationUnit(new JavaRepo(new Repo(Language.JAVA)),"");
			File baseFile = new File("trash/SmallTest.java");
			JCompilationUnit comp = loader.load(baseFile.toURI().toURL());
		
		    comp.jrepo.solveRefs();
		    //System.out.println("compilation unit:\n"+listner.comp);
		    comp.jrepo.getRepo().print();
			/*ParseTree tree = parser.compilationUnit(); 
			System.out.println(tree.toStringTree(parser));*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private static void listEngines() {
		
	    ScriptEngineManager mgr = new ScriptEngineManager();
	    List<ScriptEngineFactory> factories = mgr.getEngineFactories();
	    for (ScriptEngineFactory factory : factories)
	    {
	        System.out.println("ScriptEngineFactory Info");
	        String engName = factory.getEngineName();
	        String engVersion = factory.getEngineVersion();
	        String langName = factory.getLanguageName();
	        String langVersion = factory.getLanguageVersion();
	        System.out.printf("\tScript Engine: %s (%s)\n", engName, engVersion);
	        List<String> engNames = factory.getNames();
	        for (String name : engNames)
	        {
	            System.out.printf("\tEngine Alias: %s\n", name);
	        }
	        System.out.printf("\tLanguage: %s (%s)\n", langName, langVersion);
	    }
	
	}

}
