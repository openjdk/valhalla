
public class Test { 

    static value class MyNiceValue {
        int x;
        
        public MyNiceValue(int x) {
            this.x = x;
        }
    }

    public static boolean test(Object a, Object b) {
        return a == b;
    }

    public static void main(String[] args) {
        MyNiceValue val1 = new MyNiceValue(42);
        MyNiceValue val2 = new MyNiceValue(42);
        
        for (int i = 0; i < 100_000; ++i) {
            test(val1, val1);
            test(val1, val2);
        }
    }
}
