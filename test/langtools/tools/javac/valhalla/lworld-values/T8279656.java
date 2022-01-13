/*
 * @test /nodynamiccopyright/
 * @bug 8279656
 * @summary Javac diagnostic compiler.err.primitive.class.must.not.implement.cloneable not valid anymore
 * @compile T8279656.java
 */

public class T8279656 {
    primitive class Primitive implements Cloneable {
    }
}
