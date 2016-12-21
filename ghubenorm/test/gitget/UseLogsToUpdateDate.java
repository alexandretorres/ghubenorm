package gitget;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import db.jpa.RepoDB;
import model.Repo;

public class UseLogsToUpdateDate {
	public static String exp = "... ... \\d\\d? \\d\\d?:\\d\\d:\\d\\d BRS?T 201\\d";

	String[] paths= new String[] {
		"trash/logs/logsRunning_part1/",
		"trash/logs/LogsRunning_part2_9_11/",
		"trash/logs/LogsRunning_part3_23_11/",		
		"trash/logs/logsRunning_part4/",
		"trash/logs/ReloadLogsMainDatabase/",
		"trash/logs/LogsRunning_part5/",
	};
	class CRepo {
		CRepo(){}
		CRepo(int pid,String name) {
			this.pid=pid;this.name=name;
		}
		int pid=-1;
		String name;
	}
	@Test
	public void test() {
		
		ConfigDAO.config(JPA_DAO.instance);
		RepoDAO dao = ConfigDAO.getDAO(Repo.class);
		RepoDB rutil = new RepoDB(dao);
		
		try {
			Pattern pattern = Pattern.compile(exp);
			Pattern pstart = Pattern.compile("(\\{Repo:Start\\})\\{name:([^/]*/[^\\}]*)\\}");
			//35672 (ID:121041):rsms/smisk owner:rsms
			Pattern pvisit = Pattern.compile("\\d* \\(ID:(\\d*)\\):([^/]*/[^\\s]*)\\sowner");
			CRepo curRepo = new CRepo();
			CRepo lastRepo = curRepo;
			Date lastDate=null;
			int lastPid=-1;
			for (String path:paths) {
				File f = new File(path);
				String[] files = f.list();
				Arrays.sort(files, new Comparator<String>() {
					private Integer strip(String o) {
						o = o.replaceAll("java\\d.", "");				
						o=o.substring(0,o.indexOf("."));				
						return new Integer(o);
					}
					@Override
					public int compare(String o1, String o2) {				
						return strip(o2).compareTo(strip(o1));
					}
				} );
				
				for (int i=0;i<files.length;i++) {					
					String name=files[i];
					System.out.println(name+" "+lastRepo.pid+","+lastRepo.name);
					File file = new File(path+name);
					try (Scanner sc = new Scanner(file)) {
					
				        while (sc.hasNextLine()) {
				        				        
				            String line = sc.nextLine();
				            Matcher mat = pattern.matcher(line);
				            if (mat.find()) {
				            	String dt = line.substring(mat.start(),mat.end());			            	
				            	SimpleDateFormat df  =new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",Locale.US);
				            	lastDate = df.parse(dt);
				            	//System.out.println("	"+lastDate);
				            	
				            }
				            mat = pvisit.matcher(line);
				            if (mat.find()) {
				            	curRepo = new CRepo(new Integer(mat.group(1)),mat.group(2));
				            	//System.out.println("		"+curRepo.pid+","+curRepo.name);
				            	
				            }
				            mat = pstart.matcher(line);
				            if (mat.find()) {
				            	String rname =mat.group(2);
				            	if (!rname.equals(curRepo.name)) {
				            		curRepo = new CRepo(-1,rname);			            		
				            		//System.out.println("		"+rname);
				            	} else {
				            		//System.out.println("		(again)"+rname);
				            	}
				            }
				            if (!lastRepo.equals(curRepo) && !(curRepo.name==null && curRepo.pid<0)) {
				            	dao.beginTransaction();
				            	if (curRepo.pid==-1)
				            		rutil.setDate(curRepo.name, lastDate);
				            	else {
				            		if (lastPid<0)
				            			lastPid=0;
				            		rutil.setDate(curRepo.pid,lastDate,lastPid);
				            		lastPid=curRepo.pid;
				            	}
				            	dao.commitAndCloseTransaction();
				            }
				            lastRepo=curRepo;
				        }
					}
					curRepo = new CRepo();
				}
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
}
