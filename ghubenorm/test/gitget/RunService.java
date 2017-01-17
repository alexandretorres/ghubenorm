package gitget;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import javax.swing.JFrame;

//SC CREATE GitCrawler Displayname= "GitCrawler" binpath= "srvstart.exe GitCrawler -c C:\eclipse\eclipse_mars2_64\workspace\git\ghubenorm\svstart.ini" start= auto
public class RunService {
	public static void main(String[] params) {
		
		try {
			/*JFrame f = new JFrame();
			f.setVisible(true);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);*/
			File bla = new File("C:\\eclipse\\eclipse_mars2_64\\workspace\\git\\ghubenorm\\bla.txt");
			FileWriter fw = new FileWriter(bla);
			fw.write("data:"+new Date());
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("heeelo");
	}
}
