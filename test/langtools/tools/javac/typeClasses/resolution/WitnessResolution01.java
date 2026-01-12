/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @modules jdk.compiler/com.sun.tools.javac.api
            jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JarTask
 * @run junit/othervm WitnessResolution01
 */

import java.util.Set;
import org.junit.jupiter.api.Test;

public class WitnessResolution01 extends WitnessResolutionTest {

    @Test
    public void checkLeftToRight() {
        findWitness("T<A, B, C>", Set.of("A", "B", "C")).ambiguous();
        findWitness("T<A, B, C>", Set.of("A", "B")).ambiguous();
        findWitness("T<A, B, C>", Set.of("B", "C")).ambiguous();
        findWitness("T<A, B, C>", Set.of("A", "C")).ambiguous();
        findWitness("T<A, B, C>", Set.of("A")).success("A");
        findWitness("T<A, B, C>", Set.of("B")).success("B");
        findWitness("T<A, B, C>", Set.of("C")).success("C");
        findWitness("T<A, B, C>", Set.of()).notFound();
    }

    @Test
    public void checkInnerToOuter() {
        findWitness("A<B<C<D<E>>>>", Set.of("A", "B", "C", "D", "E")).success("E");
        findWitness("A<B<C<D<E>>>>", Set.of("A", "B", "C", "D")).success("D");
        findWitness("A<B<C<D<E>>>>", Set.of("A", "B", "C")).success("C");
        findWitness("A<B<C<D<E>>>>", Set.of("A", "B")).success("B");
        findWitness("A<B<C<D<E>>>>", Set.of("A")).success("A");
        findWitness("A<B<C<D<E>>>>", Set.of()).notFound();
    }

    @Test
    public void checkDominance() {
        findWitness("T<A<B<C>>,D<E>>", Set.of("C", "E")).ambiguous(); // no dominance between E and C (unrelated)
        findWitness("T<A<B>,B<A>>", Set.of("A", "B")).ambiguous(); // no dominance between A and B (cycle)
        findWitness("T<A<C>,A<B>,C<B>>", Set.of("A", "B", "C")).success("B"); // B dominates everything
    }
}
