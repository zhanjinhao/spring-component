package cn.addenda.component.spring.test.aroundlog;

import cn.addenda.component.spring.aroundlog.AroundLogUtils;
import cn.addenda.component.spring.test.aroundlog.service.AroundLogTestAopService;
import cn.addenda.component.spring.test.aroundlog.service.AroundLogTestAopThenUtilsService;
import cn.addenda.component.spring.test.aroundlog.service.AroundLogTestUtilsService;
import cn.addenda.component.spring.test.aroundlog.service.IAroundLogTestService;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.SQLException;

/**
 * @author addenda
 * @since 2023/3/9 14:45
 */
@Slf4j
public class AroundLogTest extends AbstractAroundLogTest {

  AnnotationConfigApplicationContext context;

  IAroundLogTestService aroundLogTestAopService;
  IAroundLogTestService aroundLogTestAopThenUtilsService;
  IAroundLogTestService aroundLogTestUtilsService = new AroundLogTestUtilsService();

  @Before
  public void before() {
    context = new AnnotationConfigApplicationContext();
    context.register(AroundLogTestConfiguration.class);
    context.register(AroundLogTestAopThenUtilsService.class);
    context.register(AroundLogTestAopService.class);
    context.refresh();

    aroundLogTestAopThenUtilsService = context.getBean("aroundLogTestAopThenUtilsService", IAroundLogTestService.class);
    aroundLogTestAopService = context.getBean("aroundLogTestAopService", IAroundLogTestService.class);
  }

  @After
  public void after() {
    context.close();
  }

  @Test
  public void testAop() {
    System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\workspace\\2022");

    Object o0 = eatThrowable(() -> aroundLogTestAopService.completeNormally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o2 = eatThrowable(() -> aroundLogTestAopService.completeBusinessExceptionally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o3 = eatThrowable(() -> {
      try {
        return aroundLogTestAopService.completeCheckedExceptionally("AA");
      } catch (SQLException e) {
        return null;
      }
    });
  }

  @Test
  public void testUtils() {
    System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\workspace\\2022");

    Object o0 = eatThrowable(() -> aroundLogTestUtilsService.completeNormally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o2 = eatThrowable(() -> aroundLogTestUtilsService.completeBusinessExceptionally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o3 = eatThrowable(() -> {
      try {
        return aroundLogTestUtilsService.completeCheckedExceptionally("AA");
      } catch (SQLException e) {
        return null;
      }
    });
  }

  @Test
  public void testAopThenUtils() {
    Object o1 = eatThrowable(() -> {
      return aroundLogTestAopThenUtilsService.completeNormally("AA");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o2 = eatThrowable(() -> {
      return aroundLogTestAopThenUtilsService.completeBusinessExceptionally("AA");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o3 = eatThrowable(() -> {
      try {
        return aroundLogTestAopThenUtilsService.completeCheckedExceptionally("AA");
      } catch (SQLException e) {
        return null;
      }
    });
  }

  @Test
  public void testUtilsThenAop() {
    Object o1 = eatThrowable(() -> {

      return AroundLogUtils.doLog(() -> {
        aroundLogTestAopService.completeNormally("AA1");
        return aroundLogTestAopService.completeNormally("AA2");
      }, "AA3");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");

    Object o2 = eatThrowable(() -> {
      return AroundLogUtils.doLog(() -> {
        aroundLogTestAopService.completeBusinessExceptionally("AA4");
        return aroundLogTestAopService.completeBusinessExceptionally("AA5");
      }, "AA6");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");

    Object o3 = eatThrowable(() -> {
      return AroundLogUtils.doLog(() -> {
        aroundLogTestAopService.completeCheckedExceptionally("AA7");
        return aroundLogTestAopService.completeCheckedExceptionally("AA8");
      }, "AA9");
    });
  }

}
