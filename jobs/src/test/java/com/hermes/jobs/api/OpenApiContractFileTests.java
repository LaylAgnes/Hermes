package com.hermes.jobs.api;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiContractFileTests {

    @Test
    @SuppressWarnings("unchecked")
    void shouldContainV1ContractWithMainPathsAndSchemas() throws Exception {
        Path specPath = Path.of("docs/api/openapi-v1.yaml");
        assertTrue(Files.exists(specPath), "openapi-v1.yaml deve existir em docs/api");

        try (InputStream in = Files.newInputStream(specPath)) {
            Map<String, Object> root = new Yaml().load(in);

            assertEquals("3.0.3", root.get("openapi"));

            Map<String, Object> info = (Map<String, Object>) root.get("info");
            assertEquals("v1", info.get("version"));

            List<Map<String, Object>> servers = (List<Map<String, Object>>) root.get("servers");
            assertEquals("/api/v1", servers.get(0).get("url"));

            Map<String, Object> paths = (Map<String, Object>) root.get("paths");
            assertTrue(paths.containsKey("/search/options"));
            assertTrue(paths.containsKey("/search"));
            assertTrue(paths.containsKey("/search/filters"));
            assertTrue(paths.containsKey("/jobs/import"));
            assertTrue(paths.containsKey("/jobs/import-urls"));

            Map<String, Object> components = (Map<String, Object>) root.get("components");
            Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

            assertTrue(schemas.containsKey("SearchRequest"));
            assertTrue(schemas.containsKey("StructuredSearchRequest"));
            assertTrue(schemas.containsKey("SearchOptionsResponse"));
            assertTrue(schemas.containsKey("JobImportRequest"));
            assertTrue(schemas.containsKey("JobDocument"));
            assertTrue(schemas.containsKey("JobResponse"));
            assertTrue(schemas.containsKey("JobPage"));
        }
    }
}
