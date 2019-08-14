/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8229537
 * @summary [lworld] Poor interaction between explicit lambda parameters and nullable projection types.
 * @compile -XDallowWithFieldOperator ExplicitLambdaWithNullableTypes2.java
 * @run main/othervm ExplicitLambdaWithNullableTypes2
 */

import java.util.List;
import java.util.ArrayList;
import java.util.function.*;
import java.util.NoSuchElementException;
import java.util.stream.*;

inline class OptionalInt {
    // private static final OptionalInt EMPTY = OptionalInt.default;

    private boolean isPresent = false;
    private int v = 0;

    public static OptionalInt empty() {
        return OptionalInt.default;
    }

    public static OptionalInt of(int val) {
        OptionalInt self = OptionalInt.default;
        self = __WithField(self.v, val);
        self = __WithField(self.isPresent, true);
        return self;
    }

    public int getAsInt() {
        if (!isPresent)
            throw new NoSuchElementException("No value present");

        return v;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public void ifPresent(IntConsumer consumer) {
        if (isPresent)
            consumer.accept(v);
    }

    public int orElse(int other) {
        return isPresent ? v : other;
    }
}

public final class ExplicitLambdaWithNullableTypes2 {

   public static void main(String[] args) {
       List<OptionalInt?> opts = new ArrayList<>();
       for (int i=0; i < 5; i++) {
           opts.add(OptionalInt.of(i));
           opts.add(OptionalInt.empty());
           opts.add(null);
       }

       Stream<OptionalInt?> soi = opts.stream();
       ToIntFunction<OptionalInt?> f = (OptionalInt? o) -> {
            if (o == null) return 0;
            OptionalInt op = (OptionalInt)o;
            return op.orElse(0);
       };

       IntStream sint = soi.mapToInt(f);
       int total = sint.reduce(0, (x, y) -> x + y);

       if (total != 10) {
            throw new AssertionError("Incorrect output: " + total);
       }
   }
}
