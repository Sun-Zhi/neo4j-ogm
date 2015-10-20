/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.neo4j.ogm.core.mapper.cypher;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.core.cypher.Filters;
import org.neo4j.ogm.core.cypher.query.SortOrder;
import org.neo4j.ogm.core.session.request.strategy.QueryStatements;
import org.neo4j.ogm.core.session.request.strategy.VariableDepthRelationshipQuery;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 */
public class RelationshipEntityQuerySortingTest {
    
    private final QueryStatements query = new VariableDepthRelationshipQuery();
    private SortOrder sortOrder;
    private Filters filters;

    @Before
    public void setUp() {
        sortOrder = new SortOrder();
        filters = new Filters();
    }

    @Test
    public void testFindAllCollection() throws Exception {
        sortOrder.add("distance");
        assertEquals("MATCH (n)-[r]->() WHERE ID(r) IN { ids } WITH n,r ORDER BY r.distance MATCH p=(n)-[*0..1]-(m) RETURN p", query.findAll(Arrays.asList(1L, 2L, 3L), 1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        sortOrder.add("distance");
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r ORDER BY r.distance RETURN p", query.findByType("ORBITS", 3).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        filters.add("distance", 60.2);
        sortOrder.add("aphelion");
        assertEquals("MATCH (n)-[r:`ORBITS`]->(m) WHERE r.`distance` = { `distance` } WITH n,r ORDER BY r.aphelion MATCH p=(n)-[*0..1]-() RETURN p, ID(r)", query.findByProperties("ORBITS", filters, 1).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testMultipleSortOrders() {
        sortOrder.add(SortOrder.Direction.DESC, "distance", "aphelion");
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r ORDER BY r.distance DESC,r.aphelion DESC RETURN p", query.findByType("ORBITS", 3).setSortOrder(sortOrder).getStatement());
    }

    @Test
    public void testDifferentSortDirections() {
        sortOrder.add(SortOrder.Direction.DESC, "type").add("name");
        assertEquals("MATCH p=()-[r:`ORBITS`*..3]-() WITH p,r ORDER BY r.type DESC,r.name RETURN p", query.findByType("ORBITS", 3).setSortOrder(sortOrder).getStatement());
    }

}
