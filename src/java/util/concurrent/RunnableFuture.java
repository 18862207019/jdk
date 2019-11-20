

package java.util.concurrent;

/**
 * RunnableFuture 继承Future、Runnable两个接口，为两者的合体，
 * 即所谓的Runnable的Future。提供了一个run()方法可以完成Future并允许访问其结果。
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {
    //在未被取消的情况下，将此 Future 设置为计算的结果
    void run();
}
