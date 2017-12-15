/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.apache.skywalking.apm.collector.agent.jetty.handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.skywalking.apm.collector.agent.stream.AgentStreamModule;
import org.apache.skywalking.apm.collector.agent.stream.service.register.IInstanceIDService;
import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.server.jetty.ArgumentsParseException;
import org.apache.skywalking.apm.collector.server.jetty.JettyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class InstanceDiscoveryServletHandler extends JettyHandler {

    private final Logger logger = LoggerFactory.getLogger(InstanceDiscoveryServletHandler.class);

    private final IInstanceIDService instanceIDService;
    private final Gson gson = new Gson();

    private static final String APPLICATION_ID = "ai";
    private static final String AGENT_UUID = "au";
    private static final String REGISTER_TIME = "rt";
    private static final String INSTANCE_ID = "ii";
    private static final String OS_INFO = "oi";

    public InstanceDiscoveryServletHandler(ModuleManager moduleManager) {
        this.instanceIDService = moduleManager.find(AgentStreamModule.NAME).getService(IInstanceIDService.class);
    }

    @Override public String pathSpec() {
        return "/instance/register";
    }

    @Override protected JsonElement doGet(HttpServletRequest req) throws ArgumentsParseException {
        throw new UnsupportedOperationException();
    }

    @Override protected JsonElement doPost(HttpServletRequest req) throws ArgumentsParseException {
        JsonObject responseJson = new JsonObject();
        try {
            JsonObject instance = gson.fromJson(req.getReader(), JsonObject.class);
            int applicationId = instance.get(APPLICATION_ID).getAsInt();
            String agentUUID = instance.get(AGENT_UUID).getAsString();
            long registerTime = instance.get(REGISTER_TIME).getAsLong();
            JsonObject osInfo = instance.get(OS_INFO).getAsJsonObject();

            int instanceId = instanceIDService.getOrCreate(applicationId, agentUUID, registerTime, osInfo.toString());
            responseJson.addProperty(APPLICATION_ID, applicationId);
            responseJson.addProperty(INSTANCE_ID, instanceId);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return responseJson;
    }
}
