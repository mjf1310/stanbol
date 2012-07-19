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
package org.apache.stanbol.ontologymanager.ontonet.api;

import java.util.Collection;
import java.util.Map;

/**
 * Contains ownership and collector information on all ontology networks currently configured.
 * 
 * @author alexdma
 * 
 */
public class OntologyNetworkConfiguration {

    private Map<String,Collection<String>> coreOntologiesForScopes, customOntologiesForScopes,
            ontologiesForSessions, scopesForSessions;

    public OntologyNetworkConfiguration(Map<String,Collection<String>> coreOntologiesForScopes,
                                        Map<String,Collection<String>> customOntologiesForScopes,
                                        Map<String,Collection<String>> ontologiesForSessions,
                                        Map<String,Collection<String>> scopesForSessions) {
        this.coreOntologiesForScopes = coreOntologiesForScopes;
        this.customOntologiesForScopes = customOntologiesForScopes;
        this.ontologiesForSessions = ontologiesForSessions;
        this.scopesForSessions = scopesForSessions;
    }

    public Collection<String> getAttachedScopes(String sessionId) {
        return scopesForSessions.get(sessionId);
    }

    public Collection<String> getCoreOntologyKeysForScope(String scopeId) {
        return coreOntologiesForScopes.get(scopeId);
    }

    public Collection<String> getCustomOntologyKeysForScope(String scopeId) {
        return customOntologiesForScopes.get(scopeId);
    }

    public Collection<String> getOntologyKeysForSession(String sessionId) {
        return ontologiesForSessions.get(sessionId);
    }

    public Collection<String> getScopeIDs() {
        return coreOntologiesForScopes.keySet();
    }

    public Collection<String> getSessionIDs() {
        return ontologiesForSessions.keySet();
    }

}
