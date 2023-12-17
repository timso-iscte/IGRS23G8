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
// teste
public class Myapp extends SipServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private Map<String, String> RegistrarDB;
	static private Map<String, String> EstadosDB;
	static private SipFactory factory;

	public Myapp() {
		super();
		RegistrarDB = new HashMap<String, String>();
		EstadosDB = new HashMap<String, String>();
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

		SipServletResponse response;

		String aor = getSIPuri(request.getHeader("From"));

		if (!validateDomain(aor)) {
			response = request.createResponse(403);
			response.send();
		} else {

			String contact_field = request.getHeader("Contact");
			String contact = getSIPuriPort(request.getHeader("Contact"));
			log(contact_field);
			log(contact);
			String expirity = contact_field.substring(contact_field.lastIndexOf("=") + 1);
			log(expirity);
			if (expirity.equals("0")) {
				RegistrarDB.remove(aor);
				EstadosDB.remove(aor);
				response = request.createResponse(200);
				response.send();
				log("FAZER DERGEGISTAR");
			} else {

				RegistrarDB.put(aor, contact);
				EstadosDB.put(aor, "Available");
				response = request.createResponse(200);
				response.send();
			}

			// Some logs to show the content of the Registrar database.
			logContacts();
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

		logContacts();

		SipServletResponse response;

		String aor_remetente = getSIPuri(request.getHeader("From")); // Get the To AoR
		String aor_recipiente = getSIPuri(request.getHeader("To")); // Get the To AoR

		if (!validateDomain(aor_remetente) || !validateDomain(aor_recipiente)) {
			response = request.createResponse(403);
			response.send();

		} else {
			if (aor_recipiente.equals("sip:chat@a.pt")) {
				String sala = "sip:conf@127.0.0.1:5070";
				log("entrar na conferencia");
				log("a começar o proxy");
				Proxy proxy = request.getProxy();
				proxy.setRecordRoute(true);
				proxy.setSupervised(false);
				log("começou o proxy");
				URI toContact = factory.createURI(sala);
				log("leu bem a base de dados");
				proxy.proxyTo(toContact);
				log("proxy falhou");
			} else {
				if (!RegistrarDB.containsKey(aor_recipiente)) { // To AoR not in the database, reply 404
					response = request.createResponse(404);
					response.send();
				} else if (!EstadosDB.get(aor_recipiente).equals("Available")) {
					response = request.createResponse(486);
					response.send();

				} else {
					log("a começar o proxy");
					Proxy proxy = request.getProxy();
					proxy.setRecordRoute(true);
					proxy.setSupervised(false);
					log("começou o proxy");
					URI toContact = factory.createURI(RegistrarDB.get(aor_recipiente));
					log("leu bem a base de dados");
					proxy.proxyTo(toContact);
					log("proxy falhou");
				}
			}
		}
	}

	/**
	 * processes messages
	 * 
	 *
	 * @param request
	 */
	protected void doMessage(SipServletRequest request)
			throws ServletException, IOException {
		log("entrou na function message");
		SipServletResponse response;

		String aor_remetente = getSIPuri(request.getHeader("From")); // Get the From AoR
		String aor_servidor = getSIPuri(request.getHeader("To")); // Get the To AoR
		String aor_recipiente = (String) request.getContent(); // Get the AoR on the message

		if (!validateDomain(aor_remetente) || !aor_servidor.equals("sip:gofind@a.pt")
				|| !validateDomain(aor_recipiente)) {
			response = request.createResponse(403);
			response.send();
		} else if (!EstadosDB.containsKey(aor_recipiente)) {
			response = request.createResponse(404);
			response.send();
		} else {
			if (!EstadosDB.get(aor_recipiente).equals("Available")) {
				response = request.createResponse(200);
				// response.setHeader("Content-Type", "text/plain");
				// response.setContent("data-text-lines", "teste");
				response.send();
			}

		}

	}

	protected void doResponse(SipServletRequest request)
			throws ServletException, IOException {

	}

	protected void doAck(SipServletRequest request)
			throws ServletException, IOException {
		log("-------------------------------------------------------------------------------------------entrei no doAck");
		String aor_remetente = getSIPuri(request.getHeader("From")); // Get the To AoR
		String aor_recipiente = getSIPuri(request.getHeader("To")); // Get the To AoR
		if (!aor_recipiente.equals("sip:chat@a.pt")) {
			EstadosDB.put(aor_remetente, "Busy");
			EstadosDB.put(aor_recipiente, "Busy");
		} else {
			EstadosDB.put(aor_remetente, "In Conference");
		}
		logContacts();

	}

	/**
	 * processes Bye
	 * 
	 *
	 * @param request
	 */
	protected void doBye(SipServletRequest request)
			throws ServletException, IOException {

		log("entrou no doBye");

		String aor_remetente = getSIPuri(request.getHeader("From")); // Get the To AoR
		String aor_recipiente = getSIPuri(request.getHeader("To")); // Get the To AoR

		if (!aor_recipiente.equals("sip:chat@a.pt")) {
			EstadosDB.put(aor_remetente, "Available");
			EstadosDB.put(aor_recipiente, "Available");
		} else {
			EstadosDB.put(aor_remetente, "Available");
		}
		logContacts();

	}

	protected void logContacts() {
		log("***Contacts (myapp):***");
		Iterator<Map.Entry<String, String>> it = RegistrarDB.entrySet().iterator();
		Iterator<Map.Entry<String, String>> it2 = EstadosDB.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}

		while (it2.hasNext()) {
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it2.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}
		log("***Contacts (myapp):***");

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

	protected boolean validateDomain(String aor) {
		String domain = aor.substring(aor.lastIndexOf("@") + 1);
		return domain.equals("a.pt");
	}

}

// teste
