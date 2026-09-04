/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
package com.pinterest.teletraan;

import com.pinterest.teletraan.health.GenericHealthCheck;
import com.pinterest.teletraan.resource.*;
import com.pinterest.teletraan.universal.security.PrincipalNameInjector;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.Set;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class TeletraanService extends Application<TeletraanServiceConfiguration> {
    @Override
    public String getName() {
        return "teletraan-service";
    }

    @Override
    public void initialize(Bootstrap<TeletraanServiceConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(TeletraanServiceConfiguration configuration, Environment environment)
            throws Exception {
        TeletraanServiceContext context = ConfigHelper.setupContext(configuration, environment);

        environment.jersey().register(context);
        environment
                .jersey()
                .register(
                        new AuthDynamicFeature(
                                configuration.getAuthenticationFactory().create(context)));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(ResourceAuthZInfoFeature.class);

        environment.jersey().register(Builds.class);
        environment.jersey().register(Commits.class);
        environment.jersey().register(Deploys.class);
        environment.jersey().register(Agents.class);
        environment.jersey().register(EnvAgentConfigs.class);
        environment.jersey().register(EnvAgents.class);
        environment.jersey().register(EnvAlarms.class);
        environment.jersey().register(EnvDeploys.class);
        environment.jersey().register(EnvInfras.class);
        environment.jersey().register(EnvInfrasJob.class);
        environment.jersey().register(EnvCapacities.class);
        environment.jersey().register(Environs.class);
        environment.jersey().register(EnvStages.class);
        environment.jersey().register(EnvMetrics.class);
        environment.jersey().register(EnvHistory.class);
        environment.jersey().register(EnvPromotes.class);
        environment.jersey().register(EnvScriptConfigs.class);
        environment.jersey().register(EnvTokenRoles.class);
        environment.jersey().register(EnvUserRoles.class);
        environment.jersey().register(EnvWebHooks.class);
        environment.jersey().register(EnvHosts.class);
        environment.jersey().register(EnvHostTags.class);
        environment.jersey().register(DeployConstraints.class);
        environment.jersey().register(Hotfixs.class);
        environment.jersey().register(Ratings.class);
        environment.jersey().register(SystemGroupRoles.class);
        environment.jersey().register(EnvGroupRoles.class);
        environment.jersey().register(Hosts.class);
        environment.jersey().register(Systems.class);
        environment.jersey().register(Pindeploy.class);

        // Support pings as well
        environment.jersey().register(Pings.class);

        environment.jersey().register(DeployCandidates.class);
        environment.jersey().register(Schedules.class);
        environment.jersey().register(Tags.class);
        environment.jersey().register(Groups.class);
        environment.jersey().register(EnvAlerts.class);

        // Schedule workers if configured
        ConfigHelper.scheduleWorkers(configuration, context);

        environment.healthChecks().register("generic", new GenericHealthCheck(context));

        environment.jersey().register(PrincipalNameInjector.class);

        // Swagger API docs generation related
        OpenAPI openApi =
                new OpenAPI().info(new Info().title("Teletraan API Docs").version("1.0.0"));
        SwaggerConfiguration openApiConfig =
                new SwaggerConfiguration()
                        .openAPI(openApi)
                        .prettyPrint(true)
                        .resourcePackages(Set.of("com.pinterest.teletraan.resource"));
        new JaxrsOpenApiContextBuilder<>().openApiConfiguration(openApiConfig).buildContext(true);
        environment.jersey().register(SecureApiListingResource.class);

        // Enable CORS headers
        FilterRegistration.Dynamic filter =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        filter.setInitParameter(
                CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");
        filter.setInitParameter(
                "allowedHeaders",
                "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
        filter.setInitParameter("allowCredentials", "true");
    }

    public static void main(String[] args) throws Exception {
        new TeletraanService().run(args);
    }
}
