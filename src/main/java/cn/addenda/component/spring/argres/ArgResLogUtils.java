package cn.addenda.component.spring.argres;

import cn.addenda.component.base.exception.ExceptionUtils;
import cn.addenda.component.base.lambda.TRunnable;
import cn.addenda.component.base.lambda.TSupplier;

/**
 * @author addenda
 * @since 2023/3/9 11:17
 */
public class ArgResLogUtils extends ArgResLogSupport {

  public static void doLog(TRunnable runnable, Object... arguments) {
    doLog(null, runnable, arguments);
  }

  public static void doLog(String callerInfo, TRunnable runnable, Object... arguments) {
    doLog(callerInfo,
            () -> {
              runnable.run();
              return null;
            }, arguments);
  }

  public static <R> R doLog(TSupplier<R> supplier, Object... arguments) {
    return doLog(null, supplier, arguments);
  }

  public static <R> R doLog(String callerInfo, TSupplier<R> supplier, Object... arguments) {
    try {
      return invoke(arguments, supplier, callerInfo);
    }
    // invoke在两种情况下会发生异常：
    // 第一种情况：executor内部的异常，此时原始异常会被包装成ArgResLogException。
    //           当原始异常为RuntimeException时，将原始异常抛出
    // 第二种情况：ArgRes功能本身出现的异常，这类异常直接扔出去。
    catch (Throwable throwable) {
      throw ExceptionUtils.wrapAsRuntimeException(throwable, ArgResLogException.class);
    }
  }
}
