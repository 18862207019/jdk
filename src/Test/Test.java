package Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author hupd
 */
public class Test {
  public static void main(String[] args) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    for (int i =0;i<10;i++) {
      executorService.execute(new Runnable() {
        @Override public void run() {
          try {
            Thread.sleep(1111111111);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          System.out.println(11111111);
        }
      });
    }
  }
}
