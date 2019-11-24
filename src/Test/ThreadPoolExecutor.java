
package Test;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ExecutorService的默认实现，作为自定义线程池的主要类。
 */
class MyThreadPoolExecutor  extends  AbstractExecutorService{

    // ct1是一个原子整数型，其中打包了两个概念
    // 其中高3位是维护线程池的运行状态，低29位是用来位置线程池中线程的数量
    // 3位的原因是因为线程有五种状态，向上取2次方数是8也就是3位
    //// 初始状态为RUNNING
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    // 这个是29，做位运算用的
    private static final int COUNT_BITS = Integer.SIZE - 3;
    // 这个是线程的容量，也就是低29位的最大值
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
    //RUNNING，表示可接受新任务，且可执行队列中的任务； -- 对应的高3位值是111。 -536870912
    private static final int RUNNING    = -1 << COUNT_BITS;
    //SHUTDOWN，表示不接受新任务，但可执行队列中的任务；-- 对应的高3位值是000。  0
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    //STOP，表示不接受新任务，且不再执行队列中的任务，且中断正在执行的任务；    -- 对应的高3位值是001。 536870912
    private static final int STOP       =  1 << COUNT_BITS;
    // TIDYING，所有任务已经中止，且工作线程数量为0，  -- 对应的高3位值是010。 1073741824
    // 最后变迁到这个状态的线程将要执行terminated()钩子方法，
    // 只会有一个线程执行这个方法；
    private static final int TIDYING    =  2 << COUNT_BITS;
    //中止状态，已经执行完terminated()钩子方法； -- 对应的高3位值是011。  1610612736
    private static final int TERMINATED =  3 << COUNT_BITS;


    //高3位的值，也就是线程池的状态
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    // 获取低29位  当前线程的运行数量
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    // 将线程池状态和运行的线程数量进行打包
    private static int ctlOf(int rs, int wc) { return rs | wc; }
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }
    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }
    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }
    private final BlockingQueue<Runnable> workQueue;
    private final ReentrantLock mainLock = new ReentrantLock();
    private final HashSet<Worker> workers = new HashSet<Worker>();
    private final Condition termination = mainLock.newCondition();
    private int largestPoolSize;
    private long completedTaskCount;
    private volatile ThreadFactory threadFactory;
    private volatile RejectedExecutionHandler handler;
    private volatile long keepAliveTime;
    private volatile boolean allowCoreThreadTimeOut;
    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");
    private final AccessControlContext acc;
    private static final RejectedExecutionHandler defaultHandler = new ThreadPoolExecutor.AbortPolicy();



    /**
     * Worker的源码中我们可以看到Woker继承AQS，实现Runnable接口，所以可以认为Worker既是一个可以执行的任务，
     * 也可以达到获取锁释放锁的效果。这里继承AQS主要是为了方便线程的中断处理。
     * 这里注意两个地方：构造函数、run()。构造函数主要是做三件事：
     * 1.设置同步状态state为-1，同步状态大于0表示就已经获取了锁，
     * 2.设置将当前任务task设置为firstTask，
     * 3.利用Worker本身对象this和ThreadFactory创建线程对象。
     */
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {

        private static final long serialVersionUID = 6138294804551838833L;
        // task 的thread
        final Thread thread;
        // 运行的任务task
        Runnable firstTask;
        // 之前完成的Task数量
        volatile long completedTasks;

        Worker(Runnable firstTask) {
            //设置AQS的同步状态大于0  不允许中断(等于0未获取到锁 大于0获取到锁 ) shutdownNow方法getState() >= 0 才能被中断  interruptIdleWorkers只有获取到锁才能被中断
            //也就是说state=0之前不允许进行中断操作
            setState(-1);

            //线程执行的任务
            this.firstTask = firstTask;

            // 利用ThreadFactory和 Worker这个Runnable创建的线程对象 创建的就是Worker对象本身
            this.thread = getThreadFactory().newThread(this);
        }

        // 启动线程执行任务使用
        @Override
        public void run() {
            runWorker(this);
        }

         // 是否持有独占锁，如果state=0是未加锁状态，其他为加锁状态
        @Override protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        // 尝试获取锁，是对AQS中的方法的实现,这里在获取锁时与0对比，所以在runWorker之前是无法获取锁的
        @Override
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 尝试释放锁，对AQS中方法的实现
        @Override protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        // 这里就能看出在state=-1的状态是不能interrput,只有调用runWorker方法后会将状态置为1
        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                    ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                break;
            }
        }
    }

    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            // 线程池处于Running状态
            // 线程池已经终止了
            // 线程池处于ShutDown状态，但是阻塞队列不为空
            if (isRunning(c) ||
                    runStateAtLeast(c, TIDYING) ||
                    (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty())) {
                return;
            }

            // 执行到这里，就意味着线程池要么处于STOP状态，要么处于SHUTDOWN且阻塞队列为空
            // 这时如果线程池中还存在线程，则会尝试中断线程
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // 尝试终止线程池
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        // 线程池状态转为TERMINATED
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }

        }
    }
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers) {
                    security.checkAccess(w.thread);
                }
            } finally {
                mainLock.unlock();
            }
        }
    }
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                w.interruptIfStarted();
            }
        } finally {
            mainLock.unlock();
        }
    }
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                // 这里如果要中断正在执行的线程就必须要获取到锁
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }
    private static final boolean ONLY_ONE = true;
    final void reject(Runnable command) {
//        handler.rejectedExecution(command, this);
        System.out.println("拒绝");
    }
    void onShutdown() {
    }
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }
    /**
     * 创建线程执行任务
     * 根据worker获取要执行的任务task，
     * 然后调用unlock()方法释放锁，
     * 这里释放锁的主要目的在于中断，因为在new Worker时，设置的state为-1，
     * 调用unlock()方法可以将state设置为0，
     * 这里主要原因就在于interruptWorkers()方法只有在state >= 0时才会执行；
     *
     * 通过getTask()获取执行的任务，调用task.run()执行，
     * 当然在执行之前会调用worker.lock()上锁，
     * 执行之后调用worker.unlock()放锁；
     *
     *
     * 在任务执行前后，可以根据业务场景自定义beforeExecute() 和 afterExecute()方法，则两个方法在ThreadPoolExecutor中是空实现；
     * 如果线程执行完成，则会调用getTask()方法从阻塞队列中获取新任务，
     * 如果阻塞队列为空，则根据是否超时来判断是否需要阻塞；
     * task == null或者抛出异常（beforeExecute()、task.run()、afterExecute()均有可能）导致worker线程终止，
     * 则调用processWorkerExit()方法处理worker退出流程。
     */

































    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        // 外层循环用于判断线程池状态
        for (;;) {

            // 获取当前线程状态
            int c = ctl.get();
            int rs = runStateOf(c);

            // （1）就是当前线程池是否是可添加新线程的状态
            //  总过两中情况会继续走下去
            //    1，线程处于running状态
            //    2，线程处于SHUTDOWN且阻塞队列中有未完成的任务需要添加一个任务是null的线程去完成阻塞队列中的任务
            /*
                    rs >= SHUTDOWN ，表示当前线程处于SHUTDOWN ，STOP、TIDYING、TERMINATED状态
                    rs == SHUTDOWN , firstTask != null时不允许添加线程，因为线程处于SHUTDOWN 状态，不允许添加任务
                    rs == SHUTDOWN , firstTask == null，但workQueue.isEmpty() == true，不允许添加线程，
                    因为firstTask == null是为了添加一个没有任务的线程然后再从workQueue中获取任务的，
                    如果workQueue == null，则说明添加的任务没有任何意义（说明当前添加的任务就是null的任务）。
            */
            // SHUTDOWN 状态  不允许添加任务 但是允许执行完毕已经存在的任务
            // rs >= SHUTDOWN 表示当前线程处于SHUTDOWN ，STOP、TIDYING、TERMINATED状态
            // (rs == SHUTDOWN && firstTask == null && ! workQueue.isEmpty()) //加入firstTask == null的新线程执行阻塞队列中的任务
            if (rs >= SHUTDOWN && ! (rs == SHUTDOWN && firstTask == null && ! workQueue.isEmpty())) {
                return false;
            }
            // （2）内层循环，worker + 1（运行的线程数量+1）
            for (;;) {
                //线程数量
                int wc = workerCountOf(c);
                // 如果当前线程数大于线程最大上限CAPACITY  return false
                // 若core == true，则与corePoolSize 比较，否则与maximumPoolSize ，大于 return false
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize)) {
                    return false;
                }
                // worker + 1,成功跳出retry循环
                if (compareAndIncrementWorkerCount(c)) {
                    break retry;
                }
                // CAS add worker 失败，再次读取ctl
                c = ctl.get();
                // 如果状态不等于之前获取的state，跳出内层循环，继续去外层循环判断
                if (runStateOf(c) != rs) {
                    continue retry;
                }
            }
        }
// ------上面的循环是调整线程池状态，下面是真正的添加线程--------

        // work启动标志
        boolean workerStarted = false;

        // work是否被添加到线程池中的标志
        boolean workerAdded = false;

        Worker w = null;
        try {
            //（3）新建线程：Worker(封装了线程对象代表工作线程执行具体的任务)
            w = new Worker(firstTask);
            // 工作线程 就是Worker线程本身
            final Thread t = w.thread;
            if (t != null) {
                // 获取主锁：mainLock
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // 线程状态
                    int rs = runStateOf(ctl.get());

                    // rs < SHUTDOWN ==> 线程处于RUNNING状态
                    // 或者线程处于SHUTDOWN状态，且firstTask == null（可能是workQueue中仍有未执行完成的任务，创建没有初始任务的worker线程执行任务）
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {

                        // 当前线程已经启动，抛出异常
                        if (t.isAlive()) {
                            throw new IllegalThreadStateException();
                        }

                        // (4)将新建的work加入到set中，workers是维护线程池中所有线程的hashSet
                        workers.add(w);

                        // (5) 设置最大的池大小largestPoolSize，workerAdded设置为true
                        int s = workers.size();
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        workerAdded = true;
                    }
                } finally {
                    // 处理完成后释放锁
                    mainLock.unlock();
                }
                // (6)启动线程
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            //(7)如果处理启动失败就会将失败的线程从worker中移除
            if (! workerStarted) {
                addWorkerFailed(w);
            }
        }
        return workerStarted;
    }
    final void runWorker(Worker w) {

        // 当前线程也就是work线程
        Thread wt = Thread.currentThread();

        // 要执行的任务
        Runnable task = w.firstTask;
        w.firstTask = null;

        //(在执行runWorker之前不允许被中断) 释放锁，允许中断
        w.unlock();
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                // worker 获取锁
                w.lock();

                // 确保线程在stopping时被设置中断标志，否则清除中断标志
                // 1.如果线程池处于STOP状态，并且当前线程并不是中断状态，调用wt.interrupt确保线程中断
                // 2.如果线程池不是STOP状态，但Thread.interrupted()返回是true
                // [表示当前线程是中断状态，并且清除了中断标志] 然后再次判断线程池状态是否是STOP状态
                // 如果是就再次调用wt.interrupt确保线程中断
                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                try {
                    // 本类中是空实现，子类有需要可依据情况实现，Tomcat中的线程池就重写了该方法
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        // 执行任务
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    // 完成任务+1
                    w.completedTasks++;
                    // 释放锁
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            //在runWorker()方法中，无论最终结果如何，都会执行processWorkerExit()方法对worker进行退出处理。
            processWorkerExit(w, completedAbruptly);
        }
    }

    //在addWorker()方法中，如果线程t==null，或者在add过程出现异常，
    // 会导致workerStarted == false，那么在最后会调用addWorkerFailed()方法：
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                // // 从HashSet中移除该worker
            {
                workers.remove(w);
            }
            // 线程数 - 1
            decrementWorkerCount();
            // 尝试终止线程
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // true：用户线程运行异常,需要扣减
        // false：getTask方法中扣减线程数量
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        // 获取主锁
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;//把worker的完成任务数加到线程池的完成任务数
            workers.remove(w);//从HashSet<Worker>中移除
        } finally {
            mainLock.unlock();
        }
        // 有worker线程移除，可能是最后一个线程退出需要尝试终止线程池
        tryTerminate();
        // 如果线程为running或shutdown状态，即tryTerminate()没有成功终止线程池，则判断是否有必要一个worker
        int c = ctl.get();

        if (runStateLessThan(c, STOP))
            // 正常退出，计算min：需要维护的最小线程数量
        {
            if (!completedAbruptly) {
                // allowCoreThreadTimeOut 默认false：是否需要维持核心线程的数量
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;

                // 如果min ==0 或者workerQueue为空，min = 1
                    if (min == 0 && ! workQueue.isEmpty()) {
                        min = 1;
                    }

                // 如果线程数量大于最少数量min，直接返回，不需要新增线程
                if (workerCountOf(c) >= min) {
                    return; // replacement not needed
                }
            }
        }

        // 添加一个没有firstTask的worker
        addWorker(null, false);
        }


    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?

        for (;;) {
            int c = ctl.get();
            // 线程池状态
            int rs = runStateOf(c);

            // 线程池中状态 >= STOP 不在执行任务
            // 线程池状态 == SHUTDOWN且阻塞队列为空(无任务需要执行)
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                //worker - 1，return null
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            // 判断是否需要超时控制
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
            if ((wc > maximumPoolSize || (timed && timedOut)) && (wc > 1 || workQueue.isEmpty())) {
                //减少线程。
                if (compareAndDecrementWorkerCount(c)) {
                    return null;
                }
                // 减少线程失败
                continue;
            }

            try {
                // 从阻塞队列中获取task
                // 如果需要超时控制，则调用poll()，否则调用take()
                //timed == true，调用poll()方法从阻塞队列中超时获取，如果在keepAliveTime时间内还没有获取task的话，则返回null，继续循环
                //timed == false，则调用take()方法，该方法为一个阻塞方法，没有任务时会一直阻塞挂起，直到有任务加入时对该线程唤醒，返回任务。
                Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (r != null) {
                    return r;
                }
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }
    public MyThreadPoolExecutor(int corePoolSize,//核心线程数量
                              int maximumPoolSize,//最大线程数量
                              long keepAliveTime,//超出核心线程数量以外的线程的空余线程的存活时间
                              TimeUnit unit,//存活时间的单位
                              BlockingQueue<Runnable> workQueue,//阻塞队列
                              RejectedExecutionHandler handler //拒绝操作
    ) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }



    /**
     * 构造器
     * @param corePoolSize
     *              o 线程池中的核心线程数量，当提交一个任务时，线程池创建一个新线程执行任务，直到当前线程数等于corePoolSize，
     *                即使有其他空闲线程能执行新来的任务，也会继续创建新的线程；
     *              o 如果当前线程数为corePoolSize，继续提交的任务被保存到阻塞队列中，等待被执行；
     * @param maximumPoolSize
     *              o 线程池允许线程的最大数量，在阻塞队列被填满后，会创建新的线程执行任务，前提是当前线程数小于maximumPoolSize
     *              o 当workQueue为无界队列时，maxiumPoolSize则不会起作用，因为新的任务会一直放入到workQueue中
     * @param keepAliveTime
     *              o 线程空闲时的存活时间，默认情况下，该参数只在线程数大于corePoolSize时才有用。
     * @param unit
     *              o keepAliveTime的单位
     *              o TimeUnit静态类提供常量
     * @param workQueue
     *              o 执行前用于保持任务的队列。此队列仅保持由 execute方法提交的 Runnable任务
     *              o 阻塞队列的选择
     *              ArrayBlockingQueue：是一个基于数组结构的有界阻塞队列，此队列按 FIFO（先进先出）原则对元素进行排序，
     *                                  指定队列的最大长度，使用有界队列可以防止资源耗尽，但会出现任务过多时的拒绝问题，
     *                                  需要进行协调。
     *              LinkedBlockingQueue：一个基于链表结构的有界阻塞队列，此队列按FIFO （先进先出） 排序元素，
     *                                  吞吐量通常要高于ArrayBlockingQueue。静态工厂方法Executors.newFixedThreadPool()
     *                                  使用了这个队列。
     *              SynchronousQueue：一个不存储元素的阻塞队列。每个插入操作必须等到另一个线程调用移除操作，
     *                                 否则插入操作一直处于阻塞状态，吞吐量通常要高于LinkedBlockingQueue，
     *                                 静态工厂方法Executors.newCachedThreadPool使用了这个队列。
     *              PriorityBlockingQueue：一个具有优先级的无限阻塞队列。
     * @param threadFactory
     *              o 创建线程的工厂，通过自定义的线程工厂可以给每个新建的线程设置一个具有识别度的线程名。
     *              o 默认为DefaultThreadFactory，自定义可以实现ThreadFactory接口
     * @param handler
     *              o 线程池的饱和策略，当阻塞队列满了，且没有空闲的工作线程，如果继续提交任务，必须采取一种策略处理该任务
     *              o 线程池提供如下四种策略
     *                  AbortPolicy：直接抛出异常，默认策略
     *                  CallerRunsPolicy：用调用者所在的线程来执行任务
     *                  DiscardOldestPolicy：丢弃阻塞队列中靠最前的任务，并执行当前任务
     *                  DiscardPolicy：直接丢弃任务
     *              o  可以根据具体使用场景，实现RejectedExecutionHandler接口，自定义拒绝策略
     */
    public MyThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {

        // 参数出校验，出现异常抛出参数非法异常
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0)
            throw new IllegalArgumentException();
        // 几个引用类型的非空检验，空了抛出空指针异常
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();

        // 在执行finalize()方法时使用，并不影响对线程池的理解
        this.acc = System.getSecurityManager() == null ?
                null :
                AccessController.getContext();

        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    @Override
    public void execute(Runnable command) {
        int c = ctl.get();
        //（1）如果线程池当前线程数小于corePoolSize，则调用addWorker创建新线程执行任务
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
            //防止并发情况重新获取c
            c = ctl.get();
        }
        //（2）如果线程池处于RUNNING状态，则尝试加入阻塞队列
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            // 尝试进行Double Check 判断加入到阻塞队里中的任务是否可以被执行。
            // 如果线程池不是RUNNING状态，则调用remove()方法从阻塞队列中删除该任务，
            if (! isRunning(recheck) && remove(command))
                //然后调用reject()方法处理任务
                reject(command);

            // 确保当前还有线程运行（） 因为执行到这一步阻塞队列中一定是有一个任务需要处理
            // 走到这一步只能有两种情况 线程是RUNNING状态
            // 线程不是RUNNING状态 但是从阻塞队列中移除任务失败(未能走到拒绝策略)
            // 必须保证一个线程去处理加入到阻塞队列中的任务
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        // (3) 如果向队列添加失败，尝试扩充线程池，将任务分配给新的线程，只要不大于maximumPoolSize就可以。
        else if (!addWorker(command, false))
        // (4)拒绝策略
        reject(command);
    }
    //shutdown()：按过去执行已提交任务的顺序发起一个有序的关闭，但是不接受新任务。
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            // 推进线程状态
            advanceRunState(SHUTDOWN);
            // 中断空闲的线程
            interruptIdleWorkers();
            // 交给子类实现
            onShutdown();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }
    //shutdownNow() :尝试停止所有的活动执行任务、暂停等待任务的处理，并返回等待执行的任务列表
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            // 推进线程状态
            advanceRunState(STOP);
            // 中断所有线程
            interruptWorkers();
            // 返回等待执行的任务列表
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }
    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }
    protected void finalize() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || acc == null) {
            shutdown();
        } else {
            PrivilegedAction<Void> pa = () -> { shutdown(); return null; };
            AccessController.doPrivileged(pa, acc);
        }
    }
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }
    public int getCorePoolSize() {
        return corePoolSize;
    }
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);
    }
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                : workers.size();
        } finally {
            mainLock.unlock();
        }
    }
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    protected void beforeExecute(Thread t, Runnable r) { }
    protected void afterExecute(Runnable r, Throwable t) { }
    protected void terminated() { }


    public MyThreadPoolExecutor(int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            Executors.defaultThreadFactory(), defaultHandler);
    }

    public static ExecutorService newCachedThreadPool() {
        return new MyThreadPoolExecutor(3, 10,
            60L, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    }

    public static void main(String[] args) {
        ExecutorService executorService = newCachedThreadPool();
        for (int i=0;i<10;i++){
            executorService.execute(() -> {
                try {
                    Thread.sleep(10000);
                    System.out.println(Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            });
        }

    }
}
