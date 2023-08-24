/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.acmp.extended;

import org.openjdk.bench.valhalla.types.R64long;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/*
 * to provide proper measurement the benchmark have to be executed in two modes:
 *  -wm INDI
 *  -wm BULK
 */
@Fork(1)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class SimpleClassTest {
    public value class MyValue {
		int a;
		int b;
		char c;
		double d;
		boolean e;
		Point p1;
		public MyValue(Point p1){
		    this.a = 0;
		    this.b = 1;
		    this.c = 'b';
		    this.d = 9.81;
		    this.e = true;
		    this.p1 = p1;
		}
	}

	public value class Point{
		int x;
		int y;
		public Point(int x, int y){
		    this.x = x;
		    this.y = y;
		}
	}

	public boolean test_inlined(MyValue a, MyValue b) {
		return a == b;
	}

	@CompilerControl(CompilerControl.Mode.DONT_INLINE)
	public boolean test_not_inlined(MyValue a, MyValue b) {
		return a == b;
	}

	static int counter = 0;
	MyValue a = new MyValue(new Point(1, 0));
	MyValue b = new MyValue(new Point(1, 0));
	MyValue c = new MyValue(new Point(0, 0));

    @Benchmark
    public boolean cmp_inlined() {
		return test_inlined(a, (counter++ % 2) == 0 ? b : c);
	}

	@Benchmark
    public boolean cmp_not_inlined() {
        return test_not_inlined(a, (counter++ % 2) == 0 ? b : c);
    }

	@Benchmark
    public boolean cmp_foldable(){
		MyValue v1 = new MyValue(new Point(counter++ % 2, 0));
		MyValue v2 = new MyValue(new Point(1, 0));
		return v1 == v2;
    }
}