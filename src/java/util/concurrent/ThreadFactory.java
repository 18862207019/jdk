package java.util.concurrent;

//用于设置创建线程的工厂。该对象可以通过Executors.defaultThreadFactory()
public interface ThreadFactory {
    Thread newThread(Runnable r);
}
