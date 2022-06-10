/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public interface RuntimeConstants {
    /* Access Flags */

    int ACC_NONE          = 0x0000; // <<everywhere>>
    int ACC_PUBLIC        = 0x0001; // class, inner, field, method
    int ACC_PRIVATE       = 0x0002; //        inner, field, method
    int ACC_PROTECTED     = 0x0004; //        inner, field, method
    int ACC_STATIC        = 0x0008; //        inner, field, method
    int ACC_FINAL         = 0x0010; // class, inner, field, method
    int ACC_TRANSITIVE    = 0x0010; //                                      requires(module)
    int ACC_SUPER         = 0x0020; // class
    int ACC_STATIC_PHASE  = 0x0020; //                                      requires(module)
    int ACC_SYNCHRONIZED  = 0x0020; //                      method
    int ACC_OPEN          = 0x0020; //                              module
    int ACC_VALUE         = 0x0040; // class, inner
    int ACC_VOLATILE      = 0x0040; //               field
    int ACC_BRIDGE        = 0x0040; //                      method
    int ACC_TRANSIENT     = 0x0080; //               field
    int ACC_VARARGS       = 0x0080; //                      method
    int ACC_PERMITS_VALUE = 0x0100; // class, inner
    int ACC_NATIVE        = 0x0100; //                      method
    int ACC_INTERFACE     = 0x0200; // class, inner
    int ACC_ABSTRACT      = 0x0400; // class, inner,        method
    int ACC_PRIMITIVE     = 0x0800; // class, inner
    int ACC_STRICT        = 0x0800; //                      method
    int ACC_SYNTHETIC     = 0x1000; // class, inner, field, method, module  requires(module) exports(module)
    int ACC_ANNOTATION    = 0x2000; // class, inner
    int ACC_ENUM          = 0x4000; // class, inner, field
    int ACC_MODULE        = 0x8000; // class
    int ACC_MANDATED      = 0x8000; //                      method  module  requires(module) exports(module)

   /* Attribute codes */
   int SYNTHETIC_ATTRIBUTE          = 0x00010000; // actually, this is an attribute
   int DEPRECATED_ATTRIBUTE         = 0x00020000; // actually, this is an attribute

   Map<Integer,String> ACC_NAMES = new HashMap() {{
                        put(ACC_PUBLIC       ,"public");
                        put(ACC_PRIVATE      ,"private");
                        put(ACC_PROTECTED    ,"protected");
                        put(ACC_STATIC       ,"static");
                        put(ACC_FINAL        ,"final");
                        put(ACC_SUPER        ,"super");
                        put(ACC_SYNCHRONIZED ,"synchronized");
                        put(ACC_PERMITS_VALUE,"permits_value");
                        put(ACC_VOLATILE     ,"volatile");
                        put(ACC_BRIDGE       ,"bridge");
                        put(ACC_TRANSIENT    ,"transient");
                        put(ACC_VARARGS      ,"varargs");
                        put(ACC_VALUE        ,"value");
                        put(ACC_NATIVE       ,"native");
                        put(ACC_INTERFACE    ,"interface");
                        put(ACC_ABSTRACT     ,"abstract");
                        put(ACC_PRIMITIVE    ,"primitive");
                        put(ACC_STRICT       ,"strict");
                        put(ACC_SYNTHETIC    ,"synthetic");
                        put(ACC_ANNOTATION   ,"annotation");
                        put(ACC_ENUM         ,"enum");
                        put(ACC_MODULE       ,"module");
                        put(ACC_MANDATED     ,"mandated");
                        put(SYNTHETIC_ATTRIBUTE     ,"synthetic");
  }};

    /* The version of a class file since which the compact format of stack map is necessary */
    int SPLIT_VERIFIER_CFV = 50;

}
