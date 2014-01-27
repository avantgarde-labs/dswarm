package de.avgl.dmp.controller.resources.test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.avgl.dmp.controller.resources.BasicResource;
import de.avgl.dmp.controller.resources.test.utils.AttributesResourceTestUtils;
import de.avgl.dmp.persistence.model.schema.Attribute;
import de.avgl.dmp.persistence.model.schema.proxy.ProxyAttribute;
import de.avgl.dmp.persistence.service.schema.AttributeService;

public class AttributesResourceTest extends BasicResourceTest<AttributesResourceTestUtils, AttributeService, ProxyAttribute, Attribute, Long> {

	private static final org.apache.log4j.Logger	LOG	= org.apache.log4j.Logger.getLogger(AttributesResourceTest.class);

	public AttributesResourceTest() {

		super(Attribute.class, AttributeService.class, "attributes", "attribute1.json", new AttributesResourceTestUtils());
	}

	@Test
	public void testUniquenessOfAttributes() {
		
		LOG.debug("start attribute uniqueness test");

		Attribute attribute1 = null;

		try {
			
			attribute1 = pojoClassResourceTestUtils.createObject(objectJSONString, expectedObject);
		} catch (Exception e) {
			
			LOG.error("coudln't create attribute 1 for uniqueness test");
			
			Assert.assertTrue(false);
		}
		
		Assert.assertNotNull("attribute 1 shouldn't be null in uniqueness test", attribute1);

		Attribute attribute2 = null;

		try {
			
			final Response response = target().request(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON_TYPE)
					.post(Entity.json(objectJSONString));

			Assert.assertEquals("200 OK was expected", 200, response.getStatus());

			final String responseString = response.readEntity(String.class);

			Assert.assertNotNull("the response JSON shouldn't be null", responseString);

			attribute2 = objectMapper.readValue(responseString, pojoClass);

			compareObjects(expectedObject, attribute2);
			
		} catch (Exception e) {
			
			LOG.error("coudln't create attribute 2 for uniqueness test");
			
			Assert.assertTrue(false);
		}
		
		Assert.assertNotNull("attribute 2 shouldn't be null in uniqueness test", attribute2);
		
		Assert.assertEquals("the attributes should be equal", attribute1, attribute2);
		
		cleanUpDB(attribute1);
		
		LOG.debug("end attribute uniqueness test");
	}
	
	@Override
	public void testPUTObject() throws Exception {

		LOG.debug("start attribute update test");

		Attribute attribute = null;

		try {
			
			attribute = pojoClassResourceTestUtils.createObject(objectJSONString, expectedObject);
		} catch (Exception e) {
			
			LOG.error("coudln't create attribute for update test");
			
			Assert.assertTrue(false);
		}
		
		Assert.assertNotNull("attribute shouldn't be null in update test", attribute);
		
		//modify attribute for update
		attribute.setName(attribute.getName() + " update");
		
		String attributeJSONString = objectMapper.writeValueAsString(attribute);
		
		Attribute updateAttribute = pojoClassResourceTestUtils.updateObject(attributeJSONString, attribute);
		
		Assert.assertEquals("the persisted attribute shoud be equal to the modified attribute for update", updateAttribute, attribute);
		
		ObjectNode attributeJSON = objectMapper.readValue(attributeJSONString, ObjectNode.class);
		
		Assert.assertNotNull("the attribut JSON shouldn't be null", attributeJSON);

		//uniqueness dosn't allow that
		attributeJSON.put("uri", attribute.getUri().replaceAll("http", "https"));
		
		attributeJSONString = objectMapper.writeValueAsString(attributeJSON);
		
		attribute = objectMapper.readValue(attributeJSONString, Attribute.class);
		
		updateAttribute = pojoClassResourceTestUtils.updateObject(attributeJSONString, attribute);
		
		Assert.assertNotNull("attribute shouldn't be null", attribute);
		Assert.assertNotNull("updated attribute shouldn't be null", updateAttribute);
		Assert.assertNotEquals("id should be different, when uri was \"updated\"", updateAttribute.getId(), attribute.getId());
		
		Assert.assertNotEquals("uniqueness dosn't allow update of uri", updateAttribute.getUri(), attribute.getUri());
		
		//TODO: [@fniederlein] after attribute persistence adjustments the test have to check if a new attribute was created
		
		cleanUpDB(attribute);
		
		LOG.debug("end attribute update test");
	}
}
