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
package org.dswarm.controller.resources.resource.test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dswarm.controller.resources.test.ResourceTest;

public class NonExistingResourceTest extends ResourceTest {

	private static final Logger	LOG					= LoggerFactory.getLogger(NonExistingResourceTest.class);

	private static final String	resourceDirective	= "blablub";

	public NonExistingResourceTest() {

		super(NonExistingResourceTest.resourceDirective);
	}

	@Test
	public void testNonExistingResource() throws Exception {

		NonExistingResourceTest.LOG.debug("expecting NotFoundException near this, because we are testing this exception here");

		final Response response = target().request().accept(MediaType.APPLICATION_JSON_TYPE).get(Response.class);

		Assert.assertEquals("404 NOT FOUND was expected", 404, response.getStatus());

		final String responseString = response.readEntity(String.class);
	}
}
