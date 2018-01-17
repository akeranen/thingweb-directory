package de.thingweb.directory.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import de.thingweb.directory.rest.IndexResource;
import de.thingweb.directory.rest.MethodNotAllowedException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class WelcomePageResource extends IndexResource {
	
	public WelcomePageResource() {
		super(null);
		
		TDCollectionResource tds = new TDCollectionResource();
		
		children.add(tds);
		children.add(new TDLookUpCollectionResource(tds));
		children.add(new VocabularyCollectionResource());
		children.add(new SwaggerResource());
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		// TODO return HTML page
		super.get(parameters, out);
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}

}
