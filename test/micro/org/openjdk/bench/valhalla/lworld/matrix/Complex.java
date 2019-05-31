
package org.openjdk.bench.valhalla.lworld.matrix;

value public class Complex {

    static class H {
        public static Complex ZERO = new Complex(0, 0);
    }

    private final double re;
    private final double im;

    Complex(double re, double im) {
        this.re =  re;
        this.im =  im;
    }

    public double re() { return re; }

    public double im() { return im; }

    public Complex add(Complex that) {
        return new Complex(this.re + that.re, this.im + that.im);
    }

    public Complex mul(Complex that) {
        return new Complex(this.re * that.re - this.im * that.im,
                           this.re * that.im + this.im * that.re);
    }

}
