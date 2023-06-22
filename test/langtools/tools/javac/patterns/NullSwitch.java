/*
 * @test /nodynamiccopyright/
 * @bug 8262891 8272776
 * @summary Check null handling for non-pattern switches.
 */

public class NullSwitch {

    public static void main(String[] args) {
        new NullSwitch().switchTest();
    }

    void switchTest() {
        assertEquals(0, matchingSwitch1(""));
        assertEquals(1, matchingSwitch1("a"));
        assertEquals(100, matchingSwitch1(0));
        assertEquals(-1, matchingSwitch1(null));
        assertEquals(-2, matchingSwitch1(0.0));
        assertEquals(0, matchingSwitch2(""));
        assertEquals(1, matchingSwitch2(null));
        assertEquals(1, matchingSwitch2(0.0));
        assertEquals(0, matchingSwitch3(""));
        assertEquals(1, matchingSwitch3("a"));
        assertEquals(100, matchingSwitch3(0));
        assertEquals(-1, matchingSwitch3(null));
        assertEquals(-2, matchingSwitch3(0.0));
        assertEquals(0, matchingSwitch5(""));
        assertEquals(1, matchingSwitch5("a"));
        assertEquals(100, matchingSwitch5(0));
        assertEquals(-1, matchingSwitch5(null));
        assertEquals(-2, matchingSwitch5(0.0));
        assertEquals(0, matchingSwitch6(""));
        assertEquals(1, matchingSwitch6(null));
        assertEquals(1, matchingSwitch6(0.0));
        assertEquals(0, matchingSwitch7(""));
        assertEquals(1, matchingSwitch7("a"));
        assertEquals(100, matchingSwitch7(0));
        assertEquals(-1, matchingSwitch7(null));
        assertEquals(-2, matchingSwitch7(0.0));
        assertEquals(0, matchingSwitch8(""));
        assertEquals(1, matchingSwitch8(null));
        assertEquals(1, matchingSwitch8(0.0));
        assertEquals(0, matchingSwitch9a(""));
        assertEquals(1, matchingSwitch9a(null));
        assertEquals(1, matchingSwitch9a(0.0));
        assertEquals(0, matchingSwitch10a(""));
        assertEquals(1, matchingSwitch10a(null));
        assertEquals(1, matchingSwitch10a(0.0));
        assertEquals(0, matchingSwitch9b(""));
        assertEquals(2, matchingSwitch9b(null));
        assertEquals(1, matchingSwitch9b(0.0));
        assertEquals(0, matchingSwitch10b(""));
        assertEquals(2, matchingSwitch10b(null));
        assertEquals(1, matchingSwitch10b(0.0));
        assertEquals(0, matchingSwitch11(""));
        assertEquals(2, matchingSwitch11(null));
        assertEquals(1, matchingSwitch11(0.0));
        assertEquals(0, matchingSwitch12(""));
        assertEquals(2, matchingSwitch12(null));
        assertEquals(1, matchingSwitch12(0.0));
        assertEquals(0, matchingSwitch13(""));
        assertEquals(1, matchingSwitch13(0.0));
        assertEquals(2, matchingSwitch13(null));

        // record classes and null
        assertEquals(1, matchingSwitch14(new R(null)));
        assertEquals(2, matchingSwitch15(new R(null)));
    }

    class Super {}
    class Sub extends Super {}
    record R(Super s) {}

    private int matchingSwitch14(R r) {
        return switch(r) {
            case R(Super s) -> 1;
            default -> 2;
        };
    }

    private int matchingSwitch15(R r) {
        return switch(r) {
            case R(Sub s) -> 1;
            default -> 2;
        };
    }

    private int matchingSwitch1(Object obj) {
        return switch (obj) {
            case String s -> s.length();
            case Integer i -> 100 + i;
            case null -> -1;
            default -> -2;
        };
    }

    private int matchingSwitch2(Object obj) {
        return switch (obj) {
            case String s -> 0;
            case null, default -> 1;
        };
    }

    private int matchingSwitch3(Object obj) {
        return switch (obj) {
            case String s -> s.length();
            case Integer i -> 100 + i;
            case null -> -1;
            default -> -2;
        };
    }

    private int matchingSwitch5(Object obj) {
        return switch (obj) {
            case String s: yield s.length();
            case null: yield -1;
            case Integer i: yield 100 + i;
            default: yield -2;
        };
    }

    private int matchingSwitch6(Object obj) {
        return switch (obj) {
            case String s: yield 0;
            case null: yield 1;
            default: yield 1;
        };
    }

    private int matchingSwitch7(Object obj) {
        return switch (obj) {
            case String s: yield s.length();
            case Integer i: yield 100 + i;
            case null: yield -1;
            default: yield -2;
        };
    }

    private int matchingSwitch8(Object obj) {
        return switch (obj) {
            case String s: yield 0;
            case null:
            default: yield 1;
        };
    }

    private int matchingSwitch9a(Object obj) {
        return switch (obj) {
            case String s: yield 0;
            case null: yield 1;
            case Object o: yield 1;
        };
    }

    private int matchingSwitch10a(Object obj) {
        switch (obj) {
            case String s: return 0;
            case null: return 1;
            case Object o: return 1;
        }
    }

    private int matchingSwitch9b(Object obj) {
        try {
            return switch (obj) {
                case String s: yield 0;
                case Object o: yield 1;
            };
        } catch (NullPointerException ex) {
            return 2;
        }
    }

    private int matchingSwitch10b(Object obj) {
        try {
            switch (obj) {
                case String s: return 0;
                case Object o: return 1;
            }
        } catch (NullPointerException ex) {
            return 2;
        }
    }

    private int matchingSwitch11(Object obj) {
        try {
            return switch (obj) {
                case String s: yield 0;
                default: yield 1;
            };
        } catch (NullPointerException ex) {
            return 2;
        }
    }

    private int matchingSwitch12(Object obj) {
        try {
            switch (obj) {
                case String s: return 0;
                default: return 1;
            }
        } catch (NullPointerException ex) {
            return 2;
        }
    }

    private int matchingSwitch13(Object obj) {
        try {
            switch (obj) {
                case String s: return 0;
                default: return 1;
            }
        } catch (NullPointerException ex) {
            return 2;
        }
    }

    static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected: " + expected + ", actual: " + actual);
        }
    }

}
