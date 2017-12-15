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


package org.apache.skywalking.apm.collector.storage.h2.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.apache.skywalking.apm.collector.storage.base.sql.SqlBuilder;
import org.apache.skywalking.apm.collector.storage.table.register.Instance;
import org.apache.skywalking.apm.collector.storage.table.register.InstanceTable;
import org.apache.skywalking.apm.collector.client.h2.H2Client;
import org.apache.skywalking.apm.collector.client.h2.H2ClientException;
import org.apache.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.apache.skywalking.apm.collector.storage.dao.IInstanceUIDAO;
import org.apache.skywalking.apm.collector.storage.h2.base.dao.H2DAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng, clevertension
 */
public class InstanceH2UIDAO extends H2DAO implements IInstanceUIDAO {

    private final Logger logger = LoggerFactory.getLogger(InstanceH2UIDAO.class);

    public InstanceH2UIDAO(H2Client client) {
        super(client);
    }

    private static final String GET_LAST_HEARTBEAT_TIME_SQL = "select {0} from {1} where {2} > ? limit 1";
    private static final String GET_INST_LAST_HEARTBEAT_TIME_SQL = "select {0} from {1} where {2} > ? and {3} = ? limit 1";
    private static final String GET_INSTANCE_SQL = "select * from {0} where {1} = ?";
    private static final String GET_INSTANCES_SQL = "select * from {0} where {1} = ? and {2} >= ?";
    private static final String GET_APPLICATIONS_SQL = "select {3}, count({0}) as cnt from {1} where {2} >= ? group by {3} limit 100";

    @Override
    public Long lastHeartBeatTime() {
        H2Client client = getClient();
        long fiveMinuteBefore = System.currentTimeMillis() - 5 * 60 * 1000;
        fiveMinuteBefore = TimeBucketUtils.INSTANCE.getSecondTimeBucket(fiveMinuteBefore);
        String sql = SqlBuilder.buildSql(GET_LAST_HEARTBEAT_TIME_SQL, InstanceTable.COLUMN_HEARTBEAT_TIME, InstanceTable.TABLE, InstanceTable.COLUMN_HEARTBEAT_TIME);
        Object[] params = new Object[] {fiveMinuteBefore};
        try (ResultSet rs = client.executeQuery(sql, params)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public Long instanceLastHeartBeatTime(long applicationInstanceId) {
        H2Client client = getClient();
        long fiveMinuteBefore = System.currentTimeMillis() - 5 * 60 * 1000;
        fiveMinuteBefore = TimeBucketUtils.INSTANCE.getSecondTimeBucket(fiveMinuteBefore);
        String sql = SqlBuilder.buildSql(GET_INST_LAST_HEARTBEAT_TIME_SQL, InstanceTable.COLUMN_HEARTBEAT_TIME, InstanceTable.TABLE,
            InstanceTable.COLUMN_HEARTBEAT_TIME, InstanceTable.COLUMN_INSTANCE_ID);
        Object[] params = new Object[] {fiveMinuteBefore, applicationInstanceId};
        try (ResultSet rs = client.executeQuery(sql, params)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public JsonArray getApplications(long startTime, long endTime) {
        H2Client client = getClient();
        JsonArray applications = new JsonArray();
        String sql = SqlBuilder.buildSql(GET_APPLICATIONS_SQL, InstanceTable.COLUMN_INSTANCE_ID,
            InstanceTable.TABLE, InstanceTable.COLUMN_HEARTBEAT_TIME, InstanceTable.COLUMN_APPLICATION_ID);
        Object[] params = new Object[] {startTime};
        try (ResultSet rs = client.executeQuery(sql, params)) {
            while (rs.next()) {
                Integer applicationId = rs.getInt(InstanceTable.COLUMN_APPLICATION_ID);
                logger.debug("applicationId: {}", applicationId);
                JsonObject application = new JsonObject();
                application.addProperty("applicationId", applicationId);
                application.addProperty("instanceCount", rs.getInt("cnt"));
                applications.add(application);
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return applications;
    }

    @Override
    public Instance getInstance(int instanceId) {
        H2Client client = getClient();
        String sql = SqlBuilder.buildSql(GET_INSTANCE_SQL, InstanceTable.TABLE, InstanceTable.COLUMN_INSTANCE_ID);
        Object[] params = new Object[] {instanceId};
        try (ResultSet rs = client.executeQuery(sql, params)) {
            if (rs.next()) {
                Instance instance = new Instance(rs.getString(InstanceTable.COLUMN_ID));
                instance.setApplicationId(rs.getInt(InstanceTable.COLUMN_APPLICATION_ID));
                instance.setAgentUUID(rs.getString(InstanceTable.COLUMN_AGENT_UUID));
                instance.setRegisterTime(rs.getLong(InstanceTable.COLUMN_REGISTER_TIME));
                instance.setHeartBeatTime(rs.getLong(InstanceTable.COLUMN_HEARTBEAT_TIME));
                instance.setOsInfo(rs.getString(InstanceTable.COLUMN_OS_INFO));
                return instance;
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Instance> getInstances(int applicationId, long timeBucket) {
        logger.debug("get instances info, application id: {}, timeBucket: {}", applicationId, timeBucket);
        List<Instance> instanceList = new LinkedList<>();
        H2Client client = getClient();
        String sql = SqlBuilder.buildSql(GET_INSTANCES_SQL, InstanceTable.TABLE, InstanceTable.COLUMN_APPLICATION_ID, InstanceTable.COLUMN_HEARTBEAT_TIME);
        Object[] params = new Object[] {applicationId, timeBucket};
        try (ResultSet rs = client.executeQuery(sql, params)) {
            while (rs.next()) {
                Instance instance = new Instance(rs.getString(InstanceTable.COLUMN_ID));
                instance.setApplicationId(rs.getInt(InstanceTable.COLUMN_APPLICATION_ID));
                instance.setHeartBeatTime(rs.getLong(InstanceTable.COLUMN_HEARTBEAT_TIME));
                instance.setInstanceId(rs.getInt(InstanceTable.COLUMN_INSTANCE_ID));
                instanceList.add(instance);
            }
        } catch (SQLException | H2ClientException e) {
            logger.error(e.getMessage(), e);
        }
        return instanceList;
    }
}
