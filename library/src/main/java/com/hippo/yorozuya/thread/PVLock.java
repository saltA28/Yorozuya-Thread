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

public class PVLock {

    private int mCounter;

    public PVLock(int count) {
        mCounter = count;
    }

    /**
     * Obtain.
     */
    public synchronized void p() throws InterruptedException {
        while (true) {
            if (mCounter > 0) {
                mCounter--;
                break;
            } else {
                this.wait();
            }
        }
    }

    /**
     * Obtain without {@code InterruptedException}.
     * But it will throw {@code IllegalStateException}.
     */
    public synchronized void pp() {
        while (true) {
            if (mCounter > 0) {
                mCounter--;
                break;
            } else {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Can't interrupt in PVLock.pp()");
                }
            }
        }
    }

    /**
     * Release.
     */
    public synchronized void v() {
        mCounter++;
        if (mCounter > 0) {
            this.notify();
        }
    }

    /**
     * Just the same as {@link #v}.
     */
    public synchronized void vv() {
        v();
    }
}
