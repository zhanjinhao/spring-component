package cn.addenda.component.spring.test.aroundlog.service;

import cn.addenda.component.base.exception.ServiceException;
import cn.addenda.component.spring.aroundlog.AroundLogUtils;

import java.sql.SQLException;

/**
 * @author addenda
 * @since 2023/3/9 16:19
 */
public class AroundLogTestUtilsService implements IAroundLogTestService {

  @Override
  public String completeNormally(String param) {
    return AroundLogUtils.doLog(() -> {
      return param + " hengha";
    }, param);

  }

  @Override
  public String completeBusinessExceptionally(String param) {
    return AroundLogUtils.doLog(() -> {
      throw new ServiceException(param + " hengha");
    }, param);
  }

  @Override
  public String completeCheckedExceptionally(String param) throws SQLException {
    return AroundLogUtils.doLog(() -> {
      throw new SQLException(param + " hengha");
    }, param);
  }

}
