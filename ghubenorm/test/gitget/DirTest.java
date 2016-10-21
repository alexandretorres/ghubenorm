package gitget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DirTest {

	@Test
	public void testAddChild() {
		{
			Dir r1 = Dir.newRoot();
			Dir r2 = Dir.newRoot();
			Dir f1 = r1.register("base/rep/file1.java").parent;
			Dir f2 = r2.register("base/rep/file2.java");
			f1=f1.rebase(r1, r2);
			assertEquals(f1, f2.parent);
		}
		{
			Dir r1 = Dir.newRoot();
			Dir r2 = Dir.newRoot();
			Dir f1 = r1.register("base/rep/pak/file1.java").parent.parent;	
			Dir dang = r1.register("base/rep/pak/file2.java").parent;
			Dir f2 = r2.register("base/rep/pak/file3.java");
			f1=f1.rebase(r1, r2);
			assertEquals(f1, f2.parent.parent);
			Dir pak = f1.children.get(0);
			assertEquals(pak,f2.parent);
			assertNull(dang.parent);
			
		}
	}

}
