/*
 * @test /nodynamiccopyright/
 * @bug 8267910
 * @summary Javac fails to implicitly type abstract classes as implementing IdentityObject
 * @compile/fail/ref=ImplicitIdentityTypeTest.out -XDrawDiagnostics ImplicitIdentityTypeTest.java
 */

/* An abstract implicitly implements IdentityObject
        - if it declares a field,
        - an instance initializer,
        - a non-empty constructor,
        - a synchronized method,
        - has a concrete super,
        - is an inner class.
*/

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

    void check() {
        IdentityObject i;
        A a = null;
        B b = null;
        C c = null;
        D d = null;
        E e = null;
        F f = null;
        G g = null;
        H h = null;

        i = a; // Error.
        i = b; // Error.

        // The following assignments are kosher.
        i = c; i = d; i = e; i = f; i = g; i = h;
    }
}
