package cn.addenda.component.spring.multidatasource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
public class MultiDataSourceMethodInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass());
    MultiDataSourceKey multiDataSourceKey = AnnotationUtils.findAnnotation(method, MultiDataSourceKey.class);
    if (multiDataSourceKey == null) {
      return invocation.proceed();
    }
    try {
      DataSourceContext.setActiveDataSourceKey(multiDataSourceKey.dataSourceName(), multiDataSourceKey.mode());
      return invocation.proceed();
    } finally {
      DataSourceContext.clear();
    }
  }

}
