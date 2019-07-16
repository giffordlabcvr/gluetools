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
package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

public class RequestQueueManagerException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		CONFIG_ERROR("errorTxt"),
		QUEUE_ASSIGNMENT_ERROR("errorTxt"),
		REQUEST_ERROR("errorTxt"),
		REQUEST_INTERRUPTED("errorTxt"),
		REQUEST_CANCELLED("errorTxt"),
		EXPIRED_OR_NON_EXISTENT_REQUEST("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public RequestQueueManagerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public RequestQueueManagerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
	public CommandResult executeRequestSynchronously(CommandContext cmdContext, RequestQueue requestQueue, Request request) {
		Future<CommandResult> cmdResultFuture = requestQueue.getExecutorService().submit(new Callable<CommandResult>() {
			@Override
			public CommandResult call() throws Exception {
				CommandResult cmdResult;
				try {
					cmdResult = GluetoolsEngine.getInstance().runWithGlueClassloader(new Supplier<CommandResult>(){
						@Override
						public CommandResult get() {
							return request.getCommand().execute(cmdContext);
						}
					});
				} finally {
					cmdContext.dispose();
				}
				return cmdResult;
			}
		});
		try {
			return cmdResultFuture.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if(cause instanceof GlueException) {
				throw ((GlueException) cause);
			}
			throw new RequestQueueManagerException(cause, RequestQueueManagerException.Code.REQUEST_ERROR, cause.getLocalizedMessage());
		} catch (InterruptedException e) {
			throw new RequestQueueManagerException(e, RequestQueueManagerException.Code.REQUEST_INTERRUPTED, e.getLocalizedMessage());
		} catch (CancellationException e) {
			throw new RequestQueueManagerException(e, RequestQueueManagerException.Code.REQUEST_CANCELLED, e.getLocalizedMessage());
		}
	}
	

}