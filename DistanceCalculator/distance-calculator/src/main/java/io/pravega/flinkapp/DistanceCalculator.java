package io.pravega.flinkapp;


import io.pravega.client.stream.Stream;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.PravegaConfig;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import org.apache.flink.util.Collector;
import org.apache.flink.api.java.utils.ParameterTool;

import java.net.URI;

public class DistanceCalculator {

    public static void main(String[] args) throws Exception{

        ParameterTool params = ParameterTool.fromArgs(args);
        // initialize the parameter utility tool in order to retrieve input parameters
        final String scope = params.get("pravega_scope", "examples");
        final String streamName = params.get("pravega_stream", "example-data");
        final URI controllerURI = URI.create(params.get("pravega_controller_uri", "tcp://127.0.0.1:9090"));
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


        //PravegaConfig pravegaConfig = PravegaConfig.fromDefaults()
        PravegaConfig pravegaConfig = PravegaConfig.fromParams(params)
                .withControllerURI(controllerURI)
                .withDefaultScope(scope)
                //Enable it if with Nautilus
                //.withCredentials(credentials)
                .withHostnameValidation(false);

        System.out.println("==============  pravegaConfig  =============== "+pravegaConfig);

        // create the Pravega input stream (if necessary)
        Stream stream = Utils.createStream(
                pravegaConfig,
                streamName);
        System.out.println("==============  stream  =============== "+stream);

        // initialize the Flink execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Using EventTime in Flink runtime
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // Using ProcessingTime in Flink runtime
        //env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        // create the Pravega source to read a stream of text
        FlinkPravegaReader<RawSenorData> source = FlinkPravegaReader.<RawSenorData>builder()
                .withPravegaConfig(pravegaConfig)
                .forStream(stream)
                .withDeserializationSchema(new JsonDeserializationSchema(RawSenorData.class))
                .build();

        // count each word over a 10 second time period
        DataStream<OutSenorData> dataStream = env.addSource(source).name(streamName)
                //.flatMap(new DataAnalyzer.Splitter())
                //.flatMap(new Splitter())
                /*.assignTimestampsAndWatermarks(
                        new BoundedOutOfOrdernessTimestampExtractor<RawSenorData>(Time.milliseconds(1000)) {
                         @Override
                        public long extractTimestamp(RawSenorData element) {
                             Utils.timeformat(element.getTimestamp());
                             return element.getTimestamp();
                         }
                })*/
                .setParallelism(1)
                .assignTimestampsAndWatermarks(new MyAssignTime())
                .keyBy(
                        new KeySelector<RawSenorData, String>() {
                            @Override
                            public String getKey(RawSenorData d) throws Exception {
                                return d.getId();
                            }
                        }
                )
                .window(TumblingEventTimeWindows.of(Time.milliseconds(3000)))
                .aggregate(new MyAgg(), new MyPro());

        // create an output sink to print to stdout for verification
        dataStream.print();
        // create an sink to InfluxDB
        dataStream.addSink(new InfluxdbSink(influxdbUrl, influxdbUsername, influxdbPassword, influxdbDbName));
        // execute within the Flink environment
        env.execute("DistanceCalculator");
    }


    public static class MyAssignTime implements AssignerWithPeriodicWatermarks<RawSenorData> {

        private final long maxOutOfOrderness = 2000; // 1 seconds

        private long currentMaxTimestamp;

        @Override
        public long extractTimestamp(RawSenorData element, long previousElementTimestamp) {
            long timestamp = element.getTimestamp();
            currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
            //System.out.println("timestamp: " + Utils.timeformat(timestamp) + "|" + timestamp);
            //System.out.println("previousElementTimestamp: " + previousElementTimestamp);
            System.out.println("currentMaxTimestamp: " + Utils.timeformat(currentMaxTimestamp) + "|" + currentMaxTimestamp);
            return timestamp;
        }

        @Override
        public Watermark getCurrentWatermark() {
            // return the watermark as current highest timestamp minus the out-of-orderness bound
            Watermark a = new Watermark(currentMaxTimestamp - maxOutOfOrderness);
            System.out.println("Watermark: " + a.toString());
            return a;
        }
    }

    private static class MyPro extends ProcessWindowFunction<Double, OutSenorData, String, TimeWindow> {

         public void process(String key, Context context, Iterable<Double> elements, Collector<OutSenorData> out) throws Exception {
           for (Double d: elements) {
                int trend = 0;
                Double diff = 0.0;
                //Trend Meaning:
                // 0: Normal, 2: A little Far, 3: Far
                if (d > 1 && d <= 3)  {
                        trend = 2;
                 }
                else if (d > 0 && d <= 1 ) {
                        trend = 0;
                } else  trend = 3;
                out.collect(new OutSenorData(context.window().getEnd(), key, diff, trend, d));
                }
        }
    }

    public static class AverageAccumulator{
        int count;
        Double sum;

        public AverageAccumulator() {}
        public AverageAccumulator(int count, Double sum) {
            this.count = count;
            this.sum = sum;
        }
    }

    private static class MyAgg implements AggregateFunction<RawSenorData, AverageAccumulator, Double> {

        @Override
        public AverageAccumulator createAccumulator() {
            return new AverageAccumulator(0, 0.0);
        }

        @Override
        public AverageAccumulator merge(AverageAccumulator a, AverageAccumulator b) {
            a.count += b.count;
            a.sum += b.sum;
            return a;
        }

        @Override
        public AverageAccumulator add(RawSenorData value, AverageAccumulator acc) {
            acc.count ++;
            acc.sum += value.getValue();
            System.out.println("count: " + acc.count + "sum: " + acc.sum);
            return acc;
        }

        @Override
        public Double getResult(AverageAccumulator acc) {
            return acc.sum / (double) acc.count;
        }
    }

}

