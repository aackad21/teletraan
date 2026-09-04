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
import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo.Location;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/promotes")
@Tag(name = "Environments", description = "Environment info APIs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvPromotes {
    private static final Logger LOG = LoggerFactory.getLogger(EnvPromotes.class);
    private EnvironHandler environHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private EnvironDAO environDAO;

    public EnvPromotes(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
    }

    @GET
    @Operation(
            summary = "Get promote info",
            description = "Returns a promote info object given environment and stage names")
    public PromoteBean get(
            @Parameter(description = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @Parameter(description = "Stage name", required = true) @PathParam("stageName")
                    String stageName)
            throws Exception {
        return environHandler.getEnvPromote(envName, stageName);
    }

    @PUT
    @Operation(
            summary = "Update promote info",
            description =
                    "Updates promote info given environment and stage names by given promote info object")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = Location.PATH)
    public void update(
            @Context SecurityContext sc,
            @Parameter(description = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @Parameter(description = "Stage name", required = true) @PathParam("stageName")
                    String stageName,
            @Parameter(description = "Promote object to update with", required = true) @Valid
                    PromoteBean promoteBean)
            throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        String operator = sc.getUserPrincipal().getName();
        environHandler.updateEnvPromote(environBean, promoteBean, operator);
        configHistoryHandler.updateConfigHistory(
                environBean.getEnv_id(), Constants.TYPE_ENV_PROMOTE, promoteBean, operator);
        configHistoryHandler.updateChangeFeed(
                Constants.CONFIG_TYPE_ENV,
                environBean.getEnv_id(),
                Constants.TYPE_ENV_PROMOTE,
                operator,
                environBean.getExternal_id());
        LOG.info(
                "Successfully updated promote with {} to env {}/{} by {}.",
                promoteBean,
                envName,
                stageName,
                operator);
    }
}
