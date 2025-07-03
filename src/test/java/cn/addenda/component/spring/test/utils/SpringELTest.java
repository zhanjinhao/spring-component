package cn.addenda.component.spring.test.utils;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;

public class SpringELTest {

  @Test
  public void test1() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression("#list[0]+#list[1]");
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("name", "addenda");
    context.setVariable("age", 1);
    List<Object> objectList = new ArrayList<>();
    objectList.add("addenda");
    objectList.add(1);
    context.setVariable("list", objectList);
    System.out.println(exp.getValue(context));
    Assert.assertEquals("addenda1", exp.getValue(context));
  }

  @Test
  public void test2() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression("#list");
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariable("name", "addenda");
    context.setVariable("age", 1);
    List<Object> objectList = new ArrayList<>();
    objectList.add("addenda");
    objectList.add(1);
    context.setVariable("list", objectList);
    System.out.println(exp.getValue(context));
    Assert.assertEquals("[addenda, 1]", exp.getValue(context).toString());

  }


}