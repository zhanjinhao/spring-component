package cn.addenda.component.spring.test.aroundlog.service;

import java.sql.SQLException;

/**
 * @author addenda
 * @since 2023/4/17 21:42
 */
public interface IAroundLogTestService {

  String completeNormally(String param);

  String completeBusinessExceptionally(String param);

  String completeCheckedExceptionally(String param) throws SQLException;

}
