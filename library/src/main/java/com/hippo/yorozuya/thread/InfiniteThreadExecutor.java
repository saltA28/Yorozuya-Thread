/*
 * Copyright 2015-2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.yorozuya.thread;

import android.support.annotation.NonNull;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InfiniteThreadExecutor implements Executor {

    private long mKeepAliveMillis;
    private Queue<Runnable> mWorkQueue;
    private ThreadFactory mThreadFactory;

    private final AtomicInteger mThreadCount = new AtomicInteger();
    private final AtomicInteger mEmptyThreadCount = new AtomicInteger();

    private final Lock mThreadLock = new ReentrantLock();
    private final Object mWaitLock = new Object();

    public InfiniteThreadExecutor(long keepAliveMillis, Queue<Runnable> workQueue, ThreadFactory threadFactory) {
        mKeepAliveMillis = keepAliveMillis;
        mWorkQueue = workQueue;
        mThreadFactory = threadFactory;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        mThreadLock.lock();

        mWorkQueue.add(command);

        // Ensure thread
        if (mEmptyThreadCount.get() > 0) {
            mThreadCount.decrementAndGet();
            synchronized (mWaitLock) {
                mWaitLock.notify();
            }
        } else {
            mThreadFactory.newThread(new Task()).start();
        }

        mThreadLock.unlock();
    }

    public int getThreadCount() {
        return mThreadCount.get();
    }

    public class Task implements Runnable {

        @Override
        public void run() {
            mThreadCount.incrementAndGet();

            boolean end = false;
            mThreadLock.lock();
            for (;;) {
                Runnable command = mWorkQueue.poll();
                if (command == null) {
                    mEmptyThreadCount.decrementAndGet();
                    end = true;
                }

                mThreadLock.unlock();

                if (end) {
                    break;
                }

                try {
                    command.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mEmptyThreadCount.incrementAndGet();
                synchronized (mWaitLock) {
                    try {
                        mWaitLock.wait(mKeepAliveMillis);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
                mThreadLock.lock();
            }

            mThreadCount.decrementAndGet();
        }
    }
}
