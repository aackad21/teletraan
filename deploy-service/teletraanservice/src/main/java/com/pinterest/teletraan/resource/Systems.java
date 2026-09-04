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

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.ChatMessageBean;
import com.pinterest.deployservice.bean.HostBeanWithStatuses;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/system")
@Tag(name = "Hosts and Systems", description = "Host info APIs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Systems {

    private static final Logger LOG = LoggerFactory.getLogger(Systems.class);
    private SourceControlManagerProxy sourceControlManagerProxy;
    private HostDAO hostDAO;
    private ChatManager chatManager;

    public Systems(@Context TeletraanServiceContext context) {
        sourceControlManagerProxy = context.getSourceControlManagerProxy();
        chatManager = context.getChatManager();
        hostDAO = context.getHostDAO();
    }

    @GET
    @Path("/scm_link_template")
    @Operation(
            summary = "Get SCM commit link template",
            description = "Returns a Source Control Manager specific commit link template.")
    public String getSCMLinkTemplate(@QueryParam("scm") Optional<String> scm) throws Exception {
        return String.format(
                "{\"template\": \"%s\"}",
                sourceControlManagerProxy.getCommitLinkTemplate(scm.or("")));
    }

    @GET
    @Path("/scm_url")
    @Operation(summary = "Get SCM url", description = "Returns a Source Control Manager Url.")
    public String getSCMUrl(@QueryParam("scm") Optional<String> scm) throws Exception {
        return String.format(
                "{\"url\": \"%s\"}", sourceControlManagerProxy.getUrlPrefix(scm.or("")));
    }

    @GET
    @Path("/get_host/{hostName : [a-zA-Z0-9\\-_]+}")
    @Operation(
            summary = "Get all host info",
            description = "Returns a list of host info objects given a host name")
    public List<HostBeanWithStatuses> getHosts(
            @Parameter(description = "Host name", required = true) @PathParam("hostName")
                    String hostName)
            throws Exception {
        return hostDAO.getHosts(hostName);
    }

    @POST
    @Path("/send_chat_message")
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(type = AuthZResource.Type.SYSTEM)
    @Operation(
            summary = "Send chat message",
            description =
                    "Sends a chatroom message given a ChatMessageRequest to configured chat client")
    public void sendChatMessage(
            @Parameter(description = "ChatMessageRequest object", required = true) @Valid
                    ChatMessageBean request)
            throws Exception {
        List<String> chatrooms = Arrays.asList(request.getTo().split(","));
        for (String chatroom : chatrooms) {
            chatManager.send(request.getFrom(), chatroom.trim(), request.getMessage(), "yellow");
        }
        LOG.info("Successfully handled send message requset {}", request);
    }
}
