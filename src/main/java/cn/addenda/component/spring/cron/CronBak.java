package cn.addenda.component.spring.cron;

import cn.addenda.component.base.collection.ArrayUtils;
import cn.addenda.component.base.sql.ConnectionUtils;
import cn.addenda.component.base.string.Slf4jUtils;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2024/3/27 19:43
 */
@Slf4j
public class CronBak extends CronClean implements InitializingBean {

  private final DataSource dataSource;

  private final int oneBatch;

  private final String tableName;

  private final String condition;

  private final Set<String> columnSet;

  private final Set<String> primaryKeyColumnSet;

  private final String bakTableName;

  private final boolean graveColumn;

  private volatile boolean init = false;
  private volatile boolean start = false;

  private ThreadPoolTaskScheduler threadPoolTaskScheduler;

  public CronBak(String cron, DataSource dataSource, int oneBatch, boolean graveColumn,
                 String tableName, String condition, Set<String> columnSet, String primaryKeyColumn,
                 String bakTableName) {
    this(cron, dataSource, oneBatch, graveColumn,
            tableName, condition, columnSet, ArrayUtils.asHashSet(primaryKeyColumn),
            bakTableName);
  }

  public CronBak(String cron, DataSource dataSource, int oneBatch, boolean graveColumn,
                 String tableName, String condition, Set<String> columnSet, Set<String> primaryKeyColumnSet,
                 String bakTableName) {
    super(cron);
    Assert.notNull(dataSource, "`dataSource` can not be null!");
    Assert.isTrue(oneBatch > 0, "`oneBatch` must greater than 0!");
    Assert.notNull(tableName, "`tableName` can not be null!");
    Assert.notNull(condition, "`condition` can not be null!");
    Assert.isTrue(!CollectionUtils.isEmpty(columnSet), "`columnSet` can not be empty!");
    Assert.isTrue(!CollectionUtils.isEmpty(primaryKeyColumnSet), "`primaryKeyColumnSet` can not be empty!");
    Assert.notNull(bakTableName, "`bakTableName` can not be null!");

    this.dataSource = dataSource;
    this.oneBatch = oneBatch;
    this.graveColumn = graveColumn;
    this.tableName = tableName;
    this.condition = condition;
    this.columnSet = columnSet.stream()
            .map(this::removeGrave)
            .collect(Collectors.toSet());
    this.primaryKeyColumnSet = primaryKeyColumnSet.stream()
            .map(this::removeGrave)
            .collect(Collectors.toSet());
    this.bakTableName = bakTableName;
    init();
  }

  private void init() {
    validColumn();
    log.info("tableName: {}", graveIfNecessary(tableName));
    log.info("columnSet: {}", String.join(", ", columnSet.stream().map(this::graveIfNecessary).collect(Collectors.toSet())));
    log.info("primaryKeyColumnSet: {}", String.join(", ", primaryKeyColumnSet.stream().map(this::graveIfNecessary).collect(Collectors.toSet())));
    log.info("cron description: {}", cronDescription());
    log.info("GENERATE_QUERY_SQL: {}", GENERATE_QUERY_SQL());
    log.info("GENERATE_DELETE_SQL: {}", GENERATE_DELETE_SQL());
    log.info("GENERATE_SAVE_HIS_SQL: {}", GENERATE_SAVE_HIS_SQL());
  }

  private void validColumn() {
    for (String primaryKeyColumn : primaryKeyColumnSet) {
      Assert.notNull(primaryKeyColumn, "primaryKeyColumnSet 中不能存在null！");
    }
    for (String column : columnSet) {
      Assert.notNull(column, "columnSet 中不能存在null！");
    }
    for (String primaryKeyColumn : primaryKeyColumnSet) {
      if (!columnSet.contains(primaryKeyColumn)) {
        throw new IllegalArgumentException(Slf4jUtils.format("primaryKeyColumnSet中有但columnSet中没有{}.", primaryKeyColumn));
      }
    }
  }

  private String graveIfNecessary(String a) {
    if (graveColumn) {
      return "`" + a + "`";
    }
    return a;
  }

  private String GENERATE_DELETE_SQL() {
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

  private String GENERATE_SAVE_HIS_SQL() {
    StringBuilder sql = new StringBuilder();
    sql.append("insert into ").append(graveIfNecessary(bakTableName));
    sql.append("(");
    for (String column : columnSet) {
      sql.append(graveIfNecessary(column)).append(",");
    }
    sql = new StringBuilder(sql.substring(0, sql.length() - 1));
    sql.append(") values (");
    for (int i = 0; i < columnSet.size(); i++) {
      sql.append(" ?,");
    }
    sql = new StringBuilder(sql.substring(0, sql.length() - 1));
    sql.append(")");
    return sql.toString();
  }

  private String GENERATE_QUERY_SQL() {
    StringBuilder columns = new StringBuilder();
    for (String column : columnSet) {
      columns.append(graveIfNecessary(column)).append(" ,");
    }
    columns = new StringBuilder(columns.substring(0, columns.length() - 1));
    String querySql = "select " + columns + " from " + graveIfNecessary(tableName) + " where " + condition;
    return querySql + " limit " + oneBatch + " offset 0";
  }

  @SneakyThrows
  private void bak() {
    try (Connection connection = dataSource.getConnection()) {
      boolean originalAutoCommit = ConnectionUtils.setAutoCommitFalse(connection);
      try {
        List<Map<String, Object>> expiredList = queryExpired(connection);
        if (!expiredList.isEmpty()) {
          int[] deleteResults = deleteByPrimaryKey(connection, expiredList);
          saveHis(connection, expiredList, deleteResults);
        }
        connection.commit();
      } finally {
        ConnectionUtils.setAutoCommitAndClose(connection, originalAutoCommit);
      }
    }
  }

  @SneakyThrows
  private List<Map<String, Object>> queryExpired(Connection connection) {
    String generatedQuerySql = GENERATE_QUERY_SQL();
    log.debug("CronBak [{}], execute generatedQuerySql: [{}].", graveIfNecessary(tableName), generatedQuerySql);
    try (PreparedStatement preparedStatement = connection.prepareStatement(generatedQuerySql)) {
      ResultSet resultSet = preparedStatement.executeQuery();
      List<Map<String, Object>> result = new ArrayList<>();
      while (resultSet.next()) {
        Map<String, Object> map = new HashMap<>();
        for (String column : columnSet) {
          map.put(column, resultSet.getObject(column));
        }
        result.add(map);
      }
      return result;
    }
  }

  @SneakyThrows
  private int[] deleteByPrimaryKey(Connection connection, List<Map<String, Object>> expiredList) {
    String generatedDeleteSql = GENERATE_DELETE_SQL();
    log.debug("CronBak [{}], execute generatedDeleteSql: [{}].", graveIfNecessary(tableName), generatedDeleteSql);
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
  private void saveHis(Connection connection, List<Map<String, Object>> expiredList, int[] deleteResults) {
    String generatedSaveHisSql = GENERATE_SAVE_HIS_SQL();
    log.debug("CronBak [{}], execute generatedSaveHisSql: [{}].", graveIfNecessary(tableName), generatedSaveHisSql);
    try (PreparedStatement ps = connection.prepareStatement(generatedSaveHisSql)) {
      List<String> deleteKey = new ArrayList<>();
      for (int i = 0; i < expiredList.size(); i++) {
        int deleteResult = deleteResults[i];
        if (deleteResult == 1) {
          Map<String, Object> expired = expiredList.get(i);
          deleteKey.add(generateKey(expired));
          int j = 1;
          for (String column : columnSet) {
            ps.setObject(j, expired.get(column));
            j++;
          }
          ps.addBatch();
        } else if (deleteResult != 0) {
          // 正常情况下是不会进入这里的
          log.error("删除操作的结果异常，当前索引[{}]的结果是[{}]，主键集合[{}]，结果集合[{}]。", i, deleteResult,
                  expiredList.stream().map(this::generateKey).collect(Collectors.joining(",")),
                  Arrays.stream(deleteResults).mapToObj(String::valueOf).collect(Collectors.joining(",")));
        }
      }
      if (!CollectionUtils.isEmpty(deleteKey)) {
        ps.executeBatch();
        log.debug("CronBak [{}], 主键为[{}]的数据已备份。", graveIfNecessary(tableName), String.join(",", deleteKey));
      }
    }
  }

  private String generateKey(Map<String, Object> expired) {
    return primaryKeyColumnSet.stream()
            .map(expired::get)
            .map(String::valueOf)
            .collect(Collectors.joining(","));
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    // 作为Spring Bean存在时，自动开启cronClean()
    cronClean();
  }

  @Override
  public void clean() {
    this.bak();
  }

  @Override
  public synchronized void cronClean() {
    if (!init) {
      log.info("备份表[{}]至[{}]的定时任务开始启动！", graveIfNecessary(tableName), graveIfNecessary(bakTableName));
      init = true;
      start = true;
      threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
      threadPoolTaskScheduler.initialize();
      threadPoolTaskScheduler.schedule(() -> {
        try {
          if (!start) {
            return;
          }
          this.bak();
        } catch (Exception e) {
          log.error("备份表[{}]至[{}]的过程中，出现了异常！", graveIfNecessary(tableName), graveIfNecessary(bakTableName), e);
        }
      }, new CronTrigger(getCron()));
      log.info("备份表[{}]至[{}]的定时任务启动成功！", graveIfNecessary(tableName), graveIfNecessary(bakTableName));
    } else {
      log.error("备份表[{}]至[{}]的定时任务已启动，本次不再启动！", graveIfNecessary(tableName), graveIfNecessary(bakTableName));
    }
  }

  @Override
  public void close() {
    start = false;
    if (init && threadPoolTaskScheduler != null) {
      log.info("备份表[{}]至[{}]的定时任务开始关闭！", graveIfNecessary(tableName), graveIfNecessary(bakTableName));
      threadPoolTaskScheduler.destroy();
      log.info("备份表[{}]至[{}]的定时任务关闭成功！", graveIfNecessary(tableName), graveIfNecessary(bakTableName));
    }
  }

}
