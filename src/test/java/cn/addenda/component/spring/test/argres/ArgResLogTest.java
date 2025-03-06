package cn.addenda.component.spring.test.argres;

import cn.addenda.component.spring.argres.ArgResLogUtils;
import cn.addenda.component.spring.test.argres.service.ArgResLogTestAopService;
import cn.addenda.component.spring.test.argres.service.ArgResLogTestAopThenUtilsService;
import cn.addenda.component.spring.test.argres.service.ArgResLogTestUtilsService;
import cn.addenda.component.spring.test.argres.service.IArgResLogTestService;
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
public class ArgResLogTest extends AbstractArgResLogTest {

  AnnotationConfigApplicationContext context;

  IArgResLogTestService argResLogTestAopService;
  IArgResLogTestService argResLogTestAopThenUtilsService;
  IArgResLogTestService argResLogTestUtilsService = new ArgResLogTestUtilsService();

  @Before
  public void before() {
    context = new AnnotationConfigApplicationContext();
    context.register(ArgResLogTestConfiguration.class);
    context.register(ArgResLogTestAopThenUtilsService.class);
    context.register(ArgResLogTestAopService.class);
    context.refresh();

    argResLogTestAopThenUtilsService = context.getBean("argResLogTestAopThenUtilsService", IArgResLogTestService.class);
    argResLogTestAopService = context.getBean("argResLogTestAopService", IArgResLogTestService.class);
  }

  @After
  public void after() {
    context.close();
  }

  @Test
  public void testAop() {
    System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\workspace\\2022");

    Object o0 = eatThrowable(() -> argResLogTestAopService.completeNormally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o2 = eatThrowable(() -> argResLogTestAopService.completeBusinessExceptionally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o3 = eatThrowable(() -> {
      try {
        return argResLogTestAopService.completeCheckedExceptionally("AA");
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
    });
  }

  @Test
  public void testUtils() {
    System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\workspace\\2022");

    Object o0 = eatThrowable(() -> argResLogTestUtilsService.completeNormally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o2 = eatThrowable(() -> argResLogTestUtilsService.completeBusinessExceptionally("AA"));
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o3 = eatThrowable(() -> {
      try {
        return argResLogTestUtilsService.completeCheckedExceptionally("AA");
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
    });
  }

  @Test
  public void testAopThenUtils() {
    Object o1 = eatThrowable(() -> {
      return argResLogTestAopThenUtilsService.completeNormally("AA");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o2 = eatThrowable(() -> {
      return argResLogTestAopThenUtilsService.completeBusinessExceptionally("AA");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");
    Object o3 = eatThrowable(() -> {
      try {
        return argResLogTestAopThenUtilsService.completeCheckedExceptionally("AA");
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
    });
  }

  @Test
  public void testUtilsThenAop() {
    Object o1 = eatThrowable(() -> {
      return ArgResLogUtils.doLog(() -> {
        return argResLogTestAopService.completeNormally("AA");
      }, "AA");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");

    Object o2 = eatThrowable(() -> {
      return ArgResLogUtils.doLog(() -> {
        return argResLogTestAopService.completeBusinessExceptionally("AA");
      }, "AA");
    });
    System.out.print("\n------------------------------------------------------------------------------------------\n");

    Object o3 = eatThrowable(() -> {
      return ArgResLogUtils.doLog(() -> {
        return argResLogTestAopService.completeCheckedExceptionally("AA");
      }, "AA");
    });
  }

}
