package cn.addenda.component.spring.cron;

import cn.addenda.component.base.datetime.DateUtils;
import lombok.Getter;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author addenda
 * @since 2024/3/29 10:34
 */
public abstract class CronClean {

  @Getter
  private final String cron;

  protected CronClean(String cron) {
    this.cron = cron;
    Assert.notNull(cron, "`cron` can not be null!");
    Assert.isTrue(CronExpression.isValidExpression(cron), String.format("`cron`[%s] is not valid!", cron));
  }

  public abstract void clean();

  public abstract void cronClean();

  public abstract void close();

  protected String cronDescription() {
    LocalDateTime now = LocalDateTime.now();
    CronExpression cronExpression = CronExpression.parse(cron);
    LocalDateTime next1 = cronExpression.next(now);
    LocalDateTime next2 = Optional.ofNullable(next1).map(cronExpression::next).orElse(null);
    LocalDateTime next3 = Optional.ofNullable(next2).map(cronExpression::next).orElse(null);
    return String.format("Cron表达式是[%s]。当前时间是[%s]。前三次执行时间是[%s]、[%s]、[%s]。",
            cron, DateUtils.format(now, DateUtils.yMdHmsS_FORMATTER),
            Optional.ofNullable(next1).map(s -> DateUtils.format(s, DateUtils.yMdHmsS_FORMATTER)).orElse("不执行"),
            Optional.ofNullable(next2).map(s -> DateUtils.format(s, DateUtils.yMdHmsS_FORMATTER)).orElse("不执行"),
            Optional.ofNullable(next3).map(s -> DateUtils.format(s, DateUtils.yMdHmsS_FORMATTER)).orElse("不执行"));
  }

  protected String removeGrave(String str) {
    if (str == null) {
      throw new NullPointerException("fieldName or tableName can not be null!");
    }
    str = str.trim();
    if ("`".equals(str)) {
      return str;
    }
    int start = 0;
    int end = str.length();
    if (str.startsWith("`")) {
      start = start + 1;
    }
    if (str.endsWith("`")) {
      end = end - 1;
    }

    if (start != 0 || end != str.length()) {
      return str.substring(start, end);
    }
    return str;
  }

}
