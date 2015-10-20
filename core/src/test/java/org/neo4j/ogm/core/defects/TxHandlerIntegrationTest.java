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

package org.neo4j.ogm.core.defects;

import org.junit.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.ogm.core.session.Session;
import org.neo4j.ogm.core.session.SessionFactory;
import org.neo4j.ogm.api.service.Components;
import org.neo4j.ogm.core.testutil.IntegrationTestRule;
import org.neo4j.ogm.domain.bike.WheelWithUUID;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Michal Bachman
 */
public class TxHandlerIntegrationTest {

    @Rule
    public IntegrationTestRule testServer = new IntegrationTestRule(Components.driver(), 7577);

    private static SessionFactory sessionFactory;
    private Session session;

    @BeforeClass
    public static void init() throws IOException {
        sessionFactory = new SessionFactory("org.neo4j.ogm.domain.bike");
    }

    @Before
    public void setUp() throws Exception {
        testServer.getGraphDatabaseService().registerTransactionEventHandler(new TransactionEventHandler.Adapter<Object>() {
            @Override
            public Object beforeCommit(TransactionData data) throws Exception {
                for (Node createdNode : data.createdNodes()) {
                    createdNode.setProperty("uuid", UUID.randomUUID().toString());
                }

                return null;
            }
        });
        session = sessionFactory.openSession(testServer.driver());
    }

    @Test
    @Ignore  // FIXME (but how?)
    public void shouldPropagateDatabaseDrivenChangesToObjectGraph() throws InterruptedException {
        WheelWithUUID wheel = new WheelWithUUID();
        wheel.setSpokes(2);

        session.save(wheel);

        long id = wheel.getId();

        String uuid;
        try (Transaction tx = testServer.getGraphDatabaseService().beginTx()) {
            uuid = testServer.getGraphDatabaseService().getNodeById(id).getProperty("uuid", "unknown").toString();
            tx.success();
        }

        assertNotNull(uuid);

        //fails here
        assertEquals(uuid, wheel.getUuid());
    }

}
