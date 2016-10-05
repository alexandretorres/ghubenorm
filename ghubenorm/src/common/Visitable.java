package common;

public interface Visitable
{
   /**
    * for each dependent:
    *   visitor.callAccept(dependent);
    * visitor.visit(this);
    * @param visitor
    */
	public void accept(ReflectiveVisitor visitor);
}