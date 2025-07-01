package cn.addenda.component.spring.aroundlog;

import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author addenda
 * @since 2022/9/29 13:50
 */
public class AroundLogSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{
                AutoProxyRegistrar.class.getName(),
                AroundLogConfiguration.class.getName()};
    }

}
