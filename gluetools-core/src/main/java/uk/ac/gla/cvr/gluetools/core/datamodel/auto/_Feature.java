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
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;

/**
 * Class _Feature was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Feature extends GlueDataObject {

    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String DISPLAY_NAME_PROPERTY = "displayName";
    public static final String NAME_PROPERTY = "name";
    public static final String CHILDREN_PROPERTY = "children";
    public static final String FEATURE_LOCATIONS_PROPERTY = "featureLocations";
    public static final String FEATURE_METATAGS_PROPERTY = "featureMetatags";
    public static final String PARENT_PROPERTY = "parent";

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

    public void addToChildren(Feature obj) {
        addToManyTarget(CHILDREN_PROPERTY, obj, true);
    }
    public void removeFromChildren(Feature obj) {
        removeToManyTarget(CHILDREN_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<Feature> getChildren() {
        return (List<Feature>)readProperty(CHILDREN_PROPERTY);
    }


    public void addToFeatureLocations(FeatureLocation obj) {
        addToManyTarget(FEATURE_LOCATIONS_PROPERTY, obj, true);
    }
    public void removeFromFeatureLocations(FeatureLocation obj) {
        removeToManyTarget(FEATURE_LOCATIONS_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<FeatureLocation> getFeatureLocations() {
        return (List<FeatureLocation>)readProperty(FEATURE_LOCATIONS_PROPERTY);
    }


    public void addToFeatureMetatags(FeatureMetatag obj) {
        addToManyTarget(FEATURE_METATAGS_PROPERTY, obj, true);
    }
    public void removeFromFeatureMetatags(FeatureMetatag obj) {
        removeToManyTarget(FEATURE_METATAGS_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<FeatureMetatag> getFeatureMetatags() {
        return (List<FeatureMetatag>)readProperty(FEATURE_METATAGS_PROPERTY);
    }


    public void setParent(Feature parent) {
        setToOneTarget(PARENT_PROPERTY, parent, true);
    }

    public Feature getParent() {
        return (Feature)readProperty(PARENT_PROPERTY);
    }


}
