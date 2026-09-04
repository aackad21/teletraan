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

import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/system/user_roles")
@Tag(name = "User Roles", description = "User Roles related APIs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SystemUserRoles extends UserRoles {
    private static final AuthZResource.Type RESOURCE_TYPE = AuthZResource.Type.SYSTEM;
    private static final String RESOURCE_ID = AuthZResource.ALL;

    public SystemUserRoles(TeletraanServiceContext context) {
        super(context);
    }

    @GET
    @Operation(
            summary = "Get all system level user role objects",
            description = "Returns a list of all system level UserRoles objects")
    public List<UserRolesBean> getByResource() throws Exception {
        return super.getByResource(RESOURCE_ID, RESOURCE_TYPE);
    }

    @GET
    @Path("/{userName : [a-zA-Z0-9\\-_]+}")
    @Operation(
            summary = "Get system level user role objects by user name",
            description =
                    "Returns a system level UserRoles objects containing info for given user name")
    public UserRolesBean getByNameAndResource(
            @Parameter(description = "Name of user", required = true) @PathParam("userName")
                    String userName)
            throws Exception {
        return super.getByNameAndResource(userName, RESOURCE_ID, RESOURCE_TYPE);
    }

    @PUT
    @Path("/{userName : [a-zA-Z0-9\\-_]+}")
    @Operation(
            summary = "Update a system level user's role",
            description =
                    "Updates a system level user's role given specified user name and replacement UserRoles object")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.SYSTEM)
    public void update(
            @Parameter(description = "Name of user.", required = true) @PathParam("userName")
                    String userName,
            @Parameter(description = "UserRolesBean object", required = true) UserRolesBean bean)
            throws Exception {
        super.update(bean, userName, RESOURCE_ID, RESOURCE_TYPE);
    }

    @POST
    @Operation(
            summary = "Create a new system level user",
            description = "Creates a system level user for given UserRoles object")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.SYSTEM)
    public Response create(
            @Context UriInfo uriInfo,
            @Parameter(description = "UserRolesBean object.", required = true) @Valid
                    UserRolesBean bean)
            throws Exception {
        return super.create(uriInfo, bean, RESOURCE_ID, RESOURCE_TYPE);
    }

    @DELETE
    @Operation(
            summary = "Delete a system level user",
            description = "Deletes a system level user by specified user name")
    @Path("/{userName : [a-zA-Z0-9\\-_]+}")
    @RolesAllowed(TeletraanPrincipalRole.Names.DELETE)
    @ResourceAuthZInfo(type = AuthZResource.Type.SYSTEM)
    public void delete(
            @Parameter(description = "User name", required = true) @PathParam("userName")
                    String userName)
            throws Exception {
        super.delete(userName, RESOURCE_ID, RESOURCE_TYPE);
    }
}
