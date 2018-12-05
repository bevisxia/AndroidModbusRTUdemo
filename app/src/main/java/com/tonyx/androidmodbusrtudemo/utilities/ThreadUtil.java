package com.tonyx.androidmodbusrtudemo.utilities;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

public class ThreadUtil {
    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private static final Timer timer = new Timer();

    /**
     * 休眠当前(子)线程
     *
     * @param millis
     */
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    /**
     * 使用线程池执行方法
     *
     * @param callback
     */
    public static void execute(final Runnable callback) {
        executor.execute(callback);
    }

    /**
     * 使用线程池程延时执行方法
     *
     * @param callback
     * @param delay
     */
    public static TimerTask execute(Timer timer, final Runnable callback, long delay) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                callback.run();
            }
        };
        timer.schedule(task, delay);
        return task;
    }

    /**
     * 使用线程池程延时执行方法
     *
     * @param callback
     * @param delay
     */
    public static TimerTask execute(final Runnable callback, long delay) {

        return execute(timer, callback, delay);
    }

    /**
     * 启动新线程延时并周期执行方法
     *
     * @param callback
     * @param delay
     * @param period
     * @return
     */
    public static TimerTask execute(Timer timer, final Runnable callback, long delay, long period) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                callback.run();
            }
        };
        timer.schedule(task, delay, period);
        return task;
    }

    /**
     * 使用线程池延时并周期执行方法
     *
     * @param callback
     * @param delay
     * @param period
     */
    public static TimerTask execute(final Runnable callback, long delay, long period) {
        return execute(timer, callback, delay, period);
    }

    /**
     * 停止Timer定时或延时调度
     *
     * @param task
     */
    public static void cancel(TimerTask task) {
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ex) {
                // nothing
            }
        }
    }

    /**
     * 停止Timer定时或延时调度
     */
    public static void cancel() {
        try {
            timer.cancel();
        } catch (Exception ex) {
            // nothing
        }
    }

    public static Action execute(Timer timer, final Action action) {
        final TimerTask[] timerTasks = new TimerTask[1];
        timerTasks[0] = new TimerTask() {
            @Override
            public void run() {
                if (action.isRunning) {
                    if (action.timeout > 0 && System.currentTimeMillis() - action.startTime > action.timeout) {
                        action.isRunning = false;
                        action.lastError = new TimeoutException();
                    } else if (action.isCanceled) {
                        action.isRunning = false;
                        action.lastError = new CancellationException();
                    }
                }

                if (action.isRunning) {
                    try {
                        action.execute();
                    } catch (Exception ex) {
                        action.lastError = ex;
                    }

                    if (action.timeout > 0 && System.currentTimeMillis() - action.startTime > action.timeout) {
                        action.isRunning = false;
                        action.lastError = new TimeoutException();
                    } else if (action.isCanceled) {
                        action.isRunning = false;
                        action.lastError = new CancellationException();
                    } else if (action.period == 0) {
                        action.isRunning = false;
                    }
                }

                if (!action.isRunning) {
                    action.stopTime = System.currentTimeMillis();
                    try {
                        timerTasks[0].cancel();

                        if (action.lastError == null) {
                            action.success();
                        } else {
                            action.error(action.lastError);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (action.lastError != null) {
                    try {
                        action.error(action.lastError);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        action.startTime = System.currentTimeMillis() + action.delay;
        action.stopTime = 0;
        action.isRunning = true;
        action.isCanceled = false;

        if (action.period > 0) {
            timer.schedule(timerTasks[0], action.delay, action.period);
        } else {
            timer.schedule(timerTasks[0], action.delay);
        }

        return action;
    }

    public static Action execute(final Action action) {
        return execute(timer, action);
    }

    public abstract static class Action {
        private int delay;
        private int period;
        private int timeout;

        private long startTime;
        private long stopTime;
        private boolean isRunning;
        private boolean isCanceled;

        private Exception lastError;

        /**
         * @param delay   首次执行前延时时间
         * @param period  周期执行间隔时间
         * @param timeout 任务过期时间，若到期后任务仍旧在执行，则会回调TimeoutException
         */
        public Action(int delay, int period, int timeout) {
            this.delay = Math.max(delay, 0);
            this.period = Math.max(period, 0);
            this.timeout = Math.max(timeout, 0);
        }

        public Action(int delay, int period) {
            this(delay, period, 0);
        }

        public Action(int delay) {
            this(delay, 0, 0);
        }

        /**
         * 执行一个周期的动作
         */
        public abstract void execute() throws Exception;

        /**
         * 执行完成切无异常时回调
         */
        public abstract void success();

        /**
         * 执行过程中或执行完成发生异常时回调
         *
         * @param ex
         */
        public abstract void error(Exception ex);

        /**
         * 撤销循环任务，将回调CancellationException异常
         */
        public void cancel() {
            isCanceled = true;
        }

        /**
         * 提前完成循环任务
         */
        public void finish() {
            isRunning = false;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public long getDuration() {
            return (startTime > 0 && stopTime > 0) ? (stopTime - startTime) : 0;
        }

        public Exception getLastError() {
            return this.lastError;
        }
    }
}
