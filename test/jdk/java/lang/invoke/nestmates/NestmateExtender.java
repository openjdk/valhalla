/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import jdk.internal.org.objectweb.asm.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class NestmateExtender {

    // the input stream to read the original module-info.class
    private final InputStream in;

    // the value of the ModuleMainClass attribute
    private String host;

    // the value for the ModuleTarget attribute
    private Set<String> members;


    private NestmateExtender(InputStream in) {
        this.in = in;
    }

    /**
     * Sets the nest host in the NestHost attribute
     */
    public NestmateExtender nestHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Sets the nest members in the NestMembers attribute
     */
    public NestmateExtender nestMembers(Set<String> members) {
        this.members = members;
        return this;
    }

    /**
     * Outputs the modified class file to the given output stream.
     * Once this method has been called then the NestmateExtender object should
     * be discarded.
     */
    public void write(OutputStream out) throws IOException {
        // emit to the output stream
        out.write(toByteArray());
    }

    /**
     * Returns the bytes of the modified class file.
     * Once this method has been called then the NestmateExtender object should
     * be discarded.
     */
    public byte[] toByteArray() throws IOException {
        if (host != null && members != null) {
            throw new IllegalStateException("cannot set both the nest host and members");
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);

        ClassReader cr = new ClassReader(in);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {
            @Override
            public void visitNestHost(final String nestHost) {
                throw new IllegalArgumentException("should not have NestHost attribute: " + nestHost);
            }
            @Override
            public void visitNestMember(final String nestMember) {
                throw new IllegalArgumentException("should not have NestMembers attribute: " + nestMember);
            }
            @Override
            public MethodVisitor visitMethod(final int access,
                                             final String name,
                                             final String descriptor,
                                             final String signature,
                                             final String[] exceptions) {
                int modifiers = access;
                if (name.equals("name") || name.equals("lookup")) {
                    modifiers = (access & ~ACC_PUBLIC) | ACC_PRIVATE;
                }
                return super.visitMethod(modifiers, name, descriptor, signature, exceptions);
            }
        };
        cr.accept(cv, 0);

        // add NestHost or NestMembers attributes
        if (host != null) {
            cw.visitNestHost(host);
        }

        if (members != null) {
            for (String name : members) {
                cw.visitNestMember(name);
            }
        }

        return cw.toByteArray();
    }

    public static NestmateExtender newExtender(InputStream in) {
        return new NestmateExtender(in);
    }
}
