package de.thingweb.directory.coap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.handlers.TDLookUpHandler;
import de.thingweb.directory.handlers.ThingDescriptionCollectionHandler;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;

public class CoAPRESTResource extends CoapResource {

	protected RESTHandler handler;
	
	public CoAPRESTResource(RESTHandler handler) {
		super(handler.name());
		this.handler = handler;

		if (handler instanceof ThingDescriptionCollectionHandler) {
			this.getAttributes().addResourceType("core.rd");
			super.setObservable(true);
			
		} else if (handler instanceof TDLookUpHandler) {
			this.getAttributes().addResourceType("core.rd-lookup");
		}
	}
	
	public void hasChanged() {
		super.changed();
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		try {
			RESTResource res = handler.get(uri(), params(exchange));
			exchange.respond(ResponseCode.VALID, res.content, toContentFormatCode(res.contentType));
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (NotFoundException e) {
			exchange.respond(ResponseCode.NOT_FOUND);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
	}
	
	public void handlePOST(CoapExchange exchange) {
		try {
			RESTResource resource = handler.post(uri(), params(exchange), payload(exchange));
			if(resource.content != null){
				exchange.respond(ResponseCode.VALID, resource.content, toContentFormatCode(resource.contentType));
			}
			else{
			Response response = new Response(ResponseCode.CREATED);
			response.setOptions(new OptionSet().addLocationPath(trim(resource.path)));
			exchange.respond(response);
			}
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		this.hasChanged();
	}
	
	@Override
	public void handlePUT(CoapExchange exchange) {
		try {
			handler.put(uri(), params(exchange), payload(exchange));
			exchange.respond(ResponseCode.CHANGED);
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		this.hasChanged();
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		try {
			handler.delete(uri(), params(exchange), payload(exchange));
			exchange.respond(ResponseCode.DELETED);
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		this.hasChanged();
	}
	
	protected URI uri() {
		return URI.create(ThingDirectory.get().baseURI + getURI());
	}
	
	protected Map<String, String> params(CoapExchange exchange) {
		Map<String, String> params = new HashMap<>();
		try {
			// content negotiation headers
			Integer acceptCode = exchange.getRequestOptions().getAccept();
			if (acceptCode > 0) {
				params.put(RESTHandler.PARAMETER_ACCEPT, MediaTypeRegistry.toString(acceptCode));
			}
			Integer cfCode = exchange.getRequestOptions().getContentFormat();
			if (cfCode > 0) {
				params.put(RESTHandler.PARAMETER_CONTENT_TYPE, MediaTypeRegistry.toString(cfCode));
			}
			
			// query parameters
			for (String pair : exchange.getRequestOptions().getUriQuery()) {
				pair = URLDecoder.decode(pair, "UTF-8");
				if (pair.contains("=")) {
					String[] p = pair.split("=");
					if (p.length > 1) {
						params.put(p[0], p[1]);
					} else {
						params.put(p[0], "");
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			System.err.println("UTF-8 encoding not supported!");
		}
		return params;
	}
	
	protected InputStream payload(CoapExchange exchange) {
	  return new ByteArrayInputStream(exchange.getRequestPayload());
	}
	
	protected String trim(String path) {
	  if (path.charAt(0) == '/') {
		return path.substring(1);
	  }
	  return path;
	}

	protected int toContentFormatCode(String contentType) {
		if (contentType.equals("application/ld+json")) {
			contentType = "application/json";
		}
    	return MediaTypeRegistry.parse(contentType);
	}

}