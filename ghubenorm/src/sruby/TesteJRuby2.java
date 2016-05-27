package sruby;
import org.jrubyparser.CompatVersion;
import org.jrubyparser.NodeVisitor;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.*;
import org.jrubyparser.parser.ParserConfiguration;
import org.jrubyparser.rewriter.ReWriteVisitor;
import org.jrubyparser.util.NoopVisitor;

import gitget.Auth;
import model.Repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TesteJRuby2 { 
	static long parseTime=0;
	static long walkTime=0;
	static int fileCnt=0;
	public static void main(String[] args)  {
		Node n=null;
		try {
			testeDB();
			
			long initTime =0; 
			long endTime=0;
			RubyRepo repo = new RubyRepo();
			Parser rubyParser = new Parser();
	        //StringReader in = new StringReader(string);
			//FileReader in = new FileReader("20141121125249_add_card.rb");
			CompatVersion version = CompatVersion.RUBY2_0;
	        ParserConfiguration config = new ParserConfiguration(0, version);
	        //first warm up the parser
			FileReader in = new FileReader("warmup.rb");
			n=rubyParser.parse("", in, config);
			initTime=System.currentTimeMillis();
			n.accept(new NoopVisitor());
			System.out.println("noop visit:"+(System.currentTimeMillis()-initTime));
			// 
	
			initTime =System.currentTimeMillis();
			//ps-deathstar-master
			//in = new FileReader("repos/chroma32-master/db/schema.rb");
			//in = new FileReader("repos/ps-deathstar-master/db/schema.rb");
			in = new FileReader("repos/promoweb-master/db/schema.rb");
			fileCnt++;
			n = rubyParser.parse("", in, config);
			parseTime+= (System.currentTimeMillis()-initTime);
			initTime =System.currentTimeMillis();
	        //System.out.println(n);
	        SchemaVisitor sv = new SchemaVisitor(repo);
	        n.accept(sv);      
	        walkTime+= (System.currentTimeMillis()-initTime);
	        //
	        //File baseFile = new File("repos/chroma32-master/app/models/");//
	        //File baseFile = new File("repos/ps-deathstar-master/app/models/");//
	        File baseFile = new File("repos/promoweb-master/app/models/");//
	        
	        RubyVisitor v = new RubyVisitor(repo);
	        for (File f:baseFile.listFiles()) {
	        	if (f.isFile()) {
	        		read(v,repo,rubyParser,config,f);
	        		fileCnt++;
	        	}
	        }	
	        initTime =System.currentTimeMillis();
			repo.solveRefs(v);
			System.out.println("files:"+fileCnt);
			System.out.println("resolve time:"+(System.currentTimeMillis()-initTime));
			System.out.println("walk time:"+walkTime);
			System.out.println("parse time:"+parseTime);
			System.out.println("walk+parse time by file:"+((parseTime+walkTime)/fileCnt));
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
	public static void read(RubyVisitor v ,RubyRepo repo,Parser rubyParser,ParserConfiguration config,File f) throws Exception {
		long initTime;
		v.reset();
		Node n=null;
		initTime =System.currentTimeMillis();
		FileReader in = new FileReader(f);
        n = rubyParser.parse("", in, config);	
        parseTime+= (System.currentTimeMillis()-initTime);
        initTime =System.currentTimeMillis();
		n.accept(v);
		walkTime+= (System.currentTimeMillis()-initTime);
	}
	private static void testeDB() {
		Map<String,String> props = new HashMap<String,String>();
		props.put("hibernate.connection.password", Auth.getProperty("hibernate.connection.password"));
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("gitenorm",props);
		
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
             
            Repo repo = new Repo();
            repo.setName("abc123");
           // repo.setId(1);
             
            em.persist(repo);
             
            em.getTransaction().commit();
        }
        catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        }
        finally{
            emf.close();
        }
	}

}
