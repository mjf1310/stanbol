/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.ontologymanager.web.resources;

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OntologyLoadingException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.IrremovableOntologyException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.web.util.OntologyPrettyPrintResource;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * The REST resource of an OntoNet {@link Session} whose identifier is known.
 * 
 * @author alexdma
 * 
 */
@Path("/ontonet/session/{id}")
public class SessionResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected ONManager onMgr;

    protected OntologyProvider<TcProvider> provider;

    /*
     * Placeholder for the session manager to be fetched from the servlet context.
     */
    protected SessionManager sesMgr;

    protected Session session;

    public SessionResource(@PathParam(value = "id") String sessionId, @Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.sesMgr = (SessionManager) ContextHelper.getServiceFromContext(SessionManager.class,
            servletContext);
        this.provider = (OntologyProvider<TcProvider>) ContextHelper.getServiceFromContext(
            OntologyProvider.class, servletContext);
        this.onMgr = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        session = sesMgr.getSession(sessionId);
    }

    @GET
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response asOntologyGraph(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        // Export to Clerezza Graph, which can be rendered as JSON-LD.
        ResponseBuilder rb = Response.ok(session.export(Graph.class, merge, prefix));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE})
    public Response asOntologyMixed(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        ResponseBuilder rb;
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        // Export smaller graphs to OWLOntology due to the more human-readable rendering.
        if (merge) rb = Response.ok(session.export(Graph.class, merge, prefix));
        else rb = Response.ok(session.export(OWLOntology.class, merge, prefix));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response asOntologyOWL(@PathParam("scopeid") String scopeid,
                                  @DefaultValue("false") @QueryParam("merge") boolean merge,
                                  @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        // Export to OWLOntology, the only to support OWL formats.
        ResponseBuilder rb = Response.ok(session.export(OWLOntology.class, merge, prefix));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Used to create an OntoNet session with a specified identifier.
     * 
     * @param sessionId
     *            the identifier of the session to be created.
     * @param uriInfo
     * @param headers
     * @return {@link Status#OK} if the creation was successful, or {@link Status#CONFLICT} if a session with
     *         that ID already exists.
     */
    @PUT
    public Response createSession(@PathParam("id") String sessionId,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        try {
            session = sesMgr.createSession(sessionId);
        } catch (DuplicateSessionIDException e) {
            throw new WebApplicationException(e, CONFLICT);
        } catch (SessionLimitException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        }
        ResponseBuilder rb = Response.created(uriInfo.getRequestUri());
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Destroys the session and unmanages its ontologies (which are also lost unless stored).
     * 
     * @param sessionId
     *            the session identifier
     * @param uriInfo
     * @param headers
     * @return {@link Status#OK} if the deletion was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all.
     */
    @DELETE
    public Response deleteSession(@PathParam("id") String sessionId,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        sesMgr.destroySession(sessionId);
        session = null;
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /*
     * Needed for freemarker
     */
    public Set<OntologyScope> getAppendableScopes() {
        Set<OntologyScope> notAppended = new HashSet<OntologyScope>();
        for (OntologyScope sc : onMgr.getRegisteredScopes())
            if (!session.getAttachedScopes().contains(sc.getID())) notAppended.add(sc);
        return notAppended;
    }

    /*
     * Needed for freemarker
     */
    public Set<OntologyScope> getAppendedScopes() {
        Set<OntologyScope> appended = new HashSet<OntologyScope>();
        for (OntologyScope sc : onMgr.getRegisteredScopes())
            if (session.getAttachedScopes().contains(sc.getID())) appended.add(sc);
        return appended;
    }

    private URI getCreatedResource(String ontologyIRI) {
        return URI.create("/" + ontologyIRI);
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (session == null) rb = Response.status(NOT_FOUND);
        else rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public SortedSet<String> getOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        for (IRI iri : session.listManagedOntologies())
            result.add(iri.toString());
        return result;
    }

    /*
     * Needed for freemarker
     */
    public Session getSession() {
        return session;
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers, GET, POST, PUT, DELETE, OPTIONS);
        return rb.build();
    }

    @OPTIONS
    @Path("/{ontologyId:.+}")
    public Response handleCorsPreflightOntology(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers, GET, DELETE, OPTIONS);
        return rb.build();
    }

    /**
     * Gets the ontology with the given identifier in its version managed by the session.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return the requested managed ontology, or {@link Status#NOT_FOUND} if either the sessionn does not
     *         exist, or the if the ontology either does not exist or is not managed.
     */
    @GET
    @Path(value = "/{ontologyId:.+}")
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response managedOntologyGetGraph(@PathParam("id") String sessionId,
                                            @PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        Graph o = session.getOntology(IRI.create(ontologyId), Graph.class, merge, prefix);
        ResponseBuilder rb = (o != null) ? Response.ok(o) : Response.status(NOT_FOUND);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Gets the ontology with the given identifier in its version managed by the session.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return the requested managed ontology, or {@link Status#NOT_FOUND} if either the sessionn does not
     *         exist, or the if the ontology either does not exist or is not managed.
     */
    @GET
    @Path(value = "/{ontologyId:.+}")
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE})
    public Response managedOntologyGetMixed(@PathParam("id") String sessionId,
                                            @PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (session == null) rb = Response.status(NOT_FOUND);
        else {
            IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
            if (merge) {
                Graph g = session.getOntology(IRI.create(ontologyId), Graph.class, merge, prefix);
                rb = (g != null) ? Response.ok(g) : Response.status(NOT_FOUND);
            } else {
                OWLOntology o = session.getOntology(IRI.create(ontologyId), OWLOntology.class, merge, prefix);
                rb = (o != null) ? Response.ok(o) : Response.status(NOT_FOUND);
            }
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Gets the ontology with the given identifier in its version managed by the session.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return the requested managed ontology, or {@link Status#NOT_FOUND} if either the sessionn does not
     *         exist, or the if the ontology either does not exist or is not managed.
     */
    @GET
    @Path(value = "/{ontologyId:.+}")
    @Produces(value = {MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response managedOntologyGetOWL(@PathParam("id") String sessionId,
                                          @PathParam("ontologyId") String ontologyId,
                                          @DefaultValue("false") @QueryParam("merge") boolean merge,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        OWLOntology o = session.getOntology(IRI.create(ontologyId), OWLOntology.class, merge, prefix);
        ResponseBuilder rb = (o != null) ? Response.ok(o) : Response.status(NOT_FOUND);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(TEXT_HTML)
    public Response managedOntologyShow(@PathParam("ontologyId") String ontologyId,
                                        @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (session == null) rb = Response.status(NOT_FOUND);
        else if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
            OWLOntology o = session.getOntology(IRI.create(ontologyId), OWLOntology.class, false, prefix);
            if (o == null) rb = Response.status(NOT_FOUND);
            else try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                o.getOWLOntologyManager().saveOntology(o, new ManchesterOWLSyntaxOntologyFormat(), out);
                rb = Response.ok(new Viewable("ontology", new OntologyPrettyPrintResource(servletContext,
                        uriInfo, out, session)));
            } catch (OWLOntologyStorageException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        }
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Tells the session to no longer manage the ontology with the supplied <i>logical</i> identifier. The
     * ontology will be lost if not stored or not managed by another collector.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return {@link Status#OK} if the removal was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all, {@link Status#FORBIDDEN} if the session or the ontology is locked or cannot
     *         modified for some other reason, {@link Status#INTERNAL_SERVER_ERROR} if some other error
     *         occurs.
     */
    @DELETE
    @Path(value = "/{ontologyId:.+}")
    public Response managedOntologyUnload(@PathParam("id") String sessionId,
                                          @PathParam("ontologyId") String ontologyId,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (session == null) rb = Response.status(NOT_FOUND);
        else {
            IRI iri = IRI.create(ontologyId);
            OWLOntology o = session.getOntology(iri, OWLOntology.class);
            if (o == null) rb = Response.notModified();
            else try {
                session.removeOntology(iri);
                rb = Response.ok();
            } catch (IrremovableOntologyException e) {
                throw new WebApplicationException(e, FORBIDDEN);
            } catch (UnmodifiableOntologyCollectorException e) {
                throw new WebApplicationException(e, FORBIDDEN);
            } catch (OntologyCollectorModificationException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Tells the session that it should manage the ontology obtained by parsing the supplied content.<br>
     * <br>
     * Note that the PUT method cannot be used, as it is not possible to predict what ID the ontology will
     * have until it is parsed.
     * 
     * @param content
     *            the ontology content
     * @return {@link Status#OK} if the addition was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all, {@link Status#FORBIDDEN} if the session is locked or cannot modified for some
     *         other reason, {@link Status#INTERNAL_SERVER_ERROR} if some other error occurs.
     */
    @POST
    @Consumes(value = {RDF_XML, OWL_XML, N_TRIPLE, N3, TURTLE, X_TURTLE, FUNCTIONAL_OWL, MANCHESTER_OWL,
                       RDF_JSON})
    public Response manageOntology(InputStream content, @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        ResponseBuilder rb;
        String mt = headers.getMediaType().toString();
        if (session == null) rb = Response.status(NOT_FOUND); // Always check session first
        else try {
            log.debug("POST content claimed to be of type {}.", mt);
            OntologyInputSource<?> src;
            if (OWL_XML.equals(mt) || FUNCTIONAL_OWL.equals(mt) || MANCHESTER_OWL.equals(mt)) src = new OntologyContentInputSource(
                    content);
            else // content = new BufferedInputStream(content);
            src = new GraphContentInputSource(content, mt, provider.getStore());
            log.debug("SUCCESS parse with media type {}.", mt);
            String key = session.addOntology(src);
            if (key == null || key.isEmpty()) {
                log.error("FAILED parse with media type {}.", mt);
                throw new WebApplicationException(INTERNAL_SERVER_ERROR);
            }
            // FIXME ugly but will have to do for the time being
            log.debug("SUCCESS add ontology to session {}.", session.getID());
            log.debug("Storage key : {}", key);
            String uri = key.split("::")[1];
            URI created = null;
            if (uri != null && !uri.isEmpty()) {
                created = getCreatedResource(uri);
                rb = Response.created(created);
            } else rb = Response.ok();
            log.info("POST request for ontology addition completed in {} ms.",
                (System.currentTimeMillis() - before));
            log.info("New resource URL is {}", created);
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            log.error("FAILED parse with media type {}.", mt);
            throw new WebApplicationException(e, BAD_REQUEST);
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Tells the session that it should manage the ontology obtained by dereferencing the supplied IRI.<br>
     * <br>
     * Note that the PUT method cannot be used, as it is not possible to predict what ID the ontology will
     * have until it is parsed.
     * 
     * @param content
     *            the ontology physical IRI
     * @return {@link Status#OK} if the addition was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all, {@link Status#FORBIDDEN} if the session is locked or cannot modified for some
     *         other reason, {@link Status#INTERNAL_SERVER_ERROR} if some other error occurs.
     */
    @POST
    @Consumes(value = MediaType.TEXT_PLAIN)
    public Response manageOntology(String iri, @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        try {
            session.addOntology(new RootOntologyIRISource(IRI.create(iri)));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @SuppressWarnings("unused")
    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, TEXT_PLAIN, RDF_XML, TURTLE, X_TURTLE, N3})
    public Response postOntology(FormDataMultiPart data, @Context HttpHeaders headers) {
        log.debug(" post(FormDataMultiPart data)");
        long before = System.currentTimeMillis();
        ResponseBuilder rb;

        IRI location = null;
        File file = null; // If found, it takes precedence over location.
        String format = null;
        OntologyScope scope = null;

        for (BodyPart bpart : data.getBodyParts()) {
            log.debug("Found body part of type {}", bpart.getClass());
            if (bpart instanceof FormDataBodyPart) {
                FormDataBodyPart dbp = (FormDataBodyPart) bpart;
                String name = dbp.getName();
                if (name.equals("file")) {
                    file = bpart.getEntityAs(File.class);
                } else if (name.equals("format") && !dbp.getValue().equals("auto")) format = dbp.getValue();
                else if (name.equals("url")) try {
                    URI.create(dbp.getValue()); // To throw 400 if malformed.
                    location = IRI.create(dbp.getValue());
                } catch (Exception ex) {
                    log.error("Malformed IRI for " + dbp.getValue(), ex);
                    throw new WebApplicationException(ex, BAD_REQUEST);
                }
                if (name.equals("scope")) {
                    scope = onMgr.getScope(dbp.getValue());
                }
            }
        }
        boolean fileOk = file != null && file.canRead() && file.exists();
        if (fileOk || location != null) { // File and location take precedence
            // Then add the file
            OntologyInputSource<?> src = null;
            if (fileOk) { // File first
                try {
                    // Use a buffered stream that can be reset for multiple attempts.
                    long b4buf = System.currentTimeMillis();
                    InputStream content = new BufferedInputStream(new FileInputStream(file));
                    // new FileInputStream(file);
                    log.debug("Streams created in {} ms", System.currentTimeMillis() - b4buf);
                    log.debug("Creating ontology input source...");
                    b4buf = System.currentTimeMillis();
                    src = new GraphContentInputSource(content, format, provider.getStore());
                    log.debug("Done in {} ms", System.currentTimeMillis() - b4buf);
                    log.debug("SUCCESS parse with format {}.", format);
                } catch (OntologyLoadingException e) {
                    log.error("FAILURE parse with format {}.", format);
                    throw new WebApplicationException(e, BAD_REQUEST);
                } catch (IOException e) {
                    log.error("FAILURE parse with format {}.", format);
                    throw new WebApplicationException(e, BAD_REQUEST);
                }
            } else if (location != null) {
                try {
                    src = new RootOntologyIRISource(location);
                } catch (Exception e) {
                    log.error("Failed to load ontology from " + location, e);
                    throw new WebApplicationException(e, BAD_REQUEST);
                }
            } else {
                log.error("Bad request");
                log.error(" file is: {}", file);
                throw new WebApplicationException(BAD_REQUEST);
            }

            if (src != null) {
                log.debug("Adding ontology from input source {}", src);
                long b4add = System.currentTimeMillis();
                String key = session.addOntology(src);
                if (key == null || key.isEmpty()) throw new WebApplicationException(INTERNAL_SERVER_ERROR);
                // FIXME ugly but will have to do for the time being
                log.debug("Addition done in {} ms.", System.currentTimeMillis() - b4add);
                log.debug("Storage key : {}", key);
                String uri = key.split("::")[1];
                if (uri != null && !uri.isEmpty()) rb = Response.created(URI.create("/" + uri));
                else rb = Response.ok();
            } else rb = Response.status(INTERNAL_SERVER_ERROR);
        } else if (scope != null) { // Scope comes next
            log.info("Attaching scope \"{}\" to session \"{}\".", scope.getID(), session.getID());
            session.attachScope(scope.getID());
            rb = Response.seeOther(URI.create("/ontonet/session/" + session.getID()));
        } else {
            log.error("Nothing to add to session {}.", session.getID());
            throw new WebApplicationException(BAD_REQUEST);
        }
        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        log.info("POST ontology completed in {} ms.", System.currentTimeMillis() - before);
        return rb.build();
    }
}
