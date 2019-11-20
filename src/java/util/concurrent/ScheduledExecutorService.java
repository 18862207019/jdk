/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 *  ScheduledExecutorService接口继承了ExecutorService接口，提供了带"周期执行"功能ExecutorService；
 */
public interface ScheduledExecutorService extends ExecutorService {

    /**
     * 在给定延时后，创建并执行一个一次性的Runnable任务
     * 任务执行完毕后，ScheduledFuture#get()方法会返回null
     */
    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit);

    /**
     * 在给定延时后，创建并执行一个ScheduledFutureTask
     * 返回ScheduledFuture 可以获取结果或取消任务
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit);

    /**
     * 创建并执行一个在给定初始延迟后首次启用的定期操作，后续操作具有给定的周期
     * 也就是将在 initialDelay 后开始执行，然后在 initialDelay+period 后执行，接着在 initialDelay
     * + 2 * period 后执行，依此类推
     * 如果执行任务发生异常，随后的任务将被禁止，否则任务只会在被取消或者Executor被终止后停止
     * 如果任何执行的任务超过了周期，随后的执行会延时，不会并发执行
     * 例如延时为3s，第2s开始执行任务，下一次执行就会是第5s，下下次就是8s
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit);

    /**
     * 创建并执行一个在给定初始延迟后首次启用的定期操作，随后，在每一次执行终止和下一次执行开始之间都存在给
     * 定的延迟
     * 如果执行任务发生异常，随后的任务将被禁止，否则任务只会在被取消或者Executor被终止后停止
     * 例如延时为3s，任务执行4s，第2s开始执行任务，下一次执行任务就是9s，下下次就是16s
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit);

}
