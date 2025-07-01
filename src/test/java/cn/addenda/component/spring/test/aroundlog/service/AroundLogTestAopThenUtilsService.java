package cn.addenda.component.spring.test.aroundlog.service;

import cn.addenda.component.base.exception.ServiceException;
import cn.addenda.component.spring.aroundlog.AroundLog;
import cn.addenda.component.spring.aroundlog.AroundLogUtils;

import java.sql.SQLException;

/**
 * @author addenda
 * @since 2023/3/9 16:19
 */
public class AroundLogTestAopThenUtilsService implements IAroundLogTestService {

  @Override
  @AroundLog
  public String completeNormally(String param) {
    AroundLogUtils.doLog(() -> param + " hengha1", param);
    return AroundLogUtils.doLog(() -> param + " hengha2", param) + " ! ";
  }

  @Override
  @AroundLog
  public String completeBusinessExceptionally(String param) {
    AroundLogUtils.doLog(() -> {
      return param + " hengha3";
    }, param);
    return AroundLogUtils.doLog(() -> {
      throw new ServiceException(param + " hengha4");
    }, param) + " ! ";
  }

  @Override
  @AroundLog
  public String completeCheckedExceptionally(String param) throws SQLException {
    return AroundLogUtils.doLog(() -> {
      throw new SQLException(param + " hengha3");
    }, param) + " ! ";
  }

}
