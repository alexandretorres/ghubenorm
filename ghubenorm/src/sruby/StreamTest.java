package sruby;

import java.util.ArrayList;

public class StreamTest {

	public static void main(String[] args) {
		ArrayList<String> bla = new ArrayList<String>();
		bla.add("teste");
		String[] res = bla.stream().toArray((String[]::new));
		System.out.println(String.join(",", res));
	}

}
