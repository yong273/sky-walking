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


package org.apache.skywalking.apm.collector.storage.h2.define;

import org.apache.skywalking.apm.collector.storage.h2.base.define.H2ColumnDefine;
import org.apache.skywalking.apm.collector.storage.h2.base.define.H2TableDefine;
import org.apache.skywalking.apm.collector.storage.table.jvm.MemoryMetricTable;

/**
 * @author peng-yongsheng
 */
public class MemoryMetricH2TableDefine extends H2TableDefine {

    public MemoryMetricH2TableDefine() {
        super(MemoryMetricTable.TABLE);
    }

    @Override public void initialize() {
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_ID, H2ColumnDefine.Type.Varchar.name()));
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_INSTANCE_ID, H2ColumnDefine.Type.Int.name()));
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_IS_HEAP, H2ColumnDefine.Type.Boolean.name()));
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_INIT, H2ColumnDefine.Type.Bigint.name()));
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_MAX, H2ColumnDefine.Type.Bigint.name()));
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_USED, H2ColumnDefine.Type.Bigint.name()));
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_COMMITTED, H2ColumnDefine.Type.Bigint.name()));
        addColumn(new H2ColumnDefine(MemoryMetricTable.COLUMN_TIME_BUCKET, H2ColumnDefine.Type.Bigint.name()));
    }
}
