package io.pravega.flinkapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawSenorData implements Serializable {
    public String id;
    public String time;
    public Double value;

    public RawSenorData() {
        id = "";
        time="";
        value = 0.0;
    }

    public String getId() {return id;}
    public Double getValue() {return value;}
    public long getTimestamp() {
        long t = timeTomMllisecond(time);
        return (t);
    }

    public static Long timeTomMllisecond(String time){
        SimpleDateFormat sdf=new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        long aftertime=0;
        try {
            Object d1= sdf.parse(time).getTime();
            long d1time=Long.parseLong(d1.toString());
            aftertime = d1time;
            System.out.println("aftertime: " + aftertime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return aftertime;

    }

    @Override
    public String toString() {return "id: "+ id + ": " + " time: " + time + " value: " + value;}
}

