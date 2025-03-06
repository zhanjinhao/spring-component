package cn.addenda.component.spring.multidatasource;

import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MultiDataSourceSelector.class)
public @interface EnableMultiDataSource {

  int order() default Ordered.LOWEST_PRECEDENCE;

}
