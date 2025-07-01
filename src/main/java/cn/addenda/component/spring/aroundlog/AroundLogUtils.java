package cn.addenda.component.spring.aroundlog;

import cn.addenda.component.base.exception.ExceptionUtils;
import cn.addenda.component.base.lambda.TRunnable;
import cn.addenda.component.base.lambda.TSupplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author addenda
 * @since 2023/3/9 11:17
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AroundLogUtils extends AroundLogSupport {

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
      return invoke(callerInfo, supplier, arguments);
    } catch (Throwable throwable) {
      throw ExceptionUtils.wrapAsRuntimeException(throwable, AroundLogException.class);
    }
  }
}
