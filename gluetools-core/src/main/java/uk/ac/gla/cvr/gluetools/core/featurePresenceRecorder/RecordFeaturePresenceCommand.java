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
package uk.ac.gla.cvr.gluetools.core.featurePresenceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentShowFeaturePresenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.MemberFeatureCoverage;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberCreateFLocNoteCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.memberFLocNote.MemberFLocNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
		commandWords={"record", "feature-presence"}, 
		description = "Record feature presence as member-feature-location notes", 
		docoptUsages = { "<almtName> [-c] [-w <whereClause>] -f <featureName> [-d] [-p]" },
		docoptOptions = { 
		"-c, --recursive                                   Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>     Qualify members",
		"-f <featureName>, --featureName <featureName>     Feature name",
		"-d, --descendentFeatures                          Include descendent features",
		"-p, --previewOnly                                 Preview only",
			
		},
		furtherHelp = 
		"If --alignmentRecursive is used, member-feature-location notes will be generated for members of both this and descendent alignments. "+
		"If --previewOnly is used, no notes will be created / updated, only a preview is returned. ",
		metaTags = {}	
)
public class RecordFeaturePresenceCommand extends ModulePluginCommand<RecordFeaturePresenceResult, FeaturePresenceRecorder> {

	private static final int BATCH_SIZE = 1000;
	
	public final static String ALIGNMENT_NAME = "almtName";
	public static final String RECURSIVE = "recursive";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String FEATURE_NAME = "featureName";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	public final static String PREVIEW_ONLY = "previewOnly";
	
	private String almtName; 
	private Boolean recursive;
	private Optional<Expression> whereClause;
	private String featureName;
	private Boolean descendentFeatures;
	private boolean previewOnly; 
	
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		this.descendentFeatures = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
		this.previewOnly = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, PREVIEW_ONLY, false)).orElse(false);
	}
	
	
	@Override
	protected RecordFeaturePresenceResult execute(CommandContext cmdContext,
			FeaturePresenceRecorder featurePresenceRecorder) {

		List<String> featureNameList = new ArrayList<String>();
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		featureNameList.add(feature.getName());
		if(this.descendentFeatures) {
			featureNameList.addAll(feature.getDescendents()
					.stream()
					.map(f -> f.getName())
					.collect(Collectors.toList()));
		}
		
		List<MemberFeaturePresence> memberFeaturePresenceList = new ArrayList<MemberFeaturePresence>();

		int totalMembers = countTotalMembers(cmdContext);

		int offset = 0;
		while(offset < totalMembers) {
			Alignment namedAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(this.almtName));
			ReferenceSequence namedAlignmentRef = namedAlignment.getConstrainingRef();
			
			int lastBatchIndex = Math.min(offset+BATCH_SIZE, totalMembers);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Retrieving members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);
			List<AlignmentMember> memberBatch = AlignmentListMemberCommand
					.listMembers(cmdContext, namedAlignment, recursive, whereClause, offset, BATCH_SIZE, BATCH_SIZE);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Processing members "+(offset+1)+" to "+lastBatchIndex+" of "+totalMembers);

			Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
			
			for(AlignmentMember member: memberBatch) {
				List<ReferenceSequence> ancestorPathReferences = member.getAlignment().getAncestorPathReferences(cmdContext, namedAlignmentRef.getName());
				List<String> ancestorPathRefNames = ancestorPathReferences.stream().map(apr -> apr.getName()).collect(Collectors.toList());
				
				Expression featureLocExp = 
						ExpressionFactory.inExp(FeatureLocation.REF_SEQ_NAME_PATH, ancestorPathRefNames)
						.andExp(ExpressionFactory.inExp(FeatureLocation.FEATURE_NAME_PATH, featureNameList));
				List<FeatureLocation> featureLocs = 
						GlueDataObject.query(cmdContext, FeatureLocation.class, 
								new SelectQuery(FeatureLocation.class, featureLocExp));

				for(FeatureLocation featureLoc: featureLocs) {
					MemberFeatureCoverage memberFeatureCoverage = AlignmentShowFeaturePresenceCommand
						.alignmentFeatureCoverage(cmdContext, featureLoc.getReferenceSequence(), featureLoc, member);
					Double refNtCoverage = memberFeatureCoverage.getFeatureReferenceNtCoverage();
					if(refNtCoverage >= featurePresenceRecorder.getMinRefNtCoveragePct()) {
						memberFeaturePresenceList.add(new MemberFeaturePresence(member.pkMap(), featureLoc.pkMap(), refNtCoverage));
						if(!previewOnly) {
							MemberFLocNote memberFLocNote = MemberCreateFLocNoteCommand.createFLocNote(cmdContext, member, featureLoc, true);
							PropertyCommandDelegate.executeSetField(cmdContext, project, 
									ConfigurableTable.member_floc_note.name(), memberFLocNote, 
									featurePresenceRecorder.getRefNtCoverageFieldName(), refNtCoverage, true);
						}
					}
				}
			}
			offset = offset+BATCH_SIZE;
			cmdContext.commit();
			cmdContext.newObjectContext();
		}
		GlueLogger.getGlueLogger().log(Level.FINEST, "Processed "+totalMembers+" members");
		return new RecordFeaturePresenceResult(memberFeaturePresenceList);
	}

	private int countTotalMembers(CommandContext cmdContext) {
		Alignment namedAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(this.almtName));
		GlueLogger.getGlueLogger().log(Level.FINEST, "Searching for alignment members");
		int totalMembers = AlignmentListMemberCommand.countMembers(cmdContext, namedAlignment, recursive, whereClause);
		GlueLogger.getGlueLogger().log(Level.FINEST, "Found "+totalMembers+" alignment members");
		return totalMembers;
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("almtName", Alignment.class, Alignment.NAME_PROPERTY);
			registerDataObjectNameLookup("featureName", Feature.class, Feature.NAME_PROPERTY);
		}


	}
	
}

