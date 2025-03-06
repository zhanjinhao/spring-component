package cn.addenda.component.spring.multidatasource;

import cn.addenda.component.spring.SpringException;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
public class MultiDataSourceException extends SpringException {

  public MultiDataSourceException() {
  }

  public MultiDataSourceException(String message) {
    super(message);
  }

  public MultiDataSourceException(String message, Throwable cause) {
    super(message, cause);
  }

  public MultiDataSourceException(Throwable cause) {
    super(cause);
  }

  public MultiDataSourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


  @Override
  public String moduleName() {
    return "spring";
  }

  @Override
  public String componentName() {
    return "multiDataSource";
  }

}
