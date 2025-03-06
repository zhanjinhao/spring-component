package cn.addenda.component.spring.test.cron;

import cn.addenda.component.base.collection.ArrayUtils;
import cn.addenda.component.base.datetime.DateUtils;
import cn.addenda.component.spring.cron.CronBak;
import cn.addenda.component.spring.cron.CronDelete;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Properties;

public class CronBakTest {

  static Properties properties;

  static {
    try {
      String path = CronBakTest.class.getClassLoader()
              .getResource("db.properties").getPath();
      properties = new Properties();
      properties.load(new FileInputStream(path));
    } catch (Exception e) {

    }
  }

  private HikariDataSource dataSource;

  @Before
  public void before() {
    dataSource = dataSource();
  }

  public HikariDataSource dataSource1() {
    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setDriverClassName("org.h2.Driver");
    hikariDataSource.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    hikariDataSource.setUsername("root");
    hikariDataSource.setPassword("root");
    hikariDataSource.setMaximumPoolSize(3);

    return hikariDataSource;
  }

  public HikariDataSource dataSource() {
    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setDriverClassName(properties.getProperty("driver"));
    hikariDataSource.setJdbcUrl(properties.getProperty("url"));
    hikariDataSource.setUsername(properties.getProperty("username"));
    hikariDataSource.setPassword(properties.getProperty("password"));
    hikariDataSource.setMaximumPoolSize(3);

    return hikariDataSource;
  }

  @Test
  public void test() {
    createTable();
    test1();
    test2();
    dropTable();
  }

  @SneakyThrows
  public void createTable() {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);
    String createUserTable =
            "    create table `user`\n" +
                    "    (\n" +
                    "    `id` bigint auto_increment not null\n" +
                    "    primary key,\n" +
                    "    `nickname` varchar(36) null,\n" +
                    "    `age` int null,\n" +
                    "    `create_time` datetime(3) null\n" +
                    "    )";
    String createUserBakTable =
            "    create table `user_bak`\n" +
                    "    (\n" +
                    "    `id` bigint auto_increment not null\n" +
                    "    primary key,\n" +
                    "    `nickname` varchar(36) null,\n" +
                    "    `age` int null,\n" +
                    "    `create_time` datetime(3) null\n" +
                    "    )";
    Statement statement = connection.createStatement();
    statement.executeUpdate(createUserTable);
    statement.executeUpdate(createUserBakTable);
    connection.commit();

    String insertSql1 = "insert into `user` set `id` = 1, `nickname` = 'a', `age` = 10, `create_time` = '2025-02-24 10:10:00'";
    String insertSql2 = "insert into `user` set `id` = 2, `nickname` = 'a', `age` = 10, `create_time` = '2025-02-24 10:11:00'";
    Statement statement1 = connection.createStatement();
    statement1.executeUpdate(insertSql1);
    statement1.executeUpdate(insertSql2);
    connection.commit();
    connection.close();
  }

  @SneakyThrows
  public void dropTable() {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);
    String dropUserTable = "drop table `user`";
    String dropUserBakTable = "drop table `user_bak`";
    Statement statement = connection.createStatement();
    statement.executeUpdate(dropUserTable);
    statement.executeUpdate(dropUserBakTable);
    connection.commit();
    connection.close();
  }

  @SneakyThrows
  public void test1() {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);
    CronBak cronBak = new CronBak("0 * * * * ?", dataSource, 1, true,
            "user", "`create_time` > '2025-02-24 10:10:30'",
            ArrayUtils.asHashSet("id", "nickname", "age", "create_time"), ArrayUtils.asHashSet("id"),
            "user_bak");
    cronBak.clean();
    String query = query(connection);
    Assert.assertEquals("2 a 10 2025-02-24T00:00", query);
    connection.close();
  }

  @SneakyThrows
  private String query(Connection connection) {
    String a = "";
    Statement statement2 = connection.createStatement();
    ResultSet resultSet = statement2.executeQuery("select `id`, `nickname`, `age`, `create_time` from `user_bak`");
    while (resultSet.next()) {
      long id = resultSet.getLong("id");
      String nickname = resultSet.getString("nickname");
      int age = resultSet.getInt("age");
      LocalDateTime create_time = DateUtils.dateToLocalDateTime(new Date(resultSet.getDate("create_time").getTime()));
      a = id + " " + nickname + " " + age + " " + create_time;
    }
    return a;
  }

  @SneakyThrows
  public void test2() {
    Connection connection = dataSource.getConnection();
    connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    connection.setAutoCommit(false);
    String query1 = query(connection);
    Assert.assertEquals("2 a 10 2025-02-24T00:00", query1);

    CronDelete cronDelete = new CronDelete("0 * * * * ?", dataSource, 1, true,
            "user_bak", "`create_time` > '2025-02-24 10:10:30'", ArrayUtils.asHashSet("id"),
            CronDelete.DELETE_BY_PRIMARY_KEY);
    cronDelete.clean();

    String query2 = query(connection);
    Assert.assertEquals("", query2);
    connection.close();
  }

  @Test
  @SneakyThrows
  public void testDeleteFailed() {
    createTable();

    Connection connection = dataSource.getConnection();
    connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    connection.setAutoCommit(false);

    String deleteSql = "delete from `user` where `id` = ?";
    PreparedStatement preparedStatement = connection.prepareStatement(deleteSql);
    preparedStatement.setLong(1, 10L);
    preparedStatement.addBatch();
    int[] ints = preparedStatement.executeBatch();

    Assert.assertEquals(1, ints.length);
    Assert.assertEquals(0, ints[0]);

    connection.commit();

    preparedStatement = connection.prepareStatement(deleteSql);
    preparedStatement.setLong(1, 1L);
    preparedStatement.addBatch();
    preparedStatement.setLong(1, 2L);
    preparedStatement.addBatch();
    preparedStatement.setLong(1, 3L);
    preparedStatement.addBatch();
    ints = preparedStatement.executeBatch();
    Assert.assertEquals(3, ints.length);
    Assert.assertEquals(1, ints[0]);
    Assert.assertEquals(1, ints[1]);
    Assert.assertEquals(0, ints[2]);

    connection.commit();

    dropTable();
  }

  @After
  public void after() {
    dataSource.close();
  }

}
