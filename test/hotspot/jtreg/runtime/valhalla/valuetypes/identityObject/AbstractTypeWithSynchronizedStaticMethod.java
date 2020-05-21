public abstract class AbstractTypeWithSynchronizedStaticMethod {
    static synchronized int getInt() {
        return 42;
    }
}
