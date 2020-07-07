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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.InsideAlignmentMode;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass( 
	commandWords={"mask","positions"}, 
	docoptUsages={"-r <relRefName> <refStart> <refEnd>"},
	description="Delete homology in specific nucleotide positions", 
	docoptOptions = { 
	   "-r <relRefName>, --relRefName <relRefName>     Related reference",
	},
	metaTags = {}) 
public class MemberMaskPositionCommand extends MemberModeCommand<OkResult> {

	public static final String REL_REF_NAME = "relRefName";
	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	
	private String relRefName;
	private int refStart;
	private int refEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, 1, true, null, false, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, 1, true, null, false, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		AlignmentMember almtMemb = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(getAlignmentName(), getSourceName(), getSequenceID()));
		
		Alignment alignment = almtMemb.getAlignment();
		ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, this.relRefName);
		if(refStart > refEnd) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "<refStart> may not be greater than <refEnd>");
		}
		if(refEnd > relatedRef.getSequence().getSequenceObject().getNucleotides(cmdContext).length()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "<refEnd> may not be greater than length of "+relatedRef.getName());
		}

		List<QueryAlignedSegment> membToAlmtSegs = almtMemb.segmentsAsQueryAlignedSegments();

		List<QueryAlignedSegment> membToRefSegs = alignment.translateToRelatedRef(cmdContext, membToAlmtSegs, relatedRef);
		List<QueryAlignedSegment> membToRefSegsMasked = QueryAlignedSegment.subtract(membToRefSegs, 
				Arrays.asList(new ReferenceSegment(refStart, refEnd)));

		List<QueryAlignedSegment> refToMembSegsMasked = QueryAlignedSegment.invertList(membToRefSegsMasked);
		List<ReferenceSegment> membSegsMasked = QueryAlignedSegment.asReferenceSegments(refToMembSegsMasked);

		List<QueryAlignedSegment> almtToMembSegs = QueryAlignedSegment.invertList(membToAlmtSegs);
		List<QueryAlignedSegment> almtToMembSegsMasked = QueryAlignedSegment.intersection(almtToMembSegs, membSegsMasked, QueryAlignedSegment.cloneLeftSegMerger());

		List<QueryAlignedSegment> membToAlmtSegsMasked = QueryAlignedSegment.invertList(almtToMembSegsMasked);;

		for(QueryAlignedSegment segToDelete: membToAlmtSegs) {
			GlueDataObject.delete(cmdContext, AlignedSegment.class, 
					AlignedSegment.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), 
							segToDelete.getRefStart(), 
							segToDelete.getRefEnd(), 
							segToDelete.getQueryStart(), 
							segToDelete.getQueryEnd()), false);
		}
		cmdContext.commit();
		for(QueryAlignedSegment segToAdd: membToAlmtSegsMasked) {
			AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
					AlignedSegment.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), 
							segToAdd.getRefStart(), 
							segToAdd.getRefEnd(), 
							segToAdd.getQueryStart(), 
							segToAdd.getQueryEnd()), false);
			alignedSegment.setAlignmentMember(almtMemb);
		}
		cmdContext.commit();
		return new OkResult();
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					InsideAlignmentMode insideAlignmentMode = (InsideAlignmentMode) cmdContext.peekCommandMode();
					String almtName = insideAlignmentMode.getAlignmentName();
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
					return alignment.getRelatedRefs().stream()
							.map(ancCR -> new CompletionSuggestion(ancCR.getName(), true))
							.collect(Collectors.toList());
				}
			});
		}
	}
}
