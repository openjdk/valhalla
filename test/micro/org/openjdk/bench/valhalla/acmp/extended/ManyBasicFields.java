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

import org.openjdk.jmh.annotations.*;

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
public class ManyBasicFields {
	public value class ManyObjectFields1 {
		int field1;
		int field2;
		int field3;

		public ManyObjectFields1(int field1, int field2, int field3) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
		}
	}

	ManyObjectFields1 obj1_1 = new ManyObjectFields1(1, 2, 3);
	ManyObjectFields1 obj1_2 = new ManyObjectFields1(1, 2, 3);
	ManyObjectFields1 obj1_3 = new ManyObjectFields1(0, 2, 3);

	public boolean test_03(ManyObjectFields1 a, ManyObjectFields1 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_03_fields() {return test_03(obj1_1, obj1_2);}

	@Benchmark
	public boolean cmp_not_eq_03_fields() {return test_03(obj1_1, obj1_3);}


	public value class ManyObjectFields2 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;

		public ManyObjectFields2(int field1, int field2, int field3, int field4, int field5, int field6) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
		}
	}

	ManyObjectFields2 obj2_1 = new ManyObjectFields2(1, 2, 3, 4, 5, 6);
	ManyObjectFields2 obj2_2 = new ManyObjectFields2(1, 2, 3, 4, 5, 6);
	ManyObjectFields2 obj2_3 = new ManyObjectFields2(0, 2, 3, 4, 5, 6);

	public boolean test_06(ManyObjectFields2 a, ManyObjectFields2 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_06_fields() {return test_06(obj2_1, obj2_2);}

	@Benchmark
	public boolean cmp_not_eq_06_fields() {return test_06(obj2_1, obj2_3);}


	public value class ManyObjectFields3 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;

		public ManyObjectFields3(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
		}
	}

	ManyObjectFields3 obj3_1 = new ManyObjectFields3(1, 2, 3, 4, 5, 6, 7, 8, 9);
	ManyObjectFields3 obj3_2 = new ManyObjectFields3(1, 2, 3, 4, 5, 6, 7, 8, 9);
	ManyObjectFields3 obj3_3 = new ManyObjectFields3(0, 2, 3, 4, 5, 6, 7, 8, 9);

	public boolean test_09(ManyObjectFields3 a, ManyObjectFields3 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_09_fields() {return test_09(obj3_1, obj3_2);}

	@Benchmark
	public boolean cmp_not_eq_09_fields() {return test_09(obj3_1, obj3_3);}


	public value class ManyObjectFields4 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;
		int field10;
		int field11;
		int field12;

		public ManyObjectFields4(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9, int field10, int field11, int field12) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
			this.field10 = field10;
			this.field11 = field11;
			this.field12 = field12;
		}
	}

	ManyObjectFields4 obj4_1 = new ManyObjectFields4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	ManyObjectFields4 obj4_2 = new ManyObjectFields4(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	ManyObjectFields4 obj4_3 = new ManyObjectFields4(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

	public boolean test_12(ManyObjectFields4 a, ManyObjectFields4 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_12_fields() {return test_12(obj4_1, obj4_2);}

	@Benchmark
	public boolean cmp_not_eq_12_fields() {return test_12(obj4_1, obj4_3);}


	public value class ManyObjectFields5 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;
		int field10;
		int field11;
		int field12;
		int field13;
		int field14;
		int field15;

		public ManyObjectFields5(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9, int field10, int field11, int field12, int field13, int field14, int field15) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
			this.field10 = field10;
			this.field11 = field11;
			this.field12 = field12;
			this.field13 = field13;
			this.field14 = field14;
			this.field15 = field15;
		}
	}

	ManyObjectFields5 obj5_1 = new ManyObjectFields5(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
	ManyObjectFields5 obj5_2 = new ManyObjectFields5(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
	ManyObjectFields5 obj5_3 = new ManyObjectFields5(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);

	public boolean test_15(ManyObjectFields5 a, ManyObjectFields5 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_15_fields() {return test_15(obj5_1, obj5_2);}

	@Benchmark
	public boolean cmp_not_eq_15_fields() {return test_15(obj5_1, obj5_3);}


	public value class ManyObjectFields6 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;
		int field10;
		int field11;
		int field12;
		int field13;
		int field14;
		int field15;
		int field16;
		int field17;
		int field18;

		public ManyObjectFields6(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9, int field10, int field11, int field12, int field13, int field14, int field15, int field16, int field17, int field18) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
			this.field10 = field10;
			this.field11 = field11;
			this.field12 = field12;
			this.field13 = field13;
			this.field14 = field14;
			this.field15 = field15;
			this.field16 = field16;
			this.field17 = field17;
			this.field18 = field18;
		}
	}

	ManyObjectFields6 obj6_1 = new ManyObjectFields6(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);
	ManyObjectFields6 obj6_2 = new ManyObjectFields6(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);
	ManyObjectFields6 obj6_3 = new ManyObjectFields6(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);

	public boolean test_18(ManyObjectFields6 a, ManyObjectFields6 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_18_fields() {return test_18(obj6_1, obj6_2);}

	@Benchmark
	public boolean cmp_not_eq_18_fields() {return test_18(obj6_1, obj6_3);}


	public value class ManyObjectFields7 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;
		int field10;
		int field11;
		int field12;
		int field13;
		int field14;
		int field15;
		int field16;
		int field17;
		int field18;
		int field19;
		int field20;
		int field21;

		public ManyObjectFields7(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9, int field10, int field11, int field12, int field13, int field14, int field15, int field16, int field17, int field18, int field19, int field20, int field21) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
			this.field10 = field10;
			this.field11 = field11;
			this.field12 = field12;
			this.field13 = field13;
			this.field14 = field14;
			this.field15 = field15;
			this.field16 = field16;
			this.field17 = field17;
			this.field18 = field18;
			this.field19 = field19;
			this.field20 = field20;
			this.field21 = field21;
		}
	}

	ManyObjectFields7 obj7_1 = new ManyObjectFields7(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
	ManyObjectFields7 obj7_2 = new ManyObjectFields7(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
	ManyObjectFields7 obj7_3 = new ManyObjectFields7(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);

	public boolean test_21(ManyObjectFields7 a, ManyObjectFields7 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_21_fields() {return test_21(obj7_1, obj7_2);}

	@Benchmark
	public boolean cmp_not_eq_21_fields() {return test_21(obj7_1, obj7_3);}


	public value class ManyObjectFields8 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;
		int field10;
		int field11;
		int field12;
		int field13;
		int field14;
		int field15;
		int field16;
		int field17;
		int field18;
		int field19;
		int field20;
		int field21;
		int field22;
		int field23;
		int field24;

		public ManyObjectFields8(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9, int field10, int field11, int field12, int field13, int field14, int field15, int field16, int field17, int field18, int field19, int field20, int field21, int field22, int field23, int field24) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
			this.field10 = field10;
			this.field11 = field11;
			this.field12 = field12;
			this.field13 = field13;
			this.field14 = field14;
			this.field15 = field15;
			this.field16 = field16;
			this.field17 = field17;
			this.field18 = field18;
			this.field19 = field19;
			this.field20 = field20;
			this.field21 = field21;
			this.field22 = field22;
			this.field23 = field23;
			this.field24 = field24;
		}
	}

	ManyObjectFields8 obj8_1 = new ManyObjectFields8(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24);
	ManyObjectFields8 obj8_2 = new ManyObjectFields8(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24);
	ManyObjectFields8 obj8_3 = new ManyObjectFields8(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24);

	public boolean test_24(ManyObjectFields8 a, ManyObjectFields8 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_24_fields() {return test_24(obj8_1, obj8_2);}

	@Benchmark
	public boolean cmp_not_eq_24_fields() {return test_24(obj8_1, obj8_3);}


	public value class ManyObjectFields9 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;
		int field10;
		int field11;
		int field12;
		int field13;
		int field14;
		int field15;
		int field16;
		int field17;
		int field18;
		int field19;
		int field20;
		int field21;
		int field22;
		int field23;
		int field24;
		int field25;
		int field26;
		int field27;

		public ManyObjectFields9(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9, int field10, int field11, int field12, int field13, int field14, int field15, int field16, int field17, int field18, int field19, int field20, int field21, int field22, int field23, int field24, int field25, int field26, int field27) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
			this.field10 = field10;
			this.field11 = field11;
			this.field12 = field12;
			this.field13 = field13;
			this.field14 = field14;
			this.field15 = field15;
			this.field16 = field16;
			this.field17 = field17;
			this.field18 = field18;
			this.field19 = field19;
			this.field20 = field20;
			this.field21 = field21;
			this.field22 = field22;
			this.field23 = field23;
			this.field24 = field24;
			this.field25 = field25;
			this.field26 = field26;
			this.field27 = field27;
		}
	}

	ManyObjectFields9 obj9_1 = new ManyObjectFields9(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
	ManyObjectFields9 obj9_2 = new ManyObjectFields9(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
	ManyObjectFields9 obj9_3 = new ManyObjectFields9(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);

	public boolean test_27(ManyObjectFields9 a, ManyObjectFields9 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_27_fields() {return test_27(obj9_1, obj9_2);}

	@Benchmark
	public boolean cmp_not_eq_27_fields() {return test_27(obj9_1, obj9_3);}


	public value class ManyObjectFields10 {
		int field1;
		int field2;
		int field3;
		int field4;
		int field5;
		int field6;
		int field7;
		int field8;
		int field9;
		int field10;
		int field11;
		int field12;
		int field13;
		int field14;
		int field15;
		int field16;
		int field17;
		int field18;
		int field19;
		int field20;
		int field21;
		int field22;
		int field23;
		int field24;
		int field25;
		int field26;
		int field27;
		int field28;
		int field29;
		int field30;

		public ManyObjectFields10(int field1, int field2, int field3, int field4, int field5, int field6, int field7, int field8, int field9, int field10, int field11, int field12, int field13, int field14, int field15, int field16, int field17, int field18, int field19, int field20, int field21, int field22, int field23, int field24, int field25, int field26, int field27, int field28, int field29, int field30) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
			this.field6 = field6;
			this.field7 = field7;
			this.field8 = field8;
			this.field9 = field9;
			this.field10 = field10;
			this.field11 = field11;
			this.field12 = field12;
			this.field13 = field13;
			this.field14 = field14;
			this.field15 = field15;
			this.field16 = field16;
			this.field17 = field17;
			this.field18 = field18;
			this.field19 = field19;
			this.field20 = field20;
			this.field21 = field21;
			this.field22 = field22;
			this.field23 = field23;
			this.field24 = field24;
			this.field25 = field25;
			this.field26 = field26;
			this.field27 = field27;
			this.field28 = field28;
			this.field29 = field29;
			this.field30 = field30;
		}
	}

	ManyObjectFields10 obj10_1 = new ManyObjectFields10(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30);
	ManyObjectFields10 obj10_2 = new ManyObjectFields10(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30);
	ManyObjectFields10 obj10_3 = new ManyObjectFields10(0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30);

	public boolean test_30(ManyObjectFields10 a, ManyObjectFields10 b) {return a == b;}

	@Benchmark
	public boolean cmp_eq_30_fields() {return test_30(obj10_1, obj10_2);}

	@Benchmark
	public boolean cmp_not_eq_30_fields() {return test_30(obj10_1, obj10_3);}
}


