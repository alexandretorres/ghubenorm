package sjava;

import static gitget.Log.LOG;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.junit.Test;

import model.Language;
import model.Repo;


public class JavaLoaderTest {
	private JavaLoader loader = new JavaLoader();
	JavaRepo jrepo=new JavaRepo(new Repo(Language.JAVA));
	private void loadDir(File baseFile) throws IOException {
		if (baseFile.isFile())
			read(baseFile);
		else
			for (File f:baseFile.listFiles()) {
	        	if (f.isFile()) {	        		
	    			read(f);        		
	        	} else {
	        		loadDir(f);
	        	}
			}	
	}
	private void read(File baseFile) throws IOException {
		if (!baseFile.getName().endsWith(".java"))
			return;
		Prof.open("loader.load");
		JCompilationUnit comp = loader.load(baseFile.toURI().toURL());
		Prof.close("loader.load");
	}
	@Test
	public void testLoad() {
		try {
			loader.setJrepo(jrepo);
			//File baseFile = new File("trash/SmallTest.java");
			File baseFile = new File("repos/MSD_File_Sharing-e1d5650d8cf477355ebe69b52f507c85c12b2ba6/WHAM project war/WHAM/src");
			Prof.open("loadDir");
			loadDir(baseFile);
			Prof.close("loadDir");
			//--
			
			Prof.open("solverefs");
		    jrepo.solveRefs();
		    Prof.close("solverefs");
		    //System.out.println("compilation unit:\n"+listner.comp);
		    jrepo.getRepo().print();
		    Prof.print();
			/*ParseTree tree = parser.compilationUnit(); 
			System.out.println(tree.toStringTree(parser));*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
