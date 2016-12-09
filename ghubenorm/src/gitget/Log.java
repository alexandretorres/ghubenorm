package gitget;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import model.Repo;

public class Log {
	public final static int SKIP_TIME = 60000;
	public final static Logger LOG;// = Logger.getLogger(Log.class.getName()); 
	static {
		LOG = Logger.getLogger(Log.class.getName());
		Formatter formatter = new Formatter() {
			private Date printtime;
			@Override
			public String format(LogRecord record) {
				StringWriter sb=new StringWriter();
				//StringBuffer sb = new StringBuffer();
				if (printtime==null || (record.getMillis()- printtime.getTime()>SKIP_TIME)) {
					printtime = new Date(record.getMillis());
					sb.append(printtime+"\n");
				}
				if (record.getLevel()==Level.SEVERE) {						
					sb.append("[!!SEVERE!!] ");
				} 
				if (record.getLevel()==Level.WARNING) {						
					sb.append("[WARN] ");
				}
				//sb.append(record.getLevel().intValue());
				
				sb.append(record.getMessage());
				sb.append("\n");
				if (record.getThrown()!=null) {			
					sb.append("Exception: ");
			    	PrintWriter pw = new PrintWriter(sb);
					record.getThrown().printStackTrace(pw);
					pw.flush();
				}
				sb.flush();
				return sb.toString();
			}
			
		};
		if (!Options.WEB) {
		FileHandler fhandler;
			try {
				fhandler = new FileHandler("logs/java%u.%g.log", 1024*1024, 1000);
				fhandler.setLevel(Level.ALL);
				fhandler.setFormatter(formatter);
				LOG.addHandler(fhandler);
				
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
		      public void uncaughtException(Thread t, Throwable e) {		    	  
		          LOG.log(Level.SEVERE, t + " threw an exception: ", e);
		      };
		  });
		
		
		for (Handler h:LOG.getParent().getHandlers()) {
			h.setFormatter(formatter);
		}
	}
	public static void log(Repo repo,Level level, String msg, Throwable thrown) {
		if (repo!=null)
			repo.setErrorLevel(level.intValue());		
		LOG.log(level, msg, thrown);
	}
	
}
