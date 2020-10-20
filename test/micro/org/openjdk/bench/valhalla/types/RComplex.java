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
package org.openjdk.bench.valhalla.types;

public class RComplex implements Complex {

    public final double re;
    public final double im;

    public RComplex(double re, double im) {
        this.re =  re;
        this.im =  im;
    }

    @Override
    public double re() { return re; }

    @Override
    public double im() { return im; }

    @Override
    public RComplex add(Complex that) {
        return new RComplex(this.re + that.re(), this.im + that.im());
    }

    public RComplex add(RComplex that) {
        return new RComplex(this.re + that.re, this.im + that.im);
    }

    @Override
    public RComplex mul(Complex that) {
        return new RComplex(this.re * that.re() - this.im * that.im(),
                           this.re * that.im() + this.im * that.re());
    }

    public RComplex mul(RComplex that) {
        return new RComplex(this.re * that.re - this.im * that.im,
                           this.re * that.im + this.im * that.re);
    }


}
