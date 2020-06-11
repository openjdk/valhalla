/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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


package org.graalvm.compiler.hotspot.replacements;

import static org.graalvm.compiler.core.common.GraalOptions.ImmutableCode;
import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import org.graalvm.compiler.core.common.type.AbstractObjectStamp;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.graph.spi.Canonicalizable;
import org.graalvm.compiler.graph.spi.CanonicalizerTool;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.AbstractStateSplit;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.DeoptBciSupplier;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.memory.SingleMemoryKill;
import org.graalvm.compiler.nodes.spi.Lowerable;
import jdk.internal.vm.compiler.word.LocationIdentity;

import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.JavaConstant;

@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public class IdentityHashCodeNode extends AbstractStateSplit implements Canonicalizable, Lowerable, SingleMemoryKill, DeoptBciSupplier {

    public static final NodeClass<IdentityHashCodeNode> TYPE = NodeClass.create(IdentityHashCodeNode.class);

    @Input ValueNode object;
    private int bci;

    public IdentityHashCodeNode(ValueNode object, int bci) {
        super(TYPE, StampFactory.forInteger(32));
        this.object = object;
        this.bci = bci;
    }

    @Override
    public LocationIdentity getKilledLocationIdentity() {
        return HotSpotReplacementsUtil.MARK_WORD_LOCATION;
    }

    public ValueNode object() {
        return object;
    }

    @Override
    public int bci() {
        return bci;
    }

    @Override
    public void setBci(int bci) {
        this.bci = bci;
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (object.isConstant()) {
            assert object.stamp(NodeView.DEFAULT) instanceof AbstractObjectStamp;
            JavaConstant c = (JavaConstant) object.asConstant();
            if (ImmutableCode.getValue(tool.getOptions())) {
                return this;
            }
            JavaConstant identityHashCode = null;
            if (c.isNull()) {
                identityHashCode = JavaConstant.forInt(0);
            } else {
                identityHashCode = JavaConstant.forInt(((HotSpotObjectConstant) c).getIdentityHashCode());
            }

            return new ConstantNode(identityHashCode, StampFactory.forConstant(identityHashCode));
        }
        return this;
    }
}
