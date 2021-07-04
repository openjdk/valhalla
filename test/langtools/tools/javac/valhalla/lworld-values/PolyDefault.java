/*
 * @test /nodynamiccopyright/
 * @bug 8210906
 * @summary [lworld] default value creation should not impose raw types on users.
 * @compile/fail/ref=PolyDefault.out -Xlint:all -Werror -XDrawDiagnostics -XDdev PolyDefault.java
 */
import java.util.concurrent.Callable;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;

public primitive class PolyDefault<E> implements Callable<E> {
    E value;
    protected PolyDefault() { this.value = E.default; }
    PolyDefault(E value) { this.value = value; }

    @Override
    public E call() throws Exception {
        return value;
    }

    @FunctionalInterface
    interface PolyProducer {
        PolyDefault<String> produce();
    }

    interface Foo<X extends Number> extends List<X> {
    }

    public static <T extends Boolean> String overload(List<T> nums) {
        return "";
    }

    public static <T extends Number> T overload(Collection<T> nums) {
        return T.default;
    }

    public static void main(String [] args) throws Exception {

        List<Integer> il = LinkedList.default;
        Integer i0 = il.get(0);

        var a = overload(Foo.default);
        var b = a.intValue();

        // Things which should just work
        PolyDefault<LinkedList<Long>> foo = PolyDefault.default; // Poly expression
        LinkedList<Long> c = foo.call(); // This should be fine, inferred above
        var genericDefault1 = PolyDefault<LinkedList<Long>>.default;

        PolyProducer genericDefault = () -> PolyDefault.default;

        // Problems
        List<String> boing = new PolyDefault<>(); // Error: Can't make a PolyDefault into a list
        List<String> boom = PolyDefault.default; // Error: Can't make a PolyDefault into a list
        List<String> tschak = Foo.default; // Error: Can make a Foo into a list, but then must be List<? extends Number>
    }
}
