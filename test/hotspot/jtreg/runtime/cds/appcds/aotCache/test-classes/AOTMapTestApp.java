import jdk.internal.misc.PreviewFeatures;
import jdk.internal.value.ValueClass;

public class AOTMapTestApp {

    public static value class Wrapper implements Comparable<Wrapper> {
        Integer i;

        public String toString() {
            return i.toString();
        }

        public int compareTo(Wrapper o) {
            return i - o.i;
        }

        Wrapper(int i) {
            this.i = new Integer(i);
        }
    }

    // Check that arrays of both migrated value
    // classes and custom value classes are archived
    public static class ArchivedData {
        Wrapper[] wrapperArray;
    }

    static ArchivedData archivedObjects;
    static {
        if (archivedObjects == null) {
            archivedObjects = new ArchivedData();
            archivedObjects.wrapperArray = new Wrapper[3];
            archivedObjects.wrapperArray[0] = new Wrapper(0);
            archivedObjects.wrapperArray[1] = new Wrapper(1);
            archivedObjects.wrapperArray[2] = new Wrapper(2);
        } else {
            System.out.println("Initialized from CDS");
            System.out.println("wrapperArray " + archivedObjects.wrapperArray);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Hello FlatAOTMapTestApp");
        Class.forName("Hello");
        if (PreviewFeatures.isEnabled() && !ValueClass.isFlatArray(archivedObjects.wrapperArray)) {
            throw new RuntimeException("Wrapper array should be flat");
        }
    }
}
