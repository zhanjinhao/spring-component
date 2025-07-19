package cn.addenda.component.spring.aroundlog;

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
public class AroundLogSupport {

  private static final String NULL_STR = "_NIL";
  private static final String ERROR_STR = "_ERROR";

  private static final Map<String, AtomicLong> SEQ_GENERATOR_MAP = new ConcurrentHashMap<>();

  private static final AtomicLong GLOBAL_SEQ = new AtomicLong(0L);

  private static final ThreadLocal<Deque<AroundLogBo>> ARG_RES_DEQUE_TL = ThreadLocal.withInitial(ArrayDeque::new);

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
    long globalSeq = GLOBAL_SEQ.getAndIncrement();
    long callerSeq = SEQ_GENERATOR_MAP.computeIfAbsent(callerInfo, s -> new AtomicLong(0L)).getAndIncrement();

    AroundLogBo cur = new AroundLogBo();
    Deque<AroundLogBo> aroundLogBoDeque = ARG_RES_DEQUE_TL.get();
    if (aroundLogBoDeque.isEmpty()) {
      aroundLogBoDeque.push(cur);
    } else {
      AroundLogBo parent = aroundLogBoDeque.peek();
      parent.getChildren().add(cur);
      aroundLogBoDeque.push(cur);
    }

    cur.setGlobalSeq(globalSeq);
    cur.setCallerSeq(callerSeq);
    cur.setCaller(callerInfo);
    cur.setStartDt(LocalDateTime.now());
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
        cur.setEndDt(LocalDateTime.now());
        cur.setCost(DateUtils.localDateTimeToTimestamp(cur.getEndDt()) - DateUtils.localDateTimeToTimestamp(cur.getStartDt()));
      }
    } finally {
      AroundLogBo pop = aroundLogBoDeque.pop();
      if (aroundLogBoDeque.isEmpty()) {
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
  public static class AroundLogBo {

    private long globalSeq;
    private long callerSeq;
    private String caller;

    private LocalDateTime startDt;
    private LocalDateTime endDt;

    private Object argument;
    private Object result;

    @JsonIgnore
    private Throwable error;
    private Long cost;

    private List<AroundLogBo> children = new ArrayList<>();

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
      return collection.stream().map(AroundLogSupport::toStr).collect(Collectors.joining(",", "[", "]"));
    } else if (o.getClass().isArray()) {
      // A 是 B 的子类，则 A[] 是 B[] 的子类；
      // 所以 o 可以转换为 Object[]
      Object[] array = (Object[]) o;
      return Arrays.stream(array).map(AroundLogSupport::toStr).collect(Collectors.joining(",", "[", "]"));
    } else if (o instanceof Map.Entry) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
      return toStr(entry.getKey()) + "=" + toStr(entry.getValue());
    } else if (o instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) o;
      return "{" + map.entrySet().stream().map(AroundLogSupport::toStr).collect(Collectors.joining(",")) + "}";
    } else if (o instanceof Throwable) {
      Throwable throwable = (Throwable) o;
      StringWriter sw = new StringWriter();
      throwable.printStackTrace(new PrintWriter(sw));
      return sw.toString();
    }
    return o.toString();
  }

}
