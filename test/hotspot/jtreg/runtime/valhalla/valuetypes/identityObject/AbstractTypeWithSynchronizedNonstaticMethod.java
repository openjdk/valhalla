public abstract class AbstractTypeWithSynchronizedNonstaticMethod {
    synchronized int getInt() {
        return 42;
    }
}
