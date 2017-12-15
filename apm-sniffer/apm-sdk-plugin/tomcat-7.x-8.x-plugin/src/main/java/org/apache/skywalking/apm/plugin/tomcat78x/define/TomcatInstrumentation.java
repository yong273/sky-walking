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


package org.apache.skywalking.apm.plugin.tomcat78x.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * {@link TomcatInstrumentation} presents that skywalking using class {@link org.apache.skywalking.apm.plugin.tomcat78x.TomcatInvokeInterceptor} to intercept
 * {@link org.apache.catalina.core.StandardWrapperValve#invoke} and using class {@link
 * org.apache.skywalking.apm.plugin.tomcat78x.TomcatExceptionInterceptor} to intercept {@link
 * org.apache.catalina.core.StandardWrapperValve#exception}.
 *
 * @author zhangxin
 */
public class TomcatInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    /**
     * Enhance class.
     */
    private static final String ENHANCE_CLASS = "org.apache.catalina.core.StandardWrapperValve";

    /**
     * The intercept class for "invoke" method in the class "org.apache.catalina.core.StandardWrapperValve"
     */
    private static final String INVOKE_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.tomcat78x.TomcatInvokeInterceptor";

    /**
     * The intercept class for "exception" method in the class "org.apache.catalina.core.StandardWrapperValve"
     */
    private static final String EXCEPTION_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.tomcat78x.TomcatExceptionInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;
    }

    @Override
    protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("invoke");
                }

                @Override
                public String getMethodsInterceptor() {
                    return INVOKE_INTERCEPT_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("exception");
                }

                @Override public String getMethodsInterceptor() {
                    return EXCEPTION_INTERCEPT_CLASS;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
