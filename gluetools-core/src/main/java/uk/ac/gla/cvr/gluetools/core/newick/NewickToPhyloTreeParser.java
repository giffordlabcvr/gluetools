/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.newick;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.newick.NewickLexer.Token;
import uk.ac.gla.cvr.gluetools.core.newick.NewickLexer.TokenType;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

public class NewickToPhyloTreeParser {

	private NewickInterpreter newickInterpreter;
	
	public NewickToPhyloTreeParser() {
		this(new NewickInterpreter() {});
	}
	
	public NewickToPhyloTreeParser(NewickInterpreter newickInterpreter) {
		this.newickInterpreter = newickInterpreter;
	}

	public PhyloTree parseNewick(String newickString) {
		ArrayList<Token> tokens = NewickLexer.lex(newickString);
		List<Token> meaningfulTokens = NewickLexer.meaningfulTokens(tokens);

		int numTokens = meaningfulTokens.size();
		// delete :0.0 before final semicolon, allowing us to accept the Tree -> Branch production as long as it is a zero-length branch
		if(numTokens >= 3) {
			if(meaningfulTokens.get(numTokens-1).getType() == TokenType.SEMICOLON &&
					meaningfulTokens.get(numTokens-2).getType() == TokenType.NUMBER && 
					meaningfulTokens.get(numTokens-3).getType() == TokenType.COLON) {
				if(Double.parseDouble(meaningfulTokens.get(numTokens-2).getData()) == 0.0) {
					meaningfulTokens.remove(meaningfulTokens.size()-2);
					meaningfulTokens.remove(meaningfulTokens.size()-2);
				} else {
					throw new PhyloNewickException(PhyloNewickException.Code.FORMAT_ERROR, "Can parse Tree->Branch production but only if root branch is of length 0.0");
				}
			}
		}
		
		LinkedList<State> stateStack = new LinkedList<State>();
		TreeState treeState = new TreeState();
		push(null, stateStack, treeState);
		
		for(Token token: meaningfulTokens) {
			// System.out.println("TOKEN: "+token);
			stateStack.peek().consume(stateStack, token);
		}
		if(!stateStack.isEmpty()) {
			throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR, null, null, null);
		}
		return treeState.phyloTree;
	}
	
	
	private abstract class State {
		public void consume(LinkedList<State> stateStack, Token token) {
			throw new PhyloNewickException(PhyloNewickException.Code.UNEXPECTED_NEWICK_TOKEN, token);
		}
	}
	
	private void assertTrue(Token token, boolean proposition) {
		String tokenType = "unknown";
		String tokenValue = "unknown";
		String position = "unknown";
		if(token != null) {
			tokenType = token.getType().name();
			tokenValue = token.render();
			position = Integer.toString(token.getPosition());
		}
		if(!proposition) {
			throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR, tokenType, tokenValue, position);
		}
	}

	private void push(Token token, LinkedList<State> stateStack, State state) {
		//System.out.println("PUSH: "+state.getClass().getSimpleName());
		stateStack.push(state);
	}
	
	private <S extends State> S pop(Token token, LinkedList<State> stateStack, Class<S> theClass) {
		assertTrue(token, !stateStack.isEmpty());
		State state = stateStack.pop();
		//System.out.println("POP: "+state.getClass().getSimpleName());
		assertTrue(token, theClass.isAssignableFrom(state.getClass()));
		return theClass.cast(state);
	}

	private <S extends State> S peek(Token token, LinkedList<State> stateStack, Class<S> theClass) {
		assertTrue(token, !stateStack.isEmpty());
		State state = stateStack.peek();
		assertTrue(token, theClass.isAssignableFrom(state.getClass()));
		return theClass.cast(state);
	}

	
	private class TreeState extends State {
		PhyloTree phyloTree = new PhyloTree();

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case LEFTPAREN:
				assertTrue(token, phyloTree.getRoot() == null);
				InternalState internalState = new InternalState();
				phyloTree.setRoot(internalState.internal);
				push(token, stateStack, internalState);
				internalState.consume(stateStack, token);
				break;
			case NAME:
				assertTrue(token, phyloTree.getRoot() == null);
				LeafState leafState = new LeafState();
				phyloTree.setRoot(leafState.leaf);
				push(token, stateStack, leafState);
				leafState.consume(stateStack, token);
				break;
			case SEMICOLON:
				if(phyloTree.getRoot() == null) {
					phyloTree.setRoot(new PhyloLeaf());
				}
				pop(token, stateStack, TreeState.class);
				break;
			default:
				String tokenType = token.getType().name();
				String tokenValue = token.render();
				String position = Integer.toString(token.getPosition());
				throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR, tokenType, tokenValue, position);
			}
		}

	}
	
	private class InternalState extends State {
		PhyloInternal internal = new PhyloInternal();
		boolean allBranches = false;
		boolean nameSet = false;

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case LEFTPAREN:
				assertTrue(token, internal.getBranches().size() == 0); 
				assertTrue(token, !allBranches);
				{
					BranchState branchState = new BranchState();
					push(token, stateStack, branchState);
					internal.addBranch(branchState.branch);
				}
				break;
			case COMMA:
				assertTrue(token, internal.getBranches().size() > 0);
				if(allBranches) {
					pop(token, stateStack, InternalState.class);
					peek(token, stateStack, BranchState.class).consume(stateStack, token);
				} else {
					BranchState branchState = new BranchState();
					push(token, stateStack, branchState);
					internal.addBranch(branchState.branch);
				}
				break;
			case RIGHTPAREN:
				assertTrue(token, internal.getBranches().size() > 0);
				if(allBranches) {
					pop(token, stateStack, InternalState.class);
					peek(token, stateStack, BranchState.class).consume(stateStack, token);
				} else {
					allBranches = true;
				}
				break;
			case NAME:
				assertTrue(token, allBranches);
				assertTrue(token, !nameSet);
				newickInterpreter.parseInternalName(internal, token.render());
				this.nameSet = true;
				pop(token, stateStack, InternalState.class);
				break;
			case NUMBER:
				assertTrue(token, allBranches);
				assertTrue(token, !nameSet);
				newickInterpreter.parseInternalName(internal, token.render());
				this.nameSet = true;
				pop(token, stateStack, InternalState.class);
				break;
			default:
				assertTrue(token, allBranches);
				pop(token, stateStack, InternalState.class);
				peek(token, stateStack, State.class).consume(stateStack, token);
				break;
			}
		}
	}

	private class BranchState extends State {
		PhyloBranch branch = new PhyloBranch();
		boolean colon = false;
		boolean number = false;
		boolean comment = false;
		boolean branchLabel = false;

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case LEFTPAREN:
				assertTrue(token, branch.getSubtree() == null);
				assertTrue(token, !colon);
				InternalState internalState = new InternalState();
				branch.setSubtree(internalState.internal);
				push(token, stateStack, internalState);
				internalState.consume(stateStack, token);
				break;
			case NAME:
				assertTrue(token, branch.getSubtree() == null);
				assertTrue(token, !colon);
				LeafState leafState = new LeafState();
				branch.setSubtree(leafState.leaf);
				push(token, stateStack, leafState);
				leafState.consume(stateStack, token);
				break;
			case COLON:
				assertTrue(token, !colon);
				if(branch.getSubtree() == null) {
					branch.setSubtree(new PhyloLeaf());
				}
				colon = true;
				break;
			case NUMBER: 
				assertTrue(token, colon);
				assertTrue(token, !number);
				newickInterpreter.parseBranchLength(branch, token.render());
				number = true;
				break;
			case COMMENT: 
				assertTrue(token, number || !colon);
				assertTrue(token, !comment);
				newickInterpreter.parseBranchComment(branch, token.render());
				comment = true;
				break;
			case BRANCHLABEL: 
				assertTrue(token, number || !colon);
				assertTrue(token, !branchLabel);
				newickInterpreter.parseBranchLabel(branch, token.render());
				branchLabel = true;
				break;
			case COMMA: 
			case RIGHTPAREN: 
				assertTrue(token, number || !colon);
				if(branch.getSubtree() == null) {
					branch.setSubtree(new PhyloLeaf());
				}
				pop(token, stateStack, BranchState.class);
				peek(token, stateStack, InternalState.class).consume(stateStack, token);;
				break;
			default:
				throw new PhyloNewickException(PhyloNewickException.Code.PARSE_ERROR);
			}
		}

	}

	
	private class LeafState extends State {
		PhyloLeaf leaf = new PhyloLeaf();
		private boolean nameSet = false;

		@Override
		public void consume(LinkedList<State> stateStack, Token token) {
			switch(token.getType()) {
			case NAME:
				assertTrue(token, !nameSet);
				newickInterpreter.parseLeafName(leaf, token.render());
				nameSet = true;
				pop(token, stateStack, LeafState.class);
				break;
			case SEMICOLON:
				pop(token, stateStack, LeafState.class);
				peek(token, stateStack, TreeState.class).consume(stateStack, token);
				break;
			case COMMA:
			case RIGHTPAREN:
				pop(token, stateStack, LeafState.class);
				peek(token, stateStack, InternalState.class).consume(stateStack, token);
				break;
			default:
				super.consume(stateStack, token);
			}
		}
	}
	
	private static String phyloTreeToNewick(PhyloTree phyloTree) {
		PhyloTreeToNewickGenerator newickPhyloTreeVisitor = new PhyloTreeToNewickGenerator();
		phyloTree.accept(newickPhyloTreeVisitor);
		return newickPhyloTreeVisitor.getNewickString();
	}
	


	private void test(String input) {
		System.out.println("Input/output:\n"+input);
		PhyloTree result = parseNewick(input);
		System.out.println(phyloTreeToNewick(result));
		System.out.println("------");
	}

	
	public static void main(String[] args) {
		NewickToPhyloTreeParser parser = new NewickToPhyloTreeParser();
		parser.test("(,,(,));");
		parser.test("(A,B,(C,D));");
		parser.test("(A,B,(C,D)E)F;");
		parser.test("(:0.1,:0.2,(:0.3,:0.4):0.5);");
		// support the Tree -> Branch ; production, but only if root branch is of length 0.
		parser.test("(:0.1,:0.2,(:0.3,:0.4):0.5):0.0;");
		// non length zero example
		// parser.test("(:0.1,:0.2,(:0.3,:0.4):0.5):0.000000000000001;");
		parser.test("(A:0.1,B:0.2,(C:0.3,D:0.4):0.5);");
		parser.test("(A:0.1,B:0.2,(C:0.3,D:0.4)E:0.5)F;");
		parser.test("((B:0.2,(C:0.3,D:0.4)E:0.5)F:0.1)A;");
		parser.test("((B:0.2,(C:0.3,D:0.4)100:0.5)0:0.1)100;");
		parser.test("((C:4,D:2):3,(E:5,G:3):2);");
		parser.test("((B:0.2[Comm1],(C:0.3[Comm2],D:0.4)E:0.5)F:0.1[Comm3])A;");
		parser.test("(member/ncbi-refseqs/KM252792:0.01863094234626353971[I0],((member/ncbi-refseqs/EF108306:0.83244915133851027367[I3],(((((((((((((((((((((member/hcv-additional-refs/AF511950:0.04206220364114309634[I25],member/ncbi-refseqs/AF009606:0.01592292013236466450[I26]):0.00000100000050002909[I24],member/ncbi-refseqs/EF407457:0.05233399796960445244[I27]):0.00000100000050002909[I23],member/ncbi-refseqs/HQ850279:0.06978035123034541376[I28]):0.00010055513749021910[I22],member/ncbi-refseqs/M62321:0.03034179361075641304[I29]):0.00160547628816843052[I21],member/ncbi-refseqs/M67463:0.02254220878957092361[I30]):0.20470709150411237065[I20],((((((member/hcv-additional-refs/AY587016:0.05786148900646046617[I37],member/hcv-additional-refs/D11355:0.05806504210367680746[I38]):0.00378690405913181686[I36],member/hcv-additional-refs/EF032892:0.06132208330925192213[I39]):0.00209751564258228501[I35],member/ncbi-refseqs/D90208:0.05825945655072565371[I40]):0.00293417185604461731[I34],member/ncbi-refseqs/EU781827:0.04336382635844195088[I41]):0.00420691781536202975[I33],member/ncbi-refseqs/EU781828:0.05487550162120769653[I42]):0.00816521433530948901[I32],member/ncbi-refseqs/M58335:0.05275475005072843837[I43]):0.15283166504620673831[I31]):0.00000100000050002909[I19],((member/ncbi-refseqs/AY051292:0.03391842178073233016[I46],member/ncbi-refseqs/AY651061:0.07329390924926336426[I47]):0.01499803996744142158[I45],member/ncbi-refseqs/D14853:0.03518607411440680816[I48]):0.22450441658266812039[I44]):0.00000100000050002909[I18],member/ncbi-refseqs/KJ439768:0.19225048067914699002[I49]):0.01441716320982227607[I17],member/ncbi-refseqs/KC248194:0.26065022905000384545[I50]):0.00000100000050002909[I16],member/ncbi-refseqs/AM910652:0.25399821247388060241[I51]):0.00039158198950655780[I15],(member/ncbi-refseqs/KC248198:0.04469943777007186203[I53],member/ncbi-refseqs/KC248199:0.05037754869788137885[I54]):0.18354536801350193609[I52]):0.00113281806647946696[I14],member/ncbi-refseqs/KJ439772:0.19868997449726033544[I55]):0.08945940100525619221[I13],member/ncbi-refseqs/KJ439773:0.14595715876222170593[I56]):0.00351469112872792580[I12],member/ncbi-refseqs/KJ439774:0.13439532826103464669[I57]):0.00203444301761748509[I11],((member/ncbi-refseqs/KC248193:0.03930837725959639845[I60],member/ncbi-refseqs/KC248196:0.04723089767624426977[I61]):0.01695537258799994715[I59],member/ncbi-refseqs/KC248197:0.02539108934153057676[I62]):0.28380617722949941228[I58]):0.00918900789533876571[I10],(member/ncbi-refseqs/KJ439778:0.08206019308790600542[I64],member/ncbi-refseqs/KJ439782:0.09594645689536697442[I65]):0.07660351132212660230[I63]):0.00762992109047667087[I9],(member/ncbi-refseqs/KJ439775:0.09702377182270001399[I67],member/ncbi-refseqs/KJ439781:0.10764477744582622021[I68]):0.05552204324295562793[I66]):0.25210223750025290146[I8],((((((((((((((((member/hcv-additional-refs/AY746460:0.07738119124209555388[I85],member/ncbi-refseqs/AB047639:0.08096754470457676045[I86]):0.00037794821596670288[I84],member/ncbi-refseqs/D00944:0.06228343223382937915[I87]):0.00336017725379235887[I83],member/ncbi-refseqs/HQ639944:0.06233962779227633644[I88]):0.12919685459814242434[I82],(((member/ncbi-refseqs/AB030907:0.05296219613691313666[I92],member/ncbi-refseqs/AB661382:0.07094826081527719208[I93]):0.00557039868476834604[I91],member/ncbi-refseqs/AB661388:0.07440212031625273448[I94]):0.00137502572567530239[I90],member/ncbi-refseqs/D10988:0.04218747048682949113[I95]):0.26807370025398763458[I89]):0.03427509154359992954[I81],(member/ncbi-refseqs/D50409:0.06268806730523544812[I97],member/ncbi-refseqs/JX227949:0.06736935824179858534[I98]):0.11276956044088938536[I96]):0.00694064081704182968[I80],member/ncbi-refseqs/JF735114:0.14954344783623488291[I99]):0.00320050293086175924[I79],member/ncbi-refseqs/JF735120:0.19222018475187233055[I100]):0.00000100000050002909[I78],(member/ncbi-refseqs/KC844042:0.06469067361819014206[I102],member/ncbi-refseqs/KC844050:0.05521183305520577927[I103]):0.11123240855223946733[I101]):0.01162345667824307358[I77],member/ncbi-refseqs/DQ155561:0.17386681660502550928[I104]):0.00907597319046622025[I76],((member/ncbi-refseqs/HM777358:0.06027783035472877166[I107],member/ncbi-refseqs/HM777359:0.05469284076159657698[I108]):0.01096359874596435947[I106],member/ncbi-refseqs/JF735113:0.08127497075625991774[I109]):0.12633662474850143598[I105]):0.03468758262189636926[I75],(member/ncbi-refseqs/AB031663:0.05714593416352850563[I111],member/ncbi-refseqs/JX227953:0.06094470580273043253[I112]):0.12637257858729109006[I110]):0.00092823497170964765[I74],(member/ncbi-refseqs/JF735111:0.04749054108256575701[I114],member/ncbi-refseqs/JX227967:0.04552305190832182935[I115]):0.13582593572265225235[I113]):0.00284090405546721806[I73],(member/ncbi-refseqs/FN666428:0.05866265472646167017[I117],member/ncbi-refseqs/FN666429:0.05352558118817610233[I118]):0.11867886003060021394[I116]):0.01434015658867470421[I72],member/ncbi-refseqs/JF735115:0.17607446549256003387[I119]):0.01970464868427389271[I71],member/ncbi-refseqs/KC197238:0.30019488581939740568[I120]):0.01332778886097100816[I70],member/ncbi-refseqs/JF735112:0.14301570133535956852[I121]):0.55253647976728492708[I69]):0.04949760153591974132[I7],((((((((QUERY___query1:0.029573,((((member/hcv-additional-refs/NC_009824:0.00000100000050002909[I133],member/ncbi-refseqs/D17763:0.00000100000050002909[I134]):0.02822853038357158936[I132],member/ncbi-refseqs/D28917:0.04841808397611693793[I135]):0.00485472386402539206[I131],member/ncbi-refseqs/JN714194:0.08539958516146799183[I136]):0.00241491362363128450[I130],member/ncbi-refseqs/X76918:0.02934451066229874089[I137]):0.07678640041306139530[I129]):0.07678640041306139530[I129],(member/ncbi-refseqs/D49374:0.07703578275700408151[I139],member/ncbi-refseqs/JQ065709:0.06958687548490775920[I140]):0.16124645213285154766[I138]):0.00400752061310971630[I128],member/ncbi-refseqs/KJ470619:0.18150706276258490868[I141]):0.00254494346570026224[I127],member/ncbi-refseqs/KJ470618:0.22623928380401658877[I142]):0.04819035204900033037[I126],(member/ncbi-refseqs/JF735123:0.07501566657349267864[I144],member/ncbi-refseqs/JX227954:0.05214262611096715655[I145]):0.12455816310977699257[I143]):0.02433331634299859894[I125],(member/ncbi-refseqs/JF735121:0.06048256497965733003[I147],member/ncbi-refseqs/JF735126:0.03590886895515862237[I148]):0.48859668133232880827[I146]):0.00000100000050002909[I124],(member/ncbi-refseqs/FJ407092:0.03057207333017111436[I150],member/ncbi-refseqs/JX227955:0.02646073795154670408[I151]):0.13167366471449759580[I149]):0.10567554553372811299[I123],(member/ncbi-refseqs/D63821:0.05308213258536979839[I153],member/ncbi-refseqs/JF735122:0.03739623853801678222[I154]):0.22870756015614676637[I152]):0.31588844835225299290[I122]):0.01056552926413984947[I6],((((((((((((((((((((member/hcv-additional-refs/DQ418788:0.02896056191830391480[I175],member/ncbi-refseqs/DQ418789:0.02905908255628294329[I176]):0.03146133520275170431[I174],member/ncbi-refseqs/DQ988074:0.05139105204150953149[I177]):0.00572382143608011953[I173],member/ncbi-refseqs/Y11604:0.07231013778775265133[I178]):0.08840403134900630400[I172],member/ncbi-refseqs/FJ462435:0.29851540782916408112[I179]):0.00000100000050002909[I171],member/ncbi-refseqs/FJ462436:0.11978354660040797797[I180]):0.04000333055543798394[I170],((member/ncbi-refseqs/DQ418786:0.05766263918484666046[I183],member/ncbi-refseqs/EU392172:0.02159886778330603885[I184]):0.00565849510748545840[I182],member/ncbi-refseqs/FJ462437:0.02795159531726007160[I185]):0.15828499387872907445[I181]):0.01026466252428185830[I169],((member/ncbi-refseqs/EF589161:0.05232306006084975342[I188],member/ncbi-refseqs/EU392174:0.05338518620849353608[I189]):0.01021132054786106323[I187],member/ncbi-refseqs/EU392175:0.04121670917575472742[I190]):0.15465741624822018641[I186]):0.01081055264648380677[I168],((member/ncbi-refseqs/FJ462432:0.04847676290650341363[I193],member/ncbi-refseqs/JX227963:0.14070313383465879453[I194]):0.00058367219317393304[I192],member/ncbi-refseqs/JX227971:0.04431675976229097758[I195]):0.19278220336285464831[I191]):0.00000100000050002909[I167],((member/ncbi-refseqs/EU392171:0.05063020518644596041[I198],member/ncbi-refseqs/EU392173:0.05679935961263613692[I199]):0.00996542554632422715[I197],member/ncbi-refseqs/FJ462438:0.03976642901796140217[I200]):0.19380598235807980356[I196]):0.01776799337987252508[I166],(member/ncbi-refseqs/FJ839870:0.03758553172286465544[I202],member/ncbi-refseqs/JX227957:0.06160811132017386671[I203]):0.12400668538160064536[I201]):0.00282754512194510537[I165],(member/ncbi-refseqs/FJ462433:0.05086564489308576659[I205],member/ncbi-refseqs/JX227972:0.04914574133182875171[I206]):0.15624031204844790466[I204]):0.00066848085268467845[I164],(member/ncbi-refseqs/FJ462441:0.05565440096735932429[I208],member/ncbi-refseqs/JX227970:0.05912945768956340276[I209]):0.13747433484568424844[I207]):0.00294400426684431364[I163],(member/ncbi-refseqs/FJ462440:0.04863176696389686499[I211],member/ncbi-refseqs/JX227977:0.05578239538430068006[I212]):0.15626384796348913309[I210]):0.01137204891234729759[I162],member/ncbi-refseqs/FJ462431:0.18699544874415874318[I213]):0.00254545066547704184[I161],member/ncbi-refseqs/FJ462434:0.16132909895258479294[I214]):0.00134325152195598121[I160],(member/ncbi-refseqs/FJ462439:0.05396745967760844392[I216],member/ncbi-refseqs/JX227976:0.05437676999434674918[I217]):0.21406378124756794690[I215]):0.00212934059597941552[I159],member/ncbi-refseqs/JF735136:0.21914619438499588489[I218]):0.00320285876673411353[I158],member/ncbi-refseqs/FJ839869:0.19815054061998432777[I219]):0.00717934920382189059[I157],(((member/ncbi-refseqs/HQ537008:0.03382969964448111211[I223],member/ncbi-refseqs/HQ537009:0.04072229327648139302[I224]):0.01490779937635534382[I222],member/ncbi-refseqs/JX227959:0.03712888879826087979[I225]):0.00554432704351726958[I221],member/ncbi-refseqs/JX227960:0.03534024402386566621[I226]):0.13095451260382090508[I220]):0.06638391448686056795[I156],(member/ncbi-refseqs/FJ025855:0.02178892110776075447[I228],member/ncbi-refseqs/FJ025856:0.03183897117622239842[I229]):0.16636064611638531896[I227]):0.23920742702199762619[I155]):0.01844034943216460096[I5],((member/hcv-additional-refs/NC_009826:0.00000100000050002909[I232],member/ncbi-refseqs/AF064490:0.11888181848904237625[I233]):0.00000100000050002909[I231],member/ncbi-refseqs/Y13184:0.00000100000050002909[I234]):0.54131123896313360078[I230]):0.03714441449119678523[I4]):0.17958901233920801510[I2],(((((((((((((((((((((((((((((member/ncbi-refseqs/AY859526:0.02212439825514584193[I264],member/ncbi-refseqs/EU246930:0.06861260478240283067[I265]):0.00064001804743355926[I263],member/ncbi-refseqs/HQ639936:0.03198066757238907792[I266]):0.00000100000050002909[I262],member/ncbi-refseqs/Y12083:0.03453207652851564280[I267]):0.15466303185630697614[I261],member/ncbi-refseqs/D84262:0.16915563422794585580[I268]):0.33513693879772726847[I260],member/ncbi-refseqs/EF424629:0.18771649866114417660[I269]):0.01324885593516893570[I259],member/ncbi-refseqs/D84263:0.21548145730889969873[I270]):0.04072541044366616986[I258],((member/ncbi-refseqs/DQ314805:0.05711797490167160041[I273],member/ncbi-refseqs/EU246931:0.12208971693594473928[I274]):0.00603910322454462387[I272],member/ncbi-refseqs/EU246932:0.05421976096486785107[I275]):0.18770240669071239226[I271]):0.01960860892889196988[I257],(member/ncbi-refseqs/DQ835760:0.03129623149931114873[I277],member/ncbi-refseqs/EU246936:0.03076184645641664994[I278]):0.22386957097161250263[I276]):0.06746123907468905279[I256],(member/ncbi-refseqs/D63822:0.04309798684740283325[I280],member/ncbi-refseqs/DQ314806:0.03590177917078831576[I281]):0.27998128211165679291[I279]):0.19156929887891194220[I255],member/ncbi-refseqs/D84265:0.20031180986354263363[I282]):0.02921114924815122749[I254],(member/ncbi-refseqs/DQ835762:0.03509164990799990697[I284],member/ncbi-refseqs/DQ835770:0.02022453325362127616[I285]):0.13337333218349506359[I283]):0.00000100000050002909[I253],(member/ncbi-refseqs/DQ835761:0.01775303921300162152[I287],member/ncbi-refseqs/DQ835769:0.01894472616706890586[I288]):0.15335792641347992249[I286]):0.12884792399286335018[I252],member/ncbi-refseqs/D84264:0.18773596305208870016[I289]):0.00825775237931679860[I251],(member/ncbi-refseqs/EF424628:0.02678307126100628807[I291],member/ncbi-refseqs/JX183556:0.04072119143851583856[I292]):0.16295405849758073935[I290]):0.05113621502534688307[I250],(member/ncbi-refseqs/DQ835766:0.02059701540058261546[I294],member/ncbi-refseqs/DQ835767:0.01916342943134107316[I295]):0.15757465517995916660[I293]):0.01375323452845543526[I249],((member/ncbi-refseqs/DQ278894:0.02159222065585627431[I298],member/ncbi-refseqs/DQ835768:0.03027529875769278261[I299]):0.00723615907890616181[I297],member/ncbi-refseqs/EU246938:0.01829669370046542454[I300]):0.13565576291613806736[I296]):0.23889892675672555478[I248],(member/ncbi-refseqs/EF424627:0.05436266289310769106[I302],member/ncbi-refseqs/EU246934:0.03808503588705945686[I303]):0.17223754834133608860[I301]):0.00460186623996949092[I247],member/ncbi-refseqs/EF424626:0.20714567362327276911[I304]):0.02514014289307920508[I246],member/ncbi-refseqs/EF424625:0.22982075287779699102[I305]):0.01392273371227694019[I245],member/ncbi-refseqs/EU408328:0.28551867626755483842[I306]):0.00711685468406140446[I244],member/ncbi-refseqs/EU408329:0.39033678438002444855[I307]):0.00000100000050002909[I243],(member/ncbi-refseqs/EF632071:0.03978114715097060688[I309],member/ncbi-refseqs/EU246939:0.04753337297224728003[I310]):0.20404047987984441637[I308]):0.01630314919816900493[I242],member/ncbi-refseqs/EU246940:0.24242000059509166698[I311]):0.06672338115864710761[I241],((member/ncbi-refseqs/EU158186:0.00413682512377145425[I314],member/ncbi-refseqs/EU798760:0.04556092452627739237[I315]):0.00000100000050002909[I313],member/ncbi-refseqs/EU798761:0.00215483520993111494[I316]):0.41479706931246174140[I312]):0.00000100000050002909[I240],((member/ncbi-refseqs/DQ278892:0.06365333492329171283[I319],member/ncbi-refseqs/EU643834:0.04993698666302379130[I320]):0.00677597121766718592[I318],member/ncbi-refseqs/EU643836:0.05286131112347146332[I321]):0.34923771634738182135[I317]):0.01691069844152969653[I239],((member/ncbi-refseqs/EU408330:0.00819979575110113372[I324],member/ncbi-refseqs/EU408331:0.00580739476411674949[I325]):0.00069401348380624783[I323],member/ncbi-refseqs/EU408332:0.01238098637795083043[I326]):0.35120763836790436230[I322]):0.00575028215611623258[I238],(member/ncbi-refseqs/JX183552:0.05465927588484221361[I328],member/ncbi-refseqs/KJ567645:0.04771628125396740194[I329]):0.30455169707792661971[I327]):0.00000100000050002909[I237],member/ncbi-refseqs/KJ567651:0.29599878867737716703[I330]):0.02151420901108365771[I236],((member/ncbi-refseqs/KM252789:0.07096595083436499363[I333],member/ncbi-refseqs/KM252790:0.08546217142275794321[I334]):0.02428080797389193732[I332],member/ncbi-refseqs/KM252791:0.07407601256751042418[I335]):0.32829171718301380922[I331]):0.01082268035648975210[I235]):0.27854714344494196920[I1],member/ncbi-refseqs/JX183557:0.01842993026708059437[I336]);");
		parser.test("(((((((((((((((((((((((member/hcv-additional-refs/AF511950:0.04206220364114309634{0},member/ncbi-refseqs/AF009606:0.01592292013236466450{1}):0.00000100000050002909{2},member/ncbi-refseqs/EF407457:0.05233399796960445244{3}):0.00000100000050002909{4},member/ncbi-refseqs/HQ850279:0.06978035123034541376{5}):0.00010055513749021910{6},member/ncbi-refseqs/M62321:0.03034179361075641304{7}):0.00160547628816843052{8},member/ncbi-refseqs/M67463:0.02254220878957092361{9}):0.20470709150411237065{10},((((((member/hcv-additional-refs/AY587016:0.05786148900646046617{11},member/hcv-additional-refs/D11355:0.05806504210367680746{12}):0.00378690405913181686{13},member/hcv-additional-refs/EF032892:0.06132208330925192213{14}):0.00209751564258228501{15},member/ncbi-refseqs/D90208:0.05825945655072565371{16}):0.00293417185604461731{17},member/ncbi-refseqs/EU781827:0.04336382635844195088{18}):0.00420691781536202975{19},member/ncbi-refseqs/EU781828:0.05487550162120769653{20}):0.00816521433530948901{21},member/ncbi-refseqs/M58335:0.05275475005072843837{22}):0.15283166504620673831{23}):0.00000100000050002909{24},((member/ncbi-refseqs/AY051292:0.03391842178073233016{25},member/ncbi-refseqs/AY651061:0.07329390924926336426{26}):0.01499803996744142158{27},member/ncbi-refseqs/D14853:0.03518607411440680816{28}):0.22450441658266812039{29}):0.00000100000050002909{30},member/ncbi-refseqs/KJ439768:0.19225048067914699002{31}):0.01441716320982227607{32},member/ncbi-refseqs/KC248194:0.26065022905000384545{33}):0.00000100000050002909{34},member/ncbi-refseqs/AM910652:0.25399821247388060241{35}):0.00039158198950655780{36},(member/ncbi-refseqs/KC248198:0.04469943777007186203{37},member/ncbi-refseqs/KC248199:0.05037754869788137885{38}):0.18354536801350193609{39}):0.00113281806647946696{40},member/ncbi-refseqs/KJ439772:0.19868997449726033544{41}):0.08945940100525619221{42},member/ncbi-refseqs/KJ439773:0.14595715876222170593{43}):0.00351469112872792580{44},member/ncbi-refseqs/KJ439774:0.13439532826103464669{45}):0.00203444301761748509{46},((member/ncbi-refseqs/KC248193:0.03930837725959639845{47},member/ncbi-refseqs/KC248196:0.04723089767624426977{48}):0.01695537258799994715{49},member/ncbi-refseqs/KC248197:0.02539108934153057676{50}):0.28380617722949941228{51}):0.00918900789533876571{52},(member/ncbi-refseqs/KJ439778:0.08206019308790600542{53},member/ncbi-refseqs/KJ439782:0.09594645689536697442{54}):0.07660351132212660230{55}):0.00762992109047667087{56},(member/ncbi-refseqs/KJ439775:0.09702377182270001399{57},member/ncbi-refseqs/KJ439781:0.10764477744582622021{58}):0.05552204324295562793{59}):0.25210223750025290146{60},((((((((((((((((member/hcv-additional-refs/AY746460:0.07738119124209555388{61},member/ncbi-refseqs/AB047639:0.08096754470457676045{62}):0.00037794821596670288{63},member/ncbi-refseqs/D00944:0.06228343223382937915{64}):0.00336017725379235887{65},member/ncbi-refseqs/HQ639944:0.06233962779227633644{66}):0.12919685459814242434{67},(((member/ncbi-refseqs/AB030907:0.05296219613691313666{68},member/ncbi-refseqs/AB661382:0.07094826081527719208{69}):0.00557039868476834604{70},member/ncbi-refseqs/AB661388:0.07440212031625273448{71}):0.00137502572567530239{72},member/ncbi-refseqs/D10988:0.04218747048682949113{73}):0.26807370025398763458{74}):0.03427509154359992954{75},(member/ncbi-refseqs/D50409:0.06268806730523544812{76},member/ncbi-refseqs/JX227949:0.06736935824179858534{77}):0.11276956044088938536{78}):0.00694064081704182968{79},member/ncbi-refseqs/JF735114:0.14954344783623488291{80}):0.00320050293086175924{81},member/ncbi-refseqs/JF735120:0.19222018475187233055{82}):0.00000100000050002909{83},(member/ncbi-refseqs/KC844042:0.06469067361819014206{84},member/ncbi-refseqs/KC844050:0.05521183305520577927{85}):0.11123240855223946733{86}):0.01162345667824307358{87},member/ncbi-refseqs/DQ155561:0.17386681660502550928{88}):0.00907597319046622025{89},((member/ncbi-refseqs/HM777358:0.06027783035472877166{90},member/ncbi-refseqs/HM777359:0.05469284076159657698{91}):0.01096359874596435947{92},member/ncbi-refseqs/JF735113:0.08127497075625991774{93}):0.12633662474850143598{94}):0.03468758262189636926{95},(member/ncbi-refseqs/AB031663:0.05714593416352850563{96},member/ncbi-refseqs/JX227953:0.06094470580273043253{97}):0.12637257858729109006{98}):0.00092823497170964765{99},(member/ncbi-refseqs/JF735111:0.04749054108256575701{100},member/ncbi-refseqs/JX227967:0.04552305190832182935{101}):0.13582593572265225235{102}):0.00284090405546721806{103},(member/ncbi-refseqs/FN666428:0.05866265472646167017{104},member/ncbi-refseqs/FN666429:0.05352558118817610233{105}):0.11867886003060021394{106}):0.01434015658867470421{107},member/ncbi-refseqs/JF735115:0.17607446549256003387{108}):0.01970464868427389271{109},member/ncbi-refseqs/KC197238:0.30019488581939740568{110}):0.01332778886097100816{111},member/ncbi-refseqs/JF735112:0.14301570133535956852{112}):0.55253647976728492708{113}):0.04949760153591974132{114},(((((((((((member/hcv-additional-refs/NC_009824:0.00000100000050002909{115},member/ncbi-refseqs/D17763:0.00000100000050002909{116}):0.02822853038357158936{117},member/ncbi-refseqs/D28917:0.04841808397611693793{118}):0.00485472386402539206{119},member/ncbi-refseqs/JN714194:0.08539958516146799183{120}):0.00241491362363128450{121},member/ncbi-refseqs/X76918:0.02934451066229874089{122}):0.15357280082612279060{123},(member/ncbi-refseqs/D49374:0.07703578275700408151{124},member/ncbi-refseqs/JQ065709:0.06958687548490775920{125}):0.16124645213285154766{126}):0.00400752061310971630{127},member/ncbi-refseqs/KJ470619:0.18150706276258490868{128}):0.00254494346570026224{129},member/ncbi-refseqs/KJ470618:0.22623928380401658877{130}):0.04819035204900033037{131},(member/ncbi-refseqs/JF735123:0.07501566657349267864{132},member/ncbi-refseqs/JX227954:0.05214262611096715655{133}):0.12455816310977699257{134}):0.02433331634299859894{135},(member/ncbi-refseqs/JF735121:0.06048256497965733003{136},member/ncbi-refseqs/JF735126:0.03590886895515862237{137}):0.48859668133232880827{138}):0.00000100000050002909{139},(member/ncbi-refseqs/FJ407092:0.03057207333017111436{140},member/ncbi-refseqs/JX227955:0.02646073795154670408{141}):0.13167366471449759580{142}):0.10567554553372811299{143},(member/ncbi-refseqs/D63821:0.05308213258536979839{144},member/ncbi-refseqs/JF735122:0.03739623853801678222{145}):0.22870756015614676637{146}):0.31588844835225299290{147}):0.01056552926413984947{148},((((((((((((((((((((member/hcv-additional-refs/DQ418788:0.02896056191830391480{149},member/ncbi-refseqs/DQ418789:0.02905908255628294329{150}):0.03146133520275170431{151},member/ncbi-refseqs/DQ988074:0.05139105204150953149{152}):0.00572382143608011953{153},member/ncbi-refseqs/Y11604:0.07231013778775265133{154}):0.08840403134900630400{155},member/ncbi-refseqs/FJ462435:0.29851540782916408112{156}):0.00000100000050002909{157},member/ncbi-refseqs/FJ462436:0.11978354660040797797{158}):0.04000333055543798394{159},((member/ncbi-refseqs/DQ418786:0.05766263918484666046{160},member/ncbi-refseqs/EU392172:0.02159886778330603885{161}):0.00565849510748545840{162},member/ncbi-refseqs/FJ462437:0.02795159531726007160{163}):0.15828499387872907445{164}):0.01026466252428185830{165},((member/ncbi-refseqs/EF589161:0.05232306006084975342{166},member/ncbi-refseqs/EU392174:0.05338518620849353608{167}):0.01021132054786106323{168},member/ncbi-refseqs/EU392175:0.04121670917575472742{169}):0.15465741624822018641{170}):0.01081055264648380677{171},((member/ncbi-refseqs/FJ462432:0.04847676290650341363{172},member/ncbi-refseqs/JX227963:0.14070313383465879453{173}):0.00058367219317393304{174},member/ncbi-refseqs/JX227971:0.04431675976229097758{175}):0.19278220336285464831{176}):0.00000100000050002909{177},((member/ncbi-refseqs/EU392171:0.05063020518644596041{178},member/ncbi-refseqs/EU392173:0.05679935961263613692{179}):0.00996542554632422715{180},member/ncbi-refseqs/FJ462438:0.03976642901796140217{181}):0.19380598235807980356{182}):0.01776799337987252508{183},(member/ncbi-refseqs/FJ839870:0.03758553172286465544{184},member/ncbi-refseqs/JX227957:0.06160811132017386671{185}):0.12400668538160064536{186}):0.00282754512194510537{187},(member/ncbi-refseqs/FJ462433:0.05086564489308576659{188},member/ncbi-refseqs/JX227972:0.04914574133182875171{189}):0.15624031204844790466{190}):0.00066848085268467845{191},(member/ncbi-refseqs/FJ462441:0.05565440096735932429{192},member/ncbi-refseqs/JX227970:0.05912945768956340276{193}):0.13747433484568424844{194}):0.00294400426684431364{195},(member/ncbi-refseqs/FJ462440:0.04863176696389686499{196},member/ncbi-refseqs/JX227977:0.05578239538430068006{197}):0.15626384796348913309{198}):0.01137204891234729759{199},member/ncbi-refseqs/FJ462431:0.18699544874415874318{200}):0.00254545066547704184{201},member/ncbi-refseqs/FJ462434:0.16132909895258479294{202}):0.00134325152195598121{203},(member/ncbi-refseqs/FJ462439:0.05396745967760844392{204},member/ncbi-refseqs/JX227976:0.05437676999434674918{205}):0.21406378124756794690{206}):0.00212934059597941552{207},member/ncbi-refseqs/JF735136:0.21914619438499588489{208}):0.00320285876673411353{209},member/ncbi-refseqs/FJ839869:0.19815054061998432777{210}):0.00717934920382189059{211},(((member/ncbi-refseqs/HQ537008:0.03382969964448111211{212},member/ncbi-refseqs/HQ537009:0.04072229327648139302{213}):0.01490779937635534382{214},member/ncbi-refseqs/JX227959:0.03712888879826087979{215}):0.00554432704351726958{216},member/ncbi-refseqs/JX227960:0.03534024402386566621{217}):0.13095451260382090508{218}):0.06638391448686056795{219},(member/ncbi-refseqs/FJ025855:0.02178892110776075447{220},member/ncbi-refseqs/FJ025856:0.03183897117622239842{221}):0.16636064611638531896{222}):0.23920742702199762619{223}):0.01844034943216460096{224},((member/hcv-additional-refs/NC_009826:0.00000100000050002909{225},member/ncbi-refseqs/AF064490:0.11888181848904237625{226}):0.00000100000050002909{227},member/ncbi-refseqs/Y13184:0.00000100000050002909{228}):0.54131123896313360078{229}):0.03714441449119678523{230},((((((((((((((((((((((((((((((member/ncbi-refseqs/AY859526:0.02212439825514584193{231},member/ncbi-refseqs/EU246930:0.06861260478240283067{232}):0.00064001804743355926{233},member/ncbi-refseqs/HQ639936:0.03198066757238907792{234}):0.00000100000050002909{235},member/ncbi-refseqs/Y12083:0.03453207652851564280{236}):0.15466303185630697614{237},member/ncbi-refseqs/D84262:0.16915563422794585580{238}):0.33513693879772726847{239},member/ncbi-refseqs/EF424629:0.18771649866114417660{240}):0.01324885593516893570{241},member/ncbi-refseqs/D84263:0.21548145730889969873{242}):0.04072541044366616986{243},((member/ncbi-refseqs/DQ314805:0.05711797490167160041{244},member/ncbi-refseqs/EU246931:0.12208971693594473928{245}):0.00603910322454462387{246},member/ncbi-refseqs/EU246932:0.05421976096486785107{247}):0.18770240669071239226{248}):0.01960860892889196988{249},(member/ncbi-refseqs/DQ835760:0.03129623149931114873{250},member/ncbi-refseqs/EU246936:0.03076184645641664994{251}):0.22386957097161250263{252}):0.06746123907468905279{253},(member/ncbi-refseqs/D63822:0.04309798684740283325{254},member/ncbi-refseqs/DQ314806:0.03590177917078831576{255}):0.27998128211165679291{256}):0.19156929887891194220{257},member/ncbi-refseqs/D84265:0.20031180986354263363{258}):0.02921114924815122749{259},(member/ncbi-refseqs/DQ835762:0.03509164990799990697{260},member/ncbi-refseqs/DQ835770:0.02022453325362127616{261}):0.13337333218349506359{262}):0.00000100000050002909{263},(member/ncbi-refseqs/DQ835761:0.01775303921300162152{264},member/ncbi-refseqs/DQ835769:0.01894472616706890586{265}):0.15335792641347992249{266}):0.12884792399286335018{267},member/ncbi-refseqs/D84264:0.18773596305208870016{268}):0.00825775237931679860{269},(member/ncbi-refseqs/EF424628:0.02678307126100628807{270},member/ncbi-refseqs/JX183556:0.04072119143851583856{271}):0.16295405849758073935{272}):0.05113621502534688307{273},(member/ncbi-refseqs/DQ835766:0.02059701540058261546{274},member/ncbi-refseqs/DQ835767:0.01916342943134107316{275}):0.15757465517995916660{276}):0.01375323452845543526{277},((member/ncbi-refseqs/DQ278894:0.02159222065585627431{278},member/ncbi-refseqs/DQ835768:0.03027529875769278261{279}):0.00723615907890616181{280},member/ncbi-refseqs/EU246938:0.01829669370046542454{281}):0.13565576291613806736{282}):0.23889892675672555478{283},(member/ncbi-refseqs/EF424627:0.05436266289310769106{284},member/ncbi-refseqs/EU246934:0.03808503588705945686{285}):0.17223754834133608860{286}):0.00460186623996949092{287},member/ncbi-refseqs/EF424626:0.20714567362327276911{288}):0.02514014289307920508{289},member/ncbi-refseqs/EF424625:0.22982075287779699102{290}):0.01392273371227694019{291},member/ncbi-refseqs/EU408328:0.28551867626755483842{292}):0.00711685468406140446{293},member/ncbi-refseqs/EU408329:0.39033678438002444855{294}):0.00000100000050002909{295},(member/ncbi-refseqs/EF632071:0.03978114715097060688{296},member/ncbi-refseqs/EU246939:0.04753337297224728003{297}):0.20404047987984441637{298}):0.01630314919816900493{299},member/ncbi-refseqs/EU246940:0.24242000059509166698{300}):0.06672338115864710761{301},((member/ncbi-refseqs/EU158186:0.00413682512377145425{302},member/ncbi-refseqs/EU798760:0.04556092452627739237{303}):0.00000100000050002909{304},member/ncbi-refseqs/EU798761:0.00215483520993111494{305}):0.41479706931246174140{306}):0.00000100000050002909{307},((member/ncbi-refseqs/DQ278892:0.06365333492329171283{308},member/ncbi-refseqs/EU643834:0.04993698666302379130{309}):0.00677597121766718592{310},member/ncbi-refseqs/EU643836:0.05286131112347146332{311}):0.34923771634738182135{312}):0.01691069844152969653{313},((member/ncbi-refseqs/EU408330:0.00819979575110113372{314},member/ncbi-refseqs/EU408331:0.00580739476411674949{315}):0.00069401348380624783{316},member/ncbi-refseqs/EU408332:0.01238098637795083043{317}):0.35120763836790436230{318}):0.00575028215611623258{319},(member/ncbi-refseqs/JX183552:0.05465927588484221361{320},member/ncbi-refseqs/KJ567645:0.04771628125396740194{321}):0.30455169707792661971{322}):0.00000100000050002909{323},member/ncbi-refseqs/KJ567651:0.29599878867737716703{324}):0.02151420901108365771{325},((member/ncbi-refseqs/KM252789:0.07096595083436499363{326},member/ncbi-refseqs/KM252790:0.08546217142275794321{327}):0.02428080797389193732{328},member/ncbi-refseqs/KM252791:0.07407601256751042418{329}):0.32829171718301380922{330}):0.01082268035648975210{331},(member/ncbi-refseqs/JX183557:0.01842993026708059437{332},member/ncbi-refseqs/KM252792:0.01863094234626353971{333}):0.27854714344494196920{334}):0.17958901233920801510{335}):0.41622457566925513683{336},member/ncbi-refseqs/EF108306:0.41622457566925513683{337});");
	
	}

	
	
}
