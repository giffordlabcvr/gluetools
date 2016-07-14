package uk.ac.gla.cvr.gluetools.utils;

import java.util.regex.Pattern;

public enum IsoCountry {
	
	AF("Afghanistan", "AF", "AFG", "004"),
	AX("Aland Islands", "AX", "ALA", "248"),
	AL("Albania", "AL", "ALB", "008"),
	DZ("Algeria", "DZ", "DZA", "012"),
	AS("American Samoa", "AS", "ASM", "016"),
	AD("Andorra", "AD", "AND", "020"),
	AO("Angola", "AO", "AGO", "024"),
	AI("Anguilla", "AI", "AIA", "660"),
	AQ("Antarctica", "AQ", "ATA", "010"),
	AG("Antigua and Barbuda", "AG", "ATG", "028"),
	AR("Argentina", "AR", "ARG", "032"),
	AM("Armenia", "AM", "ARM", "051"),
	AW("Aruba", "AW", "ABW", "533"),
	AU("Australia", "AU", "AUS", "036"),
	AT("Austria", "AT", "AUT", "040"),
	IR("Iran", "Islamic Republic of Iran", "IR", "IRN", "364","Iran"), // before Azerbaijan, because of the Iranian territory
	AZ("Azerbaijan", "AZ", "AZE", "031"),
	BS("Bahamas", "BS", "BHS", "044"),
	BH("Bahrain", "BH", "BHR", "048"),
	BD("Bangladesh", "BD", "BGD", "050"),
	BB("Barbados", "BB", "BRB", "052"),
	BY("Belarus", "BY", "BLR", "112"),
	BE("Belgium", "BE", "BEL", "056"),
	BZ("Belize", "BZ", "BLZ", "084"),
	BJ("Benin", "BJ", "BEN", "204"),
	BM("Bermuda", "BM", "BMU", "060"),
	BT("Bhutan", "BT", "BTN", "064"),
	BO("Bolivia", "BO", "BOL", "068"),
	BA("Bosnia and Herzegovina", "BA", "BIH", "070"),
	BW("Botswana", "BW", "BWA", "072"),
	BV("Bouvet Island", "BV", "BVT", "074"),
	BR("Brazil", "BR", "BRA", "076"),
	VG("British Virgin Islands", "VG", "VGB", "092"),
	IO("British Indian Ocean Territory", "IO", "IOT", "086"),
	BN("Brunei Darussalam", "BN", "BRN", "096"),
	BG("Bulgaria", "BG", "BGR", "100"),
	BF("Burkina Faso", "BF", "BFA", "854"),
	BI("Burundi", "BI", "BDI", "108"),
	KH("Cambodia", "KH", "KHM", "116"),
	CM("Cameroon", "CM", "CMR", "120"),
	CA("Canada", "CA", "CAN", "124"),
	CV("Cape Verde", "CV", "CPV", "132"),
	KY("Cayman Islands", "KY", "CYM", "136"),
	CF("Central African Republic", "CF", "CAF", "140"),
	TD("Chad", "TD", "TCD", "148"),
	CL("Chile", "CL", "CHL", "152"),
	CN("China", "CN", "CHN", "156"),
	HK("Hong Kong", "Hong Kong, Special Administrative Region of China", "HK", "HKG", "344", "Hong Kong"),
	MO("Macao", "Macao, Special Administrative Region of China", "MO", "MAC", "446", "Macao"),
	CX("Christmas Island", "CX", "CXR", "162"),
	CC("Cocos (Keeling) Islands", "CC", "CCK", "166"),
	CO("Colombia", "CO", "COL", "170"),
	KM("Comoros", "KM", "COM", "174"),
	CG("Congo (Brazzaville)", "CG", "COG", "178"),
	CD("Democratic Republic of the Congo", "CD", "COD", "180"),
	CK("Cook Islands", "CK", "COK", "184"),
	CR("Costa Rica", "CR", "CRI", "188"),
	CI("Côte d'Ivoire", "Côte d'Ivoire", "CI", "CIV", "384", "C[ôo]te d'Ivoire"),
	HR("Croatia", "HR", "HRV", "191"),
	CU("Cuba", "CU", "CUB", "192"),
	CY("Cyprus", "CY", "CYP", "196"),
	CZ("Czech Republic", "CZ", "CZE", "203"),
	DK("Denmark", "DK", "DNK", "208"),
	DJ("Djibouti", "DJ", "DJI", "262"),
	DM("Dominica", "DM", "DMA", "212"),
	DO("Dominican Republic", "DO", "DOM", "214"),
	EC("Ecuador", "EC", "ECU", "218"),
	EG("Egypt", "EG", "EGY", "818"),
	SV("El Salvador", "SV", "SLV", "222"),
	GQ("Equatorial Guinea", "GQ", "GNQ", "226"),
	ER("Eritrea", "ER", "ERI", "232"),
	EE("Estonia", "EE", "EST", "233"),
	ET("Ethiopia", "ET", "ETH", "231"),
	FK("Falkland Islands", "FK", "FLK", "238"),
	FO("Faroe Islands", "FO", "FRO", "234"),
	FJ("Fiji", "FJ", "FJI", "242"),
	FI("Finland", "FI", "FIN", "246"),
	GF("French Guiana", "French Guiana", "GF", "GUF", "254", "Guyane", "French Guiana"), // before France
	FR("France", "FR", "FRA", "250"),
	PF("French Polynesia", "PF", "PYF", "258"),
	TF("French Southern Territories", "TF", "ATF", "260"),
	GA("Gabon", "GA", "GAB", "266"),
	GM("Gambia", "GM", "GMB", "270"),
	UM("United States Minor Outlying Islands", "UM", "UMI", "581"), // before United States
	US("United States", "United States of America", "US", "USA", "840", "United States of America", "United States", "USA"), // before Georgia, because of the state
	GE("Georgia", "GE", "GEO", "268"),
	DE("Germany", "DE", "DEU", "276"),
	GH("Ghana", "GH", "GHA", "288"),
	GI("Gibraltar", "GI", "GIB", "292"),
	GR("Greece", "GR", "GRC", "300"),
	GL("Greenland", "GL", "GRL", "304"),
	GD("Grenada", "GD", "GRD", "308"),
	GP("Guadeloupe", "GP", "GLP", "312"),
	GU("Guam", "GU", "GUM", "316"),
	GT("Guatemala", "GT", "GTM", "320"),
	GG("Guernsey", "GG", "GGY", "831"),
	GW("Guinea-Bissau", "GW", "GNB", "624"), // before Guinea
	GN("Guinea", "GN", "GIN", "324"),
	GY("Guyana", "GY", "GUY", "328"),
	HT("Haiti", "HT", "HTI", "332"),
	HM("Heard Island and Mcdonald Islands", "HM", "HMD", "334"),
	VA("Holy See (Vatican City State)", "VA", "VAT", "336"),
	HN("Honduras", "HN", "HND", "340"),
	HU("Hungary", "HU", "HUN", "348"),
	IS("Iceland", "IS", "ISL", "352"),
	IN("India", "IN", "IND", "356"),
	ID("Indonesia", "ID", "IDN", "360"),
	IQ("Iraq", "IQ", "IRQ", "368"),
	IE("Ireland", "IE", "IRL", "372"),
	IM("Isle of Man", "IM", "IMN", "833"),
	IL("Israel", "IL", "ISR", "376"),
	IT("Italy", "IT", "ITA", "380"),
	JM("Jamaica", "JM", "JAM", "388"),
	JP("Japan", "JP", "JPN", "392"),
	JE("Jersey", "JE", "JEY", "832"),
	JO("Jordan", "JO", "JOR", "400"),
	KZ("Kazakhstan", "KZ", "KAZ", "398"),
	KE("Kenya", "KE", "KEN", "404"),
	KI("Kiribati", "KI", "KIR", "296"),
	KP("North Korea", "Democratic People's Republic of Korea", "KP", "PRK", "408", "Democratic People's Republic of Korea", "North Korea", "DPRK", "DPR Korea"),
	KR("South Korea", "Republic of Korea", "KR", "KOR", "410", "Republic of Korea", "South Korea"),
	KW("Kuwait", "KW", "KWT", "414"),
	KG("Kyrgyzstan", "KG", "KGZ", "417"),
	LA("Lao", "People's Democratic Republic of Lao", "LA", "LAO", "418", "Lao", "Laos"),
	LV("Latvia", "LV", "LVA", "428"),
	LB("Lebanon", "LB", "LBN", "422"),
	LS("Lesotho", "LS", "LSO", "426"),
	LR("Liberia", "LR", "LBR", "430"),
	LY("Libya", "LY", "LBY", "434"),
	LI("Liechtenstein", "LI", "LIE", "438"),
	LT("Lithuania", "LT", "LTU", "440"),
	LU("Luxembourg", "LU", "LUX", "442"),
	MK("Republic of Macedonia", "MK", "MKD", "807"),
	MG("Madagascar", "MG", "MDG", "450"),
	MW("Malawi", "MW", "MWI", "454"),
	MY("Malaysia", "MY", "MYS", "458"),
	MV("Maldives", "MV", "MDV", "462"),
	ML("Mali", "ML", "MLI", "466"),
	MT("Malta", "MT", "MLT", "470"),
	MH("Marshall Islands", "MH", "MHL", "584"),
	MQ("Martinique", "MQ", "MTQ", "474"),
	MR("Mauritania", "MR", "MRT", "478"),
	MU("Mauritius", "MU", "MUS", "480"),
	YT("Mayotte", "YT", "MYT", "175"),
	MX("Mexico", "MX", "MEX", "484"),
	FM("Federated States of Micronesia", "FM", "FSM", "583"),
	MD("Moldova", "MD", "MDA", "498"),
	MC("Monaco", "MC", "MCO", "492"),
	MN("Mongolia", "MN", "MNG", "496"),
	ME("Montenegro", "ME", "MNE", "499"),
	MS("Montserrat", "MS", "MSR", "500"),
	MA("Morocco", "MA", "MAR", "504"),
	MZ("Mozambique", "MZ", "MOZ", "508"),
	MM("Myanmar", "MM", "MMR", "104"),
	NA("Namibia", "NA", "NAM", "516"),
	NR("Nauru", "NR", "NRU", "520"),
	NP("Nepal", "NP", "NPL", "524"),
	AN("Netherlands Antilles", "AN", "ANT", "530"), // before Netherlands
	NL("Netherlands", "Netherlands", "NL", "NLD", "528", "Netherlands"),
	NC("New Caledonia", "NC", "NCL", "540"),
	NZ("New Zealand", "NZ", "NZL", "554"),
	NG("Nigeria", "NG", "NGA", "566"), // before Niger
	NE("Niger", "NE", "NER", "562"),
	NI("Nicaragua", "NI", "NIC", "558"),
	NU("Niue", "NU", "NIU", "570"),
	NF("Norfolk Island", "NF", "NFK", "574"),
	MP("Northern Mariana Islands", "MP", "MNP", "580"),
	NO("Norway", "NO", "NOR", "578"),
	OM("Oman", "OM", "OMN", "512"),
	PK("Pakistan", "PK", "PAK", "586"),
	PW("Palau", "PW", "PLW", "585"),
	PS("Palestinian Territory", "Palestinian Territory", "PS", "PSE", "275", "Palestine", "Palestinian [Tt]erritory", "West [Bb]ank", "Gaza"),
	PA("Panama", "PA", "PAN", "591"),
	PG("Papua New Guinea", "PG", "PNG", "598"),
	PY("Paraguay", "PY", "PRY", "600"),
	PE("Peru", "PE", "PER", "604"),
	PH("Philippines", "PH", "PHL", "608"),
	PN("Pitcairn", "PN", "PCN", "612"),
	PL("Poland", "PL", "POL", "616"),
	PT("Portugal", "PT", "PRT", "620"),
	PR("Puerto Rico", "PR", "PRI", "630"),
	QA("Qatar", "QA", "QAT", "634"),
	RE("Réunion", "Réunion", "RE", "REU", "638", "R[ée]union"),
	RO("Romania", "RO", "ROU", "642"),
	RU("Russia", "Russian Federation", "RU", "RUS", "643", "Russia"),
	RW("Rwanda", "RW", "RWA", "646"),
	BL("Saint-Barthélemy", "Saint-Barthélemy", "BL", "BLM", "652", "Saint-Barth[ée]lemy"),
	SH("Saint Helena", "SH", "SHN", "654"),
	KN("Saint Kitts and Nevis", "KN", "KNA", "659"),
	LC("Saint Lucia", "LC", "LCA", "662"),
	MF("Saint-Martin (French part)", "MF", "MAF", "663"),
	PM("Saint Pierre and Miquelon", "PM", "SPM", "666"),
	VC("Saint Vincent and Grenadines", "VC", "VCT", "670"),
	WS("Samoa", "WS", "WSM", "882"),
	SM("San Marino", "SM", "SMR", "674"),
	ST("Sao Tome and Principe", "ST", "STP", "678"),
	SA("Saudi Arabia", "SA", "SAU", "682"),
	SN("Senegal", "SN", "SEN", "686"),
	XK("Kosovo", "XK", "XKX", "???"), // partially recognized country, before Serbia
	RS("Serbia", "RS", "SRB", "688"),
	SC("Seychelles", "SC", "SYC", "690"),
	SL("Sierra Leone", "SL", "SLE", "694"),
	SG("Singapore", "SG", "SGP", "702"),
	SK("Slovakia", "SK", "SVK", "703"),
	SI("Slovenia", "SI", "SVN", "705"),
	SB("Solomon Islands", "SB", "SLB", "090"),
	SO("Somalia", "SO", "SOM", "706"),
	ZA("South Africa", "ZA", "ZAF", "710"),
	GS("South Georgia and the South Sandwich Islands", "GS", "SGS", "239"),
	SS("South Sudan", "SS", "SSD", "728"),
	ES("Spain", "ES", "ESP", "724"),
	LK("Sri Lanka", "LK", "LKA", "144"),
	SD("Sudan", "SD", "SDN", "736"),
	SR("Suriname", "SR", "SUR", "740"),
	SJ("Svalbard and Jan Mayen Islands", "SJ", "SJM", "744"),
	SZ("Swaziland", "SZ", "SWZ", "748"),
	SE("Sweden", "SE", "SWE", "752"),
	CH("Switzerland", "CH", "CHE", "756"),
	SY("Syria", "Syrian Arab Republic", "SY", "SYR", "760", "Syria"),
	TW("Taiwan", "Taiwan, Republic of China", "TW", "TWN", "158", "Taiwan"),
	TJ("Tajikistan", "TJ", "TJK", "762"),
	TZ("Tanzania", "United Republic of Tanzania", "TZ", "TZA", "834", "Tanzania"),
	TH("Thailand", "TH", "THA", "764"),
	TL("Timor-Leste", "TL", "TLS", "626"),
	TG("Togo", "TG", "TGO", "768"),
	TK("Tokelau", "TK", "TKL", "772"),
	TO("Tonga", "TO", "TON", "776"),
	TT("Trinidad and Tobago", "TT", "TTO", "780"),
	TN("Tunisia", "TN", "TUN", "788"),
	TR("Turkey", "TR", "TUR", "792"),
	TM("Turkmenistan", "TM", "TKM", "795"),
	TC("Turks and Caicos Islands", "TC", "TCA", "796"),
	TV("Tuvalu", "TV", "TUV", "798"),
	UG("Uganda", "UG", "UGA", "800"),
	UA("Ukraine", "UA", "UKR", "804"),
	AE("United Arab Emirates", "AE", "ARE", "784"),
	GB("United Kingdom", "GB", "GBR", "826"),
	UY("Uruguay", "UY", "URY", "858"),
	UZ("Uzbekistan", "UZ", "UZB", "860"),
	VU("Vanuatu", "VU", "VUT", "548"),
	VE("Venezuela", "Bolivarian Republic of Venezuela", "VE", "VEN", "862", "Venezuela"),
	VN("Vietnam", "Socialist Republic of Vietnam", "VN", "VNM", "704", "Viet ?[Nn]am"),
	VI("Virgin Islands, US", "VI", "VIR", "850"),
	WF("Wallis and Futuna Islands", "WF", "WLF", "876"),
	EH("Western Sahara", "EH", "ESH", "732"),
	YE("Yemen", "YE", "YEM", "887"),
	ZM("Zambia", "ZM", "ZMB", "894"),
	ZW("Zimbabwe", "ZW", "ZWE", "716");
	
	private String officialName;
	private String shortName;
	private Pattern[] patterns;
	private String alpha2;
	private String alpha3;
	private String numeric;

	private IsoCountry(String name, String alpha2, String alpha3, String numeric) {
		this(name, name, alpha2, alpha3, numeric, Pattern.quote(name));
	}		
	private IsoCountry(String shortName, String officialName, String alpha2, String alpha3, String numeric, String ... patternStrings) {
		if(patternStrings.length == 0) {
			throw new RuntimeException("No patterns for ISO country \""+shortName+"\"");
		}
		this.patterns = new Pattern[patternStrings.length];
		for(int i = 0; i < patternStrings.length; i++) {
			patterns[i] = Pattern.compile(patternStrings[i]);
		}
		this.shortName = shortName;
		this.officialName = officialName;
		this.alpha2 = alpha2;
		this.alpha3 = alpha3;
		this.numeric = numeric;
	}

	public String getOfficialName() {
		return officialName;
	}
	public String getShortName() {
		return shortName;
	}
	public Pattern[] getPatterns() {
		return patterns;
	}
	public String getAlpha2() {
		return alpha2;
	}
	public String getAlpha3() {
		return alpha3;
	}
	public String getNumeric() {
		return numeric;
	}
}