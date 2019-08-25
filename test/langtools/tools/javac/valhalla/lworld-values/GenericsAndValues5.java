/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8230121
 * @summary Javac does not properly parse nullable projection types of parameterized inline types
 * @compile GenericsAndValues5.java
 * @run main/othervm GenericsAndValues5
 */

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.List;
import java.util.ArrayList;

inline class Optional<T> {
    private T value;

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> empty() {
        return (Optional<T>) Optional.default;
    }

    private Optional(T value) {
        this.value = value;
    }

    public static <T> Optional<T> of(T value) {
        if (value == null)
            return empty();
        return new Optional<T>(value);
    }

    public T get() {
        if (value == null)
            throw new NoSuchElementException("No value present");
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent())
            return empty();
        else
            return Optional.of(mapper.apply(value));
    }

    @Override
    public String toString() {
        return value != null ? String.format("Optional[%s]", value) : "Optional.empty";
    }
}

public final class GenericsAndValues5 {

   public static void main(String[] args) {

       List<Optional<Integer>?> opts = new ArrayList<>();
       for (int i=0; i < 6; i++) {
           Optional<Integer> oi = Optional.of(i);
           opts.add((Optional<Integer>?)oi);
           Optional<Integer> oe = Optional.empty();
           opts.add((Optional<Integer>?)oe);
       }

       Integer total = opts.stream()
           .map((Optional<Integer>? o) -> {
               Optional<Integer> op = (Optional<Integer>)o;
               return op.orElse(0);
           })
           .reduce(0, (x, y) -> x + y);

        if (total != 15) {
            throw new AssertionError("Incorrect output: " + total);
        }
   }
}
