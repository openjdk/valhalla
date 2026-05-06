/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Class File Version
 */
public class CFVersion implements Cloneable{
    /**
     * Default versions of class file
     */
    public static final short DEFAULT_MAJOR_VERSION = 45;
    public static final short DEFAULT_MINOR_VERSION = 3;
    public static final short DEFAULT_MODULE_MAJOR_VERSION = 53;
    public static final short DEFAULT_MODULE_MINOR_VERSION = 0;
    public static final short UNDEFINED_VERSION = -1;

    private short major_version;
    private short minor_version;
    private boolean frozen;
    private boolean isSet;

    public CFVersion() {
        frozen = false;
        isSet = false;
        major_version = UNDEFINED_VERSION;
        minor_version = UNDEFINED_VERSION;
    }

    public CFVersion(boolean frozenCFV, short major_version, short minor_version) {
        isSet = true;
        frozen = frozenCFV;
        this.major_version = major_version;
        this.minor_version = minor_version;
    }

    public void setMajorVersion(short major_version) {
        if ( !frozen ) {
            isSet = true;
            this.major_version = major_version;
        }
    }

    public void setMinorVersion(short minor_version) {
        if (!frozen) {
            isSet = true;
            this.minor_version = minor_version;
        }
    }

    public String asString() {
        return (isSet) ? this.major_version + ":" +this.minor_version : "(undef):(undef)";
    }

    public void initModuleDefaults() {
        if( ! isSet) {
            major_version = DEFAULT_MODULE_MAJOR_VERSION;
            minor_version = DEFAULT_MODULE_MINOR_VERSION;
        }
    }

    public void initClassDefaults() {
        if( !isSet ) {
            major_version = DEFAULT_MAJOR_VERSION;
            minor_version = DEFAULT_MINOR_VERSION;
        }
    }

    public short minor_version() {
        return this.minor_version;
    }

    public short major_version() {
        return this.major_version;
    }

    public CFVersion clone() {
        try {
            return (CFVersion)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
