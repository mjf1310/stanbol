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
package org.apache.stanbol.rules.adapters.jena.atoms;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.jena.NodeClauseEntry;
import org.apache.stanbol.rules.adapters.jena.NodeFactory;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;

/**
 * 
 * It adapts a NumberAtom to a literal node in Jena.
 * 
 * @author anuzzolese
 * 
 */
public class NumberAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption {
        org.apache.stanbol.rules.manager.atoms.NumberAtom tmp = (org.apache.stanbol.rules.manager.atoms.NumberAtom) ruleAtom;

        String number = tmp.getNumber();

        Node node = null;
        
        System.out.println("Number atom " + number); 

        if (number.startsWith("?")) {

            number = number.substring(1);

            node = Node_RuleVariable.createVariable(number);
        } else {

            Number n = null;
            if (number.contains("\\.")) {
                int indexOfPoint = number.indexOf('.');

                if (indexOfPoint == number.length() - 2) {
                    n = Float.parseFloat(number);
                } else {
                    n = Double.parseDouble(number);
                }
            } else {
                n = Integer.parseInt(number);
            }

            node = NodeFactory.getTypedLiteral(n);
        }

        return (T) new NodeClauseEntry(node);

    }

}
