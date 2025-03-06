package cn.addenda.component.spring.test.argres;

import java.util.function.Supplier;

/**
 * @author addenda
 * @since 2023/3/9 16:23
 */
public class AbstractArgResLogTest {

  protected <R> R eatThrowable(Supplier<R> supplier) {
    try {
      return supplier.get();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
    return null;
  }

}
