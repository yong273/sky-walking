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


package org.apache.skywalking.apm.agent;

import java.lang.instrument.Instrumentation;
import java.util.List;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.EnhanceContext;
import org.apache.skywalking.apm.agent.core.plugin.PluginBootstrap;
import org.apache.skywalking.apm.agent.core.plugin.PluginException;
import org.apache.skywalking.apm.agent.core.plugin.PluginFinder;
import org.apache.skywalking.apm.agent.core.conf.SnifferConfigInitializer;

/**
 * The main entrance of sky-waking agent,
 * based on javaagent mechanism.
 *
 * @author wusheng
 */
public class SkyWalkingAgent {
    private static final ILog logger = LogManager.getLogger(SkyWalkingAgent.class);

    /**
     * Main entrance.
     * Use byte-buddy transform to enhance all classes, which define in plugins.
     *
     * @param agentArgs
     * @param instrumentation
     * @throws PluginException
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws PluginException {
        final PluginFinder pluginFinder;
        try {
            SnifferConfigInitializer.initialize();

            pluginFinder = new PluginFinder(new PluginBootstrap().loadPlugins());

            ServiceManager.INSTANCE.boot();
        } catch (Exception e) {
            logger.error(e, "Skywalking agent initialized failure. Shutting down.");
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                ServiceManager.INSTANCE.shutdown();
            }
        }, "skywalking service shutdown thread"));

        new AgentBuilder.Default().type(pluginFinder.buildMatch()).transform(new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                ClassLoader classLoader, JavaModule module) {
                List<AbstractClassEnhancePluginDefine> pluginDefines = pluginFinder.find(typeDescription, classLoader);
                if (pluginDefines.size() > 0) {
                    DynamicType.Builder<?> newBuilder = builder;
                    EnhanceContext context = new EnhanceContext();
                    for (AbstractClassEnhancePluginDefine define : pluginDefines) {
                        DynamicType.Builder<?> possibleNewBuilder = define.define(typeDescription.getTypeName(), newBuilder, classLoader, context);
                        if (possibleNewBuilder != null) {
                            newBuilder = possibleNewBuilder;
                        }
                    }
                    if (context.isEnhanced()) {
                        logger.debug("Finish the prepare stage for {}.", typeDescription.getName());
                    }

                    return newBuilder;
                }

                logger.debug("Matched class {}, but ignore by finding mechanism.", typeDescription.getTypeName());
                return builder;
            }
        }).with(new AgentBuilder.Listener() {
            @Override
            public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {

            }

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                boolean loaded, DynamicType dynamicType) {
                if (logger.isDebugEnable()) {
                    logger.debug("On Transformation class {}.", typeDescription.getName());
                }

                InstrumentDebuggingClass.INSTANCE.log(typeDescription, dynamicType);
            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                boolean loaded) {

            }

            @Override public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                Throwable throwable) {
                logger.error("Enhance class " + typeName + " error.", throwable);
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
            }
        }).installOn(instrumentation);
    }
}
