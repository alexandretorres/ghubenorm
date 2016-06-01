package gitget;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log {
	public final static int SKIP_TIME = 60000;
	public final static Logger LOG;// = Logger.getLogger(Log.class.getName()); 
	static {
		LOG = Logger.getLogger(Log.class.getName());
		
		for (Handler h:LOG.getParent().getHandlers()) {
			h.setFormatter(new Formatter() {
				private Date printtime;
				@Override
				public String format(LogRecord record) {
					StringBuffer sb = new StringBuffer();
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
					return sb.toString();
				}
				
			});
		}
	}
}
