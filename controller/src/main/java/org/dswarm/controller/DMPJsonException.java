/**
 * Copyright (C) 2013, 2014 SLUB Dresden & Avantgarde Labs GmbH (<code@dswarm.org>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dswarm.controller;

/**
 * User: knut Date: 12/12/13 Time: 1:36 PM
 */
public class DMPJsonException extends DMPControllerException {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * Creates a new DMP exception with the given exception message and a cause.
	 * 
	 * @param message the exception message
	 * @param cause a previously thrown exception, causing this one
	 */
	public DMPJsonException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
