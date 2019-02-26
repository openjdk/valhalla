/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 *  The classfile for this class will be loaded directly and used to define
 *  either a non-findable or anonymous class.
 */
public class NonFindable implements Test {

    NonFindable other = null;

    private void realTest() {
        other = this;  // test verification of putfield
        Object o = other;
        NonFindable local = this;
        local = other;
        local = (NonFindable) o;
        local = new NonFindable();
        other = local;

        set_other(local); // method signature test
        set_other(null);

        local = getThis();

        set_other_maybe(new Object());
        set_other_maybe(this);
        if (other != this)
            throw new Error("set_other_maybe didn't work!");
        if (other == this) {
            try {
                throw new Error("threw an exception");
            } catch (Error e) {
            }
        }
    }

    private NonFindable getThis() {
        return this; // areturn test
    }

    private void set_other(NonFindable t) {
        other = t;
    }

    private void set_other_maybe(Object o) {
        if (o instanceof NonFindable) {
         other = (NonFindable) o;
        }
    }

    public void test() {
        realTest();
    }
}
