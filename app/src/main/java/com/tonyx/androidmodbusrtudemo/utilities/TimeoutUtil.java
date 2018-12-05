package com.tonyx.androidmodbusrtudemo.utilities;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutUtil {
    /*
    执行一个方法，如果在限定时间内执行完成，返回true，否则返回false
     */
    public static boolean execute(final Runnable func, int timeout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Boolean> future = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                func.run();
                return true;
            }
        });
        executor.execute(future);
        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException e) {
            future.cancel(true);
            return true;
        } catch (ExecutionException e) {
            future.cancel(true);
            return true;
        } catch (TimeoutException e) {
            future.cancel(true);
            return false;
        } finally {
            executor.shutdown();
        }
    }
}
