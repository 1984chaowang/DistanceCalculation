package io.pravega.examples.sensormonitor;

import java.io.Serializable;

public class OutputData implements Serializable {
    public long timestamp;
    public String sensorid;
    public Double average;

    public OutputData() {
        timestamp = 0;
        sensorid = "";
        average = 0.0;

    }

    long getTimestamp() {return timestamp;}
    String getSensorid() {return sensorid;}
    Double getAverage () {return average;}

    public OutputData(long ts, String id, Double av) {
        timestamp = ts;
        sensorid = id;
        average = av;
    }

    @Override
    public String toString() {
        return "id: "+ sensorid + ": " + " timestamp: " + timestamp + " average: " + average;
    }

}
