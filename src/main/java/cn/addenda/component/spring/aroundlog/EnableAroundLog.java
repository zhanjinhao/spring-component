package cn.addenda.component.spring.aroundlog;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 *
 * todo 日志中心抽象出来，支持存库
 *
 * @author addenda
 * @since 2022/9/29 13:55
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AroundLogSelector.class)
public @interface EnableAroundLog {

  int order() default Ordered.LOWEST_PRECEDENCE;

  boolean proxyTargetClass() default false;

  AdviceMode mode() default AdviceMode.PROXY;
}
