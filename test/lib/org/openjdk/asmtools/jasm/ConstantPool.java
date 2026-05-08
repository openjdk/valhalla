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
package org.openjdk.asmtools.jasm;

import org.openjdk.asmtools.jasm.Tables.ConstType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * ConstantPool
 *
 * ConstantPool is the class responsible for maintaining constants for a given class file.
 *
 */
public class ConstantPool implements Iterable<ConstantPool.ConstCell> {


    static public enum ReferenceRank {
        LDC(0),  // 0 - highest - ref from ldc
        ANY(1),  // 1 - any ref
        NO(2);   // 2 - no ref
        final int rank;
        ReferenceRank(int rank) {
            this.rank = rank;
        }
    }

    /*-------------------------------------------------------- */
    /* ConstantPool Inner Classes */
    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue {

        protected ConstType tag;
        protected boolean isSet = false;
        private boolean visited = false;

        public ConstValue(ConstType tag) {
            this.tag = tag;
        }

        public int size() {
            return 1;
        }

        public boolean hasValue() {
            return isSet;
        }

        /**
         * Compute the hash-code, based on the value of the native (_hashCode()) hashcode.
         */
        @Override
        public int hashCode() {
            if (visited) {
                throw new Parser.CompilerError("CV hash:" + this);
            }
            visited = true;
            int res = _hashCode() + tag.value() * 1023;
            visited = false;
            return res;
        }

        // sub-classes override this.
        // this is the default for getting a hash code.
        protected int _hashCode() {
            return 37;
        }

        /**
         * Compares this object to the specified object.
         *
         * Sub-classes must override this
         *
         * @param obj the object to compare with
         * @return true if the objects are the same; false otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public String toString() {
            String tagstr = tag.printval();
            String retval = "";
            if (tagstr == null) {
                return "BOGUS_TAG:" + tag;
            }

            String valueStr = _toString();
            if (valueStr != null) {
                retval = "<" + tagstr + " " + valueStr + ">";
            } else {
                retval = "<" + tagstr + ">";
            }
            return retval;
        }

        protected String _toString() {
            return "";
        }

        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(tag.value());
        }
    } // end ConstValue

    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_Zero extends ConstValue {

        public ConstValue_Zero() {
            super(ConstType.CONSTANT_ZERO);
            isSet = false;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            throw new Parser.CompilerError("Trying to write Constant 0.");
        }
    }

    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_String extends ConstValue {

        String value;

        public ConstValue_String(String value) {
            super(ConstType.CONSTANT_UTF8);
            this.value = value;
            isSet = (value != null);
        }

        @Override
        protected String _toString() {
            return value;
        }

        @Override
        protected int _hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null) || !(obj instanceof ConstValue_String)) {
                return false;
            }
            ConstValue_String dobj = (ConstValue_String) obj;
            if (tag != dobj.tag) {
                return false;
            }
            return value.equals(dobj.value);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeUTF(value);
        }
    }

    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_Integer extends ConstValue {

        Integer value;

        public ConstValue_Integer(ConstType tag, Integer value) {
            super(tag);
            this.value = value;
            isSet = (value != null);
        }

        @Override
        protected String _toString() {
            return value.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null) || !(obj instanceof ConstValue_Integer)) {
                return false;
            }
            ConstValue_Integer dobj = (ConstValue_Integer) obj;
            if (tag != dobj.tag) {
                return false;
            }
            return value.equals(dobj.value);
        }

        @Override
        protected int _hashCode() {
            return value.hashCode();
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeInt(value.intValue());
        }
    }

    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_Long extends ConstValue {

        Long value;

        public ConstValue_Long(ConstType tag, Long value) {
            super(tag);
            this.value = value;
            isSet = (value != null);
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        protected String _toString() {
            return value.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null) || !(obj instanceof ConstValue_Long)) {
                return false;
            }
            ConstValue_Long dobj = (ConstValue_Long) obj;
            if (tag != dobj.tag) {
                return false;
            }
            return value.equals(dobj.value);
        }

        @Override
        protected int _hashCode() {
            return value.hashCode();
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeLong(value.longValue());
        }
    }

    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_Cell extends ConstValue {

        ConstCell cell;

        public ConstValue_Cell(ConstType tag, ConstCell cell) {
            super(tag);
            this.cell = cell;
            isSet = (cell != null);
        }

        @Override
        protected String _toString() {
            return cell.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null) || !(obj instanceof ConstValue_Cell)) {
                return false;
            }
            ConstValue_Cell dobj = (ConstValue_Cell) obj;
            if (tag != dobj.tag) {
                return false;
            }
            return cell.equals(dobj.cell);
        }

        @Override
        protected int _hashCode() {
            return cell.hashCode();
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            cell.write(out);
        }
    }

    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_Pair extends ConstValue {

        ConstCell left, right;

        public ConstValue_Pair(ConstType tag, ConstCell left, ConstCell right) {
            super(tag);
            this.left = left;
            this.right = right;
            isSet = (left != null && right != null);
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null) || !(obj instanceof ConstValue_Pair)) {
                return false;
            }
            ConstValue_Pair dobj = (ConstValue_Pair) obj;
            if (tag != dobj.tag) {
                return false;
            }
            if (dobj.left != null)
                if (!dobj.left.equals(left))
                    return false;
            if (dobj.right != null)
                if (!dobj.right.equals(right))
                    return false;
            return true;
        }

        @Override
        public String toString() {
            return super.toString() + "{" + left + "," + right + "}";
        }

        @Override
        protected int _hashCode() {
            return left.hashCode() * right.hashCode();
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            if (tag == ConstType.CONSTANT_METHODHANDLE) {
                out.writeByte(left.arg); // write subtag value
            } else {
                out.writeShort(left.arg);
            }
            out.writeShort(right.arg);
        }
    }

    static public class ConstValue_IndyOrCondyPair extends ConstValue {
        BootstrapMethodData bsmData;
        ConstantPool.ConstCell napeCell;

        protected ConstValue_IndyOrCondyPair(ConstType tag, BootstrapMethodData bsmdata, ConstCell napeCell) {
            super(tag);
            assert (tag == ConstType.CONSTANT_DYNAMIC && ConstValue_CondyPair.class.isAssignableFrom(getClass())) ||
                   tag == ConstType.CONSTANT_INVOKEDYNAMIC && ConstValue_IndyPair.class.isAssignableFrom(getClass());

            this.bsmData = bsmdata;
            this.napeCell = napeCell;
            isSet = (bsmdata != null && napeCell != null);
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null) || !(getClass().isInstance(obj))) {
                return false;
            }

            ConstValue_IndyOrCondyPair iobj = (ConstValue_IndyOrCondyPair) obj;
            return (iobj.bsmData == bsmData) && (iobj.napeCell == napeCell);
        }

        @Override
        public String toString() {
            return super.toString() + "{" + bsmData + "," + napeCell + "}";
        }

        @Override
        protected int _hashCode() {
            if (bsmData.isPlaceholder()) {
                return napeCell.hashCode();
            }
            return bsmData.hashCode() * napeCell.hashCode();
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);
            out.writeShort(bsmData.arg);
            out.writeShort(napeCell.arg);
        }
    }
    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_CondyPair extends ConstValue_IndyOrCondyPair {

        public ConstValue_CondyPair(BootstrapMethodData bsmdata, ConstCell napeCell) {
            super(ConstType.CONSTANT_DYNAMIC, bsmdata, napeCell);
        }
    }

    /**
     * ConstValue
     *
     * A (typed) tagged value in the constant pool.
     */
    static public class ConstValue_IndyPair extends ConstValue_IndyOrCondyPair {

        public ConstValue_IndyPair(BootstrapMethodData bsmdata, ConstCell napeCell) {
            super(ConstType.CONSTANT_INVOKEDYNAMIC, bsmdata, napeCell);
        }
    }

    /*-------------------------------------------------------- */
    /* ConstantPool Inner Classes */
    /**
     * ConstantCell
     *
     * ConstantCell is a type of data that can be in a constant pool.
     */
    static public class ConstCell extends Argument implements Data {

        ConstValue ref;
        // 0 - highest - ref from ldc, 1 - any ref, 2 - no ref
        ReferenceRank rank = ReferenceRank.NO;

        ConstCell(int arg, ConstValue ref) {
            this.arg = arg;
            this.ref = ref;
        }

        ConstCell(ConstValue ref) {
            this(NotSet, ref);
        }

        ConstCell(int arg) {
            this(arg, null);
        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(arg);
        }

        public void setRank(ReferenceRank rank) {
            // don't change a short ref to long due to limitation of ldc - max 256 indexes allowed
            if( this.rank != ReferenceRank.LDC) {
                this.rank = rank;
            }
        }

        @Override
        public int hashCode() {
            if (arg == NotSet) {
                if (ref != null) {
                    return ref.hashCode();
                } else {
                    throw new Parser.CompilerError("Can't generate Hash Code, Null ConstCell Reference.");
                }
            }
            return arg;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            ConstCell cc = (ConstCell)obj;
            if( cc.ref == null ) {
                return this.ref == null && cc.rank == this.rank;
            }
            return cc.ref.equals(this.ref) && cc.rank == this.rank;
        }

        public boolean isUnset() {
            return (arg == NotSet) && (ref == null);
        }

        @Override
        public String toString() {
            return "#" + arg + "=" + ref;
        }
    }

    /**
     * CPVisitor
     *
     * CPVisitor base class defining a visitor for decoding constants.
     */
    public static class CPTagVisitor<R> implements Constants {

        public CPTagVisitor() {
        }

        public final R visit(ConstType tag) {
            R retVal = null;
            switch (tag) {
                case CONSTANT_UTF8:
                    retVal = visitUTF8(tag);
                    break;
                case CONSTANT_INTEGER:
                    retVal = visitInteger(tag);
                    break;
                case CONSTANT_FLOAT:
                    retVal = visitFloat(tag);
                    break;
                case CONSTANT_DOUBLE:
                    retVal = visitDouble(tag);
                    break;
                case CONSTANT_LONG:
                    retVal = visitLong(tag);
                    break;
                case CONSTANT_METHODTYPE:
                    retVal = visitMethodtype(tag);
                    break;
                case CONSTANT_STRING:
                    retVal = visitString(tag);
                    break;
                case CONSTANT_CLASS:
                    retVal = visitClass(tag);
                    break;
                case CONSTANT_METHOD:
                    retVal = visitMethod(tag);
                    break;
                case CONSTANT_FIELD:
                    retVal = visitField(tag);
                    break;
                case CONSTANT_INTERFACEMETHOD:
                    retVal = visitInterfacemethod(tag);
                    break;
                case CONSTANT_NAMEANDTYPE:
                    retVal = visitNameandtype(tag);
                    break;
                case CONSTANT_METHODHANDLE:
                    retVal = visitMethodhandle(tag);
                    break;
                case CONSTANT_DYNAMIC:
                    retVal = visitDynamic(tag);
                    break;
                case CONSTANT_INVOKEDYNAMIC:
                    retVal = visitInvokedynamic(tag);
                    break;
                default:
                    visitDefault(tag);
            }
            return retVal;
        }

        public R visitUTF8(ConstType tag) {
            return null;
        }

        public R visitInteger(ConstType tag) {
            return null;
        }

        public R visitFloat(ConstType tag) {
            return null;
        }

        public R visitDouble(ConstType tag) {
            return null;
        }

        public R visitLong(ConstType tag) {
            return null;
        }

        public R visitMethodtype(ConstType tag) {
            return null;
        }

        public R visitString(ConstType tag) {
            return null;
        }

        public R visitClass(ConstType tag) {
            return null;
        }

        public R visitMethod(ConstType tag) {
            return null;
        }

        public R visitField(ConstType tag) {
            return null;
        }

        public R visitInterfacemethod(ConstType tag) {
            return null;
        }

        public R visitNameandtype(ConstType tag) {
            return null;
        }

        public R visitMethodhandle(ConstType tag) {
            return null;
        }

        public R visitDynamic(ConstType tag) {
            return null;
        }

        public R visitInvokedynamic(ConstType tag) {
            return null;
        }

        public R visitModule(ConstType tag) {
            return null;
        }

        public R visitPackage(ConstType tag) {
            return null;
        }

        public void visitDefault(ConstType tag) {
        }
    }

    /**
    * CPVisitor
    *
    * CPVisitor base class defining a visitor for decoding constants.
    */
   public static class CPVisitor<R> implements Constants {

        public CPVisitor() {
        }

        public final R visit(ConstValue val) {
            R retVal = null;
            ConstType tag = val.tag;
            switch (tag) {
                case CONSTANT_UTF8:
                    retVal = visitUTF8((ConstValue_String) val);
                    break;
                case CONSTANT_INTEGER:
                    retVal = visitInteger((ConstValue_Integer) val);
                    break;
                case CONSTANT_FLOAT:
                    retVal = visitFloat((ConstValue_Integer) val);
                    break;
                case CONSTANT_DOUBLE:
                    retVal = visitDouble((ConstValue_Long) val);
                    break;
                case CONSTANT_LONG:
                    retVal = visitLong((ConstValue_Long) val);
                    break;
                case CONSTANT_METHODTYPE:
                    retVal = visitMethodtype((ConstValue_Cell) val);
                    break;
                case CONSTANT_STRING:
                    retVal = visitString((ConstValue_Cell) val);
                    break;
                case CONSTANT_CLASS:
                    retVal = visitClass((ConstValue_Cell) val);
                    break;
                case CONSTANT_METHOD:
                    retVal = visitMethod((ConstValue_Pair) val);
                    break;
                case CONSTANT_FIELD:
                    retVal = visitField((ConstValue_Pair) val);
                    break;
                case CONSTANT_INTERFACEMETHOD:
                    retVal = visitInterfacemethod((ConstValue_Pair) val);
                    break;
                case CONSTANT_NAMEANDTYPE:
                    retVal = visitNameandtype((ConstValue_Pair) val);
                    break;
                case CONSTANT_METHODHANDLE:
                    retVal = visitMethodhandle((ConstValue_Pair) val);
                    break;
                case CONSTANT_DYNAMIC:
                    retVal = visitDynamic((ConstValue_CondyPair) val);
                    break;
                case CONSTANT_INVOKEDYNAMIC:
                    retVal = visitInvokedynamic((ConstValue_IndyPair) val);
                    break;
                case CONSTANT_MODULE:
                    retVal = visitModule((ConstValue_Cell) val);
                    break;
                case CONSTANT_PACKAGE:
                    retVal = visitPackage((ConstValue_Cell) val);
                    break;
                default:
                    visitDefault(tag);
            }
            return retVal;
        }

        public R visitUTF8(ConstValue_String p) {
            return null;
        }

        ;
        public R visitInteger(ConstValue_Integer p) {
            return null;
        }

        ;
        public R visitFloat(ConstValue_Integer p) {
            return null;
        }

        ;
        public R visitDouble(ConstValue_Long p) {
            return null;
        }

        ;
        public R visitLong(ConstValue_Long p) {
            return null;
        }

        ;
        public R visitMethodtype(ConstValue_Cell p) {
            return null;
        }

        ;
        public R visitString(ConstValue_Cell p) {
            return null;
        }

        ;
        public R visitClass(ConstValue_Cell p) {
            return null;
        }

        ;
        public R visitMethod(ConstValue_Pair p) {
            return null;
        }

        ;
        public R visitField(ConstValue_Pair p) {
            return null;
        }

        ;
        public R visitInterfacemethod(ConstValue_Pair p) {
            return null;
        }

        ;
        public R visitNameandtype(ConstValue_Pair p) {
            return null;
        }

        ;
        public R visitMethodhandle(ConstValue_Pair p) {
            return null;
        }

        ;
        public R visitDynamic(ConstValue_CondyPair p) { return null;}

        ;
        public R visitInvokedynamic(ConstValue_IndyPair p) { return null;}

        ;
        public R visitModule(ConstValue_Cell p) { return null; }

        ;
        public R visitPackage(ConstValue_Cell p) { return null; }
        ;

        public void visitDefault(ConstType tag) {}
        ;

    }



  /*-------------------------------------------------------- */
  /* Constant Pool Fields */

    private ArrayList<ConstCell> pool = new ArrayList<>(20);

    private final ConstValue ConstValue0
            = new ConstValue_String("");
//    private final ConstValue ConstValue0 =
//            new ConstValue(CONSTANT_UTF8, "");
    private final ConstCell nullConst
            = new ConstCell(null);
    private final ConstCell constant_0
            = new ConstCell(new ConstValue_Zero());
//    private final ConstCell constant_0 =
//            new ConstCell(new ConstValue(CONSTANT_ZERO, null));

    // For hashing by value
    Hashtable<ConstValue, ConstCell> cpoolHashByValue
            = new Hashtable<>(40);

    public Environment env;

    private static boolean debugCP = false;

    /*-------------------------------------------------------- */
    /**
     * main constructor
     *
     * @param env The error reporting environment
     */
    public ConstantPool(Environment env) {
        this.env = env;
        pool.add(constant_0);

    }

    public void debugStr(String s) {
        if (debugCP) {
            env.traceln(s);
        }
    }

    @Override
    public Iterator<ConstCell> iterator() {
        return pool.iterator();
    }


    /*
     * Fix Refs in constant pool.
     *
     * This is used when scanning JASM files produced from JDis with the verbose
     * option (eg. where the constant pool is declared in the jasm itself).  In
     * this scenario, we need two passes - the first pass to scan the entries
     * (which creates constant references with indexes, but no reference values);
     * and the second pass, which links references to existing constants.
     *
     */
    public void fixRefsInPool() {
        // used to fix CP refs when a constant pool is constructed by refs alone.
        env.traceln("Fixing CP for explicit Constant Entries.");
        int i = 0;
        // simply iterate through the pool.
        for (ConstCell item : pool) {
            i += 1;
            // first item is always null
            if (item == null) {
                continue;
            }

            checkAndFixCPRef(i, item);
        }
    }

    protected void CheckGlobals() {
        env.traceln("Checking Globals");
        //
        // This fn will put empty UTF8 string entries on any unset
        // CP entries - before the last CP entry.
        //
        for (int cpx = 1; cpx < pool.size(); cpx++) {
            ConstCell cell = pool.get(cpx);
            if (cell == nullConst) { // gap
                cell = new ConstCell(cpx, ConstValue0);
                pool.set(cpx, cell);
            }
            ConstValue cval = cell.ref;
            if ((cval == null) || !cval.hasValue()) {
                String name = Integer.toString(cpx);
                env.error("const.undecl", name);
            }
        }
    }

    /*
     *  Helper function for "fixRefsInPool"
     *
     *  Does recursive checking of references,
     * using a locally-defined visitor.
     */
    private void checkAndFixCPRef(int i, ConstCell item) {
        ConstValue cv = item.ref;
        if (cv != null) {
            fixCPVstr.visit(cv);
        }
    }

    private CPVisitor<Void> fixCPVstr = new CPVisitor<Void>() {
        @Override
        public Void visitUTF8(ConstValue_String p) {
            return null;
        }

        ;
        @Override
        public Void visitInteger(ConstValue_Integer p) {
            return null;
        }

        ;
        @Override
        public Void visitFloat(ConstValue_Integer p) {
            return null;
        }

        ;
        @Override
        public Void visitDouble(ConstValue_Long p) {
            return null;
        }

        ;
        @Override
        public Void visitLong(ConstValue_Long p) {
            return null;
        }

        ;
        @Override
        public Void visitMethodtype(ConstValue_Cell p) {
            handleClassRef(p);
            return null;
        }

        ;
        @Override
        public Void visitString(ConstValue_Cell p) {
            handleClassRef(p);
            return null;
        }

        ;
        @Override
        public Void visitClass(ConstValue_Cell p) {
            handleClassRef(p);
            return null;
        }

        ;
        @Override
        public Void visitMethod(ConstValue_Pair p) {
            handleMemberRef(p);
            return null;
        }

        ;
        @Override
        public Void visitField(ConstValue_Pair p) {
            handleMemberRef(p);
            return null;
        }

        ;
        @Override
        public Void visitInterfacemethod(ConstValue_Pair p) {
            handleMemberRef(p);
            return null;
        }

        ;
        @Override
        public Void visitNameandtype(ConstValue_Pair p) {
            handleMemberRef(p);
            return null;
        }

        ;
        @Override
        public Void visitMethodhandle(ConstValue_Pair p) {
            handleMemberRef(p);
            return null;
        }

        ;
        @Override
        public Void visitDynamic(ConstValue_CondyPair p) {
            return null;
        }

        ;
        @Override
        public Void visitInvokedynamic(ConstValue_IndyPair p) {
            return null;
        }
        ;

        @Override
        public Void visitModule(ConstValue_Cell p) {
            handleClassRef(p);
            return null;
        }
        ;

        @Override
        public Void visitPackage(ConstValue_Cell p) {
            handleClassRef(p);
            return null;
        }
        ;


        public void handleClassRef(ConstValue_Cell cv) {
            ConstCell clref = cv.cell;
            if (clref.ref == null) {
                ConstCell refval = cpool_get(clref.arg);
                if (refval != null) {
                    checkAndFixCPRef(clref.arg, refval);
                    clref.ref = refval.ref;
                } else {
                    clref.ref = null;
                }
                // env.traceln("FIXED ConstPool[" + i + "](" + cv.TagString(cv.tag) + ") = " + cv.value);
            }
        }

        public void handleMemberRef(ConstValue_Pair cv) {
            // env.traceln("ConstPool[" + i + "](" + cv.TagString(cv.tag) + ") = " + cv.value);
            ConstCell clref = cv.left;
            ConstCell typref = cv.right;
            if (clref.ref == null) {
                ConstCell refval = cpool_get(clref.arg);
                if (refval != null) {
                    checkAndFixCPRef(clref.arg, refval);
                    clref.ref = refval.ref;
                } else {
                    clref.ref = null;
                }
                // env.traceln("FIXED ConstPool[" + i + "](" + cv.TagString(cv.tag) + ") = " + cv.value);
            }
            if (typref.ref == null) {
                ConstCell refval = cpool_get(typref.arg);
                if (refval != null) {
                    checkAndFixCPRef(typref.arg, refval);
                    typref.ref = refval.ref;
                } else {
                    typref.ref = null;
                }
                // env.traceln("FIXED ConstPool[" + i + "](" + cv.TagString(cv.tag) + ") = " + cv.value);
            }
        }

    };

    /*
     * Help debug Constant Pools
     */
    public void printPool() {
        int i = 0;
        for (ConstCell item : pool) {
            env.traceln("^^^^^^^^^^^^^  const #" + i + ": " + item);
            i += 1;
        }
    }

    private ConstCell cpool_get(int cpx) {
        if (cpx >= pool.size()) {
            return null;
        }
        return pool.get(cpx);
    }

    private void cpool_set(int cpx, ConstCell cell, int sz) {
        debugStr("cpool_set1: " + cpx + " " + cell);
        debugStr("param_size: " + sz);
        debugStr("pool_size: " + pool.size());
        cell.arg = cpx;
        if (cpx + sz >= pool.size()) {
            debugStr("calling ensureCapacity( " + (cpx + sz + 1) + ")");
            int low = pool.size();
            int high = cpx + sz;
            for (int i = 0; i < high - low; i++) {
                pool.add(nullConst);
            }
        }
        pool.set(cpx, cell);
        if (sz == 2) {
            pool.set(cpx + 1, new ConstCell(cpx + 1, ConstValue0));
        }
        debugStr(" cpool_set2: " + cpx + " " + cell);
    }

    protected ConstCell uncheckedGetCell(int cpx) { // by index
        return pool.get(cpx);
    }

    public ConstCell getCell(int cpx) { // by index
        ConstCell cell = cpool_get(cpx);
        if (cell != null) {
            return cell;
        }
        cell = new ConstCell(cpx, null);
        return cell;
    }

    public void setCell(int cpx, ConstCell cell) {
        ConstValue value = cell.ref;
        if (value == null) {
            throw new Parser.CompilerError(env.errorStr("comperr.constcell.nullvalset"));
        }
        int sz = value.size();

        if (cpx == 0) {
            // It is correct to warn about redeclaring constant zero,
            // since this value is never written out to a class file.
            env.error("warn.const0.redecl");
        } else {
            if ((cpool_get(cpx) != null) || ((sz == 2) && (cpool_get(cpx + 1) != null))) {
                String name = "#" + cpx;
                env.error("const.redecl", name);
                return;
            }
            if (cell.isSet() && (cell.arg != cpx)) {
                env.traceln("setCell: new ConstCell");
                cell = new ConstCell(value);
            }
        }
        cpool_set(cpx, cell, sz);
    }

    protected void NumberizePool() {
        env.traceln("NumberizePool");

        for (ReferenceRank rank : ReferenceRank.values()) {
            for (ConstCell cell : cpoolHashByValue.values().stream().
                     filter(v-> !v.isSet() && rank.equals(v.rank)).
                     collect(Collectors.toList())) {

                ConstValue value = cell.ref;
                if (value == null) {
                    throw new Parser.CompilerError(env.errorStr("comperr.constcell.nullvalhash"));
                }
                int sz = value.size(), cpx;
find:
                for (cpx = 1; cpx < pool.size(); cpx++) {
                    if ((pool.get(cpx) == nullConst) && ((sz == 1) || (pool.get(cpx + 1) == nullConst))) {
                        break find;
                    }
                }
                cpool_set(cpx, cell, sz);
            }
        }

        ConstCell firstCell = cpool_get(0);
        firstCell.arg = 0;
    }

    public ConstCell FindCell(ConstValue ref) {
        if (ref == null) {
            throw new Parser.CompilerError(env.errorStr("comperr.constcell.nullval"));
        }
        ConstCell pconst = null;
        try {
            pconst = cpoolHashByValue.get(ref);
        } catch (Parser.CompilerError e) {
            throw new Parser.CompilerError(env.errorStr("comperr.constcell.nullvalhash"));
        }
        // If we fund a cached ConstValue
        if (pconst != null) {
            ConstValue value = pconst.ref;
            if (!value.equals(ref)) {
                throw new Parser.CompilerError(env.errorStr("comperr.val.noteq"));
            }
            return pconst;
        }
        // If we didn't find a cached ConstValue
        //      Add it to the cache
        pconst = new ConstCell(ref);
        cpoolHashByValue.put(ref, pconst);
        return pconst;
    }

    public ConstCell FindCell(ConstType tag, String value) {
        return FindCell(new ConstValue_String(value));
    }

    public ConstCell FindCell(ConstType tag, Integer value) {
        return FindCell(new ConstValue_Integer(tag, value));
    }

    public ConstCell FindCell(ConstType tag, Long value) {
        return FindCell(new ConstValue_Long(tag, value));
    }

    public ConstCell FindCell(ConstType tag, ConstCell value) {
        return FindCell(new ConstValue_Cell(tag, value));
    }

    public ConstCell FindCell(ConstType tag, ConstCell left, ConstCell right) {
        return FindCell(new ConstValue_Pair(tag, left, right));
    }

    public ConstCell FindCellAsciz(String str) {
        return FindCell(ConstType.CONSTANT_UTF8, str);
    }

    public ConstCell FindCellClassByName(String name) { return FindCell(ConstType.CONSTANT_CLASS, FindCellAsciz(name)); }

    public ConstCell FindCellModuleByName(String name) { return FindCell(ConstType.CONSTANT_MODULE, FindCellAsciz(name)); }

    public ConstCell FindCellPackageByName(String name) { return FindCell(ConstType.CONSTANT_PACKAGE, FindCellAsciz(name)); }

    public void write(CheckedDataOutputStream out) throws IOException {
        // Write the constant pool
        int length = pool.size();
        out.writeShort(length);
        int i;
        env.traceln("wr.pool:size=" + length);
        for (i = 1; i < length;) {
            ConstCell cell = pool.get(i);
            ConstValue value = cell.ref;
            if (cell.arg != i) {
                throw new Parser.CompilerError(env.errorStr("comperr.constcell.invarg", Integer.toString(i), cell.arg));
            }
            value.write(out);
            i += value.size();
        }
    }
}
