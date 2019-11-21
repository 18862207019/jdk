package Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 简单的测试  线程带返回值的创建方式
 * @author hupd
 */
public class CallAbleFutureTest {
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    ExecutorService executorService = Executors.newCachedThreadPool();
    Future<String> submit = executorService.submit(() -> {
      Thread.sleep(1000);
      return "casaca";
    });
    String s = submit.get();
    System.out.println(s);

  }
}
