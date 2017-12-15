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


package org.apache.skywalking.apm.collector.agent.stream.buffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.core.util.StringUtils;
import org.apache.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.apache.skywalking.apm.network.proto.UpstreamSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public enum SegmentBufferManager {
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(SegmentBufferManager.class);

    public static final String DATA_FILE_PREFIX = "data";
    private FileOutputStream outputStream;

    public synchronized void initialize(ModuleManager moduleManager) {
        logger.info("segment buffer initialize");
        try {
            OffsetManager.INSTANCE.initialize();
            if (new File(BufferFileConfig.BUFFER_PATH).mkdirs()) {
                newDataFile();
            } else {
                String writeFileName = OffsetManager.INSTANCE.getWriteFileName();
                if (StringUtils.isNotEmpty(writeFileName)) {
                    File dataFile = new File(BufferFileConfig.BUFFER_PATH + writeFileName);
                    if (dataFile.exists()) {
                        outputStream = new FileOutputStream(new File(BufferFileConfig.BUFFER_PATH + writeFileName), true);
                    } else {
                        newDataFile();
                    }
                } else {
                    newDataFile();
                }
            }
            SegmentBufferReader.INSTANCE.initialize(moduleManager);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public synchronized void writeBuffer(UpstreamSegment segment) {
        try {
            segment.writeDelimitedTo(outputStream);
            long position = outputStream.getChannel().position();
            if (position > BufferFileConfig.BUFFER_SEGMENT_MAX_FILE_SIZE) {
                newDataFile();
            } else {
                OffsetManager.INSTANCE.setWriteOffset(position);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void newDataFile() throws IOException {
        logger.debug("create new segment buffer file");
        String timeBucket = String.valueOf(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
        String writeFileName = DATA_FILE_PREFIX + "_" + timeBucket + "." + Const.FILE_SUFFIX;
        File dataFile = new File(BufferFileConfig.BUFFER_PATH + writeFileName);
        dataFile.createNewFile();
        OffsetManager.INSTANCE.setWriteOffset(writeFileName, 0);
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            outputStream = new FileOutputStream(dataFile);
            outputStream.getChannel().position(0);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public synchronized void flush() {
    }
}
