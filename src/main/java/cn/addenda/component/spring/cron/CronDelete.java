package cn.addenda.component.spring.cron;

import cn.addenda.component.base.collection.ArrayUtils;
import cn.addenda.component.base.sql.ConnectionUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2024/3/27 19:40
 */
@Slf4j
public class CronDelete extends CronClean implements InitializingBean {

  private final DataSource dataSource;

  private final String tableName;

  private final Integer oneBatch;

  private final String condition;

  private final Set<String> primaryKeyColumnSet;

  private final String deleteByType;

  public static final String DELETE_BY_CONDITION = "DBC";

  public static final String DELETE_BY_PRIMARY_KEY = "DBPK";

  private ThreadPoolTaskScheduler threadPoolTaskScheduler;

  private final boolean graveColumn;

  private volatile boolean init = false;

  private volatile boolean start = false;

  public CronDelete(String cron, DataSource dataSource, int oneBatch, boolean graveColumn,
                    String tableName, String condition, String primaryKeyColumn, String deleteByType) {
    this(cron, dataSource, oneBatch, graveColumn, tableName, condition, ArrayUtils.asHashSet(primaryKeyColumn), deleteByType);
  }

  public CronDelete(String cron, DataSource dataSource, int oneBatch, boolean graveColumn,
                    String tableName, String condition, Set<String> primaryKeyColumnSet, String deleteByType) {
    super(cron);
    Assert.notNull(dataSource, "`dataSource` can not be null!");
    Assert.isTrue(oneBatch > 0, "`oneBatch` must greater than 0!");
    Assert.notNull(tableName, "`tableName` can not be null!");
    Assert.notNull(condition, "`condition` can not be null!");
    Assert.isTrue(!CollectionUtils.isEmpty(primaryKeyColumnSet), "`primaryKeyColumnSet` can not be empty!");
    Assert.isTrue(DELETE_BY_CONDITION.equals(deleteByType) || DELETE_BY_PRIMARY_KEY.equals(deleteByType), "`deleteByType` can only be DELETE_BY_CONDITION or DELETE_BY_PRIMARY_KEY!");

    this.dataSource = dataSource;
    this.oneBatch = oneBatch;
    this.graveColumn = graveColumn;
    this.tableName = tableName;
    this.condition = condition;
    this.primaryKeyColumnSet = primaryKeyColumnSet.stream()
            .map(this::removeGrave)
            .collect(Collectors.toSet());
    this.deleteByType = deleteByType;
    init();
  }

  private void init() {
    log.info("tableName: {}", graveIfNecessary(tableName));
    if (DELETE_BY_CONDITION.equals(deleteByType)) {
      log.info("GENERATE_DELETE_BY_CONDITION_SQL: {}", GENERATE_DELETE_BY_CONDITION_SQL());
    } else if (DELETE_BY_PRIMARY_KEY.equals(deleteByType)) {
      log.info("GENERATE_QUERY_SQL: {}", GENERATE_QUERY_SQL());
      log.info("GENERATE_DELETE_BY_PRIMARY_KEY_SQL: {}", GENERATE_DELETE_BY_PRIMARY_KEY_SQL());
    }
    log.info("cron description: {}", cronDescription());
  }

  private String GENERATE_DELETE_BY_CONDITION_SQL() {
    StringBuilder sql = new StringBuilder();
    sql.append("delete from ").append(graveIfNecessary(tableName)).append(" where ").append(condition);
    sql.append(" order by ");
    for (String primaryKeyColumn : primaryKeyColumnSet) {
      sql.append(" ").append(graveIfNecessary(primaryKeyColumn)).append(" asc ,");
    }
    sql = new StringBuilder(sql.substring(0, sql.length() - 1));
    return sql + " limit " + oneBatch;
  }

  @SneakyThrows
  private void delete() {
    if (DELETE_BY_CONDITION.equals(deleteByType)) {
      deleteByCondition();
    } else if (DELETE_BY_PRIMARY_KEY.equals(deleteByType)) {
      deleteByPrimaryKey();
    }
  }

  @SneakyThrows
  private void deleteByPrimaryKey() {
    try (Connection connection = dataSource.getConnection()) {
      boolean originalAutoCommit = ConnectionUtils.setAutoCommitFalse(connection);
      try {
        List<Map<String, Object>> expiredList = queryExpired(connection);
        if (!expiredList.isEmpty()) {
          int[] deleteResults = deleteByPrimaryKey(connection, expiredList);
          logDeletedRecord(expiredList, deleteResults);
        }
        connection.commit();
      } finally {
        ConnectionUtils.setAutoCommitAndClose(connection, originalAutoCommit);
      }
    }
  }

  private void logDeletedRecord(List<Map<String, Object>> expiredList, int[] deleteResults) {
    List<String> deleteKey = new ArrayList<>();
    for (int i = 0; i < expiredList.size(); i++) {
      int deleteResult = deleteResults[i];
      if (deleteResult == 1) {
        Map<String, Object> expired = expiredList.get(i);
        deleteKey.add(generateKey(expired));
      } else if (deleteResult != 0) {
        // 正常情况下是不会进入这里的
        log.error("删除操作的结果异常，当前索引[{}]的结果是[{}]，主键集合[{}]，结果集合[{}]。", i, deleteResult,
                expiredList.stream().map(this::generateKey).collect(Collectors.joining(",")),
                Arrays.stream(deleteResults).mapToObj(String::valueOf).collect(Collectors.joining(",")));
      }
    }
    if (!CollectionUtils.isEmpty(deleteKey)) {
      log.debug("CronDelete [{}], 主键为[{}]的数据已删除。", graveIfNecessary(tableName), String.join(",", deleteKey));
    }
  }

  private String generateKey(Map<String, Object> expired) {
    return primaryKeyColumnSet.stream()
            .map(expired::get)
            .map(String::valueOf)
            .collect(Collectors.joining(","));
  }

  private String GENERATE_DELETE_BY_PRIMARY_KEY_SQL() {
    StringBuilder sql = new StringBuilder();
    sql.append("delete from ").append(graveIfNecessary(tableName)).append(" where ");
    for (String primaryKeyColumn : primaryKeyColumnSet) {
      sql.append(graveIfNecessary(primaryKeyColumn)).append("=? ,");
    }
    sql = new StringBuilder(sql.substring(0, sql.length() - 1));
//    sql.append(" order by ");
//    for (String primaryKeyColumn : primaryKeyColumnSet) {
//      sql.append(" ").append(graveIfNecessary(primaryKeyColumn)).append(" asc ,");
//    }
//    sql = new StringBuilder(sql.substring(0, sql.length() - 1));
    return sql.toString();
  }

  @SneakyThrows
  private int[] deleteByPrimaryKey(Connection connection, List<Map<String, Object>> expiredList) {
    String generatedDeleteSql = GENERATE_DELETE_BY_PRIMARY_KEY_SQL();
    log.debug("CronDelete [{}], execute generatedDeleteSql: [{}].", graveIfNecessary(tableName), generatedDeleteSql);
    try (PreparedStatement ps = connection.prepareStatement(generatedDeleteSql)) {
      for (Map<String, Object> expired : expiredList) {
        int i = 1;
        for (String primaryKeyColumn : primaryKeyColumnSet) {
          ps.setObject(i, expired.get(primaryKeyColumn));
          i++;
        }
        ps.addBatch();
      }
      return ps.executeBatch();
    }
  }

  @SneakyThrows
  private List<Map<String, Object>> queryExpired(Connection connection) {
    String generatedQuerySql = GENERATE_QUERY_SQL();
    log.debug("CronDelete [{}], execute generatedQuerySql: [{}].", graveIfNecessary(tableName), generatedQuerySql);
    try (PreparedStatement preparedStatement = connection.prepareStatement(generatedQuerySql)) {
      ResultSet resultSet = preparedStatement.executeQuery();
      List<Map<String, Object>> result = new ArrayList<>();
      while (resultSet.next()) {
        Map<String, Object> map = new HashMap<>();
        for (String column : primaryKeyColumnSet) {
          map.put(column, resultSet.getObject(column));
        }
        result.add(map);
      }
      return result;
    }
  }

  private String GENERATE_QUERY_SQL() {
    StringBuilder columns = new StringBuilder();
    for (String column : primaryKeyColumnSet) {
      columns.append(graveIfNecessary(column)).append(" ,");
    }
    columns = new StringBuilder(columns.substring(0, columns.length() - 1));
    String querySql = "select " + columns + " from " + graveIfNecessary(tableName) + " where " + condition;
    return querySql + " limit " + oneBatch;
  }

  @SneakyThrows
  private void deleteByCondition() {
    try (Connection connection = dataSource.getConnection()) {
      boolean originalAutoCommit = ConnectionUtils.setAutoCommitFalse(connection);
      try {
        try (Statement statement = connection.createStatement()) {
          String generatedDeleteSql = GENERATE_DELETE_BY_CONDITION_SQL();
          log.debug("CronDelete [{}], execute generatedDeleteSql: {}.", graveIfNecessary(tableName), generatedDeleteSql);
          int i = statement.executeUpdate(generatedDeleteSql);
          log.debug("CronDelete [{}]. 删除了[{}]条数据。", graveIfNecessary(tableName), i);
        }
        connection.commit();
      } finally {
        ConnectionUtils.setAutoCommitAndClose(connection, originalAutoCommit);
      }
    }
  }

  private String graveIfNecessary(String a) {
    if (graveColumn) {
      return "`" + a + "`";
    }
    return a;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    cronClean();
  }

  @Override
  public void clean() {
    this.delete();
  }

  @Override
  public void cronClean() {
    if (!init) {
      log.info("删除表[{}]的定时任务开始启动！", graveIfNecessary(tableName));
      init = true;
      start = true;
      threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
      threadPoolTaskScheduler.initialize();
      threadPoolTaskScheduler.schedule(() -> {
        try {
          if (!start) {
            return;
          }
          this.delete();
        } catch (Exception e) {
          log.error("删除表[{}]数据的过程中，出现了异常！", graveIfNecessary(tableName), e);
        }
      }, new CronTrigger(getCron()));
      log.info("删除表[{}]的定时任务启动成功！", graveIfNecessary(tableName));
    } else {
      log.info("删除表[{}]的定时任务已启动，本次不再启动！", graveIfNecessary(tableName));
    }
  }

  @Override
  public void close() {
    start = false;
    if (init && threadPoolTaskScheduler != null) {
      log.info("删除表[{}]的定时任务开始关闭！", graveIfNecessary(tableName));
      threadPoolTaskScheduler.destroy();
      log.info("删除表[{}]的定时任务关闭成功！", graveIfNecessary(tableName));
    }
  }

}
