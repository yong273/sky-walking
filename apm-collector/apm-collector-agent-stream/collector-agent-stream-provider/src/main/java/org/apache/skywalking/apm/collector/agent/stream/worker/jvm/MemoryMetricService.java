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


package org.apache.skywalking.apm.collector.agent.stream.worker.jvm;

import org.apache.skywalking.apm.collector.core.graph.GraphManager;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.core.util.ObjectUtils;
import org.apache.skywalking.apm.collector.agent.stream.service.graph.JvmMetricStreamGraphDefine;
import org.apache.skywalking.apm.collector.agent.stream.service.jvm.IMemoryMetricService;
import org.apache.skywalking.apm.collector.core.graph.Graph;
import org.apache.skywalking.apm.collector.storage.table.jvm.MemoryMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class MemoryMetricService implements IMemoryMetricService {

    private final Logger logger = LoggerFactory.getLogger(MemoryMetricService.class);

    private Graph<MemoryMetric> memoryMetricGraph;

    private Graph<MemoryMetric> getMemoryMetricGraph() {
        if (ObjectUtils.isEmpty(memoryMetricGraph)) {
            this.memoryMetricGraph = GraphManager.INSTANCE.createIfAbsent(JvmMetricStreamGraphDefine.MEMORY_METRIC_GRAPH_ID, MemoryMetric.class);
        }
        return memoryMetricGraph;
    }

    @Override
    public void send(int instanceId, long timeBucket, boolean isHeap, long init, long max, long used, long commited) {
        MemoryMetric memoryMetric = new MemoryMetric(timeBucket + Const.ID_SPLIT + instanceId + Const.ID_SPLIT + String.valueOf(isHeap));
        memoryMetric.setInstanceId(instanceId);
        memoryMetric.setIsHeap(isHeap);
        memoryMetric.setInit(init);
        memoryMetric.setMax(max);
        memoryMetric.setUsed(used);
        memoryMetric.setCommitted(commited);
        memoryMetric.setTimeBucket(timeBucket);

        logger.debug("push to memory metric graph, id: {}", memoryMetric.getId());
        getMemoryMetricGraph().start(memoryMetric);
    }
}
