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
package org.apache.stanbol.ontologymanager.registry;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.rdfjson.parser.RdfJsonParsingProvider;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;

public class MockOsgiContext {

    public static Parser parser;

    public static TcManager tcManager;

    static {
        reset();
    }

    public static void reset() {
        tcManager = new TcManager();
        tcManager.addWeightedTcProvider(new SimpleTcProvider());
        
        parser = new Parser();
        parser.bindParsingProvider(new JenaParserProvider());
        parser.bindParsingProvider(new RdfJsonParsingProvider());
    }

}
