// from gee.cs.oswego.edu/home/jsr166/jsr166

package jsr166tests.jtreg.util.concurrent.ThreadPoolExecutor;

/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @bug 6523756
 * @summary Race task submission against shutdownNow
 * @author Martin Buchholz
 */

// For extra chances to detect shutdownNow vs. execute races,
// crank up the iterations to, say 1<<22 and
// add a call to Thread.yield() before the call to t.start()
// in ThreadPoolExecutor.addWorker.

import java.util.*;
import java.util.concurrent.*;

public class ShutdownNowExecuteRace {
    static volatile boolean quit = false;
    static volatile ThreadPoolExecutor pool = null;

    final static Runnable sleeper = new Runnable() { public void run() {
        final long ONE_HOUR = 1000L * 60L * 60L;
        try { Thread.sleep(ONE_HOUR); }
        catch (InterruptedException ie) {}
        catch (Throwable t) { unexpected(t); }}};

    static void realMain(String[] args) throws Throwable {
        final int iterations = 1 << 8;
        Thread thread = new Thread() { public void run() {
            while (! quit) {
                ThreadPoolExecutor pool = ShutdownNowExecuteRace.pool;
                if (pool != null)
                    try { pool.execute(sleeper); }
                    catch (RejectedExecutionException e) {/* OK */}
                    catch (Throwable t) { unexpected(t); }}}};
        thread.start();
        for (int i = 0; i < iterations; i++) {
            pool = new ThreadPoolExecutor(
                10, 10, 3L, TimeUnit.DAYS,
                new ArrayBlockingQueue<Runnable>(10));
            pool.shutdownNow();
            check(pool.awaitTermination(3L, TimeUnit.MINUTES));
        }
        quit = true;
        thread.join();
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    private static abstract class Fun {abstract void f() throws Throwable;}
    static void THROWS(Class<? extends Throwable> k, Fun... fs) {
        for (Fun f : fs)
            try { f.f(); fail("Expected " + k.getName() + " not thrown"); }
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
    private static abstract class CheckedThread extends Thread {
        abstract void realRun() throws Throwable;
        public void run() {
            try {realRun();} catch (Throwable t) {unexpected(t);}}}
}
