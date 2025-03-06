package cn.addenda.component.spring.multidatasource;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
public interface SlaveDataSourceSelector {

  DataSource select(String key, List<DataSource> dataSourceList);

}
