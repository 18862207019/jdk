
package java.util.concurrent;
import java.util.List;
import java.util.Collection;

/**
 * ExecutorService接口继承了Executor，在其上做了一些shutdown()、submit()的扩展，可以说是真正的线程池接口；
 */
public interface ExecutorService extends Executor {

    /**
     * 对之前提交的任务进行一次有顺序的关闭，并且不会接受新的任务，如果已经关闭继续执行不会有额外影响。
     * 这个方法不会等待之前提交的任务执行完毕。
     */
    void shutdown();

    /**
     * 尝试停止所有正在执行的任务，暂停处理正在等待的任务，返回等待执行的任务集合
     * 这个方法不会等待正在执行的任务终止
     * 本方法不提供担保，任务的fail response都可能不会终止
     */
    List<Runnable> shutdownNow();

    /**
     * 判断executor是否被关闭
     * 如果已经被shutdown，返回true
     */
    boolean isShutdown();

    /**
     * 判断用用shutdown/shutdownNow后是否所有的任务都被结束。
     * 如果所有任务都已经被终止，返回true
     * 是否为终止状态
     */
    boolean isTerminated();

    /**
     * 在一个shutdown请求后，阻塞等待所有任务执行完毕
     * 或者到达超时时间，或者当前线程被中断
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 提交一个可执行的（Runnable）任务，返回一个Future代表这个任务执行状态
     * 等到任务成功执行，Future#get()方法会返回null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * 提交一个可以执行的任务，返回一个Future代表这个任务执行状态
     * 等到任务执行结束，Future#get()方法会返回这个给定的result
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * 提交一个有返回值的任务，并返回一个Future代表等待的任务执行的结果
     * 等到任务成功执行，Future#get()方法会返回任务执行的结果
     */
    Future<?> submit(Runnable task);

    /**
     * 执行给定任务，当所有任务完成后返回一个List<Futrue<T>>列表，持有任务执行的结果与状态
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    /**
     * 执行给定任务，当所有任务完成或超时后返回一个List<Futrue<T>>列表，持有任务执行的结果与状态
     * 如果超时会取消其他未执行完成的任务
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * 执行给定任务，有一个任务完成后，无论异常还是正常
     * 返回一个Futrue<T>，持有任务执行的结果与状态
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    /**
     * 执行给定任务，有一个任务完成或超时，无论异常还是正常
     * 返回一个Futrue<T>，持有任务执行的结果与状态
     * 超时取消正在执行的任务
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
