package de.avgl.dmp.persistence.model.internal.impl;

import com.fasterxml.jackson.databind.JsonNode;

import de.avgl.dmp.persistence.model.internal.Model;


public class RDFModel implements Model {
	
	private com.hp.hpl.jena.rdf.model.Model model;
	
	public RDFModel(final com.hp.hpl.jena.rdf.model.Model modelArg) {
		
		model = modelArg;
	}

	@Override
	public JsonNode toJSON() {
		// TODO Auto-generated method stub
		return null;
	}

}
