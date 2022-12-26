/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.internal.foreign.abi;

/**
 * Carrier class used to communicate with the VM
 *
 * It is particularly low-level since the VM will be accessing these fields directly
 */
public class ABIDescriptor {
    final Architecture arch;

    public final VMStorage[][] inputStorage;
    public final VMStorage[][] outputStorage;

    final VMStorage[][] volatileStorage;

    final int stackAlignment;
    final int shadowSpace;

    final VMStorage scratch1;
    final VMStorage scratch2;

    final VMStorage targetAddrStorage;
    final VMStorage retBufAddrStorage;
    final VMStorage capturedStateStorage;

    public ABIDescriptor(Architecture arch, VMStorage[][] inputStorage, VMStorage[][] outputStorage,
                         VMStorage[][] volatileStorage, int stackAlignment, int shadowSpace,
                         VMStorage scratch1, VMStorage scratch2,
                         VMStorage targetAddrStorage, VMStorage retBufAddrStorage,
                         VMStorage capturedStateStorage) {
        this.arch = arch;
        this.inputStorage = inputStorage;
        this.outputStorage = outputStorage;
        this.volatileStorage = volatileStorage;
        this.stackAlignment = stackAlignment;
        this.shadowSpace = shadowSpace;
        this.scratch1 = scratch1;
        this.scratch2 = scratch2;
        this.targetAddrStorage = targetAddrStorage;
        this.retBufAddrStorage = retBufAddrStorage;
        this.capturedStateStorage = capturedStateStorage;
    }

    public VMStorage targetAddrStorage() {
        return targetAddrStorage;
    }

    public VMStorage retBufAddrStorage() {
        return retBufAddrStorage;
    }

    public VMStorage capturedStateStorage() {
        return capturedStateStorage;
    }
}
