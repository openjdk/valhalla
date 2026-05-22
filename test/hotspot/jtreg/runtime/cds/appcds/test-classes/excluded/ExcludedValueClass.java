package excluded;

import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

// This value class will be placed in a signed JAR causing it to be excluded
// during a CDS dump. If a field of this type is flattened or null-restricted,
// the holder classes must be excluded from the archive as well.
public value class ExcludedValueClass {
    int i;

    public ExcludedValueClass() {
        i = 0;
        System.out.println("Hello from ExcludedValueClass!");
    }
}
