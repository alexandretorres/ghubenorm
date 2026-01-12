package gitget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import uk.ac.shef.wit.simmetrics.similaritymetrics.Jaro;

public class ClassCompare {
	List<ClassName> names = new ArrayList<>();
	static protected Jaro jaro = new Jaro();
	public ClassCompare() {
		System.out.println("LOADING...");
		File file = new File("classNames.txt");	
		File out = new File("classRels.txt");
		try (Scanner sc = new Scanner(file)){			
		
	        while (sc.hasNextLine()) {
	        	String s[] = sc.nextLine().split(",");
	        	if (s.length>2)
	        		throw new RuntimeException("More than one ,");
	        	ClassName cn = new ClassName(s[0],s[1]);
	        	names.add(cn);
	        }
	        System.out.println("Processing...");
	        try (FileWriter fw = new FileWriter(out)) {
				for (ClassName cn1:names) {
					if (cn1.count<10)
						continue;
					for (ClassName cn2:names) {
						float sim = jaro.getSimilarity(cn1.name.toLowerCase(), cn2.name.toLowerCase());
						if (sim>=0.9) {
							fw.write(cn1.name);
							fw.write(",");
							fw.write(cn2.name);
							fw.write(",");
							fw.write(""+ sim+"\n");
							///System.out.println(sim+":"+cn1.name+"=="+cn2.name);					
						}
					}
					fw.flush();
				}				
				fw.close();
	        } catch (Exception ex) {
	        	ex.printStackTrace();
	        }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}
	}
	public static void main(String[] args) {
		//System.out.println(jaro.getSimilarity("Cliente","Client"));
		new ClassCompare();
	}

}
class ClassName {
	String name;
	int count;
	ClassName(String name,String count) {
		
		this.name = name;
		this.count = Integer.parseInt(count);
	}
}
