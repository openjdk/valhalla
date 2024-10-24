/*
 * @test /nodynamiccopyright/
 * @summary Xxx.
 * @enablePreview
 *
 * @compile/fail/ref=MustUseValueTest.out -XDrawDiagnostics MustUseValueTest.java
 */
public value final class MustUseValueTest implements java.io.Serializable {

    public static void main(String[] args) {
        var array = new String[] { "A", "B", "C",  };

        Cursor c = new Cursor(array, 0, 3);
        c.next(); // bad code - ERROR
        c = c.next(); // good code - NOT ERROR

        System.out.println("Should be B: " + c.current());
    }

    value record Cursor<T>(T[] array, int position, int length) {
        Cursor {
            if (position < 0 || position >= length) {
                throw new IndexOutOfBoundsException("index %d outside array bounds [0;%d[".formatted(position, length));
            }
        }

        T current() {
            return array[position];
        }

        @MustUse
        Cursor<T> next() {
            return new Cursor(array, position+1, length);
        }
    }
}
