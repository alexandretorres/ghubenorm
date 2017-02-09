package gitget;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;

import dao.ConfigDAO;
import db.jpa.JPA_DAO;


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
	public static final String PATH="C:\\Users\\user\\Dropbox\\ufrgs\\GitCrawlerService\\";
	static GitHubCrawler ghCrawler;
	static Thread gitHubCrawler;
	static Thread copyStuff;
	static Thread readCommand;
	public static void main(String[] params) {
		ConfigDAO.config(JPA_DAO.instance);	
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
		Log.setOutputLevel(Level.SEVERE);
		try {			
			File bla = new File(PATH+"Crawler Service Init.txt");
			FileWriter fw = new FileWriter(bla);
			fw.write("date:"+new Date());
			fw.flush();
			fw.close();
			
			startCrawler();
			copyStuff = new Thread(new TickTack());
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
			
			TickTack.running=false;
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
		RunService.ghCrawler = new GitHubCrawler();
		synchronized (RunService.ghCrawler) {
			gitHubCrawler=new Thread(RunService.ghCrawler);
			//gitHubCrawler=new Thread(new TestRun());
			gitHubCrawler.start();
		}		
	}
		 
}
class BackDb implements Runnable {
	@Override
	public void run() {
		try {
			System.out.println("Backing up database "+new Date());	
			Runtime.getRuntime().exec("bakDB.bat");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
class CopyLogs implements Runnable {
	@Override
	public void run() {
		try {
			Runtime.getRuntime().exec("copyLog.bat \""+RunService.PATH+"\"");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
class TickTack implements Runnable {
	public static final long TICK_TIME = 60*1000;
	public static final int TICKS_DB = 24*60; // one day
	//--
	public static boolean running=false;	
	public int ticks=0;  // minute tick
	@Override
	public void run() {
		running = true;
		try {		
			while (running){
				ticks++;
				if (ticks%10==0)
					RunService.writeStatus();
				if (ticks%60==0) {
					new Thread(new CopyLogs()).start();					
				}			
				if (ticks%TICKS_DB==0) {									
					new Thread(new BackDb()).start();
				}
				Thread.sleep(TICK_TIME);				
			}
		} catch (InterruptedException ie) {
			System.out.println("TickTack interrupted");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
class ReadCommand implements Runnable {
	public void run() {
		try {	
			while (TickTack.running) {
				File f = new File(RunService.PATH+"start.cmd");
				if (f.exists()) {
					if (RunService.gitHubCrawler!=null && !RunService.gitHubCrawler.isAlive()) {						
						RunService.startCrawler();
						RunService.writeStatus();
					}
					f.delete();
				}
				f = new File(RunService.PATH+"stop.cmd");
				if (f.exists()) {
					if (RunService.gitHubCrawler!=null && RunService.gitHubCrawler.isAlive()) {
						try {
							RunService.ghCrawler.stop=true;
							RunService.gitHubCrawler.interrupt();
							Log.LOG.warning("**** GitCrawler asked to stop by request ***");
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					f.delete();
				}
				f = new File(RunService.PATH+"force.cmd");
				if (f.exists()) {
					if (RunService.gitHubCrawler!=null && RunService.gitHubCrawler.isAlive()) {
						try {
							RunService.ghCrawler.stop=true;
							RunService.gitHubCrawler.interrupt();
							Log.LOG.warning("**** GitCrawler asked to force restart by request ***");
						
							Thread.sleep(1000);						
							RunService.startCrawler();
							RunService.writeStatus();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					f.delete();
				}
				f = new File(RunService.PATH+"status.cmd");
				if (f.exists()) {
					RunService.writeStatus();
					f.delete();
				}
				f = new File(RunService.PATH+"backup.cmd");
				if (f.exists()) {
					new Thread(new BackDb()).start();
					new Thread(new CopyLogs()).start();	
					f.delete();
				}
				
				Thread.sleep(30000);
			}
			
		} catch (InterruptedException ie) {
			System.out.println("ReadCommand interrupted");
		}
	}
}
class TestRun implements Runnable {
	public void run() {
		try {	
			Log.LOG.severe("This message is severe");
			Log.LOG.warning("This message should not appear");
			Thread.sleep(60000);
			System.out.println("END of test run "+new Date()); 
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("TestRun interrupted");
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
