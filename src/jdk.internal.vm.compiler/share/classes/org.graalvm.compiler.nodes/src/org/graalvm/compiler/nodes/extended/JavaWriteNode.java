/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package org.graalvm.compiler.nodes.extended;

import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.StateSplit;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.memory.AbstractWriteNode;
import org.graalvm.compiler.nodes.memory.MemoryAccess;
import org.graalvm.compiler.nodes.memory.SingleMemoryKill;
import org.graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.compiler.nodes.spi.Lowerable;
import jdk.internal.vm.compiler.word.LocationIdentity;

import jdk.vm.ci.meta.JavaKind;

/**
 * Write a raw memory location according to Java field or array write semantics. It will perform
 * write barriers, implicit conversions and optionally oop compression.
 */
@NodeInfo(nameTemplate = "JavaWrite#{p#location/s}")
public final class JavaWriteNode extends AbstractWriteNode implements Lowerable, StateSplit, MemoryAccess, SingleMemoryKill {

    public static final NodeClass<JavaWriteNode> TYPE = NodeClass.create(JavaWriteNode.class);
    protected final JavaKind writeKind;
    protected final boolean compressible;
    protected final boolean hasSideEffect;

    public JavaWriteNode(JavaKind writeKind, AddressNode address, LocationIdentity location, ValueNode value, BarrierType barrierType, boolean compressible, boolean hasSideEffect) {
        super(TYPE, address, location, value, barrierType);
        this.writeKind = writeKind;
        this.compressible = compressible;
        this.hasSideEffect = hasSideEffect;
    }

    public JavaWriteNode(JavaKind writeKind, AddressNode address, LocationIdentity location, ValueNode value, BarrierType barrierType, boolean compressible) {
        super(TYPE, address, location, value, barrierType);
        this.writeKind = writeKind;
        this.compressible = compressible;
        this.hasSideEffect = true;
    }

    @Override
    public boolean canNullCheck() {
        return true;
    }

    public JavaKind getWriteKind() {
        return writeKind;
    }

    public boolean isCompressible() {
        return compressible;
    }

    @Override
    public LocationIdentity getKilledLocationIdentity() {
        return getLocationIdentity();
    }

    @Override
    public Stamp getAccessStamp(NodeView view) {
        return value().stamp(view);
    }

    @Override
    public boolean hasSideEffect() {
        return hasSideEffect;
    }
}
