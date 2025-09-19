/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import jdk.internal.jimage.ModuleReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.function.Predicate.not;
import static jdk.internal.jimage.ModuleReference.forEmptyPackage;
import static jdk.internal.jimage.ModuleReference.forResource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * @test
 * @summary Tests for ModuleReference.
 * @modules java.base/jdk.internal.jimage
 * @run junit/othervm -esa ModuleReferenceTest
 */
public final class ModuleReferenceTest {
    // Copied (not referenced) for testing.
    private static final int FLAGS_HAS_CONTENT = 0x1;
    private static final int FLAGS_HAS_NORMAL_VERSION = 0x2;
    private static final int FLAGS_HAS_PREVIEW_VERSION = 0x4;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void emptyRefs(boolean isPreview) {
        ModuleReference ref = forEmptyPackage("module", isPreview);

        assertEquals("module", ref.name());
        assertTrue(ref.isEmpty());
        assertEquals(isPreview, ref.hasPreviewVersion());
        assertEquals(isPreview, ref.isPreviewOnly());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void resourceRefs(boolean isPreview) {
        ModuleReference ref = forResource("module", isPreview);

        assertEquals("module", ref.name());
        assertFalse(ref.isEmpty());
        assertEquals(isPreview, ref.hasPreviewVersion());
        assertEquals(isPreview, ref.isPreviewOnly());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void mergedRefs(boolean isPreview) {
        ModuleReference emptyRef = forEmptyPackage("module", true);
        ModuleReference resourceRef = forResource("module", isPreview);
        ModuleReference merged = emptyRef.merge(resourceRef);

        // Merging preserves whether there's content.
        assertFalse(merged.isEmpty());
        // And clears the preview-only status unless it was set in both.
        assertEquals(isPreview, merged.isPreviewOnly());
    }

    @Test
    public void writeBuffer() {
        List<ModuleReference> refs = Arrays.asList(
                forResource("first", false),
                forEmptyPackage("alpha", false),
                forEmptyPackage("beta", false).merge(forEmptyPackage("beta", true)),
                forEmptyPackage("gamma", true));
        IntBuffer buffer = IntBuffer.allocate(2 * refs.size());
        ModuleReference.write(refs, buffer, testEncoder());
        assertArrayEquals(
                new int[]{
                        FLAGS_HAS_NORMAL_VERSION | FLAGS_HAS_CONTENT, 100,
                        FLAGS_HAS_NORMAL_VERSION, 101,
                        FLAGS_HAS_NORMAL_VERSION | FLAGS_HAS_PREVIEW_VERSION, 102,
                        FLAGS_HAS_PREVIEW_VERSION, 103},
                buffer.array());
    }

    @Test
    public void writeBuffer_badCapacity() {
        List<ModuleReference> refs = Arrays.asList(
                forResource("first", false),
                forEmptyPackage("alpha", false));
        IntBuffer buffer = IntBuffer.allocate(100);
        var err = assertThrows(
                IllegalArgumentException.class,
                () -> ModuleReference.write(refs, buffer, null));
        assertTrue(err.getMessage().contains("buffer"));
    }

    @Test
    public void writeBuffer_badOrder() {
        List<ModuleReference> refs = Arrays.asList(
                forEmptyPackage("alpha", false),
                forResource("first", false));
        IntBuffer buffer = IntBuffer.allocate(2 * refs.size());
        var err = assertThrows(
                IllegalArgumentException.class,
                () -> ModuleReference.write(refs, buffer, testEncoder()));
        assertTrue(err.getMessage().contains("non-empty"));
    }

    @Test
    public void readBuffer() {
        IntBuffer buffer = IntBuffer.wrap(new int[]{
                FLAGS_HAS_NORMAL_VERSION | FLAGS_HAS_CONTENT, 100,
                FLAGS_HAS_NORMAL_VERSION, 101,
                FLAGS_HAS_NORMAL_VERSION | FLAGS_HAS_PREVIEW_VERSION, 102,
                FLAGS_HAS_PREVIEW_VERSION, 103});
        Function<Integer, String> nameDecoder = testDecoder("one", "two", "three", "four");
        List<ModuleReference> normalRefs = ModuleReference.read(buffer, false, nameDecoder);
        List<ModuleReference> previewModeRefs = ModuleReference.read(buffer, true, nameDecoder);

        assertEquals(3, normalRefs.size());
        assertRef(normalRefs.get(0), "one", not(IS_EMPTY), not(HAS_PREVIEW), not(IS_PREVIEW_ONLY));
        assertRef(normalRefs.get(1), "two", IS_EMPTY, not(HAS_PREVIEW), not(IS_PREVIEW_ONLY));
        assertRef(normalRefs.get(2), "three", IS_EMPTY, HAS_PREVIEW, not(IS_PREVIEW_ONLY));

        assertEquals(4, previewModeRefs.size());
        assertRef(previewModeRefs.get(0), "one", not(IS_EMPTY), not(HAS_PREVIEW), not(IS_PREVIEW_ONLY));
        assertRef(previewModeRefs.get(1), "two", IS_EMPTY, not(HAS_PREVIEW), not(IS_PREVIEW_ONLY));
        assertRef(previewModeRefs.get(2), "three", IS_EMPTY, HAS_PREVIEW, not(IS_PREVIEW_ONLY));
        assertRef(previewModeRefs.get(3), "four", IS_EMPTY, HAS_PREVIEW, IS_PREVIEW_ONLY);
    }

    @Test
    public void readBuffer_badBufferSize() {
        var err = assertThrows(
                IllegalArgumentException.class,
                () -> ModuleReference.read(IntBuffer.allocate(3), false, null));
        assertTrue(err.getMessage().contains("buffer"));
    }
    @Test
    public void readBuffer_badContent() {
        IntBuffer buffer = IntBuffer.wrap(new int[]{
                FLAGS_HAS_NORMAL_VERSION | FLAGS_HAS_CONTENT, 100,
                FLAGS_HAS_NORMAL_VERSION | FLAGS_HAS_CONTENT, 101});
        var err = assertThrows(
                IllegalArgumentException.class,
                () -> ModuleReference.read(buffer, false, testDecoder("one", "two")));
        assertTrue(err.getMessage().contains("content"));
    }

    @Test
    public void readBuffer_badFlags() {
        IntBuffer buffer = IntBuffer.wrap(new int[]{FLAGS_HAS_CONTENT, 100});
        var err = assertThrows(
                IllegalArgumentException.class,
                () -> ModuleReference.read(buffer, false, testDecoder("one", "two")));
        assertTrue(err.getMessage().contains("package flags"));
    }

    @Test
    public void sortOrder() {
        // Whether it's a preview reference has no bearing on sort order.
        List<ModuleReference> refs = Arrays.asList(
                forEmptyPackage("beta", false),
                forResource("first", false),
                forEmptyPackage("gamma", true),
                forEmptyPackage("alpha", false));
        refs.sort(Comparator.naturalOrder());
        // Non-empty first with remaining sorted by name.
        assertEquals(
                List.of("first", "alpha", "beta", "gamma"),
                refs.stream().map(ModuleReference::name).toList());
    }

    // Just for readability.
    private static final Predicate<ModuleReference> IS_EMPTY = ModuleReference::isEmpty;
    private static final Predicate<ModuleReference> IS_PREVIEW_ONLY = ModuleReference::isPreviewOnly;
    private static final Predicate<ModuleReference> HAS_PREVIEW = ModuleReference::hasPreviewVersion;

    @SafeVarargs
    final void assertRef(ModuleReference ref, String name, Predicate<ModuleReference>... asserts) {
        assertEquals(name, ref.name(), "Reference name");
        for (int i = 0; i < asserts.length; i++) {
            var test = asserts[i];
            assertTrue(test.test(ref), ref + "[assert: " + i + "]");
        }
    }

    // Encodes strings sequentially starting from index 100.
    private static Function<String, Integer> testEncoder() {
        List<String> cache = new ArrayList<>();
        return s -> {
            int i = cache.indexOf(s);
            if (i == -1) {
                cache.add(s);
                return 100 + (cache.size() - 1);
            } else {
                return 100 + i;
            }
        };
    }

    // Decodes strings sequentially starting from index 100.
    private static Function<Integer, String> testDecoder(String... strings) {
        List<String> cache = Arrays.asList(strings);
        return n -> cache.get(n - 100);
    }
}
