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
package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;

import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * An ontology registry can reference zero or more ontology libraries.
 */
public interface Registry extends RegistryItem {

    final Type type = Type.REGISTRY;

    /**
     * 
     * @return
     */
    OWLOntologyManager getCache();

    /**
     * 
     * @param cache
     */
    void setCache(OWLOntologyManager cache);
}
