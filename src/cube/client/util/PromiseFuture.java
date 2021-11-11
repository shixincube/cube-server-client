/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

/**
 * 异步任务处理句柄。
 *
 * @param <T>
 */
public class PromiseFuture<T> {

    private Promise<T> promise;

    protected PromiseFuture(Promise<T> promise) {
        this.promise = promise;
    }

    /**
     * 当任务处理结束，需要通知 {@code Future} 进行 {@code come} 响应时调用该方法。
     *
     * @param data 任务预定的数据格式。
     */
    public void resolve(T data) {
        if (null != this.promise.futureTask) {
            this.execute(this.promise.futureTask, data);
        }
    }

    /**
     * 当任务处理出现异常，需要通知异常处理时调用该方法。
     *
     * @param data 任务预定的数据格式。
     */
    public void reject(T data) {
        if (null != this.promise.catchRejectTask) {
            this.execute(this.promise.catchRejectTask, data);
        }
    }

    /**
     * 当任务处理出现异常，需要通知异常处理时调用该方法。
     */
    public void reject() {
        if (null != this.promise.catchRejectTask) {
            this.execute(this.promise.catchRejectTask, null);
        }
    }

    private void execute(Promise.FutureTask task, T data) {
        this.promise.execute(new Runnable() {
            @Override
            public void run() {
                task.future.come(data);
            }
        });
    }
}
