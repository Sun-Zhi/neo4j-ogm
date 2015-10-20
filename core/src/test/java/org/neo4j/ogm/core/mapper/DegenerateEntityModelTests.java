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

package org.neo4j.ogm.core.mapper;

import org.junit.*;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.api.mapper.EntityToGraphMapper;
import org.neo4j.ogm.api.request.Statement;
import org.neo4j.ogm.api.request.Statements;
import org.neo4j.ogm.core.MetaData;
import org.neo4j.ogm.core.testutil.GraphTestUtils;
import org.neo4j.ogm.domain.filesystem.Document;
import org.neo4j.ogm.domain.filesystem.Folder;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * These tests are to establish the behaviour of degenerate entity models
 *
 * An entity model is considered degenerate if a relationship that should
 * exist between two entities is only established on one of them.
 *
 * For example if a parent object maintains a list of child objects
 * but a child object maintains a null (or incorrect) reference to its parent
 * the entity model is degenerate.
 *
 * The OGM is designed to cope with such models.
 *
 * @author Vince Bickers
 *
 */
public class DegenerateEntityModelTests {

    private EntityToGraphMapper mapper;

    private static GraphDatabaseService graphDatabase;
    private static ExecutionEngine executionEngine;
    private static MetaData mappingMetadata;
    private static MappingContext mappingContext;

    private Folder f;
    private Document a;
    private Document b;

    @BeforeClass
    public static void setUpTestDatabase() {
        graphDatabase = new TestGraphDatabaseFactory().newImpermanentDatabase();
        executionEngine = new ExecutionEngine(graphDatabase);
        mappingMetadata = new MetaData(
                "org.neo4j.ogm.domain.filesystem");
        mappingContext = new MappingContext(mappingMetadata);

    }

    @AfterClass
    public static void shutDownDatabase() {
        graphDatabase.shutdown();
    }

    @After
    public void cleanGraph() {
        executionEngine.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
        mappingContext.clear();
    }
    @Before
    public void setUp() {
        this.mapper = new EntityGraphMapper(mappingMetadata, mappingContext);

        ExecutionResult executionResult = executionEngine.execute(
                        "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) " +
                        "CREATE (f)-[:CONTAINS]->(a) " +
                        "CREATE (f)-[:CONTAINS]->(b) " +
                        "RETURN id(f) AS fid, id(a) AS aid, id(b) AS bid");


        Map<String, Object> resultSet = executionResult.iterator().next();

        a = new Document();
        a.setId((Long) resultSet.get("aid"));
        a.setName("a");

        b = new Document();
        b.setId((Long) resultSet.get("bid"));
        b.setName("b");

        f = new Folder();
        f.setId((Long) resultSet.get("fid"));
        f.setName("f");

        f.getDocuments().add(a);
        f.getDocuments().add(b);

        a.setFolder(f);
        b.setFolder(f);

        mappingContext.clear();
        mappingContext.registerNodeEntity(f, f.getId());
        mappingContext.registerNodeEntity(a, a.getId());
        mappingContext.registerNodeEntity(b, b.getId());
        mappingContext.registerRelationship(new MappedRelationship(f.getId(), "CONTAINS", a.getId(), Folder.class, Document.class));
        mappingContext.registerRelationship(new MappedRelationship(f.getId(), "CONTAINS", b.getId(), Folder.class, Document.class));
       // mappingContext.registerRelationship(new MappedRelationship(a.getId(), "CONTAINS", b.getId(), Document.class, Folder.class));
      //  mappingContext.registerRelationship(new MappedRelationship(b.getId(), "CONTAINS", b.getId(), Document.class, Folder.class));

    }

    @Test
    public void testSaveDegenerateDocument() {

        // set a's f to a new f, but don't remove from the current f's list of documents
        a.setFolder(null);

        Statements cypher = new Statements(mapper.map(a).getStatements());

        executeStatementsAndAssertSameGraph(cypher,
                "CREATE (f:Folder {name : 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) " +
                        "CREATE (f)-[:CONTAINS]->(b)");
    }

    @Test
    public void testSaveDegenerateFolder() {

        // remove f's documents, but don't clear the documents' f reference
        f.setDocuments(new HashSet<Document>());

        Statements cypher = new Statements(mapper.map(f).getStatements());

        executeStatementsAndAssertSameGraph(cypher,
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) ");
    }


    @Test
    public void testSaveDegenerateDocumentClone() {

        Document clone = new Document();
        clone.setId(a.getId());
        clone.setName(a.getName());
        clone.setFolder(null);

        Statements cypher = new Statements(mapper.map(clone).getStatements());

        executeStatementsAndAssertSameGraph(cypher,
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a'} ) " +
                        "CREATE (b:Document { name: 'b'} ) " +
                        "CREATE (f)-[:CONTAINS]->(b)");
    }

    @Test
    public void testSaveDegenerateFolderClone() {

        Folder clone = new Folder();
        clone.setId(f.getId());
        clone.setName(f.getName());
        clone.setDocuments(new HashSet<Document>());

        Statements cypher = new Statements(mapper.map(clone).getStatements());

        executeStatementsAndAssertSameGraph(cypher,
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) ");
    }

    @Test
    public void testSaveChangedDocument() {

        // set a's f to a new f, but don't remove from the current f's list of documents
        a.setFolder(new Folder());
        a.getFolder().setName("g");

        Statements cypher = new Statements(mapper.map(a).getStatements());

        executeStatementsAndAssertSameGraph(cypher,
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (g:Folder { name: 'g' } ) " +
                        "CREATE (a:Document { name: 'a' }) " +
                        "CREATE (b:Document { name: 'b' }) " +
                        "CREATE (f)-[:CONTAINS]->(b) " +
                        "CREATE (g)-[:CONTAINS]->(a) ");

    }

    @Test
    public void testSaveChangedFolder() {

        Document c = new Document();
        c.setName("c");

        f.getDocuments().add(c);
        f.getDocuments().remove(a);

        Statements cypher = new Statements(mapper.map(f).getStatements());

        executeStatementsAndAssertSameGraph(cypher,
                "CREATE (f:Folder { name: 'f' })" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) " +
                        "CREATE (c:Document { name: 'c' } ) " +
                        "CREATE (f)-[:CONTAINS]->(b) " +
                        "CREATE (f)-[:CONTAINS]->(c) ");

    }

    private void executeStatementsAndAssertSameGraph(Statements cypher, String sameGraphCypher) {

        assertNotNull("The resultant cypher statements shouldn't be null", cypher.getStatements());
        assertFalse("The resultant cypher statements shouldn't be empty", cypher.getStatements().isEmpty());

        for (Statement query : cypher.getStatements()) {
            executionEngine.execute(query.getStatement(), query.getParameters());
        }
        GraphTestUtils.assertSameGraph(graphDatabase, sameGraphCypher);
    }

}
