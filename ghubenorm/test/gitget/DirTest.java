package gitget;

import static org.junit.Assert.*;

import org.junit.Test;

public class DirTest {

	@Test
	public void testAddChild() {
		Dir r1 = Dir.newRoot();
		Dir r2 = Dir.newRoot();
		Dir f1 = r1.register("base/rep/file1.java").parent;
		Dir f2 = r2.register("base/rep/file2.java");
		f1=f1.rebase(r1, r2);
		assertEquals(f1, f2.parent);
	}

}
