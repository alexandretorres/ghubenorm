package gitget;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.junit.Test;

public class ReadLogs {
	@Test
	public void test() {
		int cnt=12;
		String path="logs/JavaOnly/";
		StringBuffer buf = new StringBuffer();
		boolean record=false;
		try {
			while (cnt>=0) {
				File file = new File(path+"java0."+cnt+".log");	
		        Scanner sc = new Scanner(file);
		        while (sc.hasNextLine()) {
		            String line = sc.nextLine();
		            if (line.startsWith("{Repo:Start}")) {
		            	record=true;
		            }
		            if (record) {
		            	buf.append(line);
		            	buf.append("\n");
		            	if (line.startsWith("{Repo:End}")) {
		            		record=false;
		            	}		            
		            	//System.out.println(line);
		            }
		        }
		        sc.close();		   
		        cnt--;
			}
			System.out.println("VAL"+buf.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();		        
		}
	}
}
