package cn.addenda.component.spring;

import cn.addenda.component.base.exception.SystemException;

/**
 * @author addenda
 * @since 2022/8/7 12:06
 */
public class SpringException extends SystemException {

  public SpringException() {
    super();
  }

  public SpringException(String message) {
    super(message);
  }

  public SpringException(String message, Throwable cause) {
    super(message, cause);
  }

  public SpringException(Throwable cause) {
    super(cause);
  }

  public SpringException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public String moduleName() {
    return "spring";
  }

  @Override
  public String componentName() {
    return "spring";
  }
}
