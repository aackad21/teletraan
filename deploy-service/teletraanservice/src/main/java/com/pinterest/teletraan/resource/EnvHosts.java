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
package com.pinterest.teletraan.resource;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostBeanWithStatuses;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/hosts")
@Tag(name = "Hosts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvHosts {
    private static final Logger LOG = LoggerFactory.getLogger(EnvHosts.class);
    private final EnvironDAO environDAO;
    private final HostDAO hostDAO;
    private final EnvironHandler environHandler;
    private final ConfigHistoryHandler configHistoryHandler;

    public EnvHosts(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        hostDAO = context.getHostDAO();
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
    }

    @GET
    @Operation(
            summary = "Get hosts for env stage",
            description = "Returns a Collections of hosts given an environment and stage")
    public Collection<HostBean> get(
            @Parameter(description = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @Parameter(description = "Stage name", required = true) @PathParam("stageName")
                    String stageName)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return hostDAO.getHostsByEnvId(envBean.getEnv_id());
    }

    @GET
    @Path("/{hostName : [a-zA-Z0-9\\-_]+}")
    @Operation(
            summary = "Get host details for stage and host name",
            description = "Returns a host given an environment, stage and host name")
    public Collection<HostBeanWithStatuses> getHostByHostName(
            @Parameter(description = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @Parameter(description = "Stage name", required = true) @PathParam("stageName")
                    String stageName,
            @Parameter(description = "Host name", required = true) @PathParam("hostName")
                    String hostName)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return hostDAO.getByEnvIdAndHostName(envBean.getEnv_id(), hostName);
    }

    @DELETE
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.ENV_STAGE,
            idLocation = ResourceAuthZInfo.Location.PATH)
    public void stopServiceOnHost(
            @Context SecurityContext sc,
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @Valid Collection<String> hostIds,
            @Parameter(description = "Replace the host or not") @QueryParam("replaceHost")
                    Optional<Boolean> replaceHost)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        environHandler.ensureHostsOwnedByEnv(envBean, hostIds);
        environHandler.stopServiceOnHosts(hostIds, replaceHost.orElse(true));
        configHistoryHandler.updateConfigHistory(
                envBean.getEnv_id(),
                Constants.TYPE_HOST_ACTION,
                String.format("STOP %s", hostIds.toString()),
                operator);
        LOG.info(
                "Successfully stopped {}/{} service on hosts {} by {}",
                envName,
                stageName,
                hostIds,
                operator);
    }
}
