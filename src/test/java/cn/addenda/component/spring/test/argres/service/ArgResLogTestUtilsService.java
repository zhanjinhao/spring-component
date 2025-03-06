package cn.addenda.component.spring.test.argres.service;

import cn.addenda.component.base.exception.ServiceException;
import cn.addenda.component.spring.argres.ArgResLogUtils;

import java.sql.SQLException;

/**
 * @author addenda
 * @since 2023/3/9 16:19
 */
public class ArgResLogTestUtilsService implements IArgResLogTestService {

  @Override
  public String completeNormally(String param) {
    return ArgResLogUtils.doLog(() -> {
      return param + " hengha";
    }, param);

  }

  @Override
  public String completeBusinessExceptionally(String param) {
    return ArgResLogUtils.doLog(() -> {
      throw new ServiceException(param + " hengha");
    }, param);
  }

  @Override
  public String completeCheckedExceptionally(String param) throws SQLException {
    return ArgResLogUtils.doLog(() -> {
      throw new SQLException(param + " hengha");
    }, param);
  }

}
