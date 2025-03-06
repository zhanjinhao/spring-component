package cn.addenda.component.spring.test.argres.service;

import cn.addenda.component.base.exception.ServiceException;
import cn.addenda.component.spring.argres.ArgResLog;

import java.sql.SQLException;

/**
 * @author addenda
 * @since 2023/3/9 16:19
 */
public class ArgResLogTestAopService implements IArgResLogTestService {

  @Override
  @ArgResLog
  public String completeNormally(String param) {
    return param + " hengha";
  }

  @Override
  @ArgResLog
  public String completeBusinessExceptionally(String param) {
    throw new ServiceException(param + " hengha");
  }

  @Override
  @ArgResLog
  public String completeCheckedExceptionally(String param) throws SQLException {
    throw new SQLException(param + " hengha");
  }

}
