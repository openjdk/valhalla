package jdk.internal.jimage;

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Accesses the underlying resource entries in a jimage file.
 *
 * <p>This API is designed only for use by the jlink classes, which manipulate
 * jimage files directly. For inspection of runtime resources, it is vital that
 * {@code previewMode} is correctly observed, making this API unsuitable.
 *
 * <p>This API ignores the {@code previewMode} of the {@link ImageReader} from
 * which it is obtained, and returns an unmapped view of entries (e.g. allowing
 * for direct access of resources in the {@code META-INF/preview/...} namespace).
 *
 * <p>It disallows access to resource directories (i.e. {@code "/modules/..."}
 * or packages (i.e. {@code "/packages/..."}.
 */
public interface ResourceEntries {
    /**
     * Returns the full entry names for all resources in the given module, in
     * random order. Entry names will always be prefixed by the given module
     * name (e.g. "/<module-name/...").
     */
    Stream<String> entryNamesIn(String module);

    /**
     * Returns the (uncompressed) size of a resource given its full entry name.
     *
     * @throws java.util.NoSuchElementException if the resource does not exist.
     */
    long sizeOf(String name);

    /**
     * Returns an {@link InputStream} for a resource given its full entry name.
     *
     * @throws java.util.NoSuchElementException if the resource does not exist.
     */
    InputStream open(String name);
}
