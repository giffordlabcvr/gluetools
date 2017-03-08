package uk.ac.gla.cvr.gluetools.utils;

import java.util.regex.Pattern;

public enum IsoCountry {
	
	AF("AFG", "Afghanistan", "AF", "004"),
	AX("ALA", "Aland Islands", "AX", "248"),
	AL("ALB", "Albania", "AL", "008"),
	DZ("DZA", "Algeria", "DZ", "012"),
	AS("ASM", "American Samoa", "AS", "016"),
	AD("AND", "Andorra", "AD", "020"),
	AO("AGO", "Angola", "AO", "024"),
	AI("AIA", "Anguilla", "AI", "660"),
	AQ("ATA", "Antarctica", "AQ", "010"),
	AG("ATG", "Antigua and Barbuda", "AG", "028"),
	AR("ARG", "Argentina", "AR", "032"),
	AM("ARM", "Armenia", "AM", "051"),
	AW("ABW", "Aruba", "AW", "533"),
	AU("AUS", "Australia", "AU", "036"),
	AT("AUT", "Austria", "AT", "040"),
	IR("IRN", "Iran", "Islamic Republic of Iran", "IR", "364","Iran"), // before Azerbaijan, because of the Iranian territory
	AZ("AZE", "Azerbaijan", "AZ", "031"),
	BS("BHS", "Bahamas", "BS", "044"),
	BH("BHR", "Bahrain", "BH", "048"),
	BD("BGD", "Bangladesh", "BD", "050"),
	BB("BRB", "Barbados", "BB", "052"),
	BY("BLR", "Belarus", "BY", "112"),
	BE("BEL", "Belgium", "BE", "056"),
	BZ("BLZ", "Belize", "BZ", "084"),
	BJ("BEN", "Benin", "BJ", "204"),
	BM("BMU", "Bermuda", "BM", "060"),
	BT("BTN", "Bhutan", "BT", "064"),
	BO("BOL", "Bolivia", "BO", "068"),
	BA("BIH", "Bosnia and Herzegovina", "BA", "070"),
	BW("BWA", "Botswana", "BW", "072"),
	BV("BVT", "Bouvet Island", "BV", "074"),
	BR("BRA", "Brazil", "BR", "076"),
	VG("VGB", "British Virgin Islands", "VG", "092"),
	IO("IOT", "British Indian Ocean Territory", "IO", "086"),
	BN("BRN", "Brunei Darussalam", "BN", "096"),
	BG("BGR", "Bulgaria", "BG", "100"),
	BF("BFA", "Burkina Faso", "BF", "854"),
	BI("BDI", "Burundi", "BI", "108"),
	KH("KHM", "Cambodia", "KH", "116"),
	CM("CMR", "Cameroon", "CM", "120"),
	CA("CAN", "Canada", "CA", "124"),
	CV("CPV", "Cape Verde", "CV", "132"),
	KY("CYM", "Cayman Islands", "KY", "136"),
	CF("CAF", "Central African Republic", "CF", "140"),
	TD("TCD", "Chad", "TD", "148"),
	CL("CHL", "Chile", "CL", "152"),
	CN("CHN", "China", "CN", "156"),
	HK("HKG", "Hong Kong", "Hong Kong, Special Administrative Region of China", "HK", "344", "Hong Kong"),
	MO("MAC", "Macao", "Macao, Special Administrative Region of China", "MO", "446", "Macao"),
	CX("CXR", "Christmas Island", "CX", "162"),
	CC("CCK", "Cocos (Keeling) Islands", "CC", "166"),
	CO("COL", "Colombia", "CO", "170"),
	KM("COM", "Comoros", "KM", "174"),
	CG("COG", "Congo (Brazzaville)", "CG", "178"),
	CD("COD", "Democratic Republic of the Congo", "Democratic Republic of the Congo", "CD", "180", "Congo", "DRC"),
	CK("COK", "Cook Islands", "CK", "184"),
	CR("CRI", "Costa Rica", "CR", "188"),
	CI("CIV", "Côte d'Ivoire", "Côte d'Ivoire", "CI", "384", "C[ôo]te d'Ivoire"),
	HR("HRV", "Croatia", "HR", "191"),
	CU("CUB", "Cuba", "CU", "192"),
	CY("CYP", "Cyprus", "CY", "196"),
	CZ("CZE", "Czech Republic", "CZ", "203"),
	DK("DNK", "Denmark", "DK", "208"),
	DJ("DJI", "Djibouti", "DJ", "262"),
	DM("DMA", "Dominica", "DM", "212"),
	DO("DOM", "Dominican Republic", "DO", "214"),
	EC("ECU", "Ecuador", "EC", "218"),
	EG("EGY", "Egypt", "EG", "818"),
	SV("SLV", "El Salvador", "SV", "222"),
	GQ("GNQ", "Equatorial Guinea", "GQ", "226"),
	ER("ERI", "Eritrea", "ER", "232"),
	EE("EST", "Estonia", "EE", "233"),
	ET("ETH", "Ethiopia", "ET", "231"),
	FK("FLK", "Falkland Islands", "FK", "238"),
	FO("FRO", "Faroe Islands", "FO", "234"),
	FJ("FJI", "Fiji", "FJ", "242"),
	FI("FIN", "Finland", "FI", "246"),
	GF("GUF", "French Guiana", "French Guiana", "GF", "254", "Guyane", "French Guiana"), // before France
	FR("FRA", "France", "FR", "250"),
	PF("PYF", "French Polynesia", "PF", "258"),
	TF("ATF", "French Southern Territories", "TF", "260"),
	GA("GAB", "Gabon", "GA", "266"),
	GM("GMB", "Gambia", "GM", "270"),
	UM("UMI", "United States Minor Outlying Islands", "UM", "581"), // before United States
	US("USA", "United States", "United States of America", "US", "840", "United States of America", "United States", "USA"), // before Georgia, because of the state
	GE("GEO", "Georgia", "GE", "268"),
	DE("DEU", "Germany", "DE", "276"),
	GH("GHA", "Ghana", "GH", "288"),
	GI("GIB", "Gibraltar", "GI", "292"),
	GR("GRC", "Greece", "GR", "300"),
	GL("GRL", "Greenland", "GL", "304"),
	GD("GRD", "Grenada", "GD", "308"),
	GP("GLP", "Guadeloupe", "GP", "312"),
	GU("GUM", "Guam", "GU", "316"),
	GT("GTM", "Guatemala", "GT", "320"),
	GG("GGY", "Guernsey", "GG", "831"),
	GW("GNB", "Guinea-Bissau", "GW", "624"), // before Guinea
	GN("GIN", "Guinea", "GN", "324"),
	GY("GUY", "Guyana", "GY", "328"),
	HT("HTI", "Haiti", "HT", "332"),
	HM("HMD", "Heard Island and Mcdonald Islands", "HM", "334"),
	VA("VAT", "Holy See (Vatican City State)", "VA", "336"),
	HN("HND", "Honduras", "HN", "340"),
	HU("HUN", "Hungary", "HU", "348"),
	IS("ISL", "Iceland", "IS", "352"),
	IN("IND", "India", "IN", "356"),
	ID("IDN", "Indonesia", "ID", "360"),
	IQ("IRQ", "Iraq", "IQ", "368"),
	IE("IRL", "Ireland", "IE", "372"),
	IM("IMN", "Isle of Man", "IM", "833"),
	IL("ISR", "Israel", "IL", "376"),
	IT("ITA", "Italy", "IT", "380"),
	JM("JAM", "Jamaica", "JM", "388"),
	JP("JPN", "Japan", "JP", "392"),
	JE("JEY", "Jersey", "JE", "832"),
	JO("JOR", "Jordan", "JO", "400"),
	KZ("KAZ", "Kazakhstan", "KZ", "398"),
	KE("KEN", "Kenya", "KE", "404"),
	KI("KIR", "Kiribati", "KI", "296"),
	KP("PRK", "North Korea", "Democratic People's Republic of Korea", "KP", "408", "Democratic People's Republic of Korea", "North Korea", "DPRK", "DPR Korea"),
	KR("KOR", "South Korea", "Republic of Korea", "KR", "410", "Republic of Korea", "Korea, Republic of", "South Korea"),
	KW("KWT", "Kuwait", "KW", "414"),
	KG("KGZ", "Kyrgyzstan", "KG", "417"),
	LA("LAO", "Lao", "People's Democratic Republic of Lao", "LA", "418", "Lao", "Laos"),
	LV("LVA", "Latvia", "LV", "428"),
	LB("LBN", "Lebanon", "LB", "422"),
	LS("LSO", "Lesotho", "LS", "426"),
	LR("LBR", "Liberia", "LR", "430"),
	LY("LBY", "Libya", "LY", "434"),
	LI("LIE", "Liechtenstein", "LI", "438"),
	LT("LTU", "Lithuania", "LT", "440"),
	LU("LUX", "Luxembourg", "LU", "442"),
	MK("MKD", "Republic of Macedonia", "MK", "807"),
	MG("MDG", "Madagascar", "MG", "450"),
	MW("MWI", "Malawi", "MW", "454"),
	MY("MYS", "Malaysia", "MY", "458"),
	MV("MDV", "Maldives", "MV", "462"),
	ML("MLI", "Mali", "ML", "466"),
	MT("MLT", "Malta", "MT", "470"),
	MH("MHL", "Marshall Islands", "MH", "584"),
	MQ("MTQ", "Martinique", "MQ", "474"),
	MR("MRT", "Mauritania", "MR", "478"),
	MU("MUS", "Mauritius", "MU", "480"),
	YT("MYT", "Mayotte", "YT", "175"),
	MX("MEX", "Mexico", "MX", "484"),
	FM("FSM", "Federated States of Micronesia", "FM", "583"),
	MD("MDA", "Moldova", "MD", "498"),
	MC("MCO", "Monaco", "MC", "492"),
	MN("MNG", "Mongolia", "MN", "496"),
	ME("MNE", "Montenegro", "ME", "499"),
	MS("MSR", "Montserrat", "MS", "500"),
	MA("MAR", "Morocco", "MA", "504"),
	MZ("MOZ", "Mozambique", "MZ", "508"),
	MM("MMR", "Myanmar", "MM", "104"),
	NA("NAM", "Namibia", "NA", "516"),
	NR("NRU", "Nauru", "NR", "520"),
	NP("NPL", "Nepal", "NP", "524"),
	AN("ANT", "Netherlands Antilles", "AN", "530"), // before Netherlands
	NL("NLD", "Netherlands", "Netherlands", "NL", "528", "Netherlands"),
	NC("NCL", "New Caledonia", "NC", "540"),
	NZ("NZL", "New Zealand", "NZ", "554"),
	NG("NGA", "Nigeria", "NG", "566"), // before Niger
	NE("NER", "Niger", "NE", "562"),
	NI("NIC", "Nicaragua", "NI", "558"),
	NU("NIU", "Niue", "NU", "570"),
	NF("NFK", "Norfolk Island", "NF", "574"),
	MP("MNP", "Northern Mariana Islands", "MP", "580"),
	NO("NOR", "Norway", "NO", "578"),
	OM("OMN", "Oman", "OM", "512"),
	PK("PAK", "Pakistan", "PK", "586"),
	PW("PLW", "Palau", "PW", "585"),
	PS("PSE", "Palestinian Territory", "Palestinian Territory", "PS", "275", "Palestine", "Palestinian [Tt]erritory", "West [Bb]ank", "Gaza"),
	PA("PAN", "Panama", "PA", "591"),
	PG("PNG", "Papua New Guinea", "PG", "598"),
	PY("PRY", "Paraguay", "PY", "600"),
	PE("PER", "Peru", "PE", "604"),
	PH("PHL", "Philippines", "PH", "608"),
	PN("PCN", "Pitcairn", "PN", "612"),
	PL("POL", "Poland", "PL", "616"),
	PT("PRT", "Portugal", "PT", "620"),
	PR("PRI", "Puerto Rico", "PR", "630"),
	QA("QAT", "Qatar", "QA", "634"),
	RE("REU", "Réunion", "Réunion", "RE", "638", "R[ée]union"),
	RO("ROU", "Romania", "RO", "642"),
	RU("RUS", "Russia", "Russian Federation", "RU", "643", "Russia"),
	RW("RWA", "Rwanda", "RW", "646"),
	BL("BLM", "Saint-Barthélemy", "Saint-Barthélemy", "BL", "652", "Saint-Barth[ée]lemy"),
	SH("SHN", "Saint Helena", "SH", "654"),
	KN("KNA", "Saint Kitts and Nevis", "KN", "659"),
	LC("LCA", "Saint Lucia", "LC", "662"),
	MF("MAF", "Saint-Martin (French part)", "MF", "663"),
	PM("SPM", "Saint Pierre and Miquelon", "PM", "666"),
	VC("VCT", "Saint Vincent and Grenadines", "VC", "670"),
	WS("WSM", "Samoa", "WS", "882"),
	SM("SMR", "San Marino", "SM", "674"),
	ST("STP", "Sao Tome and Principe", "ST", "678"),
	SA("SAU", "Saudi Arabia", "SA", "682"),
	SN("SEN", "Senegal", "SN", "686"),
	XK("XKX", "Kosovo", "XK", "???"), // partially recognized country, before Serbia
	RS("SRB", "Serbia", "RS", "688"),
	SC("SYC", "Seychelles", "SC", "690"),
	SL("SLE", "Sierra Leone", "SL", "694"),
	SG("SGP", "Singapore", "SG", "702"),
	SK("SVK", "Slovakia", "SK", "703"),
	SI("SVN", "Slovenia", "SI", "705"),
	SB("SLB", "Solomon Islands", "SB", "090"),
	SO("SOM", "Somalia", "SO", "706"),
	ZA("ZAF", "South Africa", "ZA", "710"),
	GS("SGS", "South Georgia and the South Sandwich Islands", "GS", "239"),
	SS("SSD", "South Sudan", "SS", "728"),
	ES("ESP", "Spain", "ES", "724"),
	LK("LKA", "Sri Lanka", "LK", "144"),
	SD("SDN", "Sudan", "SD", "736"),
	SR("SUR", "Suriname", "SR", "740"),
	SJ("SJM", "Svalbard and Jan Mayen Islands", "SJ", "744"),
	SZ("SWZ", "Swaziland", "SZ", "748"),
	SE("SWE", "Sweden", "SE", "752"),
	CH("CHE", "Switzerland", "CH", "756"),
	SY("SYR", "Syria", "Syrian Arab Republic", "SY", "760", "Syria"),
	TW("TWN", "Taiwan", "Taiwan, Republic of China", "TW", "158", "Taiwan"),
	TJ("TJK", "Tajikistan", "TJ", "762"),
	TZ("TZA", "Tanzania", "United Republic of Tanzania", "TZ", "834", "Tanzania"),
	TH("THA", "Thailand", "TH", "764"),
	TL("TLS", "Timor-Leste", "TL", "626"),
	TG("TGO", "Togo", "TG", "768"),
	TK("TKL", "Tokelau", "TK", "772"),
	TO("TON", "Tonga", "TO", "776"),
	TT("TTO", "Trinidad and Tobago", "TT", "780"),
	TN("TUN", "Tunisia", "TN", "788"),
	TR("TUR", "Turkey", "TR", "792"),
	TM("TKM", "Turkmenistan", "TM", "795"),
	TC("TCA", "Turks and Caicos Islands", "TC", "796"),
	TV("TUV", "Tuvalu", "TV", "798"),
	UG("UGA", "Uganda", "UG", "800"),
	UA("UKR", "Ukraine", "UA", "804"),
	AE("ARE", "United Arab Emirates", "AE", "784"),
	GB("GBR", "United Kingdom", "GB", "826"),
	UY("URY", "Uruguay", "UY", "858"),
	UZ("UZB", "Uzbekistan", "UZ", "860"),
	VU("VUT", "Vanuatu", "VU", "548"),
	VE("VEN", "Venezuela", "Bolivarian Republic of Venezuela", "VE", "862", "Venezuela"),
	VN("VNM", "Vietnam", "Socialist Republic of Vietnam", "VN", "704", "Viet ?[Nn]am"),
	VI("VIR", "Virgin Islands, US", "VI", "850"),
	WF("WLF", "Wallis and Futuna Islands", "WF", "876"),
	EH("ESH", "Western Sahara", "EH", "732"),
	YE("YEM", "Yemen", "YE", "887"),
	ZM("ZMB", "Zambia", "ZM", "894"),
	ZW("ZWE", "Zimbabwe", "ZW", "716");
	
	private String officialName;
	private String shortName;
	private Pattern[] patterns;
	private String alpha2;
	private String alpha3;
	private String numeric;

	private IsoCountry(String alpha3, String name, String alpha2, String numeric) {
		this(alpha3, name, name, alpha2, numeric, Pattern.quote(name.toUpperCase()));
	}		
	private IsoCountry(String alpha3, String shortName, String officialName, String alpha2, String numeric, String ... patternStrings) {
		if(patternStrings.length == 0) {
			throw new RuntimeException("No patterns for ISO country \""+shortName+"\"");
		}
		this.patterns = new Pattern[(patternStrings.length)];
		for(int i = 0; i < patternStrings.length; i++) {
			patterns[i] = Pattern.compile(patternStrings[i], Pattern.CASE_INSENSITIVE);
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