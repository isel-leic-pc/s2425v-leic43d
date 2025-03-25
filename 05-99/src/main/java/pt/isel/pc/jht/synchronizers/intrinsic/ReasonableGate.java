package pt.isel.pc.jht.synchronizers.intrinsic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// A Java synchronizer using intrinsic monitors.
// In Java, every object has an intrinsic monitor for synchronization.
// These monitors are created lazily for any object that requires them.
// Each intrinsic monitor supports a single condition, and all
// monitor operations are built into the Java language.
// When a method is declared with the 'synchronized' keyword,
// the monitor's mutex is acquired at the beginning of the method
// and released upon exit. The base Object class provides
// wait, notify, and notifyAll to manage waiting conditions.
//
// For many years, intrinsic monitors were the only way to achieve
// synchronization in Java, so they are common in legacy code.
// They are still sometimes used today because they are lighter
// than ReentrantLock and its Conditions.
//
public class ReasonableGate {
    private boolean open;
    private int groupId = 0;

    public ReasonableGate() {
        this(false);
    }

    public ReasonableGate(boolean initialOpen) {
        this.open = initialOpen;
    }

    public synchronized void open() {
        if (!open) {
            open = true;
            notifyAll();
        }
    }

    public synchronized void close() {
        if (open) {
            open = false;
            groupId++;
        }
    }

    public synchronized void await() throws InterruptedException {
        if (!open) {
            int myGroupId = groupId;
            do {
                wait();
            } while (!open && myGroupId == groupId);
        }
    }

    // --- Test code ---
    private static final ReasonableGate gate = new ReasonableGate();
    private static final AtomicInteger done1 = new AtomicInteger(0);
    private static final AtomicInteger done2 = new AtomicInteger(0);
    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void action1() {
        try {
            gate.await();
            done1.incrementAndGet();
        } catch (InterruptedException e) {
            // Ignore interruptions.
            Thread.currentThread().interrupt();
        }
    }

    public static void action2() {
        try {
            latch.await();
            gate.await();
            done2.incrementAndGet();
        } catch (InterruptedException e) {
            // Ignore interruptions.
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create 100 threads for action1 as daemon threads.
        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(ReasonableGate::action1);
            t.setDaemon(true);
            t.start();
        }

        // Create 100 threads for action2 as daemon threads.
        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(ReasonableGate::action2);
            t.setDaemon(true);
            t.start();
        }

        // WARNING: do not use sleep for synchronization!
        TimeUnit.SECONDS.sleep(1);

        gate.open();
        latch.countDown();

        // WARNING: do not use sleep for synchronization!
        TimeUnit.MICROSECONDS.sleep(10);
        gate.close();

        // WARNING: do not use sleep for synchronization!
        TimeUnit.SECONDS.sleep(5);

        System.out.println("terminated threads 1: " + done1.get());
        System.out.println("terminated threads 2: " + done2.get());
    }
}
