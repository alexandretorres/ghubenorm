package sjava;

import java.util.HashMap;
import java.util.Map.Entry;

import static gitget.Log.LOG;
/**
 * A simple and totally unreliable profiler. 
 * @author torres
 *
 */
public class Prof {
	private HashMap<String, ProfInfo> tab=new HashMap<String,ProfInfo>();
	private static Prof prof = new Prof();
	public static void open(String name) {
		ProfInfo p = prof.tab.get(name);
		if (p==null) {
			p = prof.newInfo();
			prof.tab.put(name, p);
		}
		p.start=System.nanoTime();
	}
	private ProfInfo newInfo() {
		ProfInfo p =new ProfInfo();
		return p;
	}
	public static void close(String name) {		
		long time = System.nanoTime();
		ProfInfo p = prof.tab.get(name);
		time = time-p.start;
		if (p.sum==0)
			p.first=time;
		p.sum+=time;
		p.times++;
	}
	public static void print() {
		for (Entry<String, ProfInfo> e:prof.tab.entrySet()) {
			LOG.warning(e.getKey()+":"+e.getValue());
		}
	}
	class ProfInfo {
		protected ProfInfo() {
			
		}
		long start;
		long sum=0;
		int times=0;
		long first;
		public String toString() {
			return (sum/1000000l)+"/"+times+"="+((sum/1000000l)/times)+"  with first="+(first/1000000l);
		}
	}
}

