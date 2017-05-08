package gitget;

import static gitget.Log.LOG;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import org.junit.Test;

public class CheckDuplicateGits {	
	static Thread thread;
	static List<String> repoList = new ArrayList<String>();
	public static void main(String args[]) {
		//
		try {
			try (Scanner sc = new Scanner(new File("picks.txt"))) {				
		        while (sc.hasNextLine()) {		        				        
		            String line = sc.nextLine();
		            repoList.add(line.trim());
		        }
			
			}
			for (String r1:repoList) {
				for (String r2:repoList) {
					String[] p1 = r1.split("/");
					String[] p2 = r2.split("/");
					if (!r1.equals(r2) && p1[0].equals(p2[0])) {
						System.out.println(r1);
					}
				}
			}
			//
			//thread= new Thread(new CallMoveDirBat());
			//thread.start();
		} catch (Exception ex)
		{ 
			ex.printStackTrace();
		}
		
	}
	
	/*
	@Test
	public void test() {
		List<String> repoList = new ArrayList<String>();
		List<String> repetidos = new ArrayList<String>();
		try {
			try (Scanner sc = new Scanner(new File("picks.txt"))) {				
		        while (sc.hasNextLine()) {		        				        
		            String line = sc.nextLine();
		            repoList.add(line.trim());		            
		        }
			
			}
			int cnt=0;
			for (String repo:repoList) {
				boolean dup=false;
				String path = repo.split("/")[1];
				if (!repetidos.contains(repo))
		            for (String r2:repoList) {
		            	String p2 = r2.split("/")[1];
		            	if (!repo.equals(r2) && path.equals(p2)) {
		            		if (!dup) {
		            			dup=true;
		            			System.out.println("Duplicate found:"+repo);
		            			cnt++;	            			
		            		}
		            		repetidos.add(r2);
		            		cnt++;
		            		System.out.println("Duplicate found:"+r2);
		            	}
		            }
				
			}
			System.out.println("total :"+cnt);
			
				
		} catch (Exception ex) { 
			ex.printStackTrace();
		}
	}
	*/
}
class CallMoveDirBat implements Runnable {
	@Override
	public void run() {
		try {
			for (String r:CheckDuplicateGits.repoList) {
				String[] path = r.split("/");
				//git clone https://github.com/mikhail-pn/Map_osm.git
				String cmd = "moveDir.bat \""+path[1]+"\" \""+path[0]+"\"";
				System.out.println("cmd:"+cmd);
				Process proc = Runtime.getRuntime().exec(cmd);
				
				while (proc.isAlive()) {
					Thread.sleep(50);
				}
				System.out.println("done");
			}
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}
}