/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 *  Charles Hayden's implementation of ELIZA (by Joseph Weizenbaum): http://www.chayden.net/eliza/Eliza.html.
 */

package examples.eliza.util;

import java.util.ArrayList;

/**
 *  Eliza decomposition list.
 */
public class DecompositionList extends ArrayList<Decomposition> {
    public boolean add(String word, boolean memory, ReassemblyList reassemblyList) {
        return super.add(new Decomposition(word, memory, reassemblyList));
    }

    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            Decomposition d = get(i);
            d.print(indent);
        }
    }
}
