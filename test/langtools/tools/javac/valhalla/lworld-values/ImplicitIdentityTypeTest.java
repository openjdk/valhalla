/*
 * @test /nodynamiccopyright/
 * @bug 8267910
 * @summary Javac fails to implicitly type abstract classes as having identity
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main ImplicitIdentityTypeTest
 */

/* An abstract implicitly implements IdentityObject
        - if it declares a field,
        - an instance initializer,
        - a non-empty constructor,
        - a synchronized method,
        - has a concrete super,
        - is an inner class.
*/

import com.sun.tools.classfile.*;

public class ImplicitIdentityTypeTest {

    static abstract class A {}  // Not an Identity class.
    static abstract class B { static { System.out.println(); } }  // Not an Identity class.


    // All abstract classes below are identity classes by implicit typing.

    abstract class C {}  // inner class implicitly implements IdentityObject
    static abstract class D { int f; }  // instance field lends it identity.
    static abstract class E { { System.out.println(); } }  // initializer lends it identity.
    static abstract class F { F(){ System.out.println(); }}  // non-empty ctor.
    static abstract class G { synchronized void f() {} }  // synchronized method.
    static abstract class H extends ImplicitIdentityTypeTest {}  // concrete super.

    public static void main(String [] args) throws Exception {

        ClassFile cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$A.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should not be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$B.class"));
        if (cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should not be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$C.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$D.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$E.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$F.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$G.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

        cls = ClassFile.read(ImplicitIdentityTypeTest.class.getResourceAsStream("ImplicitIdentityTypeTest$H.class"));
        if (!cls.access_flags.is(AccessFlags.ACC_IDENTITY))
            throw new Exception("ACC_IDENTITY flag should be set!");

    }
}
