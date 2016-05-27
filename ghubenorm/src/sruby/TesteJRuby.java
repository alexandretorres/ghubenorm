package sruby;
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
		Node n=null;
		try {
			
			RubyRepo repo = new RubyRepo();
			Parser rubyParser = new Parser();
	        //StringReader in = new StringReader(string);
			//FileReader in = new FileReader("20141121125249_add_card.rb");
			CompatVersion version = CompatVersion.RUBY2_0;
	        ParserConfiguration config = new ParserConfiguration(0, version);
	        //first warm up the parser
			FileReader in = new FileReader("warmup.rb");
			rubyParser.parse("", in, config);
			// 
			long initTime = System.currentTimeMillis();
			in = new FileReader("repos/validates_equality/test/db/schema.rb");
			n = rubyParser.parse("", in, config);
	        
	        //System.out.println(n);
	        SchemaVisitor sv = new SchemaVisitor(repo);
	        n.accept(sv);
	        
	        //
	        
	        in = new FileReader("repos/validates_equality/test/validates_equality_test.rb");
	        n = rubyParser.parse("", in, config);
			RubyVisitor v = new RubyVisitor(repo);
			n.accept(v);
			repo.solveRefs(v);
			long endTime = System.currentTimeMillis()-initTime;
			System.out.println("time:"+endTime);
			repo.print();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			FileWriter fw = new FileWriter("out.rb");
			ReWriteVisitor v = new ReWriteVisitor(fw,"");
	        n.accept(v);
	        
	        v.flushStream();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
