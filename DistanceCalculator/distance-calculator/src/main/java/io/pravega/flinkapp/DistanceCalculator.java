package io.pravega.flinkapp;


import io.pravega.client.stream.Stream;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.PravegaConfig;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import org.apache.flink.util.Collector;
import org.apache.flink.api.java.utils.ParameterTool;

import java.net.URI;
import java.text.DecimalFormat;

public class DistanceCalculator {

    public static void main(String[] args) throws Exception{

        ParameterTool params = ParameterTool.fromArgs(args);

        final String scope = params.get("scope", "examples");
        final String streamName = params.get("stream", "example-data");
        final URI controllerURI = URI.create(params.get("pravega.uri", "tcp://127.0.0.1:9090"));

        // initialize the parameter utility tool in order to retrieve input parameters
        PravegaConfig pravegaConfig = PravegaConfig.fromDefaults()
                .withControllerURI(controllerURI)
                .withDefaultScope(scope)
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

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
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
                .assignTimestampsAndWatermarks(new MyAssignTime())
                .keyBy(
                        new KeySelector<RawSenorData, String>() {
                            @Override
                            public String getKey(RawSenorData d) throws Exception {
                                return d.id;
                            }
                        }
                )
                .timeWindow(Time.milliseconds(3000))
                .aggregate(new MyAgg(), new MyPro());

        // create an output sink to print to stdout for verification
        dataStream.print();
        dataStream.addSink(new InfluxdbSink());
        // execute within the Flink environment
        env.execute("DataAnalyzer");
    }


    private static class MyAssignTime implements AssignerWithPeriodicWatermarks<RawSenorData> {

        private final long maxOutOfOrderness = 1000; // 1 seconds

        private long currentMaxTimestamp;

        @Override
        public long extractTimestamp(RawSenorData element, long previousElementTimestamp) {
            long timestamp = element.getTimestamp();
            currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
            System.out.println("timestamp: " + timestamp);
            System.out.println("currentMaxTimestamp: " + currentMaxTimestamp);
            return timestamp;
        }

        @Override
        public Watermark getCurrentWatermark() {
            // return the watermark as current highest timestamp minus the out-of-orderness bound
            return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
        }
    }

    private static class MyPro extends ProcessWindowFunction<Double, OutSenorData, String, TimeWindow> {
        private ValueState<Double> last;
        private ValueState<Double> lastlast;

        @Override
        public void open(Configuration config) throws Exception {
            ValueStateDescriptor<Double> descriptor = new ValueStateDescriptor<>("saved last", Double.class);
            last = getRuntimeContext().getState(descriptor);
            ValueStateDescriptor<Double> ldescriptor = new ValueStateDescriptor<>("saved last last", Double.class);
            lastlast = getRuntimeContext().getState(ldescriptor);
        }

/*        @Override
        public void process(String key, Context context, Iterable<Double> elements, Collector<OutSenorData> out) throws Exception {
           for (Double d: elements) {
                int trend = 0;
                Double diff = last.value() == null ? 0.0 : d - last.value();
                Double lastdiff = lastlast.value() == null ? 0.0 : last.value() - lastlast.value();
                if (diff > 100.0) {
                    if (lastdiff > 100.0) {
                        trend = 3;
                    } else {
                        trend = 1;
                    }
                } else  if (diff < -100.0) {
                    if (lastdiff < -100.0) {
                        trend = 4;
                    } else {
                        trend = 2;
                    }
                } else if ((diff <= 100.0) && (diff >= -100.0)) {
                    trend = 0;
                }
                out.collect(new OutSenorData(context.window().getEnd(), key, diff, trend, d));
                lastlast.update(last.value());
                last.update(d);
            }
        }
    }
*/

        @Override
        public void process(String key, Context context, Iterable<Double> elements, Collector<OutSenorData> out) throws Exception {
           for (Double d: elements) {
                int trend = 0;
                Double diff = last.value() == null ? 0.0 : d - last.value();
                Double lastdiff = lastlast.value() == null ? 0.0 : last.value() - lastlast.value();
                //Trend Meaning:
                // 0: Normal, 2: A little Far, 3: Far
                if (d > 1 && d <= 3)  {
                        trend = 2;
                 }
                else if (d > 0 && d <= 1 ) {
                        trend = 0;
                } else  trend = 3;
                out.collect(new OutSenorData(context.window().getEnd(), key, diff, trend, d));
               lastlast.update(last.value());
               last.update(d);
            }
        }
    }

    private static class MyAgg implements AggregateFunction<RawSenorData, AverageAccumulator, Double> {

        @Override
        public AverageAccumulator createAccumulator() {
            return new AverageAccumulator(0, 0.0);
        }

        @Override
        public AverageAccumulator add(RawSenorData nd, AverageAccumulator acc) {
            acc.sum += nd.getValue();
            acc.count++;
            System.out.println(acc.sum  + "ddd");
            return acc;
        }

        @Override
        public Double getResult(AverageAccumulator acc) {

            String test = "";
            test =  new DecimalFormat("0.00").format(acc.sum / acc.count);
            return Double.valueOf(test);
        }

        @Override
        public AverageAccumulator merge(AverageAccumulator a, AverageAccumulator b) {
            a.count += b.count;
            a.sum += b.sum;
            return a;
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

}
