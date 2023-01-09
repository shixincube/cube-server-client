/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.client.util;

import cell.util.log.Logger;

import java.util.concurrent.ExecutorService;

/**
 * 异步任务触发器。
 */
public class Promise<T> {

    protected static ExecutorService sExecutor;

    protected PromiseHandler<T> promiseHandler;

    protected PromiseFuture<T> promiseFuture;

    protected FutureTask<T> futureTask;

    protected FutureTask<T> catchRejectTask;

    private FutureTask<Exception> catchExceptionTask;

    protected Promise(PromiseHandler<T> promiseHandler) {
        this.promiseHandler = promiseHandler;
        this.promiseFuture = new PromiseFuture<T>(this);
    }

    /**
     * 设置执行器。
     *
     * @param executor
     */
    public static void setExecutor(ExecutorService executor) {
        sExecutor = executor;
    }

    /**
     * 返回执行器实例。
     *
     * @return 返回执行器实例。
     */
    public static ExecutorService getExecutor() {
        return sExecutor;
    }

    /**
     * 创建异步任务执行器。
     *
     * @param promiseHandler 指定 Promise 实例。
     * @return
     */
    public static <T> Promise<T> create(PromiseHandler<T> promiseHandler) {
        Promise<T> future = new Promise<T>(promiseHandler);
        return future;
    }

    /**
     * 异步任务结束后的对应处理句柄。
     *
     * @param future
     * @return
     */
    public Promise<T> then(Future<T> future) {
        this.futureTask = new FutureTask(future);
        return this;
    }

    /**
     * 异步任务被拒绝时对应的处理句柄。
     *
     * @param future
     * @return
     */
    public Promise<T> catchReject(Future<T> future) {
        this.catchRejectTask = new FutureTask<T>(future);
        return this;
    }

    /**
     * 异步任务抛出异常时对应的处理句柄。
     *
     * @param future
     * @return
     */
    public Promise<T> catchException(Future<Exception> future) {
        this.catchExceptionTask = new FutureTask<Exception>(future);
        return this;
    }

    /**
     * 启动异步任务。
     */
    public void launch() {
        this.launch(0);
    }

    /**
     * 启动异步任务。
     *
     * @param delayInMills 启动延时。
     */
    public void launch(long delayInMills) {
        Runnable runnable = () -> {
            try {
                if (delayInMills > 0) {
                    Thread.sleep(delayInMills);
                }

                promiseHandler.emit(promiseFuture);
            } catch (Exception e) {
                Logger.w(Promise.class, "#launch", e);
                if (null != catchExceptionTask) {
                    catchExceptionTask.future.come(e);
                }
            }
        };

        if (null != sExecutor) {
            sExecutor.execute(runnable);
        }
        else {
            (new Thread(runnable)).start();
        }
    }

    protected void execute(Runnable runnable) {
        if (null != sExecutor) {
            sExecutor.execute(runnable);
        }
        else {
            (new Thread(runnable)).start();
        }
    }

    protected class FutureTask<T> {

        protected final Future<T> future;

        protected FutureTask(Future<T> future) {
            this.future = future;
        }
    }
}
