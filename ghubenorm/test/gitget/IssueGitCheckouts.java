package gitget;

import static gitget.Log.LOG;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

import org.junit.Test;

import dao.ConfigDAO;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;

public class IssueGitCheckouts {
	final int MAX_ID=13363980;//21;//1278;
	
	public static String repos[] = new String[] {
			 "efficiency20/ops_middleware.git"
			, "naoty/stlgen.git"
			, "murphy99/rails-bootstrap.git"
			, "opf/openproject-auth_plugins.git"
			, "callumj/WebSocketsFun.git"
			, "technicalpickles/vlad-extras.git"
			, "Mulkave/remote-git-setup.git"
			, "JJ-DBC/deaf_grandma_1.git"
			, "wannabefro/gets_some.git"
			, "GlobalNamesArchitecture/karousel.git"
			, "Scrambler70/first_app.git"
			, "dancarter/GameData.git"
			, "bdef45/omrails.git"
			, "unthinkingly/hot-symbols.git"
			, "ktonon/code_node.git"
			, "TheDeadDreamer/first_app.git"
			, "kyanny/crx_unpack.git"
			, "waaaaargh/waaaaargh.github.io.git"
			, "AndrewBKang/checkers.git"
			, "lfarricker/Hello-World.git"
			, "tenno-seremel/ruby-ccg.git"
			, "ScottyLabs/printscottylabs-website.git"
			, "timcharper/git-helpers.git"
			, "joenas/pushover.git"
			, "hanza/server.git"
			, "ged/inversion.git"
			, "tcarlock/desk_case_dashboard.git"
			, "hewtwit/tiernanotoole.ie.git"
			, "mkremer/echo_service.git"
			, "oshige/BRM.git"
			, "jacksunsea/happyruby.git"
			, "stevebutterworth/house-prices.git"
			, "dbalatero/poser.git"
			, "ttilley/rails_dojo_helpers.git"
			, "ywee/cis350.git"
			, "dennybaa/chef-kibana_auth.git"
			, "davidfb/redmine_projects_approval.git"
			, "cbrumelle/blueprintr.git"
			, "hynkle/genetic_traveling_salesman.git"
			, "marcoball/first_app.git"
			, "dschnare/jekyll-tools.git"
			, "safareli/myip.git"
			, "begriffs/lucre.git"
			, "edatrix/clone_wars.git"
			, "jakewendt/bells_and_whistles_demo.git"
			, "baozidotrails/nike.git"
			, "consolo/american_date_monkey_patch.git"
			, "Bitvala/rails_demo_app.git"
			, "Banashek/ProgrammingChallenges.git"
			, "dvidlui/sample_app2.git"
			, "lstoll/heroku-rubinius.git"
			, "traels/spree-promotion-exclude-specials-rule.git"
			, "CZJ117/first_app.git"
			, "larskotthoff/v-euc.git"
			, "Parre/paralytic.git"
			, "lean-poker/poker-player-ruby.git"
			, "togle/first_app.git"
			, "joshsz/pivotal_attribution.git"
			, "egotsev/ranker.git"
			, "katepdonahue/Restful-Rabbits.git"
			, "falves/sample_app.git"
			, "Shazburg/Shell-Scripts.git"
			, "brain-scape/Podspecs.git"
			, "wieseljonas/demo_app.git"
			, "millie/heroku-buildpack-ruby-pdftk.git"
			, "planningalerts-scrapers/pittwater.git"
			, "emcien/finch.git"
			, "murraju/hadooplib.git"
			, "Starki09/sample_app.git"
			, "jasherai/role_requirement.git"
			, "dahakawang/pdfysite.git"
			, "ngerakines/preview-chef-cookbook.git"
			, "hungarianrails/first_app.git"
			, "d6u/oauth_weibo.git"
			, "nrgiser/first_app.git"
			, "liuqingbo/common_oa.git"
			, "giobox/first_app.git"
			, "seanmsmith23/angular-todo.git"
			, "Murphydbuffalo/dan-murphy-dev.git"
			, "xlunchxbox/first_app.git"
			, "endeepak/transformers.git"
			, "nruth/eraser.git"
			, "henning/aldryn-importer.git"
			, "cleverua/best_thumbnail.git"
			, "kgrz/can_grit_be_used_in_gh.git"
			, "blaxter/rtwittbot.git"
			, "burtlo/rake-core.git"
			, "mrmarcel/tag-parser.git"
			, "huynhdangkhoa/first_app.git"
			, "hambly/Programming-Challenges-and-Exercises.git"
			, "jeffhsta/SpiralMatrix.git"
			, "utestapps/utestapps.github.com.git"
			, "randallreedjr/BrainTeaser.git"
			, "andreanisme/blog.git"
			, "reppard/codeeval.git"
			, "lelayf/datachef.git"
			, "serjum/ror.git"
			, "osak/userutil.git"
			, "rschultheis/Penny-Auction-Observer.git"
			, "mmgaggle/ceph-solo.git"
			//JAVA
			, "FuryGames/FuryBall.git"
			, "zhoucong1020/cartman.git"
			, "kacso/NASP_lab1_AVL_Tree.git"
			, "gpeFC/seminario_prog_uno.git"
			, "CircleCatalyst/WebSYNCClientGUI.git"
			, "jenkinsci/cron_column-plugin.git"
			, "mikhail-pn/Map_osm.git"
			, "pochiel/yukkurisim.git"
			, "sappo/HS_RM_WORKSPACE.git"
			, "openCage/eightyfs.git"
			, "mrFlick72/MatchManager-App.git"
			, "cubieboard/openbox_packages_wallpapers_HoloSpiral.git"
			, "saturdaycoder/easy-douban-fm.git"
			, "dvjzero/ChileDenunciaSpring.git"
			, "jyotikini/test.git"
			, "SpoutDev/InfiniteObjects.git"
			, "Md-Imrul/smt-crs.git"
			, "weatherapi12/Weather1.git"
			, "kevinavery/Chess.git"
			, "jasonycw/SaliencyCamera.git"
			, "Darkrulerz/Roosterprogramma.git"
			, "DATTest/TMTestProject.git"
			, "bkrockx/Session-management-using-Filters.git"
			, "amcorrigal/matching-engine.git"
			, "nirojans/rule-maven-plugin.git"
			, "jford2282/HungryFishGame.git"
			, "gir241/iRSU.git"
			, "rosipov/SQLEngine_A1.git"
			, "stephanemartin/rivage.git"
			, "mokumoku/Niseda1ro.git"
			, "strangerss/test.git"
			, "mseclab/aksdemo-step2.git"
			, "flyfei/SwipeLeftRightMenuListView.git"
			, "jl987-Jie/Information-Retrieval.git"
			, "instaclick/PDI-Plugin-Step-Riak.git"
			, "Runsafe/InfiniteSeaGenerator.git"
			, "echozdog/HelloWorld.git"
			, "tracygrover/oo-programming.git"
			, "rubinius/rubinius-core-api.git"
			, "monking/The-Stereofyter.git"
			, "yiranqin/AlgorithmStudy.git"
			, "FriedTaco/godPowers.git"
			, "pableu/http-post-example.git"
			, "knowbody/StockControlSimulation.git"
			, "ikukohiraga/sample.git"
			, "DeimantasB/SpinnerExample.git"
			, "devmatheus/AnalisadorLexico.git"
			, "lowerlight/guesthouse.git"
			, "Atom8tik/FlatBedrock.git"
			, "armentae/CIS112_Armenta_Ernesto_Lab1_class_Random.git"
			, "Schachte/Data-Input-Display-A1.git"
			, "derekqian/CS684-Algorithm-Design-and-Analysis.git"
			, "ThibaultTricard/ProjetTurteure-.git"
			, "andune/TitanChatAddons.git"
			, "sebastianmetta/florsebarepo.git"
			, "1nsp1r3rnzt/javaexamples.git"
			, "engine-alpha/beispiel-ticker.git"
			, "pmanvi/javaiqutils.git"
			, "mataszilaitis/1.git"
			, "snehalm87/SpringPractice.git"
			, "michael-fernandez/Shape-Your-World.git"
			, "mart-dominguez/ofa-ecompras.git"
			, "DanielLukic/IntensiME.git"
			, "soumya1986/MS-Thesis.git"
			, "AayushB/1945.git"
			, "johdah/iOthello.git"
			, "ntraft/modlur.git"
			, "ZhangShuJiangAdroid/df.git"
			, "codjo/codjo-maven-common.git"
			, "DanielMichalski/eLicence-cat-b.git"
			, "devalexx/evilrain.git"
			, "thisisvictor/WSProcessor.git"
			, "kazhida/surroundcalc.git"
			, "ivanGusef/wtracker.git"
			, "m-reza-rahman/jms2-lab.git"
			, "Pr0fil3/ProjetoPPL.git"
			, "shun-tak/Soukoban.git"
			, "b-inr/spring-Config.git"
			, "danielme-com/tip-Android--25-Gallery.git"
			, "sponiro/deep-uncompress.git"
			, "zydeco/PickBoat.git"
			, "gds12/DungeonCrawler.git"
			, "tuxmonteiro/AlgoSmartJForex.git"
			, "amangel/SignaturePadView.git"
			, "elvinfucom/sample.git"
			, "ChinaLongGanHu/UserLogBaiDuSEM.git"
			, "khozzy/suspy.git"
			, "psmarques/teste.capgemini.git"
			, "arodionov/yandex-springdao.git"
			, "KDReleng/org.eclipse.dltk.examples.git"
			, "nardi/yolo-octo-dangerzone.git"
			, "chenupt/HttpClient-master.git"
			, "chaostheory/fakedatamaker.git"
			, "JelloRanger/sodexo-menu-app.git"
			, "pjungwir/launch4j-maven-plugin.git"
			, "fbacchella/jrds.git"
			, "roun512/gsloginemulator.git"
			, "hitsk26/TDDBCSendai3rd.git"
			, "TaladeRimus/LP2_4M.git"
			, "adrienperonnet/java-elevator.git"	
	};
	public static List<String> repoList = new ArrayList<String>();
	static Thread thread;
	
	public static void main(String args[]) {
		//
		try {
		try (Scanner sc = new Scanner(new File("picks.txt"))) {				
	        while (sc.hasNextLine()) {		        				        
	            String line = sc.nextLine();
	            repoList.add(line.trim());
	        }
		
		}
			//
			thread= new Thread(new CallBat());
			thread.start();
		} catch (Exception ex)
		{ 
			ex.printStackTrace();
		}
		
	}
	

}

class CallBat implements Runnable {
	@Override
	public void run() {
		try {
			for (String r:IssueGitCheckouts.repoList) {
				String path = r.split("/")[0];
				//git clone https://github.com/mikhail-pn/Map_osm.git
				String cmd = "gitClone.bat https://github.com/"+r+" "+path;
				System.out.println("cmd:"+cmd);
				Process proc = Runtime.getRuntime().exec(cmd);
				
				while (proc.isAlive()) {
					Thread.sleep(100);
				}
				System.out.println("done");
			}
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);	
		}
	}
}