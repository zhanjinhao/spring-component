package cn.addenda.component.spring.aroundlog;

import cn.addenda.component.spring.SpringException;

/**
 * @author addenda
 * @since 2023/3/9 11:21
 */
public class AroundLogException extends SpringException {

  public AroundLogException() {
  }

  public AroundLogException(String message) {
    super(message);
  }

  public AroundLogException(String message, Throwable cause) {
    super(message, cause);
  }

  public AroundLogException(Throwable cause) {
    super(cause);
  }

  public AroundLogException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  @Override
  public String moduleName() {
    return "spring";
  }

  @Override
  public String componentName() {
    return "aroundlog";
  }
}
