package cn.addenda.component.spring.argres;

import cn.addenda.component.base.datetime.DateUtils;
import cn.addenda.component.base.exception.ExceptionUtils;
import cn.addenda.component.base.jackson.util.JacksonUtils;
import cn.addenda.component.base.lambda.TSupplier;
import cn.addenda.component.stacktrace.StackTraceUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @since 2022/9/29 13:51
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArgResLogSupport {

  private static final String NULL_STR = "_NIL";
  private static final String ERROR_STR = "_ERROR";

  private static final Map<String, AtomicLong> SEQUENCE_GENERATOR_MAP = new ConcurrentHashMap<>();

  private static final AtomicLong GLOBAL_SEQUENCE = new AtomicLong(0L);

  private static final ThreadLocal<Deque<ArgResBo>> ARG_RES_DEQUE_TL = ThreadLocal.withInitial(ArrayDeque::new);

  protected static <R> R invoke(String callerInfo, TSupplier<R> supplier, Object[] arguments) throws Throwable {
    if (callerInfo == null) {
      callerInfo = StackTraceUtils.getDetailedCallerInfo(true, false, false);
    }
    return doInvoke(callerInfo, supplier, arguments);
  }

  /**
   * @param supplier 这里必须要使用TSupplier，因为
   */
  private static <R> R doInvoke(String callerInfo, TSupplier<R> supplier, Object[] arguments) throws Throwable {
    long globalSequence = GLOBAL_SEQUENCE.getAndIncrement();
    long sequence = SEQUENCE_GENERATOR_MAP.computeIfAbsent(callerInfo, s -> new AtomicLong(0L)).getAndIncrement();

    ArgResBo cur = new ArgResBo();
    Deque<ArgResBo> argResBoDeque = ARG_RES_DEQUE_TL.get();
    if (argResBoDeque.isEmpty()) {
      argResBoDeque.push(cur);
    } else {
      ArgResBo parent = argResBoDeque.peek();
      parent.getChildren().add(cur);
      argResBoDeque.push(cur);
    }

    cur.setGlobalSequence(globalSequence);
    cur.setCallerSequence(sequence);
    cur.setCaller(callerInfo);
    cur.setStartDateTime(LocalDateTime.now());
    cur.setArgument(arguments == null || arguments.length == 0 ?
            "No arguments." : arguments);

    try {
      try {
        R result = supplier.get();
        cur.setResult(result);
        return result;
      } catch (Throwable throwable) {
        cur.setResult(ERROR_STR);
        cur.setError(ExceptionUtils.unwrapThrowable(throwable));
        throw throwable;
      } finally {
        cur.setEndDateTime(LocalDateTime.now());
        cur.setCost(DateUtils.localDateTimeToTimestamp(cur.getEndDateTime()) - DateUtils.localDateTimeToTimestamp(cur.getStartDateTime()));
      }
    } finally {
      ArgResBo pop = argResBoDeque.pop();
      if (argResBoDeque.isEmpty()) {
        if (pop.getError() != null) {
          log.error("{}", toJsonStr(pop), pop.getError());
        } else {
          log.info("{}", toJsonStr(pop));
        }
        ARG_RES_DEQUE_TL.remove();
      }
    }
  }

  @Setter
  @Getter
  @ToString
  @NoArgsConstructor
  public static class ArgResBo {

    private long globalSequence;
    private long callerSequence;
    private String caller;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private Object argument;
    private Object result;

    @JsonIgnore
    private Throwable error;
    private Long cost;

    private List<ArgResBo> children = new ArrayList<>();

  }

  private static String toJsonStr(Object o) {
    if (o == null) {
      return NULL_STR;
    }

    return JacksonUtils.toStr(o);
  }

  /**
   * 这种方法输出的字符串json短，但是不方便反序列化为对象。<p/>
   * 先保留着，后续看看有没有用武之地。
   */
  private static String toStr(Object o) {
    if (o == null) {
      return NULL_STR;
    } else if (o instanceof Collection) {
      Collection<?> collection = (Collection<?>) o;
      return collection.stream().map(ArgResLogSupport::toStr).collect(Collectors.joining(",", "[", "]"));
    } else if (o.getClass().isArray()) {
      // A 是 B 的子类，则 A[] 是 B[] 的子类；
      // 所以 o 可以转换为 Object[]
      Object[] array = (Object[]) o;
      return Arrays.stream(array).map(ArgResLogSupport::toStr).collect(Collectors.joining(",", "[", "]"));
    } else if (o instanceof Map.Entry) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      return toStr(entry.getKey()) + "=" + toStr(entry.getValue());
    } else if (o instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) o;
      return "{" + map.entrySet().stream().map(ArgResLogSupport::toStr).collect(Collectors.joining(",")) + "}";
    } else if (o instanceof Throwable) {
      Throwable throwable = (Throwable) o;
      StringWriter sw = new StringWriter();
      throwable.printStackTrace(new PrintWriter(sw));
      return sw.toString();
    }
    return o.toString();
  }

}
