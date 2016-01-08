package uk.ac.gla.cvr.gluetools.core.digs;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

/**
 * Module to search for homologous segments between different sequences in the project. 
 * 
 * The operation consists of two inputs:
 * -- the probe, a single sequence segment selected for its representativeness.
 * -- the target, a possibly large set of sequences amongst which it is thought there may be some segments homologous to the probe.
 * 
 * Homologous segments which are found by the process are referred to as 'hits'
 * Various "actions" may be taken 
 * 
 * Possible targets: 
 * 	- any defined sequence group in the GLUE project may be used as a target
 * 
 * Possible probes:
 *  - any defined feature location on a reference sequence in the GLUE project may be used as a probe
 *  
 * Possible actions:
 *  - preview hits - return some basic data about the hits
 *  - update alignment with hits
 *  
 *  
 */


@PluginClass(elemName="digsProber")
public class DigsProber extends ModulePlugin<DigsProber> {

	public DigsProber() {
		super();
		addProvidedCmdClass(BuildTargetGroupCommand.class);
		addProvidedCmdClass(PreviewHitsCommand.class);
	}

	
	
}
