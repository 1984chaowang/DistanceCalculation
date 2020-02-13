package io.pravega.flinkapp;

import java.io.Serializable;

public class OutSenorData {
    public long timestamp;
    public String sensorid;
    public Double difference;
    int trend;
    public Double average;

    public OutSenorData() {
        timestamp = 0;
        sensorid = "";
        difference = 0.0;
        trend = 0;
    }

    long getTimestamp() {return timestamp;}
    String getSensorid() {return sensorid;}
    Double getDifference() {return difference;}
    int getTrend() {return trend;}
    Double getAverage () {return average;}

    public OutSenorData(long ts, String id, Double diff, int t, Double av) {
        timestamp = ts;
        sensorid = id;
        difference = diff;
        trend = t;
        average = av;
    }

    @Override
    public String toString() {
        return "id: "+ sensorid + ": " + " timestamp: " + timestamp + " difference: " + difference + " trend: " + trend;
    }

}
