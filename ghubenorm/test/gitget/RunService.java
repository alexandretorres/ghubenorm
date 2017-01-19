package gitget;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;


/**
 * run using NSSM https://nssm.cc/usage
 * @author torres
 *
 Create a DropBox service with auto/delayed start. and this account (adm)
 uses the client dropbox.exe
 
 Create a service pointing to runservice.class
 Path: C:\Program Files (x86)\Java\jdk1.8.0_111\bin\java.exe
 Startupdir: C:\eclipse\...\ghubenorm\
 Arguments: -cp C:/eclipse/eclipse_mars2_64/workspace/git/ghubenorm/build/classes/. gitget.RunService
 or maybe a .bat
 Exit: no action
 */
//SC CREATE GitCrawler Displayname= "GitCrawler" binpath= "srvstart.exe GitCrawler -c C:\eclipse\eclipse_mars2_64\workspace\git\ghubenorm\svstart.ini" start= auto
public class RunService {
	public static final String PATH="C:\\Users\\torres\\Dropbox\\ufrgs\\";
	static Thread gitHubCrawler;
	static Thread copyStuff;
	static Thread readCommand;
	public static void main(String[] params) {
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
		try {			
			File bla = new File(PATH+"Crawler Service Init.txt");
			FileWriter fw = new FileWriter(bla);
			fw.write("date:"+new Date());
			fw.flush();
			fw.close();
			
			startCrawler();
			copyStuff = new Thread(new CopyStuff());
			copyStuff.start();
			readCommand =  new Thread(new ReadCommand());
			readCommand.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Service Started at "+new Date());
		
	}
	public static void writeStatus() {
		try {			
			//File bla = new File("C:\\eclipse\\eclipse_mars2_64\\workspace\\git\\ghubenorm\\bla.txt");
			File bla = new File(PATH+"gitCrawlerStatus.txt");
			FileWriter fw = new FileWriter(bla);
			fw.write("date:"+new Date());
			if (gitHubCrawler!=null) {
				fw.write(" running:"+gitHubCrawler.isAlive());
			}
			fw.flush();
			fw.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static class ShutdownHook implements Runnable {
		public void run() {
		   onStop();
		}
		 
		private void onStop() {
			
			CopyStuff.running=false;
			if (gitHubCrawler!=null && gitHubCrawler.isAlive())
				gitHubCrawler.interrupt();
			if (readCommand!=null && readCommand.isAlive())
				readCommand.interrupt();
			if (copyStuff!=null && copyStuff.isAlive())
				copyStuff.interrupt();
			System.out.println("Service Stoped at "+new Date());	
		}
		 
	}

	public static void startCrawler() {
		//gitHubCrawler=new Thread(new GitHubCrawler());
		gitHubCrawler=new Thread(new TestRun());
		gitHubCrawler.start();
		
	}
		 
}
class CopyStuff implements Runnable {
	public static boolean running=false;
	@Override
	public void run() {
		running = true;
		try {
			while (running){			
				Runtime.getRuntime().exec("copy.bat \""+RunService.PATH+"\"");
				RunService.writeStatus();
				Thread.sleep(6000);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
class ReadCommand implements Runnable {
	public void run() {
		try {	
			while (CopyStuff.running) {
				File f = new File(RunService.PATH+"start.cmd");
				if (f.exists()) {
					if (RunService.gitHubCrawler!=null && !RunService.gitHubCrawler.isAlive()) {
						RunService.startCrawler();
					}
					f.delete();
				}
				Thread.sleep(30000);
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
class TestRun implements Runnable {
	public void run() {
		try {			
			Thread.sleep(60000);
			System.out.println("END of test run "+new Date()); 
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
/*
p1:C:\Program Files\Java\jre1.8.0_111\bin\java.exe
p2:C:\eclipse\eclipse_mars2_64\workspace\git\ghubenorm 
p3:-Dfile.encoding=UTF-8 -classpath <fullclasspath> gitget.RunService
---
Automatic delayed start
default login options...
--
exit actions: 
restart app
delay:60000
IO:
C:\Users\torres\Dropbox\\ufrgs\TESTE\out.txt
C:\Users\torres\Dropbox\\ufrgs\TESTE\err.txt
*/