/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * A container for the java sources tied to an jasm output when -sl in on
 */
public class TextLines {

    final Path file;
    List<String> lines;

    public TextLines(Path directory, String sourceFileName) {
        file = directory == null ? Paths.get(sourceFileName) : directory.resolve(sourceFileName);
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException ignore) {}
    }

    public String getLine(int index) {
        if( lines != null ) {
            if (index < 1 || index >= lines.size()) {
                return String.format("Line number %d is out of range in \"%s\"", index, file);
            }
            return lines.get(index - 1);
        }
        return String.format("\"%s\" not found", file);
    }
}
