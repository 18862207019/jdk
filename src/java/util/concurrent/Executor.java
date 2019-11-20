
package java.util.concurrent;

//Executor是最基础的执行接口；
//可以用来执行已经提交的Runnable任务对象，这个接口提供了一种将“任务提交”与“任务执行”解耦的方法。
public interface Executor {
    void execute(Runnable command);
}
