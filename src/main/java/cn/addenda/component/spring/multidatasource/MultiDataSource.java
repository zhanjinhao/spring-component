package cn.addenda.component.spring.multidatasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
public class MultiDataSource extends AbstractRoutingDataSource {

  private Map<String, MultiDataSourceEntry> datasourceHolderMap = new HashMap<>();

  private SlaveDataSourceSelector slaveDataSourceSelector = new RandomSlaveDataSourceSelector();

  @Override
  protected Object determineCurrentLookupKey() {
    throw new MultiDataSourceException("MultiDataSource#determineCurrentLookupKey是不受支持的操作！", new UnsupportedOperationException("使用多数据源时不应该执行此方法！"));
  }

  @Override
  protected DataSource determineTargetDataSource() {
    DataSource curActiveDataSource = DataSourceContext.getActiveDataSource();
    // 存在正在活跃的数据源的时候
    if (curActiveDataSource != null) {
      return curActiveDataSource;
    }
    String key = null;
    try {
      key = DataSourceContext.getActiveDataSourceKey();
      // 但key为空的时候，即没有注解的时候，走默认的MASTER库
      if (key == null || key.length() == 0) {
        return datasourceHolderMap.get(MultiDataSourceConstant.DEFAULT).getMaster();
      }
      String[] split = key.split("\\.");
      MultiDataSourceEntry multiDataSourceEntry = datasourceHolderMap.get(split[0]);
      if (MultiDataSourceConstant.MASTER.equals(split[1])) {
        DataSource dataSource = multiDataSourceEntry.getMaster();
        DataSourceContext.setActiveDataSource(dataSource);
        return dataSource;
      } else if (MultiDataSourceConstant.SLAVE.equals(split[1])) {
        DataSource dataSource = slaveDataSourceSelector.select(key, multiDataSourceEntry.getSlaves());
        DataSourceContext.setActiveDataSource(dataSource);
        return dataSource;
      } else {
        throw new MultiDataSourceException("无法识别的多数据源模式，只能选择 MASTER 或者 SLAVE，当前：" + key);
      }
    } catch (Exception e) {
      if (MultiDataSourceException.class.equals(e.getClass())) {
        throw e;
      } else {
        throw new MultiDataSourceException("从配置的多数据源中获取数据源失败！当前key：" + key, e);
      }
    }
  }

  public void setSlaveDataSourceSelector(SlaveDataSourceSelector slaveDataSourceSelector) {
    this.slaveDataSourceSelector = slaveDataSourceSelector;
  }

  public void setDatasourceHolderMap(Map<String, MultiDataSourceEntry> datasourceHolderMap) {
    this.datasourceHolderMap = datasourceHolderMap;
  }

  public void addMultiDataSourceEntry(String key, MultiDataSourceEntry multiDataSourceEntry) {
    datasourceHolderMap.put(key, multiDataSourceEntry);
  }

  @Override
  public void afterPropertiesSet() {
    datasourceHolderMap.forEach((key, multiDataSourceEntry) -> {
      List<DataSource> slaves = multiDataSourceEntry.getSlaves();
      if (slaves == null || slaves.isEmpty()) {
        List<DataSource> slavesThatCopyFromMaster = new ArrayList<>();
        slavesThatCopyFromMaster.add(multiDataSourceEntry.getMaster());
        multiDataSourceEntry.setSlaves(slavesThatCopyFromMaster);
      }
    });
  }

}
