package cn.addenda.component.spring.multidatasource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
public class MultiDataSourceEntry {

  private DataSource master;

  private List<DataSource> slaves = new ArrayList<>();

  public MultiDataSourceEntry() {
  }

  public MultiDataSourceEntry(DataSource master, List<DataSource> slaves) {
    this.master = master;
    this.slaves = slaves;
  }

  public DataSource getMaster() {
    return master;
  }

  public void setMaster(DataSource master) {
    this.master = master;
  }

  public List<DataSource> getSlaves() {
    return slaves;
  }

  public void setSlaves(List<DataSource> slaves) {
    this.slaves = slaves;
  }

  @Override
  public String toString() {
    return "NamedDatasourceHolder{" +
            "master=" + master +
            ", slaves=" + Arrays.deepToString(slaves.toArray()) +
            '}';
  }
}
