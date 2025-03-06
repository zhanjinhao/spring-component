package cn.addenda.component.spring.multidatasource;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.util.List;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
public class RandomSlaveDataSourceSelector implements SlaveDataSourceSelector {
  private final SecureRandom random = new SecureRandom();

  @Override
  public DataSource select(String key, List<DataSource> dataSourceList) {
    return dataSourceList.get((int) Math.abs((long) random.nextInt() % dataSourceList.size()));
  }

}
