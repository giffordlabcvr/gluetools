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
package uk.ac.gla.cvr.gluetools.core.datamodel.auto;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;

/**
 * Class _Alignment was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Alignment extends GlueDataObject {

    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String DISPLAY_NAME_PROPERTY = "displayName";
    public static final String NAME_PROPERTY = "name";
    public static final String CHILDREN_PROPERTY = "children";
    public static final String MEMBERS_PROPERTY = "members";
    public static final String PARENT_PROPERTY = "parent";
    public static final String REF_SEQUENCE_PROPERTY = "refSequence";
    public static final String VAR_ALMT_NOTES_PROPERTY = "varAlmtNotes";

    public static final String NAME_PK_COLUMN = "name";

    public void setDescription(String description) {
        writeProperty(DESCRIPTION_PROPERTY, description);
    }
    public String getDescription() {
        return (String)readProperty(DESCRIPTION_PROPERTY);
    }

    public void setDisplayName(String displayName) {
        writeProperty(DISPLAY_NAME_PROPERTY, displayName);
    }
    public String getDisplayName() {
        return (String)readProperty(DISPLAY_NAME_PROPERTY);
    }

    public void setName(String name) {
        writeProperty(NAME_PROPERTY, name);
    }
    public String getName() {
        return (String)readProperty(NAME_PROPERTY);
    }

    public void addToChildren(Alignment obj) {
        addToManyTarget(CHILDREN_PROPERTY, obj, true);
    }
    public void removeFromChildren(Alignment obj) {
        removeToManyTarget(CHILDREN_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<Alignment> getChildren() {
        return (List<Alignment>)readProperty(CHILDREN_PROPERTY);
    }


    public void addToMembers(AlignmentMember obj) {
        addToManyTarget(MEMBERS_PROPERTY, obj, true);
    }
    public void removeFromMembers(AlignmentMember obj) {
        removeToManyTarget(MEMBERS_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<AlignmentMember> getMembers() {
        return (List<AlignmentMember>)readProperty(MEMBERS_PROPERTY);
    }


    public void setParent(Alignment parent) {
        setToOneTarget(PARENT_PROPERTY, parent, true);
    }

    public Alignment getParent() {
        return (Alignment)readProperty(PARENT_PROPERTY);
    }


    public void setRefSequence(ReferenceSequence refSequence) {
        setToOneTarget(REF_SEQUENCE_PROPERTY, refSequence, true);
    }

    public ReferenceSequence getRefSequence() {
        return (ReferenceSequence)readProperty(REF_SEQUENCE_PROPERTY);
    }


    public void addToVarAlmtNotes(VarAlmtNote obj) {
        addToManyTarget(VAR_ALMT_NOTES_PROPERTY, obj, true);
    }
    public void removeFromVarAlmtNotes(VarAlmtNote obj) {
        removeToManyTarget(VAR_ALMT_NOTES_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<VarAlmtNote> getVarAlmtNotes() {
        return (List<VarAlmtNote>)readProperty(VAR_ALMT_NOTES_PROPERTY);
    }


}
