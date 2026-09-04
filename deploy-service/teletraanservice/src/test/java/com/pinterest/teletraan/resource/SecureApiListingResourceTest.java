/**
 * Copyright (c) 2026 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pinterest.teletraan.TeletraanService;
import com.pinterest.teletraan.universal.security.AnonymousAuthFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
public class SecureApiListingResourceTest {

    private static final ResourceExtension resourceExtension;

    static {
        try {
            TeletraanService.buildOpenApiContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        resourceExtension =
                ResourceExtension.builder()
                        .addResource(SecureApiListingResource.class)
                        .addProvider(new AnonymousAuthFilter())
                        .build();
    }

    @Test
    public void swaggerJsonExposesOpenApiV3WithAnnotations() throws Exception {
        Response response = resourceExtension.target("/swagger.json").request().get();
        assertEquals(200, response.getStatus());

        JsonNode spec = new ObjectMapper().readTree(response.readEntity(String.class));
        assertTrue(spec.path("openapi").asText().startsWith("3."));
        assertEquals("Teletraan API Docs", spec.path("info").path("title").asText());

        JsonNode paths = spec.path("paths");
        assertTrue(paths.size() > 50, "expected resource paths to be scanned, got " + paths.size());

        // @Operation on Environs#get
        JsonNode getEnv = paths.path("/v1/envs/{id}").path("get");
        assertEquals("Get environment object", getEnv.path("summary").asText());
        assertEquals(
                "Returns an environment object given an environment id",
                getEnv.path("description").asText());
        assertEquals("Environments", getEnv.path("tags").get(0).asText());

        // @Parameter on Environs#get
        JsonNode envNameParam = getEnv.path("parameters").get(0);
        assertEquals("id", envNameParam.path("name").asText());
        assertEquals("Environment id", envNameParam.path("description").asText());
        assertTrue(envNameParam.path("required").asBoolean());

        // @Tag descriptions collected once per tag name
        List<String> tagNames = new ArrayList<>();
        spec.path("tags").forEach(t -> tagNames.add(t.path("name").asText()));
        assertEquals(tagNames.size(), tagNames.stream().distinct().count());
        assertTrue(tagNames.contains("Environments"));
        spec.path("tags")
                .forEach(
                        t -> {
                            if ("Environments".equals(t.path("name").asText())) {
                                assertEquals(
                                        "Environment info APIs", t.path("description").asText());
                            }
                        });

        // response schema inferred from the method return type
        assertEquals(
                "#/components/schemas/EnvironBean",
                getEnv.path("responses")
                        .path("default")
                        .path("content")
                        .path("application/json")
                        .path("schema")
                        .path("$ref")
                        .asText());
        assertFalse(spec.path("components").path("schemas").path("EnvironBean").isMissingNode());
    }

    @Test
    public void swaggerYamlIsServed() throws Exception {
        Response response = resourceExtension.target("/swagger.yaml").request().get();
        assertEquals(200, response.getStatus());
        JsonNode spec =
                new ObjectMapper(new YAMLFactory()).readTree(response.readEntity(String.class));
        assertTrue(spec.path("openapi").asText().startsWith("3."));
    }
}
