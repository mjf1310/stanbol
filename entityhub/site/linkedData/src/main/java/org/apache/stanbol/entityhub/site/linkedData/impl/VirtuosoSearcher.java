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
package org.apache.stanbol.entityhub.site.linkedData.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.site.AbstractEntitySearcher;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.query.clerezza.SparqlFieldQuery;
import org.apache.stanbol.entityhub.query.clerezza.SparqlFieldQueryFactory;
import org.apache.stanbol.entityhub.query.clerezza.SparqlQueryUtils.EndpointTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.slf4j.LoggerFactory;


@Component(
        name="org.apache.stanbol.entityhub.searcher.VirtuosoSearcher",
        factory="org.apache.stanbol.entityhub.searcher.VirtuosoSearcherFactory",
        policy=ConfigurationPolicy.REQUIRE, //the queryUri and the SPARQL Endpoint are required
        specVersion="1.1"
        )
public class VirtuosoSearcher extends AbstractEntitySearcher implements EntitySearcher{
    @Reference
    protected Parser parser;

    public VirtuosoSearcher() {
        super(LoggerFactory.getLogger(VirtuosoSearcher.class));
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery parsedQuery) throws IOException {
        long start = System.currentTimeMillis();
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        query.setEndpointType(EndpointTypeEnum.Virtuoso);
        String sparqlQuery = query.toSparqlConstruct();
        long initEnd = System.currentTimeMillis();
        log.info("  > InitTime: "+(initEnd-start));
        log.info("  > SPARQL query:\n"+sparqlQuery);
        InputStream in = SparqlEndpointUtils.sendSparqlRequest(getQueryUri(), sparqlQuery, SparqlSearcher.DEFAULT_RDF_CONTENT_TYPE);
        long queryEnd = System.currentTimeMillis();
        log.info("  > QueryTime: "+(queryEnd-initEnd));
        if(in != null){
            MGraph graph;
            TripleCollection rdfData = parser.parse(in, SparqlSearcher.DEFAULT_RDF_CONTENT_TYPE);
            if(rdfData instanceof MGraph){
                graph = (MGraph) rdfData;
            } else {
                graph = new SimpleMGraph(rdfData);
            }
            long parseEnd = System.currentTimeMillis();
            log.info("  > ParseTime: "+(parseEnd-queryEnd));
            return new RdfQueryResultList(query, graph);
        } else {
            return null;
        }
    }

    @Override
    public QueryResultList<String> findEntities(FieldQuery parsedQuery) throws IOException {
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        query.setEndpointType(EndpointTypeEnum.Virtuoso);
        String sparqlQuery = query.toSparqlSelect(false);
        InputStream in = SparqlEndpointUtils.sendSparqlRequest(getQueryUri(), sparqlQuery, SparqlSearcher.DEFAULT_SPARQL_RESULT_CONTENT_TYPE);
        //Move to util class!
        final List<String> entities = SparqlSearcher.extractEntitiesFromJsonResult(in,query.getRootVariableName());
        return new QueryResultListImpl<String>(query, entities.iterator(),String.class);
    }

}
