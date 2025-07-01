package cn.addenda.component.spring.aroundlog;

import java.lang.annotation.*;

/**
 * @author addenda
 * @since 2022/9/29 14:00
 */
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AroundLog {

}
