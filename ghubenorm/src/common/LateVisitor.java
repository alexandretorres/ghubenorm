package common;

public interface LateVisitor {
	public boolean exec();
	public default int getOrder() {
		return 0;
	};
}
