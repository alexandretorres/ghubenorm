package gitget;

import static gitget.Log.LOG;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import javax.json.JsonObject;
import javax.json.JsonReader;

import dao.ConfigDAO;
import dao.jpa.CascadeDeleteVisitor;
import db.daos.RepoDAO;
import db.jpa.JPA_DAO;
import model.Language;
import model.Repo;
import sjava.Prof;

public class CrawlFromList extends GitHubCrawler {
	int[] publicIds = {
			7426434
			/*2668610,
			5506899,
			6577642,
			6764581,*/
		/*	907158,
			1582484,
			4752005,
			3057594,
			2819607,
			5387897,
			4431752,
			5629708,
			1504231,
			5745161,
			2389354,
			4964431,
			3726799,
			5321155,
			3603615,
			112859,
			6776950,
			379249,
			2567562,
			5550264,
			2798344,
			6437254,
			6338641,
			6348873,
			6439697,
			3693802,
			3342522,
			4305898,
			5531376,
			2912413,
			2890991,
			1951287,
			3231670,
			6595348,
			3322861,
			5180763,
			814635,
			4134733,
			7145003,
			7384741,
			1091514,
			6090634,
			2868374,
			7275404,
			957589,
			930571,
			4361623,
			5618327,
			7117232,
			2906316,
			4173922,
			5243368,
			690050,
			4799246,
			5651684,
			4098255,
			3285183,
			3561264,
			2994206,
			2353400,
			4372229,
			2249136,
			2550502,
			4086423,
			3578589,
			2342905,
			1842636,
			5446699,
			5699203,
			4107185,
			2981013,
			4245547,
			173637,*/
		/*	3839172,
			3587743,
			7062425,
			1611320,
			5643380,
			475333,
			1127473,
			1825002,
			3433513,
			3328843,
			4718498,
			7420937,
			2592972,
			1140269,
			1470115,
			1370232,
			6051489,
			4108106,
			6778629,
			2988709,
			3046176,
			2587695,
			6962849,
			1284928,
			6534657,
			2523026,
			5174979,
			7138231,
			3303502,
			3868452,
			7276334,
			3318768,
			3757405,
			6233200,
			1602238,
			2614085,
			3112183,
			5160372,
			5287439,
			5840627,
			920291,
			5634996,
			3638346,
			4459620,
			786142,
			4457497,
			4776763,
			4244682,
			3776315,
			769888,
			6103433,*/
	/*		5531520,
			6789509,
			6812263,
			6971583,
			1744975,
			5177835,
			6723774,
			7096135,
			5891231,
			2948949,
			3037164,
			1495765,
			6329980,
			2788494,
			5264258,
			6774512,
			6596784,
			6948067,
			1354264,
			7022919,
			2827933,
			3514489,
			232256,
			1603494,
			4809610,
			7028076,
			1753391,
			5168123,
			7393106,
			3659869,
			3079583,
			7146882,
			5013136,
			5858965,
			3124829,
			272471,
			1371190,
			6276194,
			3611987,
			4671663,
			3691992,
			898068,
			1184582,
			3084883,
			2870804,
			2633256,
			6896579,
			1962629,
			3274182,
			5970435,
			3203793,
			6287116,
			3576890,
			2541570,
			6308709,
			3946901,
			4156375,
			2130082,
			7458639,
			5879522,
			6921647,
			3637054,
			6041685,
			2824888,
			3877534,
			5527245,
			4697535,
			6068922,
			1824178,
			7072487,
			1900442,
			2173227,
			3810349,
			5380711,
			4215668,
			6982676,
			7135795,
			6155890,
			3494662,
			4908749,
			2486513,
			3260855,
			1065755,
			5136568,
			4321425,
			6176296,
			4084995,
			1776883,
			5494514,
			5871595,
			4370616,
			1118354,
			5626414,
			373696,
			6747643,
			395171,
			5053684,
			5793738,
			1961416,
			5747372,
			6088456,
			7204131,
			6082490,
			4931042,
			5918028,
			4809125,
			1061745,
			6905244,
			3418629,
			6276230,
			497558,
			7132402,
			330160,
			5582621,
			5448842,
			7022341,
			780079,
			7174553,
			1394846,
			180679,
			5618989,
			593961,
			2145369,
			47929,
			5013907,
			5903946,
			6864175,
			7005849,
			3095385,
			4395961,
			3641311,
			2851858,
			319196,
			6258208,
			2545367,
			1190987,
			4760769,
			5586978,
			2464891,
			6747233,
			7030779,
			1772628,
			5513705,
			2958656,
			1175180,
			4827774,
			3011322,
			4298676,
			5171633,
			6596779,
			4536629,
			6928429,
			4397942,
			3139806,
			6612068,
			3203503,
			1784501,
			6633264,
			6138522,
			1591047,
			7022685,
			6584691,
			6760986,
			784827,
			3834025,
			7426437,
			5716641,
			7405044,
			2096071,
			1123849,
			5998395,
			3175585,
			6620571,
			2510108,
			3201233,
			2844312,
			220485,
			2514586,
			4840916,
			2021224,
			2075756,
			2714393,
			623588,
			2233442,
			5597560,
			4204222,
			6912244,
			543761,
			5521622,
			3835863,
			249245,
			3749009,
			4271266,
			3695661,
			4938085,
			5355667,
			584719,
			5805291,
			3215535,
			1813867,
			2810047,
			2521405,
			7135215,
			5203635,
			6843310,
			1629667,
			6428160,
			6657575,
			3944559,
			6386412,
			2648982,
			5099598,
			1631374,
			5884083,
			5121918,
			499106,
			5342001,
			4548124,
			7430233,
			1057780,
			5292089,
			4456835,
			2101241,
			6386403,
			214884,
			1221895,
			5925991,
			2004638,
			1691340,
			4346413,
			3993782,
			1876727,
			2107808,
			6058280,
			6456668,
			1651479,
			2600532,
			3627053,
			1603277,
			2813657,
			3790539,
			6106189,
			6686368,
			4248069,
			1758050,
			6455903,
			5668222,
			3704399,
			2507091,
			1507361,
			3356094,
			5096503,
			4075967,
			5982189,
			1800216,
			3425928,
			3002868,
			3327733,
			2312493,
			6168192,
			5701468,
			4068112,
			5525933,
			2485432,
			2480908,
			6529842,
			4365867,
			5915098,
			6529989,
			165104,
			271180,
			7192277,
			3061799,
			4606708,
			4594576,
			6838747,
			2171748,
			2768196,
			6875050,
			6871624,
			4159883,
			2590207,
			2105916,
			6153500,
			1518623,
			6453194,
			2082240,
			1115027,
			2661319,
			3073517,
			6694986,
			2657490,
			1182437,
			4963162,
			6727813,
			2180854,
			2945528,
			5168829,
			2885076,
			3979475,
			3185497,
			4733226,
			5681966,
			3133221,
			2300766,
			974126,
			2811985,
			3071706,
			3510528,
			5722423,
			2955267,
			3595001,
			1241382,
			5638359,
			1651722,
			1935222,
			2362693,*/
	/*		7193570,
			7171095,
			1072845,
			5103464,
			1155991,
			2756009,
			3254949,
			2771885,
			1410724,
			1537373,
			3765814,
			6530202,
			5298866,
			2344008,
			603679,
			5839688,
			4420107,
			4152638,
			1148726,
			5596802,
			2797742,
			2378176,
			3623191,
			5225735,
			2110701,
			2133898,
			3082449,
			5382928,
			4071417,
			5798801,
			3977993,
			1436828,
			1734894,
			3383265,
			1459419,
			1918395,
			1270080,
			4674791,
			7295922,
			4192877,
			5118438,
			6299936,
			5991290,
			2635932,
			4108078,
			1553791,
			7190440,
			3523796,
			994600,
			3116015,
			6759524,
			4145455,
			2733306,
			6625067,
			3570151,
			4768809,
			997788,
			6940340,
			5843156,
			4106296,
			1441279,
			5387593,
			5721343,
			4286865,
			3581432,
			3834938,
			1204447,
			2318558,
			3885169,
			3225775,
			1494755,
			1786333,
			5630681,
			3487762,
			3234711,
			5809019,
			5356093,
			3132985,
			2958562,
			4954001,
			7214263,
			2845434,
			1065108,
			822044,
			4535224,
			3928671,
			2295667,
			5853673,
			7015375,
			4284866,
			6828665,
			4949290,
			1786599,
			3242708,
			1933457,
			3047153,
			6355083,
			1716503,
			2946782,
			2158982,
			1524268,
			7299453,
			1054661,
			1271459,
			5869003,
			2605534,
			5170776,
			6506946,
			2833647,
			7296928,
			2811285,
			3804635,
			6103889,
			6957195,
			1665245,
			7292289,
			2553424,
			221422,
			5146615,
			7372036,
			792701,
			7361685,
			7053871,
			1823632,
			6105074,
			7175973,
			4025925,
			6569200,
			3161459,
			1347618,
			5377726,
			2124279,
			3623657,
			3442932,
			2595498,
			4203959,
			6674072,
			4603651,
			2089644,
			5558980,
			2767347,
			6008013,
			1287922,
			3806855,
			5587332,
			6578196,
			4080830,
			4480766,
			5629724,
			1665672,
			5277041,
			2994449,
			7051720,
			6963443,
			2758805,
			3289951,
			3603407,
			4099629,
			5285429,
			2954064,
			1787070,
			4712036,
			6490127,
			787875,
			4701033,
			3843929,
			4312940,
			1310939,
			4179536,
			5527166,
			3373005,
			2759656,
			3990441,
			2799466,
			3471004,
			957939,
			1083816,
			6824862,
			4816672,
			3500490,
			858685,
			261613,
			1341767,
			1380908,
			5827701,
			3627852,
			5611726,
			197051,
			3451660,
			2060559,
			4705588,
			6626480,
			4726428,
			6671526,
			2512299,
			3664810,
			4645009,
			1524224,
			2304813,
			6817010,
			6007869,
			2963512,
			1077535,
			4418194,
			3133127,
			6138101,
			5437269,
			188067,
			3874817,
			4060905,
			1263793,
			3308134,
			2140130,
			6574947,
			5478443,
			4747204,
			7270873,
			3912314,
			3577454,
			2751164,
			2100316,
			3524632,
			6474470,
			2254943,
			2582755,
			1482,
			4314816,
			2325303,
			5160580,
			3093751,
			6806806,
			3620773,
			2460496,
			6091035,
			7107717,
			4032403,
			1354332,
			1411975,
			5128116,
			5058826,
			4075672,
			2603250,
			6947564,
			6393888,
			3378850,
			492555,
			5105626,
			2721162,
			6690711,
			3831386,
			3571767,
			4889980,
			6276871,
			5955050,
			604797,
			5538029,
			6450398,
			6138735,
			186034,
			642189,
			3022494,
			4472390,
			1084060,
			6720480,
			3292907,
			1841920,
			5312921,
			2973603,
			7201513,
			5259900,
			1298353,
			7104629,
			6385773,
			5632595,
			6487662,
			1584395,
			1631309,
			1562248,
			2394818,
			4788895,
			5424753,
			4742809,
			6406772,
			3519919,
			6854879,
			4523159,
			4603135,
			4987888,
			6487268,
			4638085,
			3919345,
			1110934,
			4624055,
			7355102,
			7352962,
			5947746,
			3221197,
			990281,
			4338140,
			4296910,
			3774156,
			471706,
			2400703,
			3502597,
			4548261,
			1286261,
			3770976,
			447882,
			2567495,
			1966543,
			1466107,
			6505749,
			5453022,
			1982661,
			4339349,
			5421429,
			2582272,
			5500010,
			7411950,
			6582140,
			6076169,
			1530964,
			6652540,
			1430118,
			2909712,
			5922095,
			6653977,*/
			/*6972544,
			4718761,
			2995057,
			2218835,
			180272,
			6000912,
			6704392,
			5925237,
			5248340,
			5382634,
			3730817,
			4518292,
			5169658,
			4326585,
			5616995,
			4955657,
			2266038,
			1273360,
			803119,
			1517689,
			1033101,
			1358611,
			7175268,
			2262072,
			5563875,
			6239869,
			3372271,
			5405553,
			1115273,
			5649760,
			2288476,
			3708909,
			7149710,
			2017799,
			6799483,
			3794605,
			1823081,
			6655937,
			4895599,
			2910837,
			1892756,
			6005946,
			7113303,
			1320181,
			2775596,
			4833273,
			279216,
			1106105,
			6068927,
			6965798,
			6614028,
			6554948,
			5386293,
			1312618,
			2263505,
			357827,
			4200663,
			5854282,
			620081,
			2842207,
			3450070,
			2027408,
			2650322,
			4167686,
			2177864,
			3881244,
			5222345,
			3658032,
			1557195,
			7290348,
			3656439,
			2886570,
			6970286,
			834327,
			2177248,*/
		/*	7277311,
			3875545,
			641533,
			671934,
			866052,
			6215883,
			3253141,
			4464694,
			3340099,
			6769572,
			1244706,
			4630534,
			1497562,
			3770590,
			4297305,
			826819,
			6474045,
			3524947,
			729608,
			5189613,
			6818293,
			6291392,
			6072085,
			3743376,
			128294,
			4158693,
			2428713,
			6006115,
			206317,
			3193661,
			4972064,
			3559911,
			1056589,
			935674,
			3678540,
			5580473,
			6327957,
			5853251,
			6615562,
			2601600,
			5730659,
			3890526,
			6674080,
			4406463,
			4277100,
			5075532,
			7077848,
			3135044,
			6942110,
			3685526,
			3885063,
			1442942,
			4433748,
			3147580,
			3552745,
			4112106,
			5095719,
			2473361,
			5073742,
			300543,
			1140390,
			4259155,
			3149753,
			1508806,
			5913263,
			2960836,
			2094591,
			4878407,
			2724549,
			3555620,
			766548,
			6079761,
			6758469,
			4606342,
			7067436,
			1968668,
			4561693,
			4333494,
			7337177,
			2847848,
			1814372,
			3889997,
			1579725,
			6569546,
			5780157,
			7274854,
			5957580,
			6698908,
			5829172,
			5792361,
			3700470,
			1523369,
			6248911,
			2917365,
			295079,
			4048862,
			2930379,
			3115025,
			1451040,
			5863479,
			3017200,
			3722325,
			2808883,
			2768855,
			764708,
			6485734,
			2959956,
			5353841,
			5420739,
			3796227,
			5828617,
			5967421,
			6300878,
			3255132,
			1538158,
			2363980,
			4783716,
			2203682,
			7017600,
			5388533,
			1643050,
			7299230,
			4314723,
			1367350,
			2705287,
			4627295,
			6329833,
			6370964,
			1403619,
			5698958,
			7015481,
			1808546,
			2518901,
			2740162,
			563231,
			3757624,
			593529,
			7277635,
			4411610,
			1511527,
			6799148,
			5528032,
			4724469,
			6679885,
			3897769,
			2675737,
			4724447,
			2899580,
			7347123,
			1235773,
			3253945,
			4273716,
			2861917,
			1193974,
			6799578,
			98901,
			4625457,
			6920348,
			3350272,
			206451,
			5970242,
			1593942,
			6136429,
			4329588,
			650660,
			5885274,
			5657312,
			276536,
			1354219,
			5709280,
			2383782,
			6696943,
			1017605,
			2108930,
			1933490,
			997577,
			6963312,
			5691496,
			5785011,
			6999048,
			2100459,
			4413255,
			1556873,
			3022143,
			5892966,
			6350937,
			5820909,
			1949073,
			5781279,
			7440054,
			2576064,
			127180,
			5071158,
			1915757,
			6934230,
			6394704,
			1712715,
			3167915,
			4964739,
			2510230,
			2266816,
			2734868,
			7263016,
			5723215,
			2559853,
			6428081,
			3145943,
			876766,
			3221485,
			2806410,
			5934469,
			6672869,
			7090930,
			1431124,
			4416606,
			1102761,
			3772315,
			5975403,
			2053322,
			4462017,
			4595183,
			511297,
			3564574,
			4787562,
			6178667,
			797575,
			6690608,
			3680814,
			3310270,
			4586516,
			1265679,
			1891737,
			6350726,
			3544047,
			2015334,
			1586941,
			2628461,
			6754377,
			1383712,
			6666617,
			7220459,
			4213854,
			317654,
			5751934,
			3602006,
			2159574,
			2458086,
			4029524,
			1800795,
			3472287,
			1613886,
			2587462,
			3636483,
			5505867,
			3134006,
			7136016,
			3800577,
			6329934,
			6541525,
			2102205,
			7003505,
			2136527,
			4989060,
			2586458,
			3620938,
			2303432,
			6366747,
			5585302,
			1590781,
			4012458,
			2501197,
			6531772,
			731883,
			3881365,
			943806,
			7073042,
			3443005,
			6067186,
			3892532,
			7422160,
			1812602,
			6719453,
			3255941,
			7420091,
			7126958,
			5875316,
			2405839,
			5109297,
			3575492,
			1431813,
			6059514,
			5491588,
			4331432,
			1050944,
			7191388,
			7233622,
			4509024,
			5672206,
			3579101,
			2280339,
			4441245,
			1155961,
			4587211,
			6626389,
			3369252,
			6204547,
			5587600,
			6933314,
			2424457,
			834344,
			4827080,
			6370211,
			5822873,
			1888200,
			4636405,
			1392659,
			4711272,
			1366906,
			2298042,
			6800245,
			2960833,
			7377292,
			4487401,
			222703,
			5256068,
			2109042,
			5084932,
			6802129,
			6254721,
			4468552,
			5606179,
			2845913,
			3398413,
			3338808,
			6030012,
			2972881,
			2978442,
			7005837,
			3019890,
			2749153,
			3347755,
			7019618,
			2405587,
			5317452,
			1294896,
			726070,
			2118175,
			7297181,
			2295863,
			3871489,
			3248830,
			6080215,
			1799249,
			6215160,
			606391,
			1082726,
			6611106,
			2180338,
			4156796,
			3273629,
			3585756,
			3968190,
			3033487,
			1730438,
			6762554,
			3257509,
			457158,
			1025030,
			1420764,
			6605642,
			3276997,
			1937495,
			2765907,
			7278933,
			995984,
			4385142,
			5599811,
			2487852,
			385792,
			6679467,
			2267687,
			7039560,
			5403605,
			371937,
			7461299,
			6841749,
			5583043,
			5922361,
			1350523,
			3685464,
			7047000,
			3772567,
			3308808,
			3398821,
			4550500,
			2028089,
			4602154,
			6365816,
			928250,
			7425394,
			7007710,
			1757001,
			3070870,
			7432509,
			4574940,
			1296183,
			6439020,
			899555,
			5614336,
			1515786,
			4013531,
			2946314,
			603724,
			6426016,
			4036971,
			1870998,
			7240428,
			2086162,
			5875580,
			7113288,
			6921071,
			4292994,
			2460997,
			350425,
			6111834,
			2153090,
			5732091,
			5954922,
			3491189,
			5928569,
			7107352,
			1392302,
			317466,
			2616402,
			3693978,
			6871517,
			2659463,
			202417,
			4719593,
			2263916,
			5671957,
			851287,
			5784742,
			1550818,
			7415798,
			3748311,
			1145996,
			6587609,
			1448908,
			2507223,
			2498424,
			3460891,
			5403425,
			3617374,
			7185825,
			6720544,
			5595126,
			2475253,
			1344306,
			2169460,
			4009655,
			2829877,
			6475379,
			5877929,
			4527208,
			1710459,
			3471752,
			2328418,
			6808742,
			527237,
			2176483,
			4752987,
			6691109,
			3756580,
			6642085,
			4709330,
			7177336,
			585895,
			6652192,
			3493890,
			1007289,
			1132574,
			6874562,
			5539604,
			4678729,
			916763,
			2064328,
			586306,
			7424609,
			1557782,
			2844260,
			5178046,
			613770,
			3975644,
			111602,
			997311,
			6325206,
			5538584,
			4571867,
			2661334,
			4797606,
			5737583,
			1631408,
			3800563,
			3599932,
			1081991,
			5821717,
			5657977,
			372598,
			2526096,
			3626848,
			255324,
			4781091,
			253895,
			2855392,
			6501163,
			986620,
			3202397,
			6430856,
			1966528,
			791891,
			1613395,
			3646453,
			6769740,
			729466,
			6385078,
			2106669,
			6112399,
			989788,
			6020563,
			5594237,
			6705687,
			2820640,
			3633293,
			5602307,
			4072208,
			4220321,
			1035258,
			7172897,
			6540659,
			539508,
			6910846,
			4421771,
			820490,
			6423399,
			5889005,
			4074692,
			6495906,
			2030566,
			3373293,
			6170884,
			1416118,
			4241381,
			6343593,
			1164641,
			2591369,
			6017960,
			1160501,
			4501718,
			6206627,
			1327501,
			7230501,
			2683810,
			4330808,
			2303379,
			6765278,
			2354053,
			4606528,
			5494649,
			7305025,
			5286848,
			3326170,
			3765135,
			6886771,
			6018251,
			1365306,
			5074159,
			6403348,
			4775424,
			3359676,
			2160400,
			1786076,
			6332855,
			3182448,
			7239031,
			5720589,
			1203717,
			2663412,
			3878186,
			818080,
			4177263,
			6421908,
			1055045,
			6481228,
			1746147,
			1848631,
			5189878,
			6263450,
			6942778,
			3899588,
			935668,
			4515135,
			2970833,
			5625926,
			3273981,
			1264178,
			7370550,
			2044919,
			6810787,
			338397,
			3247499,
			3166395,
			6543739,
			5827522,
			2824557,
			3093547,
			3817775,
			302063,
			7005379,
			6737723,
			2505060,
			2849570,
			7057904,
			5119787,
			6515657,
			1102134,
			7221245,
			3960012,
			7261396,
			357958,
			842076,
			3550774,
			7219898,
			1529744,
			3696608,
			5197422,
			3030084,
			523713,
			6605290,
			1953888,
			2988101,
			4515150,
			633828,
			1132036,
			7107258,
			6242013,
			2621334,
			6570083,
			3096445,
			721650,
			6369095,
			4169887,
			2686932,
			1059824,
			1219413,
			7285872,
			5872891*/};
	public static void main(String[] args) {	
		ConfigDAO.config(JPA_DAO.instance);
		new Thread(new CrawlFromList()).start();
	}	
	@Override
	public void run() {	
		//this.gh.forceTooManyFiles=true;
		try {				
			URL uauth = new URL("https://api.github.com/?access_token="+gh.getOAuth());
			//try (InputStream is = uauth.openStream(); JsonReader rdr = Json.createReader(is)) {
			try (JsonReader rdr = gh.callApi(uauth,false)) {
				JsonObject obj = rdr.readObject();
				LOG.info(obj.toString());				
			}
			//select...
			RepoDAO dao = repoDao;			
			//remove(dao);
			reloadFromList(dao);
		} catch (Throwable ex) {
			LOG.log(Level.SEVERE,ex.getMessage(),ex);
		} finally {
			Prof.print();
			ConfigDAO.finish();
		}
	}
	public void remove(RepoDAO dao) {		
		for (int pid:this.publicIds) {
			dao.beginTransaction();		
			Repo repo = dao.findByPublicId(pid).get(0);		
			CascadeDeleteVisitor del = new CleanRepoVisitor(repo);			
			//dao.removeCascade(r);
			LOG.info("visitor executed, now issuing commit for:"+repo.getId()+" name "+repo.getName());
			dao.commitAndCloseTransaction();
		}		
	}
	public void reloadFromList(RepoDAO dao) {		
		
		for (int pid:this.publicIds) {
			/*
			dao.beginTransaction();		
			Repo repo = dao.findByPublicId(pid).get(0);		
			CascadeDeleteVisitor del = new CleanRepoVisitor(repo);			
			//dao.removeCascade(r);
			LOG.info("visitor executed, now issuing commit for:"+repo.getId()+" name "+repo.getName());
			dao.commitAndCloseTransaction();*/
			dao.beginTransaction();	
			dao.cleanRepo(pid);
			dao.commitAndCloseTransaction();
			dao.beginTransaction();		
			Repo repo = dao.findByPublicId(pid).get(0);			
			dao.commitAndCloseTransaction();
			if (!repo.getClasses().isEmpty() || !repo.getDataSources().isEmpty()) {
				throw new RuntimeException("reload repos cannot process repositories that are already loaded");
			}
			if (repo.getLanguage()==Language.JAVA) {
				repo.overrideErrorLevel(null);
				java.processRepo(repo);
			} else if (repo.getLanguage()==Language.RUBY) {
				repo.overrideErrorLevel(null);
				ruby.processRepo(repo);
			}
			LOG.info("repo reload finished:"+repo.getId()+" name "+repo.getName()+" public id "+repo.getPublicId());
		}
		
			
	}
}
