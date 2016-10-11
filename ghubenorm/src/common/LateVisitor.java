package common;

import java.util.Comparator;

public interface LateVisitor {
	static Comparator<LateVisitor> comparator = new Comparator<LateVisitor>() {

		@Override
		public int compare(LateVisitor o1, LateVisitor o2) {			
			return o1.getOrder()-o2.getOrder();
		}
		
	};
	public boolean exec();
	public default int getOrder() {
		return 0;
	};
}
