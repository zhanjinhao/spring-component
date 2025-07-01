package cn.addenda.component.spring.aroundlog;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author addenda
 * @since 2022/9/29 13:51
 */
public class AroundLogConfiguration implements ImportAware {

  protected AnnotationAttributes annotationAttributes;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.annotationAttributes = AnnotationAttributes.fromMap(
            importMetadata.getAnnotationAttributes(EnableAroundLog.class.getName(), false));
    if (this.annotationAttributes == null) {
      throw new IllegalArgumentException(
              EnableAroundLog.class.getName() + " is not present on importing class " + importMetadata.getClassName());
    }
  }

  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public AroundLogAdvisor aroundLogAdvisor() {
    AroundLogAdvisor aroundLogAdvisor = new AroundLogAdvisor();
    aroundLogAdvisor.setAdvice(new AroundLogMethodInterceptor());
    if (this.annotationAttributes != null) {
      aroundLogAdvisor.setOrder(annotationAttributes.<Integer>getNumber("order"));
    }
    return aroundLogAdvisor;
  }

}
