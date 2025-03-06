package cn.addenda.component.spring.multidatasource;

import java.lang.annotation.*;

/**
 * @author addenda
 * @since 2022/3/4 19:39
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiDataSourceKey {

  String dataSourceName() default MultiDataSourceConstant.DEFAULT;

  String mode() default MultiDataSourceConstant.MASTER;

}
