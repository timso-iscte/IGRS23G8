
/*
 * $Id: EchoServlet.java,v 1.5 2003/06/22 12:32:15 fukuda Exp $
 */
package org.mobicents.servlet.sip.example;

import java.util.*;
import java.io.IOException;

import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.ServletException;
import javax.servlet.sip.URI;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipFactory;

/**
 */
public class Myapp extends SipServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private Map<String, String> RegistrarDB;
	static private SipFactory factory;

	public Myapp() {
		super();
		RegistrarDB = new HashMap<String, String>();
	}

	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}

	/**
	 * Acts as a registrar and location service for REGISTER messages
	 * 
	 * @param request The SIP message received by the AS
	 */
	protected void doRegister(SipServletRequest request) throws ServletException,
			IOException {

		String from = request.getHeader("From");
		log(from);
		String aux = getSIPuri(from);
		log(aux);
		// String to = request.getHeader("To");
		// log(to);
		// aux = getSIPuriPort(to);
		// log(aux);
		String contact2 = request.getHeader("Contact");
		log(contact2);
		// aux = getSIPuri(contact2);
		// log(aux);
		// aux = getSIPuriPort(contact2);
		// log(aux);

		if (!(from.substring(from.lastIndexOf("@") + 1)).contains("a.pt")) {
			SipServletResponse response;
			response = request.createResponse(403);
			response.send();
		} else {

			String aor = getSIPuri(request.getHeader("To"));
			String contact = getSIPuriPort(request.getHeader("Contact"));

			RegistrarDB.put(aor, contact);
			SipServletResponse response;
			response = request.createResponse(200);
			response.send();

			// Some logs to show the content of the Registrar database.
			log("REGISTER (myapp):***");
			Iterator<Map.Entry<String, String>> it = RegistrarDB.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
				System.out.println(pairs.getKey() + " = " + pairs.getValue());
			}
			log("REGISTER (myapp):***");
		}
	}

	/**
	 * Sends SIP replies to INVITE messages
	 * - 300 if registred
	 * - 404 if not registred
	 * 
	 * @param request The SIP message received by the AS
	 */
	protected void doInvite(SipServletRequest request)
			throws ServletException, IOException {

		// Some logs to show the content of the Registrar database.
		log("INVITE (myapp):***");
		Iterator<Map.Entry<String, String>> it = RegistrarDB.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}
		log("INVITE (myapp):***");

		// String aor = getSIPuri(request.getHeader("To")); // Get the To AoR
		// if (!RegistrarDB.containsKey(aor)) { // To AoR not in the database, reply 404
		// SipServletResponse response;
		// response = request.createResponse(404);
		// response.send();
		// } else {
		// SipServletResponse response = request.createResponse(300);
		// // Get the To AoR contact from the database and add it to the response
		// response.setHeader("Contact",RegistrarDB.get(aor));
		// response.send();
		// }
		// SipServletResponse response = request.createResponse(404);
		// response.send();

		String aor = getSIPuri(request.getHeader("To")); // Get the To AoR
		String domain = aor.substring(aor.indexOf("@") + 1, aor.length());
		log(domain);
		if (domain.equals("a.pt")) { // The To domain is the same as the server
			if (!RegistrarDB.containsKey(aor)) { // To AoR not in the database, reply 404
				SipServletResponse response;
				response = request.createResponse(404);
				response.send();
			} else {
				Proxy proxy = request.getProxy();
				proxy.setRecordRoute(false);
				proxy.setSupervised(false);
				URI toContact = factory.createURI(RegistrarDB.get(aor));
				proxy.proxyTo(toContact);
			}
		} else {
			Proxy proxy = request.getProxy();
			proxy.proxyTo(request.getRequestURI());
		}

		/*
		 * if (!RegistrarDB.containsKey(aor)) { // To AoR not in the database, reply 404
		 * SipServletResponse response;
		 * response = request.createResponse(404);
		 * response.send();
		 * } else {
		 * SipServletResponse response = request.createResponse(300);
		 * // Get the To AoR contact from the database and add it to the response
		 * response.setHeader("Contact",RegistrarDB.get(aor));
		 * response.send();
		 * }
		 */
	}

	/**
	 * Auxiliary function for extracting SPI URIs
	 * 
	 * @param uri A URI with optional extra attributes
	 * @return SIP URI
	 */
	protected String getSIPuri(String uri) {
		String f = uri.substring(uri.indexOf("<") + 1, uri.indexOf(">"));
		int indexCollon = f.indexOf(":", f.indexOf("@"));
		if (indexCollon != -1) {
			f = f.substring(0, indexCollon);
		}
		return f;
	}

	/**
	 * Auxiliary function for extracting SPI URIs
	 * 
	 * @param uri A URI with optional extra attributes
	 * @return SIP URI and port
	 */
	protected String getSIPuriPort(String uri) {
		String f = uri.substring(uri.indexOf("<") + 1, uri.indexOf(">"));
		return f;
	}
}

// teste
