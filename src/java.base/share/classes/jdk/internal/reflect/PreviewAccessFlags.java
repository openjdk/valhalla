/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.reflect;

import java.lang.reflect.AccessFlag;
import java.util.EnumSet;
import java.util.Set;

import static java.lang.classfile.ClassFile.*;
import static java.lang.reflect.AccessFlag.*;

/// Provides access to preview Access Flag information.
public final class PreviewAccessFlags {
    public static int flagsMask(Location location) {
        return switch (location) {
            case FIELD -> ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED |
                    ACC_STATIC | ACC_FINAL | ACC_VOLATILE |
                    ACC_TRANSIENT | ACC_SYNTHETIC | ACC_ENUM | ACC_STRICT_INIT; // strict init
            case INNER_CLASS -> ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_IDENTITY |
                    ACC_STATIC | ACC_FINAL | ACC_INTERFACE | ACC_ABSTRACT |
                    ACC_SYNTHETIC | ACC_ANNOTATION | ACC_ENUM; // identity
            default -> location.flagsMask();
        };
    }

    public static Set<Location> locations(AccessFlag flag) {
        return switch (flag) {
            case SUPER -> Set.of();
            case IDENTITY -> Set.of(Location.CLASS, Location.INNER_CLASS);
            case STRICT_INIT -> Set.of(Location.FIELD);
            default -> flag.locations();
        };
    }

    /// Parses access flag for preview class files.
    /// @throws IllegalArgumentException if there is unrecognized flag bit
    public static Set<AccessFlag> parse(int flags, Location location) {
        return switch (location) {
            case CLASS -> doParse(flags, Location.CLASS, CLASS_PREVIEW_FLAGS);
            case INNER_CLASS -> doParse(flags, Location.INNER_CLASS, INNER_CLASS_PREVIEW_FLAGS);
            case FIELD -> doParse(flags, Location.FIELD, FIELD_PREVIEW_FLAGS);
            default -> maskToAccessFlags(flags, location);
        };
    }

    private static Set<AccessFlag> doParse(int flags, Location location, AccessFlag[] known) {
        EnumSet<AccessFlag> ans = EnumSet.noneOf(AccessFlag.class);
        for (var flag : known) {
            if ((flags & flag.mask()) != 0) {
                ans.add(flag);
                flags &= ~flag.mask();
            }
        }
        if (flags != 0) {
            throw new IllegalArgumentException("Unmatched bit position 0x" +
                    Integer.toHexString(flags) +
                    " for location " + location +
                    " for preview class files");
        }
        return Set.copyOf(ans);
    }

    private static final AccessFlag[]
            CLASS_PREVIEW_FLAGS = {PUBLIC, FINAL, IDENTITY, INTERFACE, ABSTRACT, SYNTHETIC, ANNOTATION, ENUM, MODULE},
            FIELD_PREVIEW_FLAGS = {PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, VOLATILE, TRANSIENT, SYNTHETIC, ENUM, STRICT_INIT},
            INNER_CLASS_PREVIEW_FLAGS = {PUBLIC, PRIVATE, PROTECTED, IDENTITY, STATIC, FINAL, INTERFACE, ABSTRACT, SYNTHETIC, ANNOTATION, ENUM};

    private PreviewAccessFlags() {
    }
}
