/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.management.MemoryPoolMXBean;

import sun.hotspot.WhiteBox;
import jdk.test.lib.Asserts;

/**
 * @test InlineTypeDensity
 * @summary Heap density test for InlineTypes
 * @library /test/lib
 * @compile -XDallowWithFieldOperator InlineTypeDensity.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xint -XX:FlatArrayElementMaxSize=-1 -XX:+UseCompressedOops
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                    -XX:+WhiteBoxAPI InlineTypeDensity
 * @run main/othervm -Xint -XX:FlatArrayElementMaxSize=-1 -XX:-UseCompressedOops
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                    -XX:+WhiteBoxAPI InlineTypeDensity
 * @run main/othervm -Xcomp -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI InlineTypeDensity
 * @run main/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:FlatArrayElementMaxSize=-1
 *                   -Xbootclasspath/a:. -XX:ForceNonTearable=*
 *                   -XX:+WhiteBoxAPI InlineTypeDensity
 */

public class InlineTypeDensity {

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final boolean VM_FLAG_FORCENONTEARABLE = WHITE_BOX.getStringVMFlag("ForceNonTearable").equals("*");

    public InlineTypeDensity() {
        if (WHITE_BOX.getIntxVMFlag("FlatArrayElementMaxSize") != -1) {
            throw new IllegalStateException("FlatArrayElementMaxSize should be -1");
        }
    }

    interface LocalDate {
        public int getYear();
        public short getMonth();
        public short getDay();
    }

    interface LocalTime {
        public byte getHour();
        public byte getMinute();
        public byte getSecond();
        public int getNano();
    }

    interface LocalDateTime extends LocalDate, LocalTime {}

    static final primitive class LocalDateValue implements LocalDate {
        final int   year;
        final short month;
        final short day;

        LocalDateValue() {
            year  = 0;
            month = 0;
            day   = 0;
        }

        public int   getYear()  { return year; }
        public short getMonth() { return month; }
        public short getDay()   { return day; }

        public static LocalDateValue create(int year, short month, short day) {
            LocalDateValue localDate = LocalDateValue.default;
            localDate = __WithField(localDate.year, year);
            localDate = __WithField(localDate.month, month);
            localDate = __WithField(localDate.day, day);
            return localDate;
        }
    }

    static final primitive class LocalTimeValue implements LocalTime {
        final byte hour;
        final byte minute;
        final byte second;
        final int nano;

        LocalTimeValue() {
            hour   = 0;
            minute = 0;
            second = 0;
            nano   = 0;
        }

        public byte getHour()   { return hour; }
        public byte getMinute() { return minute; }
        public byte getSecond() { return second; }
        public int getNano()    { return nano; }

        public static LocalTimeValue create(byte hour, byte minute, byte second, int nano) {
            LocalTimeValue localTime = LocalTimeValue.default;
            localTime = __WithField(localTime.hour, hour);
            localTime = __WithField(localTime.minute, minute);
            localTime = __WithField(localTime.second, second);
            localTime = __WithField(localTime.nano, nano);
            return localTime;
        }
    }

    static final primitive class LocalDateTimeValue implements LocalDateTime {
        final LocalDateValue date;
        final LocalTimeValue time;

        LocalDateTimeValue() {
            // Well this is a little weird...
            date = LocalDateValue.create(0, (short)0, (short)0);
            time = LocalTimeValue.create((byte)0, (byte)0, (byte)0, 0);
        }

        public int   getYear()  { return date.year; }
        public short getMonth() { return date.month; }
        public short getDay()   { return date.day; }

        public byte getHour()   { return time.hour; }
        public byte getMinute() { return time.minute; }
        public byte getSecond() { return time.second; }
        public int getNano()    { return time.nano; }

        public static LocalDateTimeValue create(LocalDateValue date, LocalTimeValue time) {
            LocalDateTimeValue localDateTime = LocalDateTimeValue.default;
            localDateTime = __WithField(localDateTime.date, date);
            localDateTime = __WithField(localDateTime.time, time);
            return localDateTime;
        }
    }

    static final class LocalDateClass implements LocalDate {
        final int   year;
        final short month;
        final short day;

        LocalDateClass(int year, short month, short day) {
            this.year  = year;
            this.month = month;
            this.day   = day;
        }

        public int   getYear()  { return year; }
        public short getMonth() { return month; }
        public short getDay()   { return day; }
    }

    static final class LocalTimeClass implements LocalTime {
        final byte hour;
        final byte minute;
        final byte second;
        final int nano;

        LocalTimeClass(byte hour, byte minute, byte second, int nano) {
            this.hour   = hour;
            this.minute = minute;
            this.second = second;
            this.nano   = nano;
        }

        public byte getHour()   { return hour; }
        public byte getMinute() { return minute; }
        public byte getSecond() { return second; }
        public int getNano()    { return nano; }
    }

    static final class LocalDateTimeClass implements LocalDateTime {
        final LocalDateClass date;
        final LocalTimeClass time;

        LocalDateTimeClass(LocalDateClass date, LocalTimeClass time) {
            this.date = date;
            this.time = time;
        }

        public LocalDateClass getDate() { return date; }
        public LocalTimeClass getTime() { return time; }

        public int   getYear()  { return date.year; }
        public short getMonth() { return date.month; }
        public short getDay()   { return date.day; }

        public byte getHour()   { return time.hour; }
        public byte getMinute() { return time.minute; }
        public byte getSecond() { return time.second; }
        public int getNano()    { return time.nano; }
    }

    public void ensureArraySizeWin() {
        int arrayLength = 1000;
        System.out.println("ensureArraySizeWin for length " + arrayLength);
        LocalDateTimeClass[] objectArray = new LocalDateTimeClass[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            objectArray[i] = new LocalDateTimeClass(new LocalDateClass(0, (short)0, (short)0),
                    new LocalTimeClass((byte)0, (byte)0, (byte)0, 0));
        }

        long objectArraySize = WHITE_BOX.getObjectSize(objectArray);
        System.out.println("Empty object array size: " + objectArraySize);
        objectArraySize += (arrayLength *
                (WHITE_BOX.getObjectSize(objectArray[0]) +
                        WHITE_BOX.getObjectSize(objectArray[0].getDate()) +
                        WHITE_BOX.getObjectSize(objectArray[0].getTime())));

        LocalDateTimeValue[] flatArray = new LocalDateTimeValue[arrayLength];
        // CMH: add "isFlatValueArray" to WhiteBox API, to ensure we are correctly account size

        long flatArraySize = WHITE_BOX.getObjectSize(flatArray);
        System.out.println("Object array and elements: " + objectArraySize + " versus Flat Array: " + flatArraySize);
        Asserts.assertLessThan(flatArraySize, objectArraySize, "Flat array accounts for more heap than object array + elements !");
    }

    static primitive class MyByte  { byte  v = 0; }
    static primitive class MyShort { short v = 0; }
    static primitive class MyInt   { int   v = 0; }
    static primitive class MyLong  { long  v = 0; }

    void assertArraySameSize(Object a, Object b, int nofElements) {
        long aSize = WHITE_BOX.getObjectSize(a);
        long bSize = WHITE_BOX.getObjectSize(b);
        Asserts.assertEquals(aSize, bSize,
            a + "(" + aSize + " bytes) not equivalent size " +
            b + "(" + bSize + " bytes)" +
            (nofElements >= 0 ? " (array of " + nofElements + " elements)" : ""));
    }

    void testByteArraySizesSame(int[] testSizes) {
        for (int testSize : testSizes) {
            byte[] ba = new byte[testSize];
            MyByte[] mba = new MyByte[testSize];
            assertArraySameSize(ba, mba, testSize);
        }
    }

    void testShortArraySizesSame(int[] testSizes) {
        for (int testSize : testSizes) {
            short[] sa = new short[testSize];
            MyShort[] msa = new MyShort[testSize];
            assertArraySameSize(sa, msa, testSize);
        }
    }

    void testIntArraySizesSame(int[] testSizes) {
        for (int testSize : testSizes) {
            int[] ia = new int[testSize];
            MyInt[] mia = new MyInt[testSize];
            assertArraySameSize(ia, mia, testSize);
        }
    }

    void testLongArraySizesSame(int[] testSizes) {
        for (int testSize : testSizes) {
            long[] la = new long[testSize];
            MyLong[] mla = new MyLong[testSize];
            assertArraySameSize(la, mla, testSize);
        }
    }

    public void testPrimitiveArraySizesSame() {
        int[] testSizes = new int[] { 0, 1, 2, 3, 4, 7, 10, 257 };
        testByteArraySizesSame(testSizes);
        testShortArraySizesSame(testSizes);
        testIntArraySizesSame(testSizes);
        testLongArraySizesSame(testSizes);
    }

    static primitive class bbValue { byte b = 0; byte b2 = 0;}
    static primitive class bsValue { byte b = 0; short s = 0;}
    static primitive class siValue { short s = 0; int i = 0;}
    static primitive class ssiValue { short s = 0; short s2 = 0; int i = 0;}
    static primitive class blValue { byte b = 0; long l = 0; }

    // Expect aligned array addressing to nearest pow2
    void testAlignedSize() {
        int testSize = 10;
        assertArraySameSize(new short[testSize], new bbValue[testSize], testSize);
        assertArraySameSize(new long[testSize], new siValue[testSize], testSize);
        assertArraySameSize(new long[testSize], new ssiValue[testSize], testSize);
        assertArraySameSize(new long[testSize*2], new blValue[testSize], testSize);
        assertArraySameSize(new int[testSize], new bsValue[testSize], testSize);
    }

    public void test() {
        ensureArraySizeWin();
        testPrimitiveArraySizesSame();
        if (!VM_FLAG_FORCENONTEARABLE) {
          testAlignedSize();
        }
    }

    public static void main(String[] args) {
        new InlineTypeDensity().test();
    }

}

