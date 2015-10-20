package org.neo4j.ogm.core.testutil;

import org.apache.commons.io.IOUtils;
import org.neo4j.ogm.api.driver.Driver;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author vince
 */
public class AuthenticatingTestServer extends TestServer {

    public AuthenticatingTestServer(Driver driver) {
        super(driver);
    }

    public AuthenticatingTestServer(Driver driver, int portNumber) {
        super(driver, portNumber);
    }

    public boolean enableAuthentication() {
        return true;
    }

    public String authStoreLocation() {
        // creates a temp auth store, with encrypted credentials "neo4j:password"
        try {
            Path authStore = Files.createTempFile("neo4j", "credentials");
            authStore.toFile().deleteOnExit();
            try (Writer authStoreWriter = new FileWriter( authStore.toFile() )) {
                IOUtils.write("neo4j:SHA-256,03C9C54BF6EEF1FF3DFEB75403401AA0EBA97860CAC187D6452A1FCF4C63353A,819BDB957119F8DFFF65604C92980A91:", authStoreWriter);
            }
            return authStore.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
