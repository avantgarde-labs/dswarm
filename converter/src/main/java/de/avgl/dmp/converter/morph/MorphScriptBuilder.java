package de.avgl.dmp.converter.morph;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import scala.collection.mutable.LinkedHashSet;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

import de.avgl.dmp.converter.DMPConverterException;
import de.avgl.dmp.init.util.DMPStatics;
import de.avgl.dmp.persistence.model.job.Component;
import de.avgl.dmp.persistence.model.job.Function;
import de.avgl.dmp.persistence.model.job.Mapping;
import de.avgl.dmp.persistence.model.job.Task;
import de.avgl.dmp.persistence.model.job.Transformation;
import de.avgl.dmp.persistence.model.schema.Attribute;
import de.avgl.dmp.persistence.model.schema.MappingAttributePathInstance;

/**
 * Creates a metamorph script from a given {@link Task}.
 *
 * @author phorn
 * @author tgaengler
 *
 */
public class MorphScriptBuilder {

	private static final org.apache.log4j.Logger	LOG							= org.apache.log4j.Logger.getLogger(MorphScriptBuilder.class);

	private static final String						MAPPING_PREFIX				= "mapping";

	private static final DocumentBuilderFactory		DOC_FACTORY					= DocumentBuilderFactory.newInstance();

	private static final String						SCHEMA_PATH					= "schemata/metamorph.xsd";

	private static final TransformerFactory			TRANSFORMER_FACTORY;

	private static final String						TRANSFORMER_FACTORY_CLASS	= "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";

	static {
		System.setProperty("javax.xml.transform.TransformerFactory", TRANSFORMER_FACTORY_CLASS);
		TRANSFORMER_FACTORY = TransformerFactory.newInstance();
		TRANSFORMER_FACTORY.setAttribute("indent-number", 4);

		final URL resource = Resources.getResource(SCHEMA_PATH);
		final InputSupplier<InputStream> inputStreamInputSupplier = Resources.newInputStreamSupplier(resource);

		try (final InputStream schemaStream = inputStreamInputSupplier.getInput()) {

			// final StreamSource SCHEMA_SOURCE = new StreamSource(schemaStream);
			final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = null;

			try {

				// TODO: dummy schema right now, since it couldn't parse the metamorph schema for some reason
				schema = sf.newSchema();
			} catch (final SAXException e) {

				e.printStackTrace();
			}

			if (schema == null) {

				LOG.error("couldn't parse schema");
			}

			DOC_FACTORY.setSchema(schema);

		} catch (final IOException e1) {
			LOG.error("couldn't read schema resource", e1);
		}
	}

	private Document								doc;

	private Element varDefinition(final String key, final String value) {
		final Element var = doc.createElement("var");
		var.setAttribute("name", key);
		var.setAttribute("value", value);

		return var;
	}

	public String render(final boolean indent, final Charset encoding) {
		final String defaultEncoding = encoding.name();
		final Transformer transformer;
		try {
			transformer = TRANSFORMER_FACTORY.newTransformer();
		} catch (final TransformerConfigurationException e) {
			e.printStackTrace();
			return null;
		}

		transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");

		transformer.setOutputProperty(OutputKeys.ENCODING, defaultEncoding);

		final ByteArrayOutputStream stream = new ByteArrayOutputStream();

		final StreamResult result;
		try {
			result = new StreamResult(new OutputStreamWriter(stream, defaultEncoding));
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

		try {
			transformer.transform(new DOMSource(doc), result);
		} catch (final TransformerException e) {
			e.printStackTrace();
			return null;
		}

		try {
			return stream.toString(defaultEncoding);
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String render(final boolean indent) {
		return render(indent, Charset.forName("UTF-8"));
	}

	@Override
	public String toString() {
		return render(true);
	}

	public File toFile() throws IOException {
		final String str = render(false);

		final File file = File.createTempFile("avgl_dmp", ".tmp");

		final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(str);
		bw.close();

		return file;
	}

	// public MorphScriptBuilder apply(final List<Transformation> transformations) throws DMPConverterException {
	// final DocumentBuilder docBuilder;
	// try {
	// docBuilder = DOC_FACTORY.newDocumentBuilder();
	// } catch (ParserConfigurationException e) {
	// throw new DMPConverterException(e.getMessage());
	// }
	//
	// doc = docBuilder.newDocument();
	// doc.setXmlVersion("1.0");
	//
	// final Element rootElement = doc.createElement("metamorph");
	// rootElement.setAttribute("xmlns", "http://www.culturegraph.org/metamorph");
	// rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
	// rootElement.setAttribute("xsi:schemaLocation", "http://www.culturegraph.org/metamorph metamorph.xsd");
	// rootElement.setAttribute("entityMarker", ".");
	// rootElement.setAttribute("version", "1");
	// doc.appendChild(rootElement);
	//
	// final Element meta = doc.createElement("meta");
	// rootElement.appendChild(meta);
	//
	// final Element metaName = doc.createElement("name");
	// meta.appendChild(metaName);
	//
	// // final Element vars = doc.createElement("vars");
	// // rootElement.appendChild(vars);
	//
	// final Element rules = doc.createElement("rules");
	// rootElement.appendChild(rules);
	//
	// final List<String> metas = Lists.newArrayList();
	//
	// for (final Transformation transformation : transformations) {
	// metas.add(transformation.getName());
	//
	// // for (final Element var: createVarDefinitions(transformation)) {
	// // vars.appendChild(var);
	// // }
	//
	// final Element data = createTransformation(transformation);
	//
	// rules.appendChild(data);
	// }
	//
	// metaName.setTextContent(Joiner.on(", ").join(metas));
	//
	// return this;
	// }

	// public MorphScriptBuilder apply(final Transformation transformation) throws DMPConverterException {
	//
	// return apply(Lists.newArrayList(transformation));
	// }

	public MorphScriptBuilder apply(final Task task) throws DMPConverterException {

		final DocumentBuilder docBuilder;
		try {
			docBuilder = DOC_FACTORY.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new DMPConverterException(e.getMessage());
		}

		doc = docBuilder.newDocument();
		doc.setXmlVersion("1.1");

		final Element rootElement = doc.createElement("metamorph");
		rootElement.setAttribute("xmlns", "http://www.culturegraph.org/metamorph");
		rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		rootElement.setAttribute("xsi:schemaLocation", "http://www.culturegraph.org/metamorph metamorph.xsd");
		rootElement.setAttribute("entityMarker", DMPStatics.ATTRIBUTE_DELIMITER.toString());
		rootElement.setAttribute("version", "1");
		doc.appendChild(rootElement);

		final Element meta = doc.createElement("meta");
		rootElement.appendChild(meta);

		final Element metaName = doc.createElement("name");
		meta.appendChild(metaName);

		// final Element vars = doc.createElement("vars");
		// rootElement.appendChild(vars);

		final Element rules = doc.createElement("rules");
		rootElement.appendChild(rules);

		final List<String> metas = Lists.newArrayList();

		// TODO: utilise mappings and all available data to generate the morph transformations
		// i.e. take the transformation of each mapping and handle the variable replacement correctly (?) or create a mapping in
		// the morph script for the variables

		for (final Mapping mapping : task.getJob().getMappings()) {
			metas.add(MAPPING_PREFIX + mapping.getId());

			// for (final Element var: createVarDefinitions(transformation)) {
			// vars.appendChild(var);
			// }

			// TODO: obsolete, since we expect mappings to produce multiple datas from now on:
			final Element data = createTransformation(mapping);

			rules.appendChild(data);
		}

		metaName.setTextContent(Joiner.on(", ").join(metas));

		return this;
	}

	private void createTransformations(Element rules, Mapping mapping) {
		
		// add all transformation rules (datas) to the rules that are required to implement the mapping
		// this algorithm starts at the output-side (since thsi way we can traverse a tree from its root)
		// (this is only possible as long as we allow only 1 output attribute path for mappings and 1 output component for transformations)
		
		
		// first handle the parameter mapping from the attribute paths of the mapping to the transformation component

		final Component transformationComponent = mapping.getTransformation();
		
		if(transformationComponent == null) {
			
			LOG.debug("transformation component for mapping '" + mapping.getId() + "' was empty");
			return;
		}
	
		// get all input attribute paths and create datas for them
		
		Set<MappingAttributePathInstance> inputAttributePathInstances = mapping.getInputAttributePaths();
		
		List<String> inputAttributePaths = new LinkedList<String>();
		
		for (Iterator<MappingAttributePathInstance> iterator = inputAttributePathInstances.iterator(); iterator
				.hasNext();) {
			
			MappingAttributePathInstance mappingAttributePathInstance = (MappingAttributePathInstance) iterator
					.next();
			
			String inputAttributePathString = mappingAttributePathInstance.getAttributePath().toAttributePath();
			
			final Element data = doc.createElement("data");
			data.setAttribute("source", inputAttributePathString);
			data.setAttribute("name", "@" + getKeyParameterMapping(inputAttributePathString, transformationComponent));
			
			// TODO: filter inputAttributePath in data section
			
			inputAttributePaths.add(inputAttributePathString);

			rules.appendChild(data);
		}
		
		final String outputAttributePath = mapping.getOutputAttributePath().getAttributePath().toAttributePath();
		
		final Element dataOutput = doc.createElement("data");
		dataOutput.setAttribute("source", "@" + getKeyParameterMapping(outputAttributePath, transformationComponent));
		dataOutput.setAttribute("name", outputAttributePath);
		rules.appendChild(dataOutput);		
		
		final Function transformationFunction = transformationComponent.getFunction();
		
		if(transformationFunction == null) {
			
			LOG.debug("transformation component's function for mapping '" + mapping.getId() + "' was empty");
			return;
		}
		
		
		switch (transformationFunction.getFunctionType()) {

		case Function:

			// TODO: process simple function
			
			LOG.error("transformation component's function for mapping '" + mapping.getId() + "' was a real FUNCTION. this is not supported right now.");

			break;
			
		case Transformation:

			// TODO: process simple input -> output mapping (?)

			final Transformation transformation = (Transformation) transformationFunction;
	
			final Set<Component> components = transformation.getComponents();
			
			if(components == null) {
				
				LOG.debug("transformation component's transformation's components for mapping '" + mapping.getId() + "' are empty");
				return;
			}
			
				
			for (final Component component : components) {

				String[] inputStrings = {};

				Map<String, String> componentParameterMapping = component.getParameterMappings();

				if (componentParameterMapping != null) {

					for (Entry<String, String> parameterMapping : componentParameterMapping.entrySet()) {

						if (parameterMapping.getKey().equals("inputString")) {

							inputStrings = parameterMapping.getValue().split(",");

							break;
						}
					}
				}
				
				LinkedList<String> sourceAttributes = new LinkedList<String>();
				
				for (String inputString : inputStrings) {
					
					sourceAttributes.add(inputString);
				}
				
				if (component.getInputComponents() != null) {
					
					for (Component inputComponent : component.getInputComponents()) {
						
						sourceAttributes.add("component" + inputComponent.getId());
					}
				}
				
				if (sourceAttributes.size() > 1) {
					
					String collectionNameAttribute = null;
					
					if (component.getOutputComponents() == null) {
						
						collectionNameAttribute = getKeyParameterMapping(outputAttributePath, transformationComponent);
						
					} else {
						
						collectionNameAttribute = "component" + component.getId();
					}
					
					
					final Element collection = createCollectionTag(component, collectionNameAttribute, sourceAttributes);
					
					rules.appendChild(collection);
					
				} 
				else if (sourceAttributes.size() == 1) {
					
					String dataNameAttribute = null;
					
					if (component.getOutputComponents() == null) {
						
						dataNameAttribute = getKeyParameterMapping(outputAttributePath, transformationComponent);
						
					} else {
						
						dataNameAttribute = "component" + component.getId();
					}
					
										
					final Element data = createDataTag(component, dataNameAttribute, sourceAttributes.get(0));
					
					rules.appendChild(data);			
					
				}				
					
			}

			break;
		}

	}

	/** 
	 * Recursively creates the transformation for the component if there are some input components,
	 *  or if there are none, creates the final transformation datas to the main component 
	 *  of the mapping
	 * 
	 * @param processingComponent
	 */
	private void createTransformationsForComponent(Component processingComponent, Element rules) {

		
		final Function transformationFunction = processingComponent.getFunction();
		
		if(transformationFunction == null) {
			
			LOG.debug("function/transformation for transformation component '" + processingComponent.getId() + "' was empty");
			return;
			
		} else {
			
			// TODO handle function of this component
			LOG.debug("processing function/transformation '" + transformationFunction.getId() + " (" + transformationFunction.getName() + ")");
			
			switch (transformationFunction.getFunctionType()) {

			case Function:

				// TODO: process simple function (required for our example!)
				
				LOG.error("function '" + transformationFunction.getId() + "' was a real FUNCTION. this is not supported right now.");

				break;
				
			case Transformation:

				// TODO: process simple input -> output mapping (?)

				final Transformation transformation = (Transformation) transformationFunction;
		
				final Set<Component> components = transformation.getComponents();
				
				if(components == null) {
					
					LOG.debug("components for transformation '" + transformation.getId() + "' are empty");
					return;
				}
			}
			
		}
		
		
		// handle input components recursively
		
		final Set<Component> inputComponents = processingComponent.getInputComponents();

		if (inputComponents != null && !inputComponents.isEmpty()) {
			
			int numberOfInputComponents = inputComponents.size();
			LOG.debug("Number of input components: " + numberOfInputComponents );
			
			if(numberOfInputComponents > 1) {
				// TODO create compound /collect when more than 1 input component
			}
			
			for (Iterator<Component> iterator = inputComponents.iterator(); iterator
					.hasNext();) {
				
				Component inputComponent = (Component) iterator.next();
				
				LOG.debug("creating transformations for input component '" + inputComponent.getId() + 
						" (" + inputComponent.getName() + ")");

				final Element intraData = doc.createElement("data");
				intraData.setAttribute("name", "todo");
				intraData.setAttribute("source", "todo");
				rules.appendChild(intraData);
				
				// TODO
				createTransformationsForComponent(inputComponent, rules);
			}

			
		} else { 
			
			// this component has no input components and needs to be directly mapped to the main transformation component / attribute paths
			
			LOG.debug("no input components for component " + processingComponent.getId() + " (" + processingComponent.getName() + "), "
					+ "will create transformation to main transformation component");
			
			final Element leafData = doc.createElement("data");

			// TODO (re)use createParameters(..) here for adding name/source attributes?
			leafData.setAttribute("name", "todo: get variable of the main transformation component");
			leafData.setAttribute("source", "transformationInputString"); // is this qualified enough?? probably needs prefix

			rules.appendChild(leafData);
		}
		

		
	}

	private Element createTransformation(final Mapping mapping) {

		final Element data = doc.createElement("data");

		// TODO: only one input attribute path for now
		final String inputAttributePath = mapping.getInputAttributePaths().iterator().next().getAttributePath().toAttributePath();
		final String outputAttributePath = mapping.getOutputAttributePath().getAttributePath().toAttributePath();

		// TODO: this is for the processing algorithm later
		// final Map<String, String> parameterMappings = mapping.getTransformation().getParameterMappings();
		//
		// String inputVariable = null;
		// String outputVariable = null;
		//
		// // determine input and output variables from parameter mappings
		// for (final Entry<String, String> parameterMapping : parameterMappings.entrySet()) {
		//
		// if (inputAttributePath.equals(parameterMapping.getValue())) {
		//
		// inputVariable = parameterMapping.getKey();
		// }
		//
		// if (outputAttributePath.equals(parameterMapping.getValue())) {
		//
		// outputVariable = parameterMapping.getKey();
		// }
		// }

		// data.setAttribute("source", "record." + transformation.getSource().getName());

		// TODO: maybe declare variables as metamorph variables (?) -> the format was something like ${variablename}
		// TODO: maybe remove this "record" identifier or determine it via input_data_model -> schema -> record_class
		data.setAttribute("source", inputAttributePath);
		// data.setAttribute("name", "record." + transformation.getTarget().getName());
		data.setAttribute("name", outputAttributePath);
		
		final Component transformationComponent = mapping.getTransformation();
		
		if(transformationComponent == null) {
			
			LOG.debug("transformation component for mapping '" + mapping.getId() + "' was empty");
			
			return data;
		}

		final Function transformationFunction = transformationComponent.getFunction();
		
		if(transformationFunction == null) {
			
			LOG.debug("transformation component's function for mapping '" + mapping.getId() + "' was empty");
			
			return data;
		}

		switch (transformationFunction.getFunctionType()) {

			case Function:

				// TODO: process simple function
				
				LOG.error("transformation component's function for mapping '" + mapping.getId() + "' was a real FUNCTION. this is not supported right now.");

				break;
			case Transformation:

				// TODO: process simple input -> output mapping (?)

				final Transformation transformation = (Transformation) transformationFunction;

				// determine start component(s) ( = a component that has the input variable as value of a parameter mapping) and
				// follow the output components
				final Set<Component> components = transformation.getComponents();
				
				if(components == null) {
					
					LOG.debug("transformation component's transformation's components for mapping '" + mapping.getId() + "' are empty");
					
					return data;
				}

				Component sourceComponent = null;

				for (final Component component : components) {

					// TODO: this is for the processing algorithm later
					// if (component.getParameterMappings().containsValue(inputVariable)) {
					//
					// sourceComponent = component;
					// }

					if (component.getInputComponents() == null) {

						sourceComponent = component;

						break;
					}

					if (component.getInputComponents().isEmpty()) {

						sourceComponent = component;

						break;
					}
				}

				Component processingComponent = sourceComponent;
				Component previousProcessingComponent = null;
				
				if(processingComponent == null) {
					
					LOG.debug("could find start component for transformation of mapping '" + mapping.getId() + "'");
					
					return data;
				}

				while (processingComponent != null) {

					final Element comp = doc.createElement(processingComponent.getFunction().getName());

					createParameters(processingComponent.getParameterMappings(), comp);
					data.appendChild(comp);

					previousProcessingComponent = processingComponent;

					final Set<Component> outputComponents = previousProcessingComponent.getOutputComponents();

					if (outputComponents != null && !outputComponents.isEmpty()) {

						// TODO: only one output component for now
						processingComponent = outputComponents.iterator().next();
					} else {

						processingComponent = null;
					}
				}

				break;
		}

		return data;
	}

	private void createParameters(final Map<String, String> parameterMappings, final Element component) {

		// TODO: parse parameter values that can be simple string values, JSON objects or JSON arrays (?)
		// => for now we expect only simple string values

		if (parameterMappings != null) {

			for (final Entry<String, String> parameterMapping : parameterMappings.entrySet()) {

				if (parameterMapping.getKey() != null) {
					
					if (parameterMapping.getKey().equals("inputString"))
						continue;

					if (parameterMapping.getValue() != null) {

						final Attr param = doc.createAttribute(parameterMapping.getKey());
						param.setValue(parameterMapping.getValue());
						component.setAttributeNode(param);
					}
				}
			}

			// for (final Parameter parameter : parameters.values()) {
			// if (parameter.getName() != null) {
			// if (parameter.getData() != null) {
			// final Attr param = doc.createAttribute(parameter.getName());
			// param.setValue(parameter.getData());
			// component.setAttributeNode(param);
			// } else if (parameter.isRepeat()) {
			// final Element subEl = doc.createElement(parameter.getName());
			// component.appendChild(subEl);
			// createParameters(parameter.getParameters(), subEl);
			// }
			// }
			// }
		}
	}
	
//	private Element createDataTag(final Component singleInputComponent, final String dataNameAttribute) {
//		
//		final Element data = doc.createElement("data");
//		
//		data.setAttribute("source", "@component" + singleInputComponent.getInputComponents().iterator().next().getId());
//		
//		data.setAttribute("name", "@" + dataNameAttribute);
//		
//		final Element comp = doc.createElement(singleInputComponent.getFunction().getName());
//		
//		createParameters(singleInputComponent.getParameterMappings(), comp);
//		
//		data.appendChild(comp);
//		
//		return data;
//	}

	private Element createDataTag(final Component singleInputComponent, final String dataNameAttribute, final String dataSourceAttribute) {
		
		final Element data = doc.createElement("data");
		
		data.setAttribute("source", "@" + dataSourceAttribute);
		
		data.setAttribute("name", "@" + dataNameAttribute);
		
		final Element comp = doc.createElement(singleInputComponent.getFunction().getName());
		
		createParameters(singleInputComponent.getParameterMappings(), comp);
		
		data.appendChild(comp);
		
		return data;
	}
	
	private Element createCollectionTag(final Component multipleInputComponent, final String collectionNameAttribute, final List<String> collectionSourceAttributes) {
		
		final Element collection = doc.createElement(multipleInputComponent.getFunction().getName());
		
		createParameters(multipleInputComponent.getParameterMappings(), collection);
		
		collection.setAttribute("name", "@" + collectionNameAttribute);
		
		for (String sourceAttribute : collectionSourceAttributes) {
			
			final Element collectionData = doc.createElement("data");
			
			collectionData.setAttribute("source", "@" + sourceAttribute);
			
			collection.appendChild(collectionData);
		}
		
		return collection;
	}

	private String getKeyParameterMapping(String attributePathString, Component transformationComponent) {
		
		String attributePathKey = null;
		
		Map<String, String> transformationParameterMapping = transformationComponent.getParameterMappings();
		
		for (Entry<String, String> parameterMapping : transformationParameterMapping.entrySet()) {
			
			if (parameterMapping.getValue().equals(attributePathString)) {
				
				attributePathKey = parameterMapping.getKey();
			}
		}
		
		return attributePathKey;
	}
	
	// temp until complex task test is complete
	public Object apply(Mapping mapping) throws DMPConverterException {
		final DocumentBuilder docBuilder;
		try {
			docBuilder = DOC_FACTORY.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new DMPConverterException(e.getMessage());
		}

		doc = docBuilder.newDocument();
		doc.setXmlVersion("1.1");

		final Element rootElement = doc.createElement("metamorph");
		rootElement.setAttribute("xmlns", "http://www.culturegraph.org/metamorph");
		rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		rootElement.setAttribute("xsi:schemaLocation", "http://www.culturegraph.org/metamorph metamorph.xsd");
		rootElement.setAttribute("entityMarker", DMPStatics.ATTRIBUTE_DELIMITER.toString());
		rootElement.setAttribute("version", "1");
		doc.appendChild(rootElement);

		final Element meta = doc.createElement("meta");
		rootElement.appendChild(meta);

		final Element metaName = doc.createElement("name");
		meta.appendChild(metaName);

		// final Element vars = doc.createElement("vars");
		// rootElement.appendChild(vars);

		final Element rules = doc.createElement("rules");
		rootElement.appendChild(rules);

		final List<String> metas = Lists.newArrayList();

		// TODO: utilise mappings and all available data to generate the morph transformations
		// i.e. take the transformation of each mapping and handle the variable replacement correctly (?) or create a mapping in
		// the morph script for the variables

		metas.add(MAPPING_PREFIX + mapping.getId());
		createTransformations(rules,mapping);

		metaName.setTextContent(Joiner.on(", ").join(metas));

		return this;

	}

	// private Iterable<Element> createVarDefinitions(final Transformation transformation) {
	//
	// final ArrayList<Element> vars = Lists.newArrayListWithCapacity(4);
	//
	// vars.add(varDefinition("source.resource.id", String.valueOf(transformation.getSource().getResourceId())));
	// vars.add(varDefinition("source.configuration.id", String.valueOf(transformation.getSource().getConfigurationId())));
	// vars.add(varDefinition("target.resource.id", String.valueOf(transformation.getTarget().getResourceId())));
	// vars.add(varDefinition("target.configuration.id", String.valueOf(transformation.getTarget().getConfigurationId())));
	//
	// return vars;
	// }
}
