package java.util.concurrent.locks;

import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * AQS 是一个用来构建锁和同步器的框架， 它底层用了 CAS 技术来保证操作的原子性， 同时利用 FIFO 队列实现线程间的锁竞争，
 *
 * AQS为这些类提供了基础设施，也就是提供了一个密码锁， 这些类拥有了密码锁之后可以自己来设置密码锁的密码。
 * 此外，AQS还提供了一个排队区，并且提供了一个线程训导员， 我们知道线程就像一个原始的野蛮人，它不懂得讲礼貌，它只会横冲直撞，
 * 所以你得一步一步去教它，告诉它什么时候需要去排队了，要到哪里去排队， 排队前要做些什么，排队后要做些什么。这些教化工作全部都由AQS帮你完成了，
 * 从它这里教化出来的线程都变的非常文明懂礼貌，不再是原始的野蛮人，所 以以后我们只需要和这些文明的线程打交道就行了，千万不要和原始线程有过多的接触！
 *
 * 线程在获取锁失败后首先进入同步队列排队，而想要进入条件队列该线程必须持有锁才行。接下来我们看看队列中每个结点的结构。
 */
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

  private static final long serialVersionUID = 7373984972572414691L;

  protected AbstractQueuedSynchronizer() {
  }

  /**
   * 同步队列的头节点
   *
   * head 节点可以表示成当前持有锁的线程的节点，其余线程竞争锁失败后，会加入到队尾， tail
   */
  private transient volatile Node head;

  private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.prev = null;
  }

  /**
   * 同步队列的尾节点；volatile 保证可见性
   */
  private transient volatile Node tail;

  /**
   * volatile 保证可见性
   *
   * state 字段为同步状态
   *
   * state > 0 为有锁状态 每次加锁就在原有 state 基础上加 1 ，即代表当前持有锁的线程加了 state 次锁，反之解锁时每次减1
   *
   * state = 0 为无锁状态
   *
   * 看成一个密码锁，而且还是从房间里面锁起来的密码锁， state具体的值就相当于密码控制着密码锁的开合。 当然这个锁的密码是多少就由各个子类来规定了，
   * 例如在ReentrantLock中，state等于0表示锁是开的 ，state大于0表示锁是锁着的，
   * 而在Semaphore中，state大于0表示锁是开的， state等于0表示锁是锁着的。
   */
  private volatile int state;

  /**
   * Node代表同步队列和条件队列中的一个结点，
   *
   */
  static final class Node {

    /** 表示当前线程以共享模式持有锁 */
    static final Node SHARED = new Node();

    /** 表示当前线程以独占模式持有锁 */
    static final Node EXCLUSIVE = null;

    /** 表示当前结点已经取消获取锁 */
    static final int CANCELLED = 1;

    /** 表示后继结点的线程需要运行 */
    static final int SIGNAL = -1;

    /** 表示当前结点在条件队列中排队 */
    static final int CONDITION = -2;

    /** 表示后继结点可以直接获取锁 仅在共享锁模式下使用 */
    static final int PROPAGATE = -3;

    /** 表示当前结点的等待状态 默认值0 此时节点还没有进入就绪状态 */
    volatile int waitStatus;

    /** 表示同步队列中的前继结点 */
    volatile Node prev;

    /** 表示同步队列中的后继结点 */
    volatile Node next;

    /** 当前结点持有的线程引用 */
    volatile Thread thread;

    /** 表示条件队列中的后继结点 */
    Node nextWaiter;

    /** 当前结点状态是否是共享模式 */
    final boolean isShared() {
      return nextWaiter == SHARED;
    }

    /** 返回当前节点的前置节点 */
    final Node predecessor() throws NullPointerException {
      Node p = prev;
      if (p == null)
        throw new NullPointerException();
      else
        return p;
    }

    Node() {
    }

    /** 默认用这个构造器 */
    Node(Thread thread, Node mode) {
      this.nextWaiter = mode;
      this.thread = thread;
    }

    /** 只在条件队列中使用*/
    Node(Thread thread, int waitStatus) {
      this.waitStatus = waitStatus;
      this.thread = thread;
    }
  }

  protected final int getState() {
    return state;
  }

  protected final void setState(int newState) {
    state = newState;
  }

  /**
   * 操作 CAS 更改 state 状态，保证 state 的原子性。
   */
  protected final boolean compareAndSetState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
  }

  /**
   * AQS 提供了两种锁，分别是独占锁和共享锁.
   */

  /**
   * 尝试获取独占锁
   *
   * @param arg
   * @return
   * @see ReentrantLock
   */
  protected boolean tryAcquire(int arg) {
    throw new UnsupportedOperationException();
  }

  /**
   * 释放独占锁的方法
   *
   * @param arg
   * @return
   */
  protected boolean tryRelease(int arg) {
    throw new UnsupportedOperationException();
  }

  /**
   * 获取共享锁
   *
   * @param arg
   * @return
   * @see CountDownLatch
   * @see Semaphore
   */
  protected int tryAcquireShared(int arg) {
    throw new UnsupportedOperationException();
  }

  /**
   * 释放共享锁
   *
   * @param arg
   * @return
   */
  protected boolean tryReleaseShared(int arg) {
    throw new UnsupportedOperationException();
  }

  /**
   * 获取独占锁
   *总结：
   *          首先调用tryAcquire尝试获取锁，获取失败则调用addWaiter，将该节点以独占的状态入队，加入到队列的末尾，
   *        由于可能存在多个线程调用CAS操作将节点加入到CLH队列末尾，因此enq方法以for循环的方式进行，保证该节点能添加到队列末尾。
   * 然后调用acquireQueued在队列中获取锁，过程为：检查该节点node的前驱节点是否为head节点
   * 如果是head节点并且调用tryAcquire获取锁成功，则setHead，将node作为当前队列的队首，原先的首节点出队。
   * 否则，调用shouldParkAfterFailedAcquire查看当前节点的线程是否应该park，具体为：如果当前结点node的前一个节点的waitStatus为signal则表示node节点应该
   * park等待前一个节点唤醒它，返回true;如果当前节点的小于0，即为cancelled，
   * 则应该删除node前面连续的cancelled节点，将node的prev指针指向靠近node的第一个非cancelled节点，返回false，
   * 重新判断前驱状态；若为0或者为PROPAGATE，则CAS将前驱状态改成signal返回false，重新判断。
   * 如果应该park，则调用parkAndCheckInterrupt将当前线程park，否则就进行下次循环。
   * 如果以上过程中抛出异常并且该节点还没有成功获取锁，
   * 则指向cancelAcquire,将当前节点的状态改为cancelled，
   * 该节点不在参与获取锁，同时删除该节点前面连续的cancelled状态的节点，
   * 若删除之后发现该节点的前一个节点是head，则应该调用unparkSuccessor唤醒后继。
   */
  public final void acquire(int arg) {
    //tryAcquire尝试获得该arg，失败则先addWaiter(Node.EXCLUSIVE)加入等待队列，然后acquireQueued。
    if (!tryAcquire(arg)
        && acquireQueued(
            addWaiter(Node.EXCLUSIVE),
            arg))
      //如果acquireQueued()再获取锁的过程中被中断则中断当前线程
      selfInterrupt();
  }


    /**
     * 获取共享锁
     *
     * @param arg
     */
    public final void acquireShared(int arg) {
         //tryAcquireShared，模板方法，子类实现。调用tryAcquireShared尝试获取锁
        if (tryAcquireShared(arg) < 0)
          //获取失败则调用doAcquireShared获取锁
            doAcquireShared(arg);
    }


private void doAcquireShared(int arg) {

  //以共享状态创建该线程的节点
    final Node node = addWaiter(Node.SHARED);

    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
              //尝试获取锁，r大于等于0表示获取成功
                int r = tryAcquireShared(arg);

                // 如果在这里成功获取共享锁，会进入共享锁唤醒逻辑
                if (r >= 0) {

                    //重新设置首节点，并且向后唤醒shared状态的节点
                    setHeadAndPropagate(node, r);

                    p.next = null;
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }

            //p不是首节点，查看是否应该阻塞
            if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
      //获取失败，取消节点
        if (failed)
            cancelAcquire(node);
    }
}

  /**
   * (1) 创建节点
   * (2) 如果尾节点不为null直接加到尾节点后面并且设置尾节点为当前节点并且返回新节点
   * (3) 如果尾节点为null或者设置尾节点失败(并发情况设置尾节点)则enq进行自旋锁的入节点操作
   */
  //mode:设置该节点是共享的还是独占的， Node.EXCLUSIVE for exclusive, Node.SHARED for shared
  private Node addWaiter(Node mode) {

    // 独占锁的节点
    Node node = new Node(Thread.currentThread(), mode);

    //pred指向尾节点
    Node pred = tail;


    //尾节点不为null
    if (pred != null) {
      //新node的前驱结点指向尾节点
      node.prev = pred;
      //compareAndSetTail(pred, node)：若等待队列的尾节点指向pred，则将尾节点指针指向node
      if (compareAndSetTail(pred, node)) {
        //修改AQS的尾节点成功，则将原先尾节点的next指向新节点
        pred.next = node;
        //返回当前节
        return node;
      }
    }
    //pred==null或者修改AQS尾节点失败，则进入enq，用一个for循环修改AQS的尾指针，直至成功
    enq(node);
    return node;
  }

  /**
   *  ----node 封装了线程与一些运行状态 还有内部队列的一些状态
   * （1）  如果尾节点tail(AQS的成员变量)为null 则证明队列没进行过初始化进行初始化(设置tail 和  head 都为 new Node())之后再循环一次
   * （2）  将当前node节点加入到原节点之后并设置tail节点为新加入的节点
   *
   * @param node
   * @return
   */
  private Node enq(final Node node) {

    //循环，原因：可能同时存在多个线程修改tail指针
    for (;;) {

      //t指向队列的tail
      Node t = tail;

      // 如果队尾节点为空，那么进行CAS操作初始化队列
      if (t == null) {

        //尾节点为null，则队列为空，需要初始化队列
        // 用于表示当前正在执行的节点，头节点即表示当前正在运行的节点
        if (compareAndSetHead(new Node()))
          tail = head;
      } else {

        //将node的prev指针指向AQS的tail
        node.prev = t;

        //如果队列的tail==t，则将tail指向node
        if (compareAndSetTail(t, node)) {
          //原先的尾节点的next指向新添加的node
          t.next = node;
          //修改成功，返回原先的尾节点，失败则进行下一次循环
          return t;
        }
      }
    }
  }

  /**
   * （1）  获取当前锁的前置节点
   * （2）  如果前置节点是头节点则再次尝试获取锁，如果获取成功修改当前节点为头节点 返回当前中断标志位
   * （3）  获取失败检查当前线程是否应该被挂起 (如果当前线程的前置节点的状态为SINGL则当前线程就应该被挂起)
   * （4）  如果应该被挂起则挂起当前线程醒来之后返回自己的中断标志位继续走循环逻辑
   */
  final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
      boolean interrupted = false;
      for (;;) {

        //获取node的前驱节点p
        final Node p = node.predecessor();

        //前驱p是首节点，则当前线程尝试获取锁
        if (p == head && tryAcquire(arg)) {
          //获取成功，则将当前节点设置成首节点
          setHead(node);
          p.next = null; // help GC
          failed = false;
          //返回中断标志
          return interrupted;
        }
        //node的前驱节点p不是首节点head或者获取锁失败则通过shouldParkAfterFailedAcquire来判断当前node的线程是否应该挂起park
        //应该park，则调用parkAndCheckInterrupt忽略中断进行挂起
        if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
          interrupted = true;
        //当该线程被唤醒时，尝试再次判断前驱节点p是否为head，如果是则再次尝试获取锁，这样做的目的是为了维持FIFO的顺序
        //采用阻塞park和唤醒unpark的方式进行自旋式检测获取锁

      }
    } finally {
      if (failed)
        //获取锁的过程中抛出异常则取消当前节点获取锁的逻辑
        cancelAcquire(node);
    }
  }


  //shouldParkAfterFailedAcquire:当前节点获取锁失败时，是否应该进入阻塞状态
  //（1）前驱节点的waitStatus==SINGNAL(-1):则说明该节点需要继续等待直至被前驱节点唤醒,返回true.
  //（2）前驱节点的waitStatus==cancelled(1):则说明前驱节点曾经发生过中断或者超时，则前驱节点是一个无效节点，继续向前寻找一个node，
  //    同时将waitStatus>0(cancelled)的节点删除，返回false(重新检测)。
  //（3）前驱节点的waitStatus==-3或0（PROPAGATE）:共享锁向后传播，因此需要将前驱节点的waitStatus修改成signal,然后返回false，
  private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    //ws：前驱节点的状态
    int ws = pred.waitStatus;
    //如果前驱节点是SIGNAL状态，则返回true，表示后继节点node应该被阻塞park
    if (ws == Node.SIGNAL)
      return true;
    //ws>0:cancelled,前驱节点是一个超时或者中断被取消的Node
    if (ws > 0) {
      //循环往前找第一个不是cancelled状态的节点，同时删除中间状态是cancelled的节点，调整队列。
      do {
        node.prev = pred = pred.prev;
      } while (pred.waitStatus > 0);
      pred.next = node;
    } else {
      //状态是-3PROPAGATE或0（CONDITION状态只能在CONDITION队列中出现，不会出现在CLH队列中）,
      // 则将pred前驱节点的状态设置成SIGNAL状态，表示pred节点被唤醒时,需要park它的后继者。
      compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
  }


  //parkAndCheckInterrupt：阻塞当前线程，并且返回中断状态
  private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);
    return Thread.interrupted();
  }

  //尝试释放锁，释放成功则调用unparkSuccessor唤醒后继节点
  public final boolean release(int arg) {

    //调用tryRelease释放锁。
    if (tryRelease(arg)) {
      Node h = head;
      // 释放成功，则查看head节点状态，如果不为null且状态不为0
      //（为0表示没有后继或者当前节点已经unparkSuccessor过），
      // 则调用unparkSuccessor唤醒后继节点
      if (h != null && h.waitStatus != 0)
        unparkSuccessor(h);
      return true;
    }
    return false;
  }

  static final long spinForTimeoutThreshold = 1000L;

  /**
   *  （1） 如果该节点是一个cancelled的节点，则不用管它；否则，将该节点状态置0清空，表示该节点已经获取过锁了，要出队。
   */
  //唤醒node的第一个非cancelled状态的node节点
  private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;

    //如果该节点是一个cancelled的节点，则不用管它；否则，将该节点状态置0清空，表示该节点已经获取过锁了，要出队（因为再执行获取锁的时候抛出异常了 所以必须标记为获取过锁的状态）。
    // 只有最后一个节点的状态是
    if (ws < 0)
      compareAndSetWaitStatus(node, ws, 0);

    // 后继节点
    Node s = node.next;

    //node的后继节点有可能是一个cancelled的节点,而cancelled节点的next指针指向其自身。
    //只能从tail开始寻找到node之间最靠近node的那一个非cancelled的节点，并唤醒它。
    //此处不删除node到s之间的cancelled节点，只负责唤醒s节点，删除操作在shouldParkAfterFailedAquired中进行
    if (s == null || s.waitStatus > 0) {
      s = null;

      // for循环从队列尾部一直往前找可以唤醒的节点
      for (Node t = tail; t != null && t != node; t = t.prev)
        if (t.waitStatus <= 0)
          s = t;
    }
    if (s != null)
      // 唤醒后继节点
      LockSupport.unpark(s.thread);
  }





  // 设置当前共享状态的node为head,同时，如果下一个节点是share并且waitStatus为signal或者propagate则释放当前锁，
  // 进行锁释放后的准备(doReleaseShared)
  private void setHeadAndPropagate(Node node, int propagate) {
    // 头节点
    Node h = head;

    // 设置当前节点为新的头节点
    setHead(node);

    if (propagate > 0 || h == null || h.waitStatus < 0 || (h = head) == null || h.waitStatus < 0) {

      // 后继节点
      Node s = node.next;

      //当后继节点s是一个shared状态的节点，或者s没有后继节点，则当前节点释放锁，同时处理后事
      if (s == null || s.isShared())
        doReleaseShared();
    }
  }

  private void doReleaseShared() {
    for (;;) {
      // 从头节点开始执行唤醒操作
      // 这里需要注意，如果从setHeadAndPropagate方法调用该方法，那么这里的head是新的头节点
      Node h = head;

      if (h != null && h != tail) {
        int ws = h.waitStatus;
        //首节点是signal,则直接CAS设置状态，同时调用unparkSuccessor唤醒后继节
        if (ws == Node.SIGNAL) {
          //首节点状态设置成0
          // 这里需要CAS原子操作，因为setHeadAndPropagate和releaseShared这两个方法都会顶用doReleaseShared，避免多次unpark唤醒操作
          if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
            // 如果初始化节点状态失败，继续循环执行
            continue;
          // 执行唤醒操作
          unparkSuccessor(h);
        //若当前节点的状态为0（后继为null），则调用CAS操作将状态改成PROPAGATE。
        } else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
          continue;
      }
      // 如果在唤醒的过程中头节点没有更改，退出循环
      // 这里防止其它线程又设置了头节点，说明其它线程获取了共享锁，会继续循环操作
      if (h == head)
        break;
    }
  }

  /**
   * 释放锁
   *
   * @param arg
   * @return
   */
  public final boolean releaseShared(int arg) {
    // 由用户自行实现释放锁条件
    if (tryReleaseShared(arg)) {
      // 执行释放锁
      doReleaseShared();
      return true;
    }
    return false;
  }


  //当acquireQueued的try中抛出异常node没有成功获取锁时，需要通过cancelAcquire方法取消node节点
  //主要功能：
  //（1）重新设置node的prev指针（相当于删除了node的前面连续几个cancelled节点）
  //（2）将node的状态设置为cancelled
  //（3）如果node的prev不是首节点，并且node的next节点不是一个cancelled节点，
  // 则设置node的prev指向node的next节点；否则，node的prev节点head节点，则需要唤醒node的后继节点(unparkSuccessor)
  //（4）将node的next指针指向自己
  private void cancelAcquire(Node node) {
    if (node == null)
      return;

    node.thread = null;
    //如果node的前驱节点也是一个cancelled的节点，则修改node的prev指针，直至指向一个不是cancelled状态的node
    //相当于删除了node到prev之前的状态为calcelled的前驱节点
    //原因：若新的前驱节点是head节点，则需要在该node失效时唤醒node的后继节点
    Node pred = node.prev;
    // 》0 cacled
    while (pred.waitStatus > 0)
      node.prev = pred = pred.prev;


    //前面找到了第一个不是cacelled的节点并且设置为当前节点的前置节点获取当前节点
    Node predNext = pred.next;
    // 直接设置当前节点为CANCELLED
    node.waitStatus = Node.CANCELLED;

    //如果是尾节点,直接删除node因为已经取消获取锁了
    if (node == tail && compareAndSetTail(node, pred)) {
      compareAndSetNext(pred, predNext, null);
    } else {
      // 当pred不是首节点时，如果node的后继节点需要signal，则尝试将pred的状态设置成signal，并且将pred的next指针指向node的next节点
      int ws;
      if (pred != head && ((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL)))
          && pred.thread != null) {
        Node next = node.next;
        if (next != null && next.waitStatus <= 0)
          compareAndSetNext(pred, predNext, next);
      } else {
        //当pred是首节点时，需要唤醒node的后继节点。
        unparkSuccessor(node);
      }
      //将cancelled的节点的next指针指向node自身
      node.next = node;
    }
  }

  /**
   * 中断当前线程
   */
  static void selfInterrupt() {
    Thread.currentThread().interrupt();
  }

  private void doAcquireInterruptibly(int arg) throws InterruptedException {
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
      for (;;) {
        final Node p = node.predecessor();
        if (p == head && tryAcquire(arg)) {
          setHead(node);
          p.next = null;
          failed = false;
          return;
        }
        if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
          throw new InterruptedException();
      }
    } finally {
      if (failed)
        cancelAcquire(node);
    }
  }

  private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (nanosTimeout <= 0L)
      return false;
    final long deadline = System.nanoTime() + nanosTimeout;
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
      for (;;) {
        final Node p = node.predecessor();
        if (p == head && tryAcquire(arg)) {
          setHead(node);
          p.next = null;
          failed = false;
          return true;
        }
        nanosTimeout = deadline - System.nanoTime();
        if (nanosTimeout <= 0L)
          return false;
        if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold)
          LockSupport.parkNanos(this, nanosTimeout);
        if (Thread.interrupted())
          throw new InterruptedException();
      }
    } finally {
      if (failed)
        cancelAcquire(node);
    }
  }

  private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
      for (;;) {
        final Node p = node.predecessor();
        if (p == head) {
          int r = tryAcquireShared(arg);
          if (r >= 0) {
            setHeadAndPropagate(node, r);
            p.next = null;
            failed = false;
            return;
          }
        }
        if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
          throw new InterruptedException();
      }
    } finally {
      if (failed)
        cancelAcquire(node);
    }
  }

  private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (nanosTimeout <= 0L)
      return false;
    final long deadline = System.nanoTime() + nanosTimeout;
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
      for (;;) {
        final Node p = node.predecessor();
        if (p == head) {
          int r = tryAcquireShared(arg);
          if (r >= 0) {
            setHeadAndPropagate(node, r);
            p.next = null;
            failed = false;
            return true;
          }
        }
        nanosTimeout = deadline - System.nanoTime();
        if (nanosTimeout <= 0L)
          return false;
        if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold)
          LockSupport.parkNanos(this, nanosTimeout);
        if (Thread.interrupted())
          throw new InterruptedException();
      }
    } finally {
      if (failed)
        cancelAcquire(node);
    }
  }

  protected boolean isHeldExclusively() {
    throw new UnsupportedOperationException();
  }

  public final void acquireInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    if (!tryAcquire(arg))
      doAcquireInterruptibly(arg);
  }

  public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
  }

  public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
      doAcquireSharedInterruptibly(arg);
  }

  public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
      throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
  }

  public final boolean hasQueuedThreads() {
    return head != tail;
  }

  public final boolean hasContended() {
    return head != null;
  }

  public final Thread getFirstQueuedThread() {
    return (head == tail) ? null : fullGetFirstQueuedThread();
  }

  private Thread fullGetFirstQueuedThread() {
    Node h, s;
    Thread st;
    if (((h = head) != null && (s = h.next) != null && s.prev == head && (st = s.thread) != null)
        || ((h = head) != null && (s = h.next) != null && s.prev == head
            && (st = s.thread) != null))
      return st;

    Node t = tail;
    Thread firstThread = null;
    while (t != null && t != head) {
      Thread tt = t.thread;
      if (tt != null)
        firstThread = tt;
      t = t.prev;
    }
    return firstThread;
  }

  public final boolean isQueued(Thread thread) {
    if (thread == null)
      throw new NullPointerException();
    for (Node p = tail; p != null; p = p.prev)
      if (p.thread == thread)
        return true;
    return false;
  }

  final boolean apparentlyFirstQueuedIsExclusive() {
    Node h, s;
    return (h = head) != null && (s = h.next) != null && !s.isShared() && s.thread != null;
  }

  public final boolean hasQueuedPredecessors() {
    Node t = tail;
    Node h = head;
    Node s;
    return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
  }

  public final int getQueueLength() {
    int n = 0;
    for (Node p = tail; p != null; p = p.prev) {
      if (p.thread != null)
        ++n;
    }
    return n;
  }

  public final Collection<Thread> getQueuedThreads() {
    ArrayList<Thread> list = new ArrayList<Thread>();
    for (Node p = tail; p != null; p = p.prev) {
      Thread t = p.thread;
      if (t != null)
        list.add(t);
    }
    return list;
  }

  public final Collection<Thread> getExclusiveQueuedThreads() {
    ArrayList<Thread> list = new ArrayList<Thread>();
    for (Node p = tail; p != null; p = p.prev) {
      if (!p.isShared()) {
        Thread t = p.thread;
        if (t != null)
          list.add(t);
      }
    }
    return list;
  }

  public final Collection<Thread> getSharedQueuedThreads() {
    ArrayList<Thread> list = new ArrayList<Thread>();
    for (Node p = tail; p != null; p = p.prev) {
      if (p.isShared()) {
        Thread t = p.thread;
        if (t != null)
          list.add(t);
      }
    }
    return list;
  }

  public String toString() {
    int s = getState();
    String q = hasQueuedThreads() ? "non" : "";
    return super.toString() + "[State = " + s + ", " + q + "empty queue]";
  }


  //判断该节点是否在CLH队列中
  final boolean isOnSyncQueue(Node node) {

    // 如果该节点的状态为CONDITION（该状态只能在CONDITION队列中出现，CLH队列中不会出现CONDITION状态），
    // 或者该节点的prev指针为null，则该节点一定不在CLH队列中
    if (node.waitStatus != Node.CONDITION || node.prev != null)
      return false;
      //如果该节点的next（不是nextWaiter，next指针在CLH队列中指向下一个节点）状态不为null，则该节点一定在CLH队列中
      if (node.next != null)
      return true;
    //否则只能遍历CLH队列（从尾节点开始遍历）查找该节点
    return findNodeFromTail(node);
  }

  //从尾节点开始，使用prev指针，遍历整个CLH队列
  private boolean findNodeFromTail(Node node) {
    Node t = tail;
    for (;;) {
      if (t == node)
        return true;
      if (t == null)
        return false;
      t = t.prev;
    }
  }

  //两步操作，首先enq将该node添加到CLH队列中，
  //其次若CLH队列原先尾节点为CANCELLED或者对原先尾节点CAS设置成SIGNAL失败，则唤醒node节点；
  //否则该节点在CLH队列总前驱节点已经是signal状态了，唤醒工作交给前驱节点（节省了一次park和unpark操作）
  final boolean transferForSignal(Node node) {

    //如果CAS失败，则当前节点的状态为CANCELLED
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
      return false;

    //enq将node添加到CLH队列队尾，返回node的prev节点
    Node p = enq(node);
    int ws = p.waitStatus;
    //如果p是一个取消了的节点，或者对p进行CAS设置失败，则唤醒node节点，让node所在线程进入到acquireQueue方法中，重新进行相关操作
    //否则，由于该节点的前驱节点已经是signal状态了，不用在此处唤醒await中的线程，唤醒工作留给CLH队列中前驱节点
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
      LockSupport.unpark(node.thread);
    return true;
  }


  //将取消条件等待的结点从条件队列转移到同步队列中
  final boolean transferAfterCancelledWait(Node node) {
      //如果这步CAS操作成功的话就表明中断发生在signal方法之前
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
      //状态修改成功后就将该结点放入同步队列尾部
      enq(node);
      return true;
    }
    //到这里表明CAS操作失败, 说明中断发生在signal方法之后
    while (!isOnSyncQueue(node))
        //如果sinal方法还没有将结点转移到同步队列, 就通过自旋等待一下
      Thread.yield();
    return false;
  }


  //完全释放锁,释放成功则返回，失败则将当前节点的状态设置成cancelled表示当前节点失效
  final int fullyRelease(Node node) {
    boolean failed = true;
    try {

      //获取当前锁重入的次数
      int savedState = getState();

      //释放锁
      if (release(savedState)) {
        failed = false;
        //释放成功
        return savedState;
      } else {
        throw new IllegalMonitorStateException();
      }
    } finally {
      //释放锁失败，则当前节点的状态变为cancelled（此时该节点在CONDITION队列中）
      if (failed)
        node.waitStatus = Node.CANCELLED;
    }
  }

  public final boolean owns(ConditionObject condition) {
    return condition.isOwnedBy(this);
  }

  public final boolean hasWaiters(ConditionObject condition) {
    if (!owns(condition))
      throw new IllegalArgumentException("Not owner");
    return condition.hasWaiters();
  }

  public final int getWaitQueueLength(ConditionObject condition) {
    if (!owns(condition))
      throw new IllegalArgumentException("Not owner");
    return condition.getWaitQueueLength();
  }

  public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
    if (!owns(condition))
      throw new IllegalArgumentException("Not owner");
    return condition.getWaitingThreads();
  }

















  public class ConditionObject implements Condition, java.io.Serializable {
    private static final long serialVersionUID = 1173984872572414699L;
    private transient Node firstWaiter;
    private transient Node lastWaiter;

    public ConditionObject() {
    }


    //加入到条件队列尾部
    private Node addConditionWaiter() {
      Node t = lastWaiter;
      //首先检查尾节点是否为cancelled状态的节点，如果是则调用unlinkCancelledWaiters删除CONDITION队列中所有cancelled状态的节点，
      // 不是，则直接将该新创建的节点添加到CONDITION队列的末尾。
      if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters();
        t = lastWaiter;
      }
      Node node = new Node(Thread.currentThread(), Node.CONDITION);
      if (t == null)
        firstWaiter = node;
      else
        t.nextWaiter = node;
      lastWaiter = node;
      return node;
    }

    // 对CONDITION队列中从首部开始的第一个CONDITION状态的节点，
    // 执行transferForSignal操作，将node从CONDITION队列中转换到CLH队列中，同时修改CLH队列中原先尾节点的状态
    private void doSignal(Node first) {
      do {
        //当前循环将first节点从CONDITION队列transfer到CLH队列
        //从CONDITION队列中删除first节点，调用transferForSignal将该节点添加到CLH队列中，成功则跳出循环
        if ((firstWaiter = first.nextWaiter) == null)
          lastWaiter = null;
        first.nextWaiter = null;
      } while (!transferForSignal(first) && (first = firstWaiter) != null);
    }

    private void doSignalAll(Node first) {
      lastWaiter = firstWaiter = null;
      do {
        Node next = first.nextWaiter;
        first.nextWaiter = null;
        transferForSignal(first);
        first = next;
      } while (first != null);
    }

    //遍历一次CONDITION链表，删除状态为CANCELLED的节点。
    private void unlinkCancelledWaiters() {
      Node t = firstWaiter;
      Node trail = null;
      while (t != null) {
        Node next = t.nextWaiter;
        if (t.waitStatus != Node.CONDITION) {
          t.nextWaiter = null;
          if (trail == null)
            firstWaiter = next;
          else
            trail.nextWaiter = next;
          if (next == null)
            lastWaiter = trail;
        } else
          trail = t;
        t = next;
      }
    }
    //对CONDITION队列中第一个CONDITION状态的节点（将该节点以及前面的CANCELLED状态的节点从CONDITION队列中出队）
    //将该节点从CONDITION队列中添加到CLH队列末尾，同时需要设置该节点在CLH队列中前驱节点的状态
    // （若前驱节点为cancelled状态或者给前驱节点执行CAS操作失败，则需要调用park操作在此处唤醒该线程，
    // 否则就是在CLH队列中设置前驱节点的signal状态成功，则不用在此处唤醒该线程，
    // 唤醒工作交给前驱节点，可以少进行一次park和unpark操作）
    public final void signal() {
      //判断锁是否被当前线程独占，如果不是，则当前线程不能signal其他线程
      if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
      Node first = firstWaiter;
      //CONDITION队列不为null，则doSignal方法将唤醒CONDITION队列中所有的节点线程
      if (first != null)
        doSignal(first);
    }

    public final void signalAll() {
      if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
      Node first = firstWaiter;
      if (first != null)
        doSignalAll(first);
    }

    public final void awaitUninterruptibly() {
      Node node = addConditionWaiter();
      int savedState = fullyRelease(node);
      boolean interrupted = false;
      while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        if (Thread.interrupted())
          interrupted = true;
      }
      if (acquireQueued(node, savedState) || interrupted)
        selfInterrupt();
    }

    private static final int REINTERRUPT = 1;
    private static final int THROW_IE = -1;

    // 检查条件等待的时候线程中断的情况
    private int checkInterruptWhileWaiting(Node node) {
        //中断请求在signal操作之前：THROW_IE
        //中断请求在signal操作之后：REINTERRUPT
        //期间没有收到任何中断请求：0
      return Thread.interrupted() ? (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
    }

    //在等待后发生中断，在此处根据interruptMode统一处理
    private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
      if (interruptMode == THROW_IE)
        throw new InterruptedException();
      else if (interruptMode == REINTERRUPT)
        selfInterrupt();
    }

      /**
       *    await只能在当前线程获取了锁之后调用。
       *  因此CLH队列和CONDITION队列的情况为：
       *  当前处于CLH队列队首的节点调用await方法，新new一个node,添加到CONDITION队列队尾，
       *  然后在CLH队列队首释放当前线程占有的锁，
       *  唤醒后继节点。当前线程以新node的形式在CONDITION队列中park，等待被唤醒。
       */

    //基本流程： 先将node加入condition队列，然后释放锁，挂起当前线程等待唤醒，唤醒后重新在CLH队列中调用acquireQueued获取锁。
      // （实现Object.wait方法的功能）
    public final void await() throws InterruptedException {

      //如果线程被中断则抛出异常
      if (Thread.interrupted())
        throw new InterruptedException();

      // step1:将该线程封装成node，新节点的状态为CONDITION，添加到队列尾部
      Node node = addConditionWaiter();

      //step2:尝试释放当前线程占有的锁，释放成功，则调用unparkSuccessor方法唤醒该节点在CLH队列中的后继节点。
      int savedState = fullyRelease(node);

      int interruptMode = 0;

      //step3:在while循环中调用isOnSyncQueue方法检测node是否再次transfer到CLH队列中
      //（其他线程调用signal或signalAll时，该线程可能从CONDITION队列中transfer到CLH队列中）
      // 如果没有，则park当前线程，等待唤醒，
      // 同时调用checkInterruptWhileWaiting检测当前线程在等待过程中是否发生中断，设置interruptMode的值来标志中断状态。
      // 如果检测到当前线程已经处于CLH队列中了，则跳出while循环。
      while (!isOnSyncQueue(node)) {
        //不在，则阻塞该节点
        LockSupport.park(this);

        //当前线程醒来后立马检查是否被中断, 如果是则代表结点取消条件等待, 此时需要将结点移出条件队列
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
          break;
      }


      //出了while循环，代表线程被唤醒，并且已经将该node从CONDITION队列transfer到了CLH队列中
      //step4:调用acquireQueued阻塞方法来在CLH队列中获取锁
      //step5:检查interruptMode的状态，在最后调用reportInterruptAfterWait统一抛出异常或发生中断。
      //acquireQueued在队列中获取锁，可能会阻塞当前线程，并且在上面while循环等待的过程中没有发生异常，
      //则修改interruptMode状态为REINTERRUPT
      if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
      //该节点调用transferAfterCancelledWait添加到CLH队列中的，
      // 此时该节点的nextWaiter不为null，需要调用unlinkCancelledWaiters将该节点从CONDITION队列中删除，该节点的状态为0
      if (node.nextWaiter != null)
        unlinkCancelledWaiters();

      //如果interruptMode不为0，则代表该线程在上面过程中发生了中断或者抛出了异常，则调用reportInterruptAfterWait方法在此处抛出异常
        if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
    }

    public final long awaitNanos(long nanosTimeout) throws InterruptedException {
      if (Thread.interrupted())
        throw new InterruptedException();
      Node node = addConditionWaiter();
      int savedState = fullyRelease(node);
      final long deadline = System.nanoTime() + nanosTimeout;
      int interruptMode = 0;
      while (!isOnSyncQueue(node)) {
        if (nanosTimeout <= 0L) {
          transferAfterCancelledWait(node);
          break;
        }
        if (nanosTimeout >= spinForTimeoutThreshold)
          LockSupport.parkNanos(this, nanosTimeout);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
          break;
        nanosTimeout = deadline - System.nanoTime();
      }
      if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
      if (node.nextWaiter != null)
        unlinkCancelledWaiters();
      if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
      return deadline - System.nanoTime();
    }

    public final boolean awaitUntil(Date deadline) throws InterruptedException {
      long abstime = deadline.getTime();
      if (Thread.interrupted())
        throw new InterruptedException();
      Node node = addConditionWaiter();
      int savedState = fullyRelease(node);
      boolean timedout = false;
      int interruptMode = 0;
      while (!isOnSyncQueue(node)) {
        if (System.currentTimeMillis() > abstime) {
          timedout = transferAfterCancelledWait(node);
          break;
        }
        LockSupport.parkUntil(this, abstime);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
          break;
      }
      if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
      if (node.nextWaiter != null)
        unlinkCancelledWaiters();
      if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
      return !timedout;
    }


    public final boolean await(long time, TimeUnit unit) throws InterruptedException {
      long nanosTimeout = unit.toNanos(time);
      if (Thread.interrupted())
        throw new InterruptedException();
      Node node = addConditionWaiter();
      int savedState = fullyRelease(node);
      final long deadline = System.nanoTime() + nanosTimeout;
      boolean timedout = false;
      int interruptMode = 0;
      while (!isOnSyncQueue(node)) {
        if (nanosTimeout <= 0L) {
          timedout = transferAfterCancelledWait(node);
          break;
        }
        if (nanosTimeout >= spinForTimeoutThreshold)
          LockSupport.parkNanos(this, nanosTimeout);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
          break;
        nanosTimeout = deadline - System.nanoTime();
      }
      if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
      if (node.nextWaiter != null)
        unlinkCancelledWaiters();
      if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
      return !timedout;
    }

    final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
      return sync == AbstractQueuedSynchronizer.this;
    }

    protected final boolean hasWaiters() {
      if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
      for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
        if (w.waitStatus == Node.CONDITION)
          return true;
      }
      return false;
    }

    protected final int getWaitQueueLength() {
      if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
      int n = 0;
      for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
        if (w.waitStatus == Node.CONDITION)
          ++n;
      }
      return n;
    }

    protected final Collection<Thread> getWaitingThreads() {
      if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
      ArrayList<Thread> list = new ArrayList<Thread>();
      for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
        if (w.waitStatus == Node.CONDITION) {
          Thread t = w.thread;
          if (t != null)
            list.add(t);
        }
      }
      return list;
    }
  }

  private static final Unsafe unsafe = Unsafe.getUnsafe();
  private static final long stateOffset;
  private static final long headOffset;
  private static final long tailOffset;
  private static final long waitStatusOffset;
  private static final long nextOffset;

  static {
    try {
      stateOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
      headOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
      tailOffset = unsafe
          .objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
      waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
      nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));

    } catch (Exception ex) {
      throw new Error(ex);
    }
  }

  private final boolean compareAndSetHead(Node update) {
    return unsafe.compareAndSwapObject(this, headOffset, null, update);
  }

  private final boolean compareAndSetTail(Node expect, Node update) {
    return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
  }

  private static final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
    return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
  }

  private static final boolean compareAndSetNext(Node node, Node expect, Node update) {
    return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
  }
}
