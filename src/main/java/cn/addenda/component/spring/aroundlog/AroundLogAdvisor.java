package cn.addenda.component.spring.aroundlog;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * @author addenda
 * @since 2022/9/29 13:52
 */
public class AroundLogAdvisor extends AbstractBeanFactoryPointcutAdvisor {

  @Override
  public Pointcut getPointcut() {
    return new AroundLogPointcut();
  }

  public static class AroundLogPointcut extends StaticMethodMatcherPointcut {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      AroundLog annotation = AnnotationUtils.findAnnotation(targetClass, AroundLog.class);
      if (annotation == null) {
        Method actualMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        annotation = AnnotationUtils.findAnnotation(actualMethod, AroundLog.class);
      }

      return annotation != null;
    }
  }


}
