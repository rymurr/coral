package com.linkedin.coral.hive.hive2rel;

import com.google.common.collect.ImmutableList;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.ParseDriver;
import org.apache.hadoop.hive.ql.parse.ParseException;

import static com.google.common.base.Preconditions.*;


public class TestUtils {

  public static ASTNode toAST(String sql) throws ParseException {
    checkNotNull(sql);
    ParseDriver pd = new ParseDriver();
    return pd.parse(sql);
  }

  public static class TestHive {
    public static class DB {
      DB(String name, Iterable<String> tables) {
        this.name = name;
        this.tables = ImmutableList.copyOf(tables);
      }
      String name;
      List<String> tables;
    }

    public List<String> getDbNames() {
      return databases.stream()
          .map(db -> db.name)
          .collect(Collectors.toList());
    }

    public List<String> getTables(String db) {
      return databases.stream()
          .filter(d -> d.name == db)
          .findFirst()
          .orElseThrow(() -> new RuntimeException("DB " + db + " not found"))
          .tables;
    }

    List<DB> databases;
    HiveContext context;
  }

  public static TestHive setupDefaultHive() {
    HiveContext context = initLocalHive();
    TestHive hive = new TestHive();
    hive.context = context;
    Driver driver = context.getDriver();
    try {
      driver.run("CREATE DATABASE IF NOT EXISTS test");
      driver.run("CREATE TABLE IF NOT EXISTS test.tableOne(a int, b varchar(30), c double, d timestamp)");
      driver.run("CREATE TABLE IF NOT EXISTS test.tableTwo(x int, y double)");
      driver.run("CREATE TABLE IF NOT EXISTS foo(a int, b varchar(30), c double)");
      driver.run("CREATE TABLE IF NOT EXISTS bar(x int, y double)");
      driver.run(
          "CREATE TABLE IF NOT EXISTS complex(a int, b string, c array<double>, s struct<name:string, age:int>, m map<int, string>)");
      hive.databases = ImmutableList.of(
          new TestHive.DB("test", ImmutableList.of("tableOne", "tableTwo")),
          new TestHive.DB("default", ImmutableList.of("foo", "bar", "complex"))
      );
      return hive;
    } catch (Exception e) {
      throw new RuntimeException("Failed to setup database", e);
    }
  }

  public static HiveContext initLocalHive() {
    HiveConf conf = loadResourceHiveConf();
    try {
      return HiveContext.create(conf);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static HiveConf loadResourceHiveConf() {
    InputStream hiveConfStream = TestUtils.class.getClassLoader().getResourceAsStream("hive.xml");
    HiveConf hiveConf = new HiveConf();
    hiveConf.addResource(hiveConfStream);
    hiveConf.set("mapreduce.framework.name", "local");
    hiveConf.set("_hive.hdfs.session.path", "/tmp/coral");
    hiveConf.set("_hive.local.session.path", "/tmp/coral");
    return hiveConf;
  }
}