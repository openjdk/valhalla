/*
 * @test /nodynamiccopyright/
 * @bug 8210906
 * @summary [lworld] default value creation should not impose raw types on users.
 * @compile/fail/ref=UncheckedDefault.out -Xlint:all -Werror -XDrawDiagnostics -XDdev UncheckedDefault.java
 */

public primitive class UncheckedDefault<E> {
    E value;
    UncheckedDefault(E value) { this.value = value; }
    public static void main(String [] args) {
        UncheckedDefault<String> foo = UncheckedDefault.default;
    }

    public E makeDefault() {
        E e = E.default;
        return e;
    }

}
