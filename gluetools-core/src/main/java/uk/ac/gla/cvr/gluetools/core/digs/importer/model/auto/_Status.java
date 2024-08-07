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
package uk.ac.gla.cvr.gluetools.core.digs.importer.model.auto;

import java.util.Date;

import uk.ac.gla.cvr.gluetools.core.digs.importer.model.DigsObject;

/**
 * Class _Status was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Status extends DigsObject {

    public static final String DATA_TYPE_PROPERTY = "dataType";
    public static final String GENOME_ID_PROPERTY = "genomeId";
    public static final String ORGANISM_PROPERTY = "organism";
    public static final String PROBE_GENE_PROPERTY = "probeGene";
    public static final String PROBE_ID_PROPERTY = "probeId";
    public static final String PROBE_NAME_PROPERTY = "probeName";
    public static final String TARGET_NAME_PROPERTY = "targetName";
    public static final String TIMESTAMP_PROPERTY = "timestamp";
    public static final String VERSION_PROPERTY = "version";

    public static final String RECORD_ID_PK_COLUMN = "Record_ID";

    public void setDataType(String dataType) {
        writeProperty(DATA_TYPE_PROPERTY, dataType);
    }
    public String getDataType() {
        return (String)readProperty(DATA_TYPE_PROPERTY);
    }

    public void setGenomeId(String genomeId) {
        writeProperty(GENOME_ID_PROPERTY, genomeId);
    }
    public String getGenomeId() {
        return (String)readProperty(GENOME_ID_PROPERTY);
    }

    public void setOrganism(String organism) {
        writeProperty(ORGANISM_PROPERTY, organism);
    }
    public String getOrganism() {
        return (String)readProperty(ORGANISM_PROPERTY);
    }

    public void setProbeGene(String probeGene) {
        writeProperty(PROBE_GENE_PROPERTY, probeGene);
    }
    public String getProbeGene() {
        return (String)readProperty(PROBE_GENE_PROPERTY);
    }

    public void setProbeId(String probeId) {
        writeProperty(PROBE_ID_PROPERTY, probeId);
    }
    public String getProbeId() {
        return (String)readProperty(PROBE_ID_PROPERTY);
    }

    public void setProbeName(String probeName) {
        writeProperty(PROBE_NAME_PROPERTY, probeName);
    }
    public String getProbeName() {
        return (String)readProperty(PROBE_NAME_PROPERTY);
    }

    public void setTargetName(String targetName) {
        writeProperty(TARGET_NAME_PROPERTY, targetName);
    }
    public String getTargetName() {
        return (String)readProperty(TARGET_NAME_PROPERTY);
    }

    public void setTimestamp(Date timestamp) {
        writeProperty(TIMESTAMP_PROPERTY, timestamp);
    }
    public Date getTimestamp() {
        return (Date)readProperty(TIMESTAMP_PROPERTY);
    }

    public void setVersion(String version) {
        writeProperty(VERSION_PROPERTY, version);
    }
    public String getVersion() {
        return (String)readProperty(VERSION_PROPERTY);
    }

}
