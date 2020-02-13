package io.pravega.flinkapp;


import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

import java.util.concurrent.TimeUnit;

public class InfluxdbSink extends RichSinkFunction<OutSenorData> {
    InfluxDB influxDB = null;

    public InfluxdbSink() {}
    @Override
    public void invoke(OutSenorData value) {
        try {
            String dbName = "sharktank";
            influxDB.query(new Query("CREATE DATABASE " + dbName));
            influxDB.setDatabase(dbName);
            System.out.println("value: " + value);
            influxDB.write(Point.measurement(value.getSensorid())
                    .time(value.getTimestamp(), TimeUnit.MILLISECONDS)
                    .addField("DIFFERENCE", value.getDifference())
                    .addField("TREND", value.getTrend())
                    .addField("AVERAGE", value.getAverage())
                    .build());

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void open(Configuration config) {
        influxDB = InfluxDBFactory.connect("http://monitoring-influxdb.default.svc.cluster.local:8086", "root", "root");
    }

    @Override
    public void close() throws Exception {
        if (influxDB != null) {
            influxDB.close();
        }
    }
}
