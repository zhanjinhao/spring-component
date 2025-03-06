package cn.addenda.component.spring.test.multidatasource;

import cn.addenda.component.base.collection.ArrayUtils;
import cn.addenda.component.base.datetime.DateUtils;
import cn.addenda.component.spring.cron.CronBak;
import cn.addenda.component.spring.multidatasource.MultiDataSource;
import cn.addenda.component.spring.multidatasource.MultiDataSourceConstant;
import cn.addenda.component.spring.multidatasource.MultiDataSourceEntry;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author addenda
 * @since 2022/2/26 23:00
 */
public class MultiDataSourceTest {

  private DataSource dataSource;
  private HikariDataSource hikariDataSource;

  @Before
  public void before() {
    hikariDataSource = hikariDataSource();
    dataSource = dataSource(hikariDataSource);
  }

  @After
  public void after() {
    hikariDataSource.close();
  }

  public HikariDataSource hikariDataSource() {
    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setDriverClassName("org.h2.Driver");
    hikariDataSource.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    hikariDataSource.setUsername("root");
    hikariDataSource.setPassword("root");
    hikariDataSource.setMaximumPoolSize(3);

    return hikariDataSource;
  }

  public DataSource dataSource(HikariDataSource hikariDataSource) {
    MultiDataSourceEntry multiDataSourceEntry = new MultiDataSourceEntry();
    multiDataSourceEntry.setMaster(hikariDataSource);

    MultiDataSource multiDataSource = new MultiDataSource();
    multiDataSource.addMultiDataSourceEntry(MultiDataSourceConstant.DEFAULT, multiDataSourceEntry);
    return multiDataSource;
  }

  @Test
  public void test() {
    createTable();
    test1();
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

}
