/*
 * @test /nodynamiccopyright/
 * @enablePreview
 * @summary smoke test for negative parser test for null restrictions
 * @compile/fail/ref=NullRestrictionNegParserTest.out -XDrawDiagnostics NullRestrictionNegParserTest.java
 */

class NullRestrictionNegParserTest {
    static class Foo extends Bar! { } // not valid, superclass
    static class Foo implements Bar! { } // not valid, superinterface

    class Foo<X extends String!> { } // not valid, type-param bound
    <X extends String!> void foo() { } // not valid, type-param bound

    void foo() throws Error! { } // not valid, throws type

    void testNew() {
        new Foo!(); // bad, class creation expression
    }

    void testNewArray() {
        var z = new Foo![2]; // ok
        var y = new Foo![2]!; // bad, bang can't appear at the end
        var x = new Foo![2][][][]!; // bad, bang can't appear at the end
        var x = new Foo![2][]![][]!; // bad, bang can't appear in the middle or at the end
        var x = new Foo![2][1][1][1]!; // bad, bang can't appear at the end
        var x = new Foo![2][1]![1][1]!; // bad, bang can't appear at the end
        var x = new Bar!.Foo![2][1]![1][1]!; // bad, bang can't appear at the end, and bad qualifier
    }

    void testNewArrayWithInit() {
        var z = new Foo![] { null }; // ok
        var y = new Foo![]! { null }; // bad, bang can't appear at the end
        var x = new Foo![]![] { null }; // bad, bang can't appear in the middle
        var x = new Foo![]![]! { null }; // bad, bang can't appear in the middle or at the end
    }

    void testNoBangInQualifiedTypeNames() {
        a!.x x = ""; // bad, bang before '.'
        a!.b!.x x = ""; // bad, bang before '.'
        a!.m(); // bad, bang before '.'
        a.b!.m(); // bad, bang before '.'
    }

    void testNoBangInMrefQualifier() {
        Runnable r = Foo!<String>::m;
        Runnable r = Foo<String!>::m;
        Runnable r = Foo<String>!::m;
        Runnable r = Foo<String>![]::m;
        Runnable r = Foo<String>![]!::m;
        Runnable r = Foo<String>.Foo!<Integer>::m;
        Runnable r = Foo<String>.Foo<Integer!>::m;
        Runnable r = Foo<String>.Foo<Integer>!::m;
        Runnable r = Foo<String>.Foo!<Integer>::m;
        Runnable r = Foo<String>.Foo<Integer!>::m;
        Runnable r = Foo<String>.Foo<Integer>![]::m;
        Runnable r = Foo<String>.Foo<Integer>![]!::m;
    }

    void testInnerClassCreator() {
        encl.new Foo!();
        encl.new Foo<String!>();
        encl.new Foo<String>!();
    }

    void testQualifiedType() {
        A<String>!.B<Integer> a;
        A!<String>.B<Integer> a;
        A<String>.B!<Integer> a;
        A<String>.B<Integer!> a;
        A<String!>.B<Integer> a;
    }

    static class TestConstructor {
        TestConstructor!() { } // bad, no bang in constructor type
    }

    void testVarargs(Foo!... args) { // bad, bangs and varargs
        Foo foo = (A a, B!... bs) -> { }; // bad, bangs and varargs
    }
}
