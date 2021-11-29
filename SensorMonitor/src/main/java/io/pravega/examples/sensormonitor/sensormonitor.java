package io.pravega.examples.sensormonitor;

import io.pravega.client.ClientConfig;
import io.pravega.client.admin.StreamInfo;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.admin.impl.StreamCutHelper;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.StreamCut;
import io.pravega.controller.stream.api.grpc.v1.Controller;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;


import java.net.URI;
import java.time.ZoneId;


public class sensormonitor {
    public static void main (String[] args) throws Exception {
        // set up the streaming execution environment

        ParameterTool params = ParameterTool.fromArgs(args);

        // initialize the parameter utility tool in order to retrieve input parameters
        final String scope = params.get("pravega_scope", "examples");
        final String streamName = params.get("pravega_stream", "example-data");
        final String controllerURI = params.get("pravega_controller_uri", "tcp://127.0.0.1:9090");
        final String influxdbUrl = String.valueOf(URI.create(params.get("influxdb_url", "http://127.0.0.1:8086")));
        final String influxdbUsername = params.get("influxdb_username", "root");
        final String influxdbPassword = params.get("influxdb_password", "root");
        final String influxdbDbName = params.get("influxdb_DbName", "demo");
        System.out.println("pravega_controller_uri:" + controllerURI );
        System.out.println("pravega_scope:" + scope );
        System.out.println("pravega_stream:" + streamName );
        System.out.println("influxdb_url:" + influxdbUrl );
        System.out.println("influxdb_username:" + influxdbUsername );
        System.out.println("influxdb_password:" + influxdbPassword );
        System.out.println("influxdb_DbName:" + influxdbDbName );


        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(1);
        EnvironmentSettings envSetting = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, envSetting);
        tableEnv.getConfig().setLocalTimeZone(ZoneId.of("Etc/UTC"));
        StreamManager streamManager = StreamManager.create(ClientConfig.builder().controllerURI(URI.create(controllerURI)).build());
        StreamCut streamCut = streamManager.getStreamInfo(scope,streamName).getTailStreamCut();


        String sqlDdlAnaTable = "CREATE TABLE ana_Source(type INT, datatime BIGINT, list ARRAY <ROW(id STRING, v FLOAT, q INTEGER)>, ts AS TO_TIMESTAMP(FROM_UNIXTIME(datatime)), WATERMARK FOR ts AS ts - INTERVAL '5' SECOND)" +
                " WITH (" +
                "'connector.type' = 'pravega'," +
                "'connector.version' = '1'," +
                "'connector.connection-config.controller-uri'= '" + controllerURI +"'," +
                "'connector.connection-config.default-scope' = '" + scope + "'," +
                "'connector.reader.stream-info.0.stream' = '" + streamName + "'," +
                "'connector.reader.stream-info.0.start-streamcut' = '" + streamCut.toString() + "'," +
                "'format.type' = 'json'," +
                "'format.fail-on-missing-field' = 'false', " +
                "'update-mode' = 'append')";
        tableEnv.sqlUpdate(sqlDdlAnaTable);

        String sqlJson = "SELECT ts, type, l.id AS id, l.v AS v, l.q AS q " +
                "FROM ana_Source " +
                "CROSS JOIN UNNEST(list) as l (id,v,q)";
        Table tableJsonRecord = tableEnv.sqlQuery(sqlJson);
        tableEnv.registerTable("tb_JsonRecord", tableJsonRecord);
        System.out.println("------------------print {} schema------------------" + "tb_JsonRecord");
        tableJsonRecord.printSchema();
        //tableEnv.toAppendStream(tableRecord, Row.class).print();

        String sqlAna = "SELECT ts, id, v " +
                "FROM tb_JsonRecord " +
                "WHERE q=1 AND type=1";
        Table tableAnaRecord = tableEnv.sqlQuery(sqlAna);
        tableEnv.registerTable("tb_AnaRecord", tableAnaRecord);
        System.out.println("------------------print {} schema------------------" + "tb_AnaRecord");
        tableAnaRecord.printSchema();
        //tableEnv.toAppendStream(tableAnaRecord, Row.class).print();


        String sqlAnaAvg = "SELECT id, " +
                "CAST(TUMBLE_START(ts, INTERVAL '10' SECOND) as INT) as wStart, " +
                "CAST(AVG(v) as DOUBLE) FROM tb_AnaRecord " +
                "GROUP BY TUMBLE(ts, INTERVAL '10' SECOND), id";
        Table tableAvgRecord = tableEnv.sqlQuery(sqlAnaAvg);
        tableEnv.registerTable("tb_AvgRecord", tableAvgRecord);
        System.out.println("------------------print {} schema------------------" + "tb_AvgRecord");
        tableAvgRecord.printSchema();
        DataStream<OutputData> output = tableEnv.toAppendStream(tableAvgRecord, Row.class)
                .flatMap(new FlatMapFunction<Row, OutputData>()
                {
                    @Override
                    public void flatMap(Row value, Collector<OutputData> out) throws Exception {
                        out.collect(
                                new OutputData((Integer) value.getField(1),
                                        value.getField(0).toString(),
                                        (Double) value.getField(2))
                        );
                    }

                });
        output.print();
        output.addSink(new InfluxdbSink(influxdbUrl, influxdbUsername, influxdbPassword, influxdbDbName));

        tableEnv.execute("Streaming Job");
    }
}