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
package uk.ac.gla.cvr.gluetools.core.command.configurableobject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link.Multiplicity;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public class LinkUpdateContext {
	
	public enum UpdateType {
		SET(Multiplicity.ONE_TO_ONE, Multiplicity.MANY_TO_ONE) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				return !linkUpdateContext.linkTargetPresent(thisObject, otherObject);
			}
			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				linkUpdateContext.addLinkTarget(thisObject, otherObject);
			}
		}, 
		UNSET(Multiplicity.ONE_TO_ONE, Multiplicity.MANY_TO_ONE) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				GlueDataObject currentTarget = (GlueDataObject) thisObject.readProperty(linkUpdateContext.thisLinkName);
				return currentTarget != null;
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				GlueDataObject currentTarget = (GlueDataObject) thisObject.readProperty(linkUpdateContext.thisLinkName);
				linkUpdateContext.removeLinkTarget(thisObject, currentTarget);
			}
		}, 
		CLEAR(Multiplicity.ONE_TO_MANY) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				List<?> currentValue = (List<?>) thisObject.readProperty(linkUpdateContext.thisLinkName);
				return !currentValue.isEmpty();
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				@SuppressWarnings("unchecked")
				List<GlueDataObject> currentTargets = new ArrayList<GlueDataObject>((List<GlueDataObject>) thisObject.readProperty(linkUpdateContext.thisLinkName));
				for(GlueDataObject target: currentTargets) {
					linkUpdateContext.removeLinkTarget(thisObject, target);
				}
			}
		}, 
		ADD(Multiplicity.ONE_TO_MANY) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				return !linkUpdateContext.linkTargetPresent(thisObject, otherObject);
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				linkUpdateContext.addLinkTarget(thisObject, otherObject);
			}
		}, 
		REMOVE(Multiplicity.ONE_TO_MANY) {
			@Override
			public boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				return linkUpdateContext.linkTargetPresent(thisObject, otherObject);
			}

			@Override
			public void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject) {
				linkUpdateContext.removeLinkTarget(thisObject, otherObject);
			}
		};
		
		private EnumSet<Multiplicity> thisToOtherMults;
		
		UpdateType(Link.Multiplicity first, Link.Multiplicity ... rest) {
			this.thisToOtherMults = EnumSet.of(first, rest);
		}

		public EnumSet<Multiplicity> getThisToOtherMults() {
			return thisToOtherMults;
		}

		private String cmdName() {
			return name()+" link-target";
		}
		
		public void checkMultiplicity(LinkUpdateContext ctx) {
			if(!thisToOtherMults.contains(ctx.thisToOtherMultiplicity)) {
				UpdateType alt = null;
				switch(this) {
				case SET: alt = ADD; break;
				case UNSET: alt = REMOVE; break;
				case ADD: alt = SET; break;
				case REMOVE: alt = UNSET; break;
				case CLEAR: alt = UNSET; break;
				}
				throw new LinkException(Code.LINK_MULTIPLICITY_ERROR, ctx.thisTableName, ctx.thisLinkName, 
						"Cannot use '"+cmdName()+"' on link with multiplicity "+ctx.thisToOtherMultiplicity+
						": use '"+alt.cmdName()+"' instead");
			}
			
		}

		public abstract boolean updateRequired(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject);

		public abstract void execute(LinkUpdateContext linkUpdateContext, GlueDataObject thisObject, GlueDataObject otherObject);
	}
	
	private Link link;
	private boolean isSrcLink;
	private String thisTableName, otherTableName;
	private String thisLinkName, otherLinkName;
	private Link.Multiplicity thisToOtherMultiplicity;
	private Link.Multiplicity otherToThisMultiplicity;
	
	public LinkUpdateContext(Project project, String thisTableName, String linkName) {
		this.isSrcLink = false;
		this.thisTableName = thisTableName;
		this.link = project.getSrcTableLink(thisTableName, linkName);
		if(link != null) {
			this.isSrcLink = true;
		} else {
			this.link = project.getDestTableLink(thisTableName, linkName);
		}
		if(link == null) {
			throw new LinkException(Code.NO_SUCH_LINK, thisTableName, linkName);
		}
		if(this.isSrcLink) {
			this.thisLinkName = this.link.getSrcLinkName();
			this.otherLinkName = this.link.getDestLinkName();
			this.otherTableName = this.link.getDestTableName();
			this.thisToOtherMultiplicity = Multiplicity.valueOf(this.link.getMultiplicity());
			this.otherToThisMultiplicity = this.thisToOtherMultiplicity.inverse();
		} else {
			this.thisLinkName = this.link.getDestLinkName();
			this.otherLinkName = this.link.getSrcLinkName();
			this.otherTableName = this.link.getSrcTableName();
			this.otherToThisMultiplicity = Multiplicity.valueOf(this.link.getMultiplicity());
			this.thisToOtherMultiplicity = this.otherToThisMultiplicity.inverse();
		}
	}
	public Link getLink() {
		return link;
	}
	public boolean isSrcLink() {
		return isSrcLink;
	}
	public String getThisTableName() {
		return thisTableName;
	}
	public String getOtherTableName() {
		return otherTableName;
	}
	public String getThisLinkName() {
		return thisLinkName;
	}
	public String getOtherLinkName() {
		return otherLinkName;
	}
	public Link.Multiplicity getThisToOtherMultiplicity() {
		return thisToOtherMultiplicity;
	}
	public Link.Multiplicity getOtherToThisMultiplicity() {
		return otherToThisMultiplicity;
	}
	
	private boolean linkTargetPresent(GlueDataObject thisObject, GlueDataObject otherObject) {
		switch(thisToOtherMultiplicity) {
		case ONE_TO_ONE: {
			GlueDataObject thisCurrentTarget = (GlueDataObject) thisObject.readProperty(thisLinkName);
			return thisCurrentTarget != null && thisCurrentTarget.pkMap().equals(otherObject.pkMap());
		}
		case ONE_TO_MANY: {
			GlueDataObject otherCurrentTarget = (GlueDataObject) otherObject.readProperty(otherLinkName);
			return otherCurrentTarget != null && otherCurrentTarget.pkMap().equals(thisObject.pkMap());
		}
		case MANY_TO_ONE: {
			GlueDataObject thisCurrentTarget = (GlueDataObject) thisObject.readProperty(thisLinkName);
			return thisCurrentTarget != null && thisCurrentTarget.pkMap().equals(otherObject.pkMap());
		}
		default: 
			throw new RuntimeException("Unknown multiplicity");
		}
	}
	
	// precondition -- link exists between this and other.
	private void removeLinkTarget(GlueDataObject thisObject, GlueDataObject otherObject) {
		switch(thisToOtherMultiplicity) {
		case ONE_TO_ONE: {
			thisObject.setToOneTarget(thisLinkName, null, false);
			otherObject.setToOneTarget(otherLinkName, null, false);
			return;
		}
		case ONE_TO_MANY: {
			thisObject.removeToManyTarget(thisLinkName, otherObject, false);
			otherObject.setToOneTarget(otherLinkName, null, false);
			return;
		}
		case MANY_TO_ONE: {
			thisObject.setToOneTarget(thisLinkName, null, false);
			otherObject.removeToManyTarget(otherLinkName, thisObject, false);
			return;
		}
		default: 
			throw new RuntimeException("Unknown multiplicity");
		}
	}

	private void addLinkTarget(GlueDataObject thisObject, GlueDataObject otherObject) {
		switch(thisToOtherMultiplicity) {
		case ONE_TO_ONE: {
			thisObject.setToOneTarget(thisLinkName, otherObject, false);
			otherObject.setToOneTarget(otherLinkName, thisObject, false);
			return;
		}
		case ONE_TO_MANY: {
			GlueDataObject otherCurrentTarget = (GlueDataObject) otherObject.readProperty(otherLinkName);
			if(otherCurrentTarget != null) {
				removeLinkTarget(otherCurrentTarget, otherObject);
			}
			thisObject.addToManyTarget(thisLinkName, otherObject, false);
			otherObject.setToOneTarget(otherLinkName, thisObject, false);
			return;
		}
		case MANY_TO_ONE: {
			GlueDataObject thisCurrentTarget = (GlueDataObject) thisObject.readProperty(thisLinkName);
			if(thisCurrentTarget != null) {
				removeLinkTarget(thisObject, thisCurrentTarget);
			}
			thisObject.setToOneTarget(thisLinkName, otherObject, false);
			otherObject.addToManyTarget(otherLinkName, thisObject, false);
			return;
		}
		default: 
			throw new RuntimeException("Unknown multiplicity");
		}
	}

	
}