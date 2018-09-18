/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test ValueTypeDensity
 * @summary Heap density test for ValueTypes
 * @library /test/lib
 * @compile -XDenableValueTypes -XDallowWithFieldOperator ValueTypeDensity.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xint -XX:+EnableValhalla -XX:+ValueArrayFlatten
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                    -XX:+WhiteBoxAPI ValueTypeDensity
 * @run main/othervm -Xcomp -XX:+EnableValhalla -XX:+ValueArrayFlatten
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI ValueTypeDensity
 */

public class ValueTypeDensity {

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public ValueTypeDensity() {
        if (!WHITE_BOX.getBooleanVMFlag("ValueArrayFlatten")) {
            throw new IllegalStateException("ValueArrayFlatten false");
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

    static final __ByValue class LocalDateValue implements LocalDate {
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

    static final __ByValue class LocalTimeValue implements LocalTime {
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

    static final __ByValue class LocalDateTimeValue implements LocalDateTime {
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

        LocalDateTimeValue[] valueArray = new LocalDateTimeValue[arrayLength];
        // CMH: add "isFlatValueArray" to WhiteBox API, to ensure we are correctly account size

        long valueArraySize = WHITE_BOX.getObjectSize(valueArray);
        System.out.println("Object array and elements: " + objectArraySize + " versus Value Array: " + valueArraySize);
        Asserts.assertLessThan(valueArraySize, objectArraySize, "Value array accounts for more heap than object array + elements !");
    }

    public void test() {
        ensureArraySizeWin();
    }

    public static void main(String[] args) {
        new ValueTypeDensity().test();
    }

}

