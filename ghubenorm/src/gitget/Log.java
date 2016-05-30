package gitget;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log {
	public final static Logger LOG;// = Logger.getLogger(Log.class.getName()); 
	static {
		LOG = Logger.getLogger(Log.class.getName());
		
		for (Handler h:LOG.getParent().getHandlers()) {
			h.setFormatter(new Formatter() {

				@Override
				public String format(LogRecord record) {
					StringBuffer sb = new StringBuffer();
					
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
