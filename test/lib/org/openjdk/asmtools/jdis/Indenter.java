/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

public class Indenter {

    private int indentLength;

    public Indenter(int indentLength) {
        this.indentLength = indentLength;
    }

    public Indenter() {
        this.indentLength = Options.BODY_INDENT;
    }
    /**
     * Returns current indentation length.
     *
     * @return current indentation length.
     */
    public int indent() {
        return indentLength;
    }

    /**
     * Increases indentation length.
     *
     * @param indentLength new indent length
     *
     * @throws IllegalArgumentException if indentLength is negative.
     */
    public Indenter setIndent(int indentLength) {
        if (indentLength < 0) {
            throw new IllegalArgumentException("indent length can't be negative");
        }
        this.indentLength = indentLength;
        return this;
    }

    /**
     * Increases indentation length.
     *
     * @param increase length to increase by.
     *
     * @throws IllegalArgumentException if increase is negative.
     */
    public Indenter increaseIndent(int increase) {
        if (increase < 0) {
            throw new IllegalArgumentException("indent length can't be negative");
        }
        setIndent(indent() + increase);
        return this;
    }

    /**
     * Decreases indentation length.
     *
     * @param decrease length to decrease by
     *
     * @throws IllegalArgumentException if decrease is negative, or if decrease is greater than
     *                                  {@link #indent() current indentation length}.
     */
    public Indenter decreaseIndent(int decrease) {
        if (decrease < 0) {
            throw new IllegalArgumentException("decrease can't be negative");
        }
        setIndent(indent() - decrease);
        return this;
    }

    /**
     * Creates indent string based on current indent size.
     */
    public String getIndentString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent(); i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
