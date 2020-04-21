package io.pravega.example.writers;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ObjectArrayTypeInfo;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import io.pravega.connectors.flink.Pravega;
import io.pravega.connectors.flink.PravegaConfig;
import org.apache.flink.table.api.Types;
import org.apache.flink.table.descriptors.Json;
import org.apache.flink.table.descriptors.Rowtime;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.table.descriptors.StreamTableDescriptor;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.expressions.ResolvedFieldReference;
import org.apache.flink.table.factories.StreamTableSourceFactory;
import org.apache.flink.table.factories.TableFactoryService;
import org.apache.flink.table.sources.TableSource;
import org.apache.flink.table.sources.tsextractors.TimestampExtractor;
import org.apache.flink.types.Row;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;


public class PravegaTableReader {
    public static void main (String[] args) throws Exception {
        PravegaConfig pravegaConfig = PravegaConfig.fromDefaults()
                .withControllerURI(URI.create("tcp://192.168.17.130:9090"))
                .withDefaultScope("Demo")
                //Enable it if with Nautilus
                //.withCredentials(credentials)
                .withHostnameValidation(false);
        Pravega pravega =  new Pravega();
        pravega.tableSourceReaderBuilder().forStream("test").withPravegaConfig(pravegaConfig);

        Schema schema = new Schema()
                //.field("rowtime", Types.LONG())
               // .rowtime(new Rowtime().timestampsFromField("time").watermarksPeriodicBounded(2000))
                .field("type", Types.INT())
                //.rowtime(new Rowtime().timestampsFromField("time"))
                .field("list", Types.OBJECT_ARRAY(
                                Types.ROW(
                                        new String[]{"id", "v", "q"},
                                        new TypeInformation[]{Types.STRING(), Types.DECIMAL(), Types.INT()})
                            )
                )

                ;

/*        Schema schema = new Schema()
                .field("id", Types.STRING())
                .field("time", Types.STRING())
                .field("value", Types.STRING()
                );
*/
        // set up the streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(2);
        EnvironmentSettings envSetting = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, envSetting);


        StreamTableDescriptor desc = (StreamTableDescriptor) tableEnv.connect(pravega)
                .withFormat(new Json()
                        .failOnMissingField(false)
                        .deriveSchema())
                .withSchema(schema)
                .inAppendMode();


        // Create Table source
        final Map<String, String> propertiesMap = desc.toProperties();
        final TableSource<?> source = TableFactoryService.find(StreamTableSourceFactory.class, propertiesMap)
                .createStreamTableSource(propertiesMap);


        // Register table source
        tableEnv.registerTableSource("tb_json", source);
        System.out.println("------------------print {} schema------------------" + source.getTableSchema());
        //item[1] item[10] start from 1
        //String sqlJson = "select type,time,l.id,l.value,l.q from tb_json, unnest(list) as l (id,value,q)";
        //String sqlJson = "select type from tb_json";
        String sqlJson = "SELECT type, l.id as id, l.v as v, l.q as q FROM tb_json CROSS JOIN UNNEST(list) as l (id,v,q)";
        Table tableRecord = tableEnv.sqlQuery(sqlJson);
        tableEnv.registerTable("tb_Record", tableRecord);
        System.out.println("------------------print {} schema------------------" + "tb_Record");
        tableRecord.printSchema();


        String sqlMoni = "SELECT id, v FROM tb_Record WHERE q=1 AND  type =1";
        String sqlKaiguan = "SELECT id, CAST (v AS INT) AS V  FROM tb_Record WHERE q=1 AND  type =3";

        Table tableMoniRecord = tableEnv.sqlQuery(sqlMoni);
        tableEnv.registerTable("tb_MoniRecord", tableMoniRecord);
        System.out.println("------------------print {} schema------------------" + "tb_MoniRecord");
        tableMoniRecord.printSchema();
        //tableEnv.toAppendStream(tableMoniRecord, Row.class).print();

        Table tableKaiguanRecord = tableEnv.sqlQuery(sqlKaiguan);
        tableEnv.registerTable("tb_KaiguanRecord", tableKaiguanRecord);
        System.out.println("------------------print {} schema------------------" + "tb_KaiguanRecord");
        tableKaiguanRecord.printSchema();
        tableEnv.toAppendStream(tableKaiguanRecord, Row.class).print();

        tableEnv.execute("Streaming Job");
    }
}