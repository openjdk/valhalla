/*
 * @test  /nodynamiccopyright/
 * @bug 8328649
 * @summary Verify local classes in constructor prologues don't have enclosing instances
 * @compile/fail/ref=LocalClassCtorPrologue.out -XDrawDiagnostics LocalClassCtorPrologue.java
 * @enablePreview
 */

class LocalClassCtorPrologue {

    int x;

    LocalClassCtorPrologue() {
        class Local {
            {
                x++;                // this should fail
            }
        }
        super();
    }

    public class Inner {
        public Inner() {
            class Local {
                {
                    x++;            // this should work
                }
            };
            super();
        }
    }

    class Inner2 {
        Inner2() {
            class Sup {
                int x;
            }
            class Local extends Sup {
                Local() {
                    x = 42;   // x is declared in a super type, error
                    super();
                }
            }
            super();
        }
    }

    class Inner3 {
        Inner3() {
            class Local {
                class Foo {
                    int x;

                    Foo() {
                        x = 42;  // this is OK `x` belongs to Foo
                        super();
                    }
                }
            }
            super();
        }
    }
}
