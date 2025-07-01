package cn.addenda.component.spring.test.aroundlog.service;

import cn.addenda.component.base.exception.ServiceException;
import cn.addenda.component.spring.aroundlog.AroundLog;

import java.sql.SQLException;

/**
 * @author addenda
 * @since 2023/3/9 16:19
 */
public class AroundLogTestAopService implements IAroundLogTestService {

  @Override
  @AroundLog
  public String completeNormally(String param) {
    return param + " hengha";
  }

  @Override
  @AroundLog
  public String completeBusinessExceptionally(String param) {
    throw new ServiceException(param + " hengha");
  }

  @Override
  @AroundLog
  public String completeCheckedExceptionally(String param) throws SQLException {
    throw new SQLException(param + " hengha");
  }

}
