package sjava;
import org.jrubyparser.CompatVersion;
import org.jrubyparser.NodeVisitor;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.*;
import org.jrubyparser.parser.ParserConfiguration;
import org.jrubyparser.rewriter.ReWriteVisitor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

public class TesteJRuby {

	public static void main(String[] args)  {
		try {
			Parser rubyParser = new Parser();
	        //StringReader in = new StringReader(string);
			FileReader in = new FileReader("20141121125249_add_card.rb");
	        CompatVersion version = CompatVersion.RUBY2_0;
	        ParserConfiguration config = new ParserConfiguration(0, version);
	        Node n = rubyParser.parse("<code>", in, config);
	        System.out.println(n);
	        FileWriter fw;
			
			fw = new FileWriter("out.rb");
			
	        ReWriteVisitor v = new ReWriteVisitor(fw,"");
	        n.accept(v);
	        v.flushStream();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
