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


package org.apache.skywalking.apm.collector.core.module;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.skywalking.apm.collector.core.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wu-sheng
 */
public class BootstrapFlow {
    private final Logger logger = LoggerFactory.getLogger(BootstrapFlow.class);

    private Map<String, Module> loadedModules;
    private ApplicationConfiguration applicationConfiguration;
    private List<ModuleProvider> startupSequence;

    public BootstrapFlow(Map<String, Module> loadedModules,
        ApplicationConfiguration applicationConfiguration) throws CycleDependencyException {
        this.loadedModules = loadedModules;
        this.applicationConfiguration = applicationConfiguration;
        startupSequence = new LinkedList<>();

        makeSequence();
    }

    void start(ModuleManager moduleManager,
        ApplicationConfiguration configuration) throws ProviderNotFoundException, ModuleNotFoundException, ServiceNotProvidedException {
        for (ModuleProvider provider : startupSequence) {
            String[] requiredModules = provider.requiredModules();
            if (requiredModules != null) {
                for (String module : requiredModules) {
                    if (!moduleManager.has(module)) {
                        throw new ModuleNotFoundException(module + " is required by " + provider.getModuleName()
                            + "." + provider.name() + ", but not found.");
                    }
                }
            }
            logger.info("start the provider {} in {} module.", provider.name(), provider.getModuleName());
            provider.requiredCheck(provider.getModule().services());

            provider.start(configuration.getModuleConfiguration(provider.getModuleName()).getProviderConfiguration(provider.name()));
        }
    }

    void notifyAfterCompleted() throws ProviderNotFoundException, ModuleNotFoundException, ServiceNotProvidedException {
        for (ModuleProvider provider : startupSequence) {
            provider.notifyAfterCompleted();
        }
    }

    private void makeSequence() throws CycleDependencyException {
        List<ModuleProvider> allProviders = new ArrayList<>();
        loadedModules.forEach((moduleName, module) -> {
            module.providers().forEach(provider -> {
                allProviders.add(provider);
            });
        });

        while (true) {
            int numOfToBeSequenced = allProviders.size();
            for (int i = 0; i < allProviders.size(); i++) {
                ModuleProvider provider = allProviders.get(i);
                String[] requiredModules = provider.requiredModules();
                if (CollectionUtils.isNotEmpty(requiredModules)) {
                    boolean isAllRequiredModuleStarted = true;
                    for (String module : requiredModules) {
                        // find module in all ready existed startupSequence
                        boolean exist = false;
                        for (ModuleProvider moduleProvider : startupSequence) {
                            if (moduleProvider.getModuleName().equals(module)) {
                                exist = true;
                                break;
                            }
                        }
                        if (!exist) {
                            isAllRequiredModuleStarted = false;
                            break;
                        }
                    }

                    if (isAllRequiredModuleStarted) {
                        startupSequence.add(provider);
                        allProviders.remove(i);
                        i--;
                    }
                } else {
                    startupSequence.add(provider);
                    allProviders.remove(i);
                    i--;
                }
            }

            if (numOfToBeSequenced == allProviders.size()) {
                StringBuilder unsequencedProviders = new StringBuilder();
                allProviders.forEach(provider -> {
                    unsequencedProviders.append(provider.getModuleName()).append("[provider=").append(provider.getClass().getName()).append("]\n");
                });
                throw new CycleDependencyException("Exist cycle module dependencies in \n" + unsequencedProviders.substring(0, unsequencedProviders.length() - 1));
            }
            if (allProviders.size() == 0) {
                break;
            }
        }
    }
}
