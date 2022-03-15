/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.Module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.openjdk.asmtools.jasm.ConstantPool.*;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;
import static org.openjdk.asmtools.jasm.RuntimeConstants.*;
import static org.openjdk.asmtools.jasm.Tables.*;

/**
 * This class is used to parse Jasm statements and expressions.
 * The result is a parse tree.<p>
 * <p>
 * This class implements an operator precedence parser. Errors are
 * reported to the Environment object, if the error can't be
 * resolved immediately, a SyntaxError exception is thrown.<p>
 * <p>
 * Error recovery is implemented by catching Scanner.SyntaxError exceptions
 * and discarding input scanner.tokens until an input token is reached that
 * is possibly a legal continuation.<p>
 * <p>
 * The parse tree that is constructed represents the input
 * exactly (no rewrites to simpler forms). This is important
 * if the resulting tree is to be used for code formatting in
 * a programming environment. Currently only documentation comments
 * are retained.<p>
 * <p>
 * A parser owns several components (scanner, constant-parser,
 * instruction-parser, annotations-parser) to which it delegates certain
 * parsing responsibilities.  This parser contains functions to parse the
 * overall form of a class, and any members (fields, methods, inner-classes).
 * <p>
 * <p>
 * Syntax errors, should always be caught inside the
 * parser for error recovery.
 */
class Parser extends ParseBase {

    /* Parser Fields */
    protected ConstantPool pool = null;

    ClassData cd = null;

    CodeAttr curCode;

    private ArrayList<ClassData> clsDataList = new ArrayList<>();
    private String pkg = null;
    private String pkgPrefix = "";
    private ArrayList<AnnotationData> pkgAnnttns = null;
    private ArrayList<AnnotationData> clsAnnttns = null;
    private ArrayList<AnnotationData> memberAnnttns = null;
    private boolean explicitcp = false;
    private ModuleAttr moduleAttribute;
    private CFVersion currentCFV;
    /**
     * other parser components
     */
    private ParserAnnotation annotParser;       // For parsing Annotations
    private ParserCP cpParser;                  // for parsing Constants
    private ParserInstr instrParser;            // for parsing Instructions


    /**
     * Create a parser
     */
    protected Parser(Environment sf, CFVersion cfVersion) throws IOException {
        super.init(new Scanner(sf), this, sf);
        this.currentCFV = cfVersion;
        this.annotParser = new ParserAnnotation(scanner, this, env);
        this.cpParser = new ParserCP(scanner, this, env);
        this.instrParser = new ParserInstr(scanner, this, cpParser, env);
    }

    void setDebugFlags(boolean debugScanner, boolean debugMembers,
                       boolean debugCP, boolean debugAnnot, boolean debugInstr) {

        enableDebug(debugMembers);
        scanner.enableDebug(debugScanner);
        cpParser.enableDebug(debugCP);
        annotParser.enableDebug(debugAnnot);
        instrParser.enableDebug(debugInstr);
    }

    String encodeClassString(String classname) {
        return "L" + classname + ";";
    }


    /*-------------------------------------------------------- */

    /**
     * Parses version in package statements
     */

    private void parseVersionPkg() throws IOException {
        if (scanner.token == SEMICOLON) {
            return;
        }
        parse_ver:
        {
            if (scanner.token != Token.VERSION) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            currentCFV.setMajorVersion((short) scanner.intValue);
            scanner.scan();
            if (scanner.token != Token.COLON) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            currentCFV.setMinorVersion((short) scanner.intValue);
            scanner.scan();
            debugScan("     [Parser.parseVersionPkg]: " + currentCFV.asString());
            return;
        }
        env.error(scanner.pos, "version.expected");
        throw new Scanner.SyntaxError();
    }

    private void parseVersion() throws IOException {
        if (scanner.token == Token.LBRACE) {
            return;
        }
        parse_ver:
        {
            if (scanner.token != Token.VERSION) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            cd.cfv.setMajorVersion((short) scanner.intValue);
            scanner.scan();
            if (scanner.token != Token.COLON) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            cd.cfv.setMinorVersion((short) scanner.intValue);
            scanner.scan();
            debugStr("parseVersion: " + cd.cfv.asString());
            return;
        }
        env.error(scanner.pos, "version.expected");
        throw new Scanner.SyntaxError();
    }


    /*---------------------------------------------*/

    /**
     * Parse an internal name: identifier.
     */
    String parseIdent() throws Scanner.SyntaxError, IOException {
        String v = scanner.idValue;
        scanner.expect(Token.IDENT);
        return v;
    }

    /**
     * Parse a local variable
     */
    void parseLocVarDef() throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.INTVAL) {
            int v = scanner.intValue;
            scanner.scan();
            curCode.LocVarDataDef(v);
        } else {
            String name = scanner.stringValue, type;
            scanner.expect(Token.IDENT);
            if (scanner.token == Token.COLON) {
                scanner.scan();
                type = parseIdent();
            } else {
                type = "I";                  // TBD
            }
            curCode.LocVarDataDef(name, pool.FindCellAsciz(type));
        }
    }

    Argument parseLocVarRef() throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.INTVAL) {
            int v = scanner.intValue;
            scanner.scan();
            return new Argument(v);
        } else {
            String name = scanner.stringValue;
            scanner.expect(Token.IDENT);
            return curCode.LocVarDataRef(name);
        }
    }

    void parseLocVarEnd() throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.INTVAL) {
            int v = scanner.intValue;
            scanner.scan();
            curCode.LocVarDataEnd(v);
        } else {
            String name = scanner.stringValue;
            scanner.expect(Token.IDENT);
            curCode.LocVarDataEnd(name);
        }
    }

    void parseMapItem(DataVector map) throws Scanner.SyntaxError, IOException {
        StackMapType itemType = stackMapType(scanner.intValue, null);
        ConstType tag = null;
        Argument arg = null;
        Token ptoken = scanner.token;
        int iValue = scanner.intValue;
        String sValue = scanner.stringValue;
        scanner.scan();
        resolve:
        {
            switch (ptoken) {
                case INTVAL:
                    break resolve;
                case CLASS:
                    itemType = StackMapType.ITEM_Object;
                    tag = ConstType.CONSTANT_CLASS;
                    break resolve;
                case CPINDEX:
                    itemType = StackMapType.ITEM_Object;
                    arg = pool.getCell(iValue);
                    break resolve;
                case IDENT:
                    itemType = stackMapType(sValue);
                    tag = Tables.tag(sValue);
                    if (itemType != null) { // itemType OK
                        if ((tag != null) // ambiguity: "int," or "int 77,"?
                                && (scanner.token != SEMICOLON)
                                && (scanner.token != COMMA)) {
                            itemType = StackMapType.ITEM_Object;
                        }
                        break resolve;
                    } else if (tag != null) { // tag OK
                        itemType = StackMapType.ITEM_Object;
                        break resolve;
                    }
            }
            // resolution failed:
            itemType = StackMapType.ITEM_Bogus;
            env.error("itemtype.expected", "<" + ptoken.printValue() + ">");
        }
        switch (itemType) {
            case ITEM_Object:  // followed by CP index
                if (arg == null) {
                    arg = pool.FindCell(cpParser.parseConstValue(tag));
                }
                map.addElement(new StackMapData.StackMapItem2(itemType, arg));
                break;
            case ITEM_NewObject:  // followed by label
                arg = instrParser.parseLabelRef();
                map.addElement(new StackMapData.StackMapItem2(itemType, arg));
                break;
            default:
                map.addElement(new StackMapData.StackMapItem1(itemType));
        }
    }

    /**
     * Parse an external name: CPINDEX, string, or identifier.
     */
    ConstCell parseName() throws Scanner.SyntaxError, IOException {
        debugScan("------- [Parser.parseName]: ");
        String v;
        switch (scanner.token) {
            case CPINDEX: {
                int cpx = scanner.intValue;
                scanner.scan();
                return pool.getCell(cpx);
            }
            case STRINGVAL:
                v = scanner.stringValue;
                scanner.scan();
                return pool.FindCellAsciz(v);

            // In many cases, Identifiers can correctly have the same
            // names as keywords.  We need to allow these.
            case OPEN:
            case MODULE:
            case VARARGS:
            case REQUIRES:
            case EXPORTS:
            case TO:
            case USES:
            case PROVIDES:
            case WITH:
            case OPENS:

            case ARRAY_TYPEPATH:
            case INNER_TYPE_TYPEPATH:
            case WILDCARD_TYPEPATH:
            case TYPE_ARGUMENT_TYPEPATH:
            case PERMITTEDSUBCLASSES:
            case INF:
            case NAN:
            case COMPONENT:

            case SYNTHETIC:
            case DEPRECATED:
            case VERSION:
            case BITS:
            case STACK:
            case LOCAL:
            case OF:
            case INNERCLASS:
            case STRICT:
            case FIELDREF:
            case METHODREF:
            case IDENT:
            case BRIDGE:
            case VALUE:
            case PERMITS_VALUE:
            case PRIMITIVE:
                v = scanner.idValue;
                scanner.scan();
                return pool.FindCellAsciz(v);
            default:
                env.error(scanner.pos, "name.expected", scanner.token);
                throw new Scanner.SyntaxError();
        }
    }

    /**
     * Parses a field or method reference for method handle.
     */
    ConstCell parseMethodHandle(SubTag subtag) throws Scanner.SyntaxError, IOException {
        ConstCell refCell;
        final int pos = parser.env.pos;
        switch (subtag) {
            // If the value of the reference_kind item is
            // 1 (REF_getField), 2 (REF_getStatic), 3 (REF_putField)  or 4 (REF_putStatic),
            // then the constant_pool entry at that index must be a CONSTANT_Fieldref_info structure (4.4.2)
            // representing a field for which a method handle is to be created. jvms-4.4.8-200-C-A
            case REF_GETFIELD:
            case REF_GETSTATIC:
            case REF_PUTFIELD:
            case REF_PUTSTATIC:
                refCell = pool.FindCell(cpParser.parseConstValue(ConstType.CONSTANT_FIELD));
                break;
            //  If the value of the reference_kind item is
            //  5 (REF_invokeVirtual) or 8 (REF_newInvokeSpecial),
            //  then the constant_pool entry at that index must be a CONSTANT_Methodref_info structure (4.4.2)
            //  representing a class's method or constructor (2.9.1) for which a method handle is to be created.
            //  jvms-4.4.8-200-C-B
            case REF_INVOKEVIRTUAL:
            case REF_NEWINVOKESPECIAL:
                cpParser.setExitImmediately(true);
                refCell = cpParser.parseConstRef(ConstType.CONSTANT_METHOD, ConstType.CONSTANT_INTERFACEMETHOD);
                cpParser.setExitImmediately(false);
                checkReferenceIndex(pos, ConstType.CONSTANT_METHOD, null);
                break;
            case REF_INVOKESTATIC:
            case REF_INVOKESPECIAL:
                // CODETOOLS-7902333
                // 4.4.8. The CONSTANT_MethodHandle_info Structure
                // reference_index
                // The value of the reference_index item must be a valid index into the constant_pool table.
                // The constant_pool entry at that index must be as follows:
                // If the value of the reference_kind item is 6 (REF_invokeStatic) or 7 (REF_invokeSpecial),
                // then if the class file version number is less than 52.0, the constant_pool entry at that index must be
                // a CONSTANT_Methodref_info structure representing a class's method for which a method handle is to be created;
                // if the class file version number is 52.0 or above, the constant_pool entry at that index must be
                // either a CONSTANT_Methodref_info structure or a CONSTANT_InterfaceMethodref_info structure (4.4.2)
                // representing a class's or interface's method for which a method handle is to be created.
                ConstType ctype01 = ConstType.CONSTANT_METHOD;
                ConstType ctype02 = ConstType.CONSTANT_INTERFACEMETHOD;
                if (this.cd.cfv.major_version() >= 52 && Modifiers.isInterface(this.cd.access)) {
                    ctype01 = ConstType.CONSTANT_INTERFACEMETHOD;
                    ctype02 = ConstType.CONSTANT_METHOD;
                }
                cpParser.setExitImmediately(true);
                refCell = cpParser.parseConstRef(ctype01, ctype02);
                cpParser.setExitImmediately(false);
                checkReferenceIndex(pos, ctype01, ctype02);
                break;

            case REF_INVOKEINTERFACE:
                cpParser.setExitImmediately(true);
                refCell = cpParser.parseConstRef(ConstType.CONSTANT_INTERFACEMETHOD, ConstType.CONSTANT_METHOD);
                cpParser.setExitImmediately(false);
                checkReferenceIndex(pos, ConstType.CONSTANT_INTERFACEMETHOD, null);
                break;
            default:
                // should not reach
                throw new Scanner.SyntaxError();
        }
        return refCell;
    }

    /**
     * Check the pair reference_kind:reference_index where reference_kind is any from:
     * REF_invokeVirtual, REF_newInvokeSpecial, REF_invokeStatic, REF_invokeSpecial, REF_invokeInterface
     * and reference_index is one of [Empty], Method or InterfaceMethod
     * There are possible entries:
     * ldc Dynamic REF_newInvokeSpecial:InterfaceMethod  LdcConDyTwice."<init>":
     * ldc Dynamic REF_invokeInterface:LdcConDyTwice."<init>":
     * ldc Dynamic REF_newInvokeSpecial:Method LdcConDyTwice."<init>":
     * ldc MethodHandle REF_newInvokeSpecial:InterfaceMethod  LdcConDyTwice."<init>":
     * ldc MethodHandle REF_invokeInterface:LdcConDyTwice."<init>":
     * ldc MethodHandle REF_newInvokeSpecial:Method LdcConDyTwice."<init>":
     * invokedynamic MethodHandle REF_invokeStatic:Method java/lang/invoke/StringConcatFactory.makeConcatWithConstants:
     * invokedynamic MethodHandle REF_invokeStatic:java/lang/invoke/StringConcatFactory.makeConcatWithConstants
     * ....
     * @param position   the position in a source file
     * @param defaultTag expected reference_index tag (Method or InterfaceMethod)
     * @param defaultTag 2nd expected reference_index tag (Method or InterfaceMethod)
     */
    private void checkReferenceIndex(int position, ConstType defaultTag, ConstType default2Tag) {
        if ( ! scanner.token.in(COLON, SEMICOLON) ) {
            if (default2Tag != null) {
                env.error(position, "wrong.tag2", defaultTag.parseKey(), default2Tag.parseKey());
            } else {
                env.error(position, "wrong.tag", defaultTag.parseKey());
            }
            throw new Scanner.SyntaxError().Fatal();
        }
    }

    /**
     * Parses a sub-tag value in method handle.
     */
    SubTag parseSubtag() throws Scanner.SyntaxError, IOException {
        SubTag subtag = null;
        switch (scanner.token) {
            case IDENT:
                subtag = subtag(scanner.stringValue);
                break;
            case INTVAL:
                subtag = subtag(scanner.intValue);
                break;
        }
        if (subtag == null) {
            env.error("subtag.expected");
            throw new Scanner.SyntaxError();
        }
        scanner.scan();
        return subtag;
    }

    ConstCell parseClassName(boolean uncond) throws Scanner.SyntaxError, IOException {
        String v;
        switch (scanner.token) {
            case CPINDEX: {
                int cpx = scanner.intValue;
                scanner.scan();
                return pool.getCell(cpx);
            }
            case STRINGVAL:
                v = scanner.stringValue;
                scanner.scan();
                v = prependPackage(v, uncond);
                return pool.FindCellAsciz(v);
            // Some identifiers might coincide with token names.
            // these should be OK to use as identifier names.
            case OPEN:
            case MODULE:
            case VARARGS:
            case REQUIRES:
            case EXPORTS:
            case TO:
            case USES:
            case PROVIDES:
            case WITH:
            case OPENS:

            case ARRAY_TYPEPATH:
            case INNER_TYPE_TYPEPATH:
            case WILDCARD_TYPEPATH:
            case TYPE_ARGUMENT_TYPEPATH:
            case PERMITTEDSUBCLASSES:
            case INF:
            case NAN:
            case COMPONENT:

            case SYNTHETIC:
            case DEPRECATED:
            case VERSION:
            case BITS:
            case STACK:
            case LOCAL:
            case OF:
            case INNERCLASS:
            case STRICT:
            case FIELDREF:
            case METHODREF:
            case BRIDGE:
            case IDENT:
            case VALUE:
            case PERMITS_VALUE:
            case PRIMITIVE:
                v = scanner.idValue;
                scanner.scan();
                v = prependPackage(v, uncond);
                return pool.FindCellAsciz(v);
            default:
                ConstType key = Tables.tag(scanner.token.value());
                env.traceln("%%%%% Unrecognized token [" + scanner.token + "]: '" + (key == null ? "null" : key.parseKey()) + "'.");
                env.error(scanner.prevPos, "name.expected", "\"" + scanner.token.parseKey() + "\"");
                throw new Scanner.SyntaxError();
        }
    }

    private String prependPackage(String className, boolean uncond) {
        if (uncond || (scanner.token == Token.FIELD)) {
            if ((!className.contains("/"))             // class identifier doesn't contain "/"
                    && (!className.contains("["))) {    // class identifier doesn't contain "["
                className = pkgPrefix + className; // add package
            }
        }
        return className;
    }

    /**
     * Parse a signed integer of size bytes long.
     * size = 1 or 2
     */
    Argument parseInt(int size) throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.BITS) {
            scanner.scan();
        }
        if (scanner.token != Token.INTVAL) {
            env.error(scanner.pos, "int.expected");
            throw new Scanner.SyntaxError();
        }
        int arg = scanner.intValue * scanner.sign;
        switch (size) {
            case 1:
//                if ((arg>127)||(arg<-128)) { // 0xFF not allowed
                if ((arg > 255) || (arg < -128)) { // to allow 0xFF
                    env.error(scanner.pos, "value.large", "1 byte");
                    throw new Scanner.SyntaxError();
                }
                break;
            case 2:
//                if ((arg > 32767) || (arg < -32768)) { //this seems
// natural but is not backward compatible. Some tests contain
// expressions like:
//                sipush    0x8765;

                if ((arg > 65535) || (arg < -32768)) {
                    env.error(scanner.pos, "value.large", "2 bytes");
                    throw new Scanner.SyntaxError();
                }
                break;
            default:
                throw new InternalError("parseInt(" + size + ")");
        }
        scanner.scan();
        return new Argument(arg);
    }

    /**
     * Parse an unsigned integer of size bytes long.
     * size = 1 or 2
     */
    Argument parseUInt(int size) throws Scanner.SyntaxError, IOException {
        if (scanner.token != Token.INTVAL) {
            env.error(scanner.pos, "int.expected");
            throw new Scanner.SyntaxError();
        }
        if (scanner.sign == -1) {
            env.error(scanner.pos, "neg.forbidden");
            throw new Scanner.SyntaxError();
        }
        int arg = scanner.intValue;
        switch (size) {
            case 1:
                if (arg > 255) {
                    env.error(scanner.pos, "value.large", "1 byte");
                    throw new Scanner.SyntaxError();
                }
                break;
            case 2:
                if (arg > 65535) {
                    env.error(scanner.pos, "value.large", "2 bytes");
                    throw new Scanner.SyntaxError();
                }
                break;
            default:
                throw new InternalError("parseUInt(" + size + ")");
        }
        scanner.scan();
        return new Argument(arg);
    }

    /**
     * Parse constant declaration
     */
    private void parseConstDef() throws IOException {
        for (; ; ) {
            if (scanner.token == Token.CPINDEX) {
                int cpx = scanner.intValue;
                scanner.scan();
                scanner.expect(Token.ASSIGN);
                env.traceln("parseConstDef:" + cpx);
                pool.setCell(cpx, cpParser.parseConstRef(null));
            } else {
                env.error("const.def.expected");
                throw new Scanner.SyntaxError();
            }
            if (scanner.token != COMMA) {
                scanner.expect(SEMICOLON);
                return;
            }
            scanner.scan(); // COMMA
        }
    }

    /**
     * Parse the modifiers
     */
    private int scanModifier(int mod) throws IOException {
        int nextmod, prevpos;

        while (true) {
            nextmod = 0;
            switch (scanner.token) {
                case PUBLIC:
                    nextmod = ACC_PUBLIC;
                    break;
                case PRIVATE:
                    nextmod = ACC_PRIVATE;
                    break;
                case PROTECTED:
                    nextmod = ACC_PROTECTED;
                    break;
                case STATIC:
                    nextmod = ACC_STATIC;
                    break;
                case FINAL:
                    nextmod = ACC_FINAL;
                    break;
                case SYNCHRONIZED:
                    nextmod = ACC_SYNCHRONIZED;
                    break;
                case SUPER:
                    nextmod = ACC_SUPER;
                    break;
                case VOLATILE:
                    nextmod = ACC_VOLATILE;
                    break;
                case BRIDGE:
                    nextmod = ACC_BRIDGE;
                    break;
                case TRANSIENT:
                    nextmod = ACC_TRANSIENT;
                    break;
                case VARARGS:
                    nextmod = ACC_VARARGS;
                    break;
                case NATIVE:
                    nextmod = ACC_NATIVE;
                    break;
                case INTERFACE:
                    nextmod = ACC_INTERFACE;
                    break;
                case ABSTRACT:
                    nextmod = ACC_ABSTRACT;
                    break;
                case STRICT:
                    nextmod = ACC_STRICT;
                    break;
                case ENUM:
                    nextmod = ACC_ENUM;
                    break;
                case SYNTHETIC:
                    nextmod = ACC_SYNTHETIC;
                    break;
                case ANNOTATION_ACCESS:
                    nextmod = ACC_ANNOTATION;
                    break;

                case DEPRECATED:
                    nextmod = DEPRECATED_ATTRIBUTE;
                    break;
                case MANDATED:
                    nextmod = ACC_MANDATED;
                    break;
                case VALUE:
                    nextmod = ACC_VALUE;
                    break;
                case PERMITS_VALUE:
                    nextmod = ACC_PERMITS_VALUE;
                    break;
                case PRIMITIVE:
                    nextmod = ACC_PRIMITIVE;
                    break;
                default:
                    return nextmod;
            }
            prevpos = scanner.pos;
            scanner.scan();
            if ((mod & nextmod) == 0) {
                return nextmod;
            }
            env.error(prevpos, "warn.repeated.modifier");
        }
    }

    int scanModifiers() throws IOException {
        int mod = 0, nextmod;

        while (true) {
            nextmod = scanModifier(mod);
            if (nextmod == 0) {
                return mod;
            }
            mod = mod | nextmod;
        }
    }

    /**
     * Parse a field.
     */
    private void parseField(int mod) throws Scanner.SyntaxError, IOException {
        debugStr("  [Parser.parseField]: <<<Begin>>>");
        // check access modifiers:
        Modifiers.checkFieldModifiers(cd, mod, scanner.pos);

        while (true) {
            ConstCell nameCell = parseName();
            scanner.expect(Token.COLON);
            ConstCell typeCell = parseName();

            // Define the variable
            FieldData fld = cd.addField(mod, nameCell, typeCell);

            if (memberAnnttns != null) {
                fld.addAnnotations(memberAnnttns);
            }

            // Parse the optional attribute: signature
            if (scanner.token == Token.COLON) {
                scanner.scan();
                ConstCell signatureCell = parseName();
                fld.setSignatureAttr(signatureCell);
            }

            // Parse the optional initializer
            if (scanner.token == Token.ASSIGN) {
                scanner.scan();
                fld.SetValue(cpParser.parseConstRef(null));
            }

            // If the next scanner.token is a comma, then there is more
            debugScan("  [Parser.parseField]: Field: " + fld + " ");

            if (scanner.token != COMMA) {
                scanner.expect(SEMICOLON);
                return;
            }
            scanner.scan();
        }  // end while
    }  // end parseField

    /**
     * Scan method's signature to determine size of parameters.
     */
    private int countParams(ConstCell sigCell) throws Scanner.SyntaxError {
        String sig;
        try {
            ConstValue_String strConst = (ConstValue_String) sigCell.ref;
            sig = strConst.value;
        } catch (NullPointerException | ClassCastException e) {
            return 0; // ??? TBD
        }
        int siglen = sig.length(), k = 0, loccnt = 0, errparam = 0;
        boolean arraytype = false;
        scan:
        {
            if (k >= siglen) {
                break scan;
            }
            if (sig.charAt(k) != '(') {
                errparam = 1;
                break scan;
            }
            for (k = 1; k < siglen; k++) {
                switch (sig.charAt(k)) {
                    case ')':
                        if (arraytype) {
                            errparam = 2;
                            break scan;
                        }
                        return loccnt;
                    case '[':
                        arraytype = true;
                        break;
                    case 'B':
                    case 'C':
                    case 'F':
                    case 'I':
                    case 'S':
                    case 'Z':
                        loccnt++;
                        arraytype = false;
                        break;
                    case 'D':
                    case 'J':
                        loccnt++;
                        if (arraytype) {
                            arraytype = false;
                        } else {
                            loccnt++;
                        }
                        break;
                    case 'L':
                    case 'Q':
                        for (; ; k++) {
                            if (k >= siglen) {
                                errparam = 3;
                                break scan;
                            }
                            if (sig.charAt(k) == ';') {
                                break;
                            }
                        }
                        loccnt++;
                        arraytype = false;
                        break;
                    default:
                        errparam = 4;
                        break scan;
                }
            }
        }
        env.error(scanner.pos, "msig.malformed", Integer.toString(k), Integer.toString(errparam));
        return loccnt;
    }

    /**
     * Parse a method.
     */
    private void parseMethod(int mod) throws Scanner.SyntaxError, IOException {

        // The start of the method
        int posa = scanner.pos;
        debugStr("  [Parser.parseMethod]: <<<Begin>>>");

        ConstCell nameCell = parseName();
        ConstValue_String strConst = (ConstValue_String) nameCell.ref;
        String name = strConst.value;
        boolean is_clinit = name.equals("<clinit>");
        boolean is_init = name.equals("<init>")
            && !Modifiers.isStatic(mod); // TODO: not a good way to detect factories...
        DefaultAnnotationAttr defAnnot = null;

        // check access modifiers:
        Modifiers.checkMethodModifiers(cd, mod, posa, is_init, is_clinit);

        scanner.expect(Token.COLON);
        ConstCell typeCell = parseName();
        int paramcnt = countParams(typeCell);
        if ((!Modifiers.isStatic(mod)) && !is_clinit) {
            paramcnt++;
        }
        if (paramcnt > 255) {
            env.error(scanner.pos, "warn.msig.more255", Integer.toString(paramcnt));
        }
        // Parse throws clause
        ArrayList<ConstCell> exc_table = null;
        if (scanner.token == Token.THROWS) {
            scanner.scan();
            exc_table = new ArrayList<>();
            for (; ; ) {
                posa = scanner.pos;
                ConstCell exc = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
                if (exc_table.contains(exc)) {
                    env.error(posa, "warn.exc.repeated");
                } else {
                    exc_table.add(exc);
                    env.traceln("THROWS:" + exc.arg);
                }
                if (scanner.token != COMMA) {
                    break;
                }
                scanner.scan();
            }
        }
        if (scanner.token == Token.DEFAULT) {
            // need to scan the annotation value
            defAnnot = annotParser.parseDefaultAnnotation();
        }

        MethodData curMethod = cd.StartMethod(mod, nameCell, typeCell, exc_table);
        Argument max_stack = null, max_locals = null;

        if (scanner.token == Token.STACK) {
            scanner.scan();
            max_stack = parseUInt(2);
        }
        if (scanner.token == Token.LOCAL) {
            scanner.scan();
            max_locals = parseUInt(2);
        }
        if (scanner.token == Token.INTVAL) {
            annotParser.parseParamAnnots(paramcnt, curMethod);
        }

        if (scanner.token == SEMICOLON) {
            if ((max_stack != null) || (max_locals != null)) {
                env.error("token.expected", "{");
            }
            scanner.scan();
        } else {
            scanner.expect(Token.LBRACE);
            curCode = curMethod.startCode(posa, paramcnt, max_stack, max_locals);
            while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
                instrParser.parseInstr();
                if (scanner.token == Token.RBRACE) {
                    break;
                }
                // code's type annotation(s)
                if (scanner.token == Token.ANNOTATION) {
                    curCode.addAnnotations(annotParser.scanAnnotations());
                    break;
                }
                scanner.expect(SEMICOLON);
            }
            curCode.endCode();
            scanner.expect(Token.RBRACE);
        }

        if (defAnnot != null) {
            curMethod.addDefaultAnnotation(defAnnot);
        }
        if (memberAnnttns != null) {
            curMethod.addAnnotations(memberAnnttns);
        }
        cd.EndMethod();
        debugStr("  [Parser.parseMethod]: Method: " + curMethod);

    }  // end parseMethod

    /**
     * Parse a (CPX based) BootstrapMethod entry.
     */
    private void parseCPXBootstrapMethod() throws Scanner.SyntaxError, IOException {
        // Parses in the form:
        // BOOTSTRAPMETHOD CPX_MethodHandle (CPX_Arg)* ;
        if (scanner.token == Token.CPINDEX) {
            // CPX can be a CPX to an MethodHandle constant,
            int cpx = scanner.intValue;
            ConstCell MHCell = pool.getCell(cpx);
            scanner.scan();
            ArrayList<ConstCell> bsm_args = new ArrayList<>(256);

            while (scanner.token != SEMICOLON) {
                if (scanner.token == Token.CPINDEX) {
                    bsm_args.add(pool.getCell(scanner.intValue));

                } else {
                    // throw error, bootstrap method is not recognizable
                    env.error(scanner.pos, "invalid.bootstrapmethod");
                    throw new Scanner.SyntaxError();
                }
                scanner.scan();
            }
            BootstrapMethodData bsmData = new BootstrapMethodData(MHCell, bsm_args);
            cd.addBootstrapMethod(bsmData);
        } else {
            // throw error, bootstrap method is not recognizable
            env.error(scanner.pos, "invalid.bootstrapmethod");
            throw new Scanner.SyntaxError();
        }
    }

    /**
     * Parse a NestHost entry
     */
    private void parseNestHost() throws Scanner.SyntaxError, IOException {
        // Parses in the form:
        // NESTHOST IDENT;
        debugStr("  [Parser.parseNestHost]: <<<Begin>>>");
        String className = prependPackage(parseIdent(), true);
        ConstCell hostClass = pool.FindCellClassByName(className);
        debugScan("  [Parser.parseNestHost]: NestHost: class " + className);
        scanner.expect(SEMICOLON);
        cd.addNestHost(hostClass);
    }

    /**
     * Parse a list of classes belonging to the
     * [NestMembers | PermittedSubclasses | Preload]  entry
     */
    private void parseClasses(Consumer<ArrayList<ConstCell>> classesConsumer)
            throws Scanner.SyntaxError, IOException {
        ArrayList<ConstCell> classes = new ArrayList<>();
        // Parses in the form:
        // (NESTMEMBERS|PERMITTEDSUBCLASSES)? IDENT(, IDENT)*;
        debugStr("  [Parser.parseClasses]: <<<Begin>>>");
        while (true) {
            String className = prependPackage(parseIdent(), true);
            classes.add(pool.FindCellClassByName(className));
            debugScan("  [Parser.parseClasses]: class " + className);
            if (scanner.token != COMMA) {
                scanner.expect(SEMICOLON);
                classesConsumer.accept(classes);
                return;
            }
            scanner.scan();
        }
    }

    /**
     * Parse the Record entry
     */
    private void parseRecord() throws Scanner.SyntaxError, IOException {
        // Parses in the form:
        // RECORD { (COMPONENT)+ }
        // where
        // COMPONENT Component (ANNOTATION)* NAME:DESCRIPTOR(:SIGNATURE)? (,|;)
        // NAME = (CPINDEX | IDENT)
        // DESCRIPTOR = (CPINDEX | STRING)
        // SIGNATURE  = (CPINDEX | STRING)
        debugScan("[Parser.parseRecord]:  Begin");
        scanner.expect(Token.LBRACE);

        ArrayList<AnnotationData> componentAnntts = null;
        boolean grouped = false;
        RecordData rd = cd.setRecord(scanner.pos);

        while (true) {
            if (scanner.token == Token.RBRACE) {
                if (rd.isEmpty()) {
                    env.error(scanner.pos, "warn.no.components.in.record.attribute");
                    cd.rejectRecord();
                } else if (grouped) {
                    env.error(scanner.pos, "grouped.component.expected");
                }
                scanner.scan();
                break;
            }

            ConstCell nameCell, descCell, signatureCell = null;
            if (scanner.token == Token.ANNOTATION) {
                componentAnntts = annotParser.scanAnnotations();
            }

            scanner.expect(Token.COMPONENT);

            nameCell = parseName();
            scanner.expect(Token.COLON);
            descCell = parseName();
            // Parse the optional attribute: signature
            if (scanner.token == Token.COLON) {
                scanner.scan();
                signatureCell = parseName();
            }

            rd.addComponent(nameCell, descCell, signatureCell, componentAnntts);

            switch (scanner.token) {
                case COMMA:
                    grouped = true;
                    break;
                case SEMICOLON:
                    grouped = false;
                    componentAnntts = null;
                    break;
                default:
                    env.error(scanner.pos, "one.of.two.token.expected",
                            "<" + SEMICOLON.printValue() + ">",
                            "<" + COMMA.printValue() + ">");
                    break;
            }
            // next component
            scanner.scan();
        }  // end while
        debugScan("[Parser.parseRecord]:  End");
    }

    /**
     * Parse an inner class.
     */
    private void parseInnerClass(int mod) throws Scanner.SyntaxError, IOException {
        // Parses in the form:
        // MODIFIERS (INNERCLASSNAME =)? (INNERCLASS) (OF OUTERCLASS)? ;
        //
        // where
        //    INNERCLASSNAME = (IDENT | CPX_IN-CL-NM)
        //    INNERCLASS = (CLASS IDENT | CPX_IN-CL) (S2)
        //    OUTERCLASS = (CLASS IDENT | CPX_OT-CL) (S3)
        //
        // Note:
        //    If a class reference cannot be identified using IDENT, CPX indexes must be used.

        // check access modifiers:
        debugScan("[Parser.parseInnerClass]:  Begin ");
        Modifiers.checkInnerClassModifiers(cd, mod, scanner.pos);

        ConstCell nameCell;
        ConstCell innerClass = null;
        ConstCell outerClass = null;


        if (scanner.token == Token.CLASS) {
            nameCell = pool.getCell(0);  // no NameIndex
            parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
        } else {
            if ((scanner.token == Token.IDENT) || scanner.checkTokenIdent()) {
                // Got a Class Name
                nameCell = parseName();
                parseInnerClass_s1(mod, nameCell, innerClass, outerClass);
            } else if (scanner.token == Token.CPINDEX) {
                // CPX can be either a CPX to an InnerClassName,
                // or a CPX to an InnerClassInfo
                int cpx = scanner.intValue;
                nameCell = pool.getCell(cpx);
                ConstValue nameCellValue = nameCell.ref;

                if (nameCellValue instanceof ConstValue_String) {
                    // got a name cell
                    scanner.scan();
                    parseInnerClass_s1(mod, nameCell, innerClass, outerClass);
                } else {
                    // got a CPRef cell
                    nameCell = pool.getCell(0);  // no NameIndex
                    parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
                }
            } else {
                pic_error();
            }

        }
    }

    private void parseInnerClass_s1(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        // next scanner.token must be '='
        if (scanner.token == Token.ASSIGN) {
            scanner.scan();
            parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
        } else {
            pic_error();
        }

    }

    private void parseInnerClass_s2(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        // scanner.token is either "CLASS IDENT" or "CPX_Class"
        if ((scanner.token == Token.CPINDEX) || (scanner.token == Token.CLASS)) {
            if (scanner.token == Token.CPINDEX) {
                innerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            if (scanner.token == Token.CLASS) {
                // next symbol needs to be InnerClass
                scanner.scan();
                innerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            // See if declaration is terminated
            if (scanner.token == SEMICOLON) {
                // InnerClass is complete, no OUTERINFO;
                outerClass = pool.getCell(0);
                pic_tracecreate(mod, nameCell, innerClass, outerClass);
                cd.addInnerClass(mod, nameCell, innerClass, outerClass);
            } else if (scanner.token == Token.OF) {
                // got an outer class reference
                parseInnerClass_s3(mod, nameCell, innerClass, outerClass);
            } else {
                pic_error();
            }

        } else {
            pic_error();
        }

    }

    private void parseInnerClass_s3(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        scanner.scan();
        if ((scanner.token == Token.CLASS) || (scanner.token == Token.CPINDEX)) {
            if (scanner.token == Token.CLASS) {
                // next symbol needs to be InnerClass
                scanner.scan();
                outerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }
            if (scanner.token == Token.CPINDEX) {
                outerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            if (scanner.token == SEMICOLON) {
                pic_tracecreate(mod, nameCell, innerClass, outerClass);
                cd.addInnerClass(mod, nameCell, innerClass, outerClass);
            } else {
                pic_error();
            }
        } else {
            pic_error();
        }
    }

    private void pic_tracecreate(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) {
        // throw error, IC is not recognizable
        env.trace(" Creating InnerClass: [" + Modifiers.toString(mod, CF_Context.CTX_INNERCLASS) + "], ");

        if (nameCell != pool.getCell(0)) {
            ConstValue value = nameCell.ref;
            if (value != null) {
                env.trace(value.toString() + " = ");
            }
        }

        ConstValue_Cell ici_val = (ConstValue_Cell) innerClass.ref;
        ConstCell ici_ascii = ici_val.cell;
        // Constant pool may not be numberized yet.
        //
        // check values before dereference on a trace.
        if (ici_ascii.ref == null) {
            env.trace("<#cpx-unresolved> ");
        } else {
            ConstValue_String cval = (ConstValue_String) ici_ascii.ref;
            if (cval.value == null) {
                env.trace("<#cpx-0> ");
            } else {
                env.trace(cval.value + " ");
            }
        }

        if (outerClass != pool.getCell(0)) {
            if (outerClass.arg != 0) {
                ConstValue_Cell oci_val = (ConstValue_Cell) outerClass.ref;
                ConstCell oci_ascii = oci_val.cell;
                if (oci_ascii.ref == null) {
                    env.trace(" of <#cpx-unresolved>  ");
                } else {
                    ConstValue_String cval = (ConstValue_String) oci_ascii.ref;
                    if (cval.value == null) {
                        env.trace(" of <#cpx-0>  ");
                    } else {
                        env.trace(" of " + cval.value);
                    }
                }
            }
        }

        env.traceln("");
    }

    private void pic_error() {
        // throw error, IC is not recognizable
        env.error(scanner.pos, "invalid.innerclass");
        throw new Scanner.SyntaxError();
    }

    /**
     * The match() method is used to quickly match opening
     * brackets (ie: '(', '{', or '[') with their closing
     * counter part. This is useful during error recovery.<p>
     * <p>
     * Scan to a matching '}', ']' or ')'. The current scanner.token must be
     * a '{', '[' or '(';
     */
    private void match(Token open, Token close) throws IOException {
        int depth = 1;

        while (true) {
            scanner.scan();
            if (scanner.token == open) {
                depth++;
            } else if (scanner.token == close) {
                if (--depth == 0) {
                    return;
                }
            } else if (scanner.token == Token.EOF) {
                env.error(scanner.pos, "unbalanced.paren");
                return;
            }
        }
    }

    /**
     * Recover after a syntax error in a field. This involves
     * discarding scanner.tokens until an EOF or a possible legal
     * continuation is encountered.
     */
    private void recoverField() throws Scanner.SyntaxError, IOException {
        while (true) {
            switch (scanner.token) {
                case EOF:
                case STATIC:
                case FINAL:
                case PUBLIC:
                case PRIVATE:
                case SYNCHRONIZED:
                case TRANSIENT:
                case PROTECTED:
                case VOLATILE:
                case NATIVE:
//                case INTERFACE: see below
                case ABSTRACT:
                case ANNOTATION_ACCESS:
                    // possible begin of a field, continue
                    return;

                case LBRACE:
                    match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    match(Token.LSQBRACKET, Token.RSQBRACKET);
                    scanner.scan();
                    break;

                case RBRACE:
                case INTERFACE:
                case CLASS:
                case IMPORT:
                case PACKAGE:
                    // begin of something outside a class, panic more
                    endClass();
                    scanner.debugStr("    [Parser.recoverField]: pos: [" + scanner.pos + "]: ");
                    throw new Scanner.SyntaxError().Fatal();
                default:
                    // don't know what to do, skip
                    scanner.scan();
                    break;
            }
        }
    }

    /**
     * Parse a class or interface declaration.
     */
    private void parseClass(int mod) throws IOException {
        int posa = scanner.pos;
        debugStr("   [Parser.parseClass]:  Begin ");
        // check access modifiers:
        Modifiers.checkClassModifiers(env, mod, scanner);

        if (cd == null) {
            cd = new ClassData(env, currentCFV.clone());
            pool = cd.pool;
        }

        if (clsAnnttns != null) {
            cd.addAnnotations(clsAnnttns);
        }

        // move the tokenizer to the identifier:
        if (scanner.token == Token.CLASS) {
            scanner.scan();
        } else if (scanner.token == Token.ANNOTATION) {
            scanner.scan();
            if (scanner.token == Token.INTERFACE) {
                mod |= ACC_ANNOTATION | ACC_INTERFACE;
                scanner.scan();
            } else {
                env.error(scanner.prevPos, "token.expected", Token.ANNOTATION.parseKey() + Token.INTERFACE.parseKey());
                throw new Scanner.SyntaxError();
            }
        }

        // Parse the class name
        ConstCell nm = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);

        if (scanner.token == Token.FIELD) { // DOT
            String fileExtension;
            scanner.scan();
            switch (scanner.token) {
                case STRINGVAL:
                    fileExtension = scanner.stringValue;
                    break;
                case IDENT:
                    fileExtension = scanner.idValue;
                    break;
                default:
                    env.error(scanner.pos, "name.expected");
                    throw new Scanner.SyntaxError();
            }
            scanner.scan();
            cd.fileExtension = "." + fileExtension;
        } else if (scanner.token == Token.MODULE) {
            env.error(scanner.prevPos, "token.expected", Token.OPEN.parseKey());
            throw new Scanner.SyntaxError();
        } else if (scanner.token == SEMICOLON) {
            // drop the semi-colon following a name
            scanner.scan();
        }

        // Parse extends clause
        ConstCell sup = null;
        if (scanner.token == Token.EXTENDS) {
            scanner.scan();
            sup = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            while (scanner.token == COMMA) {
                scanner.scan();
                env.error(posa, "multiple.inherit");
                cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }
        }

        // Parse implements clause
        ArrayList<Argument> impl = new ArrayList<>();
        if (scanner.token == Token.IMPLEMENTS) {
            do {
                scanner.scan();
                Argument intf = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
                if (impl.contains(intf)) {
                    env.error(posa, "warn.intf.repeated", intf);
                } else {
                    impl.add(intf);
                }
            } while (scanner.token == COMMA);
        }
        parseVersion();
        scanner.expect(Token.LBRACE);

        // Begin a new class
        cd.init(mod, nm, sup, impl);

        // Parse constant declarations

        // Parse class members
        while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
            switch (scanner.token) {
                case SEMICOLON:
                    // Empty fields are allowed
                    scanner.scan();
                    break;
                case CONST:
                    scanner.scan();
                    parseConstDef();
                    explicitcp = true;
                    break;
                default:   // scanner.token is some member.
                    parseClassMembers();
            }  // end switch
        } // while
        scanner.expect(Token.RBRACE);
        // End the class
        endClass();
    } // end parseClass

    /**
     * Parses a package or type name in a module statement(s)
     */
    private String parseTypeName() throws IOException {
        String name = "", field = "";
        while (true) {
            if (scanner.token.possibleModuleName()) {
                name = name + field + scanner.idValue;
                scanner.scan();
            } else {
                env.error(scanner.pos, "name.expected", "\"" + scanner.token.parseKey() + "\"");
                throw new Scanner.SyntaxError();
            }
            if (scanner.token == Token.FIELD) {
                env.error(scanner.pos, "warn.dot.will.be.converted");
                field = "/";
                scanner.scan();
            } else {
                break;
            }
        }
        return name;
    }

    /**
     * Parses a module name in a module statement(s)
     */
    private String parseModuleName() throws IOException {
        String name = "", field = "";
        while (true) {
            if (scanner.token.possibleModuleName()) {
                name = name + field + scanner.idValue;
                scanner.scanModuleStatement();
            } else {
                env.error(scanner.pos, "module.name.expected", "\"" + scanner.token.parseKey() + "\"");
                throw new Scanner.SyntaxError().Fatal();
            }
            if (scanner.token == Token.FIELD) {
                field = Character.toString((char) scanner.token.value());
                scanner.scanModuleStatement();
            } else {
                break;
            }
        }
        return name;
    }

    /**
     * Parse a module declaration.
     */
    private void parseModule() throws IOException {
        debugStr("   [Parser.parseModule]:  Begin ");
        if (cd == null) {
            cd = new ClassData(env, currentCFV.clone());
            pool = cd.pool;
        }
        if (clsAnnttns != null) {
            cd.addAnnotations(clsAnnttns);
        }
        moduleAttribute = new ModuleAttr(cd);

        if (scanner.token == Token.OPEN) {
            moduleAttribute.openModule();
            scanner.scan();
        }

        // move the tokenizer to the identifier:
        if (scanner.token == Token.MODULE) {
            scanner.scanModuleStatement();
            // scanner.scan();
        } else {
            env.error(scanner.pos, "token.expected", Token.MODULE.parseKey());
            throw new Scanner.SyntaxError().Fatal();
        }
        // Parse the module name
        String moduleName = parseModuleName();
        if (moduleName.isEmpty()) {
            env.error(scanner.pos, "name.expected");
            throw new Scanner.SyntaxError().Fatal();
        }
        moduleAttribute.setModuleName(moduleName);

        parseVersion();
        scanner.expect(Token.LBRACE);

        // Begin a new class as module
        cd.initAsModule();

        // Parse module statement(s)
        while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
            switch (scanner.token) {
                case REQUIRES:
                    scanRequires(moduleAttribute.requires);
                    break;
                case EXPORTS:
                    scanStatement(moduleAttribute.exports,
                            this::parseTypeName,
                            this::parseModuleName,
                            Token.TO,
                            true,
                            "exports.expected");
                    break;
                case OPENS:
                    scanStatement(moduleAttribute.opens,
                            this::parseTypeName,
                            this::parseModuleName,
                            Token.TO, true, "opens.expected");
                    break;
                case USES:
                    scanStatement(moduleAttribute.uses, "uses.expected");
                    break;
                case PROVIDES:
                    scanStatement(moduleAttribute.provides,
                            this::parseTypeName,
                            this::parseTypeName,
                            Token.WITH,
                            false,
                            "provides.expected");
                    break;
                case SEMICOLON:
                    // Empty fields are allowed
                    scanner.scan();
                    break;
                default:
                    env.error(scanner.pos, "module.statement.expected");
                    throw new Scanner.SyntaxError().Fatal();
            }  // end switch
        } // while
        scanner.expect(Token.RBRACE);
        // End the module
        endModule();
    } // end parseModule

    /**
     * Scans  ModuleStatement: requires [transitive] [static] ModuleName ;
     */
    private void scanRequires(BiConsumer<String, Integer> action) throws IOException {
        int flags = 0;
        String mn = "";
        scanner.scanModuleStatement();
        while (scanner.token != SEMICOLON) {
            switch (scanner.token) {
                case STATIC:
                    if (((flags & (1 << Module.Modifier.ACC_STATIC_PHASE.asInt())) != 0) || !mn.isEmpty()) {
                        env.error(scanner.pos, "requires.expected");
                        throw new Scanner.SyntaxError().Fatal();
                    }
                    flags |= Module.Modifier.ACC_STATIC_PHASE.asInt();
                    break;
                case TRANSITIVE:
                    if (((flags & (1 << Module.Modifier.ACC_TRANSITIVE.asInt())) != 0) || !mn.isEmpty()) {
                        env.error(scanner.pos, "requires.expected");
                        throw new Scanner.SyntaxError().Fatal();
                    }
                    flags |= Module.Modifier.ACC_TRANSITIVE.asInt();
                    break;
                case IDENT:
                    if (!mn.isEmpty()) {
                        env.error(scanner.pos, "requires.expected");
                        throw new Scanner.SyntaxError().Fatal();
                    }
                    mn = parseModuleName();
                    continue;
                default:
                    if (mn.isEmpty() && scanner.token.possibleModuleName()) {
                        mn = parseModuleName();
                        continue;
                    } else {
                        env.error(scanner.pos, "requires.expected");
                        throw new Scanner.SyntaxError().Fatal();
                    }
            }
            scanner.scanModuleStatement();
        }
        // Token.SEMICOLON
        if (mn.isEmpty()) {
            env.error(scanner.pos, "requires.expected");
            throw new Scanner.SyntaxError().Fatal();
        }
        action.accept(mn, flags);
        scanner.scanModuleStatement();
    }

    /**
     * Scans  ModuleStatement: uses TypeName;
     */
    private void scanStatement(Consumer<Set<String>> action, String err) throws IOException {
        HashSet<String> names = scanList(() -> scanner.scan(), this::parseTypeName, err, true);
        // Token.SEMICOLON
        if (names.size() != 1) {
            env.error(scanner.pos, err);
            throw new Scanner.SyntaxError().Fatal();
        }
        action.accept(names);
        scanner.scan();
    }

    /**
     * Scans  Module Statement(s):
     * exports  packageName [to ModuleName {, ModuleName}] ;
     * opens    packageName [to ModuleName {, ModuleName}] ;
     * provides TypeName with TypeName [,typeName] ;
     */
    private void scanStatement(BiConsumer<String, Set<String>> action,
                               NameSupplier source,
                               NameSupplier target,
                               Token startList,
                               boolean emptyListAllowed,
                               String err) throws IOException {
        String typeName = "";
        HashSet<String> names = new HashSet<>();
        scanner.scan();
        while (scanner.token != SEMICOLON) {
            if (scanner.token == Token.IDENT) {
                if (typeName.isEmpty()) {
                    typeName = source.get();
                    continue;
                }
                env.error(scanner.pos, err);
                throw new Scanner.SyntaxError().Fatal();
            }
            if (scanner.token == startList) {
                if (typeName.isEmpty()) {
                    env.error(scanner.pos, err);
                    throw new Scanner.SyntaxError().Fatal();
                }
                names = scanList(scanner.token == Token.TO ? () -> scanner.scanModuleStatement() : () -> scanner.scan(), target, err, false);
                break;
            } else {
                env.error(scanner.pos, err);
                throw new Scanner.SyntaxError().Fatal();
            }
        }
        // Token.SEMICOLON
        if (typeName.isEmpty() || (names.isEmpty() && !emptyListAllowed)) {
            env.error(scanner.pos, err);
            throw new Scanner.SyntaxError().Fatal();
        }
        action.accept(typeName, names);
        scanner.scan();
    }

    /**
     * Scans the "to" or "with" part of ModuleStatement: exports PackageName  [to  ModuleName {, ModuleName}] ;,
     * opens  packageName   [to  ModuleName {, ModuleName}] ;
     * provides TypeName with TypeName [,typeName] ;
     * uses TypeName;
     * : [ModuleName {, ModuleName}]; , [TypeName [,typeName]]; or TypeName;
     */
    private HashSet<String> scanList(Method scanMethod, NameSupplier target, String err, boolean onlyOneElement) throws IOException {
        HashSet<String> names = new HashSet<>();
        boolean comma = false, first = true;
        scanMethod.call();
        while (scanner.token != SEMICOLON) {
            switch (scanner.token) {
                case COMMA:
                    if (comma || first || onlyOneElement) {
                        env.error(scanner.pos, err);
                        throw new Scanner.SyntaxError().Fatal();
                    }
                    comma = true;
                    break;
                case IDENT:
                    if (!first && !comma) {
                        env.error(scanner.pos, err);
                        throw new Scanner.SyntaxError().Fatal();
                    }
                    names.add(target.get());
                    comma = false;
                    first = false;
                    continue;
                default:
                    env.error(scanner.pos, err);
                    throw new Scanner.SyntaxError().Fatal();
            }
            scanner.scan();
        }
        // Token.SEMICOLON
        if (names.isEmpty() || comma) {
            env.error(scanner.pos, err);
            throw new Scanner.SyntaxError().Fatal();
        }
        return names;
    }

    private void parseClassMembers() throws IOException {
        debugScan("[Parser.parseClassMembers]:  Begin ");
        // Parse annotations
        if (scanner.token == Token.ANNOTATION) {
            memberAnnttns = annotParser.scanAnnotations();
        }
        // Parse modifiers
        int mod = scanModifiers();
        try {
            switch (scanner.token) {
                case FIELDREF:
                    scanner.scan();
                    parseField(mod);
                    break;
                case METHODREF:
                    scanner.scan();
                    parseMethod(mod);
                    break;
                case INNERCLASS:
                    scanner.scan();
                    parseInnerClass(mod);
                    break;
                case BOOTSTRAPMETHOD:
                    scanner.scan();
                    parseCPXBootstrapMethod();
                    break;
                case NESTHOST:
                    if (cd.nestHostAttributeExists()) {
                        env.error(scanner.pos, "extra.nesthost.attribute");
                        throw new Scanner.SyntaxError();
                    } else if (cd.nestMembersAttributesExist()) {
                        env.error(scanner.pos, "both.nesthost.nestmembers.found");
                        throw new Scanner.SyntaxError();
                    }
                    scanner.scan();
                    parseNestHost();
                    break;
                case NESTMEMBERS:
                    if (cd.nestMembersAttributesExist()) {
                        env.error(scanner.pos, "extra.nestmembers.attribute");
                        throw new Scanner.SyntaxError();
                    } else if (cd.nestHostAttributeExists()) {
                        env.error(scanner.pos, "both.nesthost.nestmembers.found");
                        throw new Scanner.SyntaxError();
                    }
                    scanner.scan();
                    parseClasses(list -> cd.addNestMembers(list));
                    break;
                case PERMITTEDSUBCLASSES:         // JEP 360
                    if (cd.nestMembersAttributesExist()) {
                        env.error(scanner.pos, "extra.permittedsubclasses.attribute");
                        throw new Scanner.SyntaxError();
                    }
                    scanner.scan();
                    parseClasses(list -> cd.addPermittedSubclasses(list));
                    break;
                case RECORD:                    // JEP 359
                    if (cd.recordAttributeExists()) {
                        env.error(scanner.pos, "extra.record.attribute");
                        throw new Scanner.SyntaxError();
                    }
                    scanner.scan();
                    parseRecord();
                    break;
                case PRELOAD:
                    if (cd.preloadAttributeExists()) {
                        env.error(scanner.pos, "extra.preload.attribute");
                        throw new Scanner.SyntaxError();
                    }
                    scanner.scan();
                    parseClasses(list -> cd.addPreloads(list));
                    break;
                default:
                    env.error(scanner.pos, "field.expected");
                    throw new Scanner.SyntaxError();
            }  // end switch
        } catch (Scanner.SyntaxError e) {
            recoverField();
        }
        memberAnnttns = null;
    }

    /**
     * Recover after a syntax error in the file.
     * This involves discarding scanner.tokens until an EOF
     * or a possible legal continuation is encountered.
     */
    private void recoverFile() throws IOException {
        while (true) {
            env.traceln("recoverFile: scanner.token=" + scanner.token);
            switch (scanner.token) {
                case CLASS:
                case INTERFACE:
                    // Start of a new source file statement, continue
                    return;

                case LBRACE:
                    match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    match(Token.LSQBRACKET, Token.RSQBRACKET);
                    scanner.scan();
                    break;

                case EOF:
                    return;

                default:
                    // Don't know what to do, skip
                    scanner.scan();
                    break;
            }
        }
    }

    /**
     * End class
     */
    private void endClass() {
        if (explicitcp) {
            // Fix references in the constant pool (for explicitly coded CPs)
            pool.fixRefsInPool();
            // Fix any bootstrap Method references too
            cd.relinkBootstrapMethods();
        }
        cd.endClass();
        clsDataList.add(cd);
        cd = null;
    }

    /**
     * End module
     */
    private void endModule() {
        cd.endModule(moduleAttribute);
        clsDataList.add(cd);
        cd = null;
    }

    final ClassData[] getClassesData() {
        return clsDataList.toArray(new ClassData[0]);
    }

    /**
     * Determines whether the JASM file is for a package-info class
     * or for a module-info class.
     * <p>
     * creates the correct kind of ClassData accordingly.
     *
     * @throws IOException
     */
    private void parseJasmPackages() throws IOException {
        try {
            // starting annotations could either be
            // a package annotation, or a class annotation
            if (scanner.token == Token.ANNOTATION) {
                if (cd == null) {
                    cd = new ClassData(env, currentCFV.clone());
                    pool = cd.pool;
                }
                pkgAnnttns = annotParser.scanAnnotations();
            }
            if (scanner.token == Token.PACKAGE) {
                // Package statement
                scanner.scan();
                int where = scanner.pos;
                String id = parseIdent();
                parseVersionPkg();
                scanner.expect(SEMICOLON);

                if (pkg == null) {
                    pkg = id;
                    pkgPrefix = id + "/";
                } else {
                    env.error(where, "package.repeated");
                }
                debugScan("[Parser.parseJasmPackages] {PARSED} package-prefix: " + pkgPrefix + " ");
            }
        } catch (Scanner.SyntaxError e) {
            recoverFile();
        }
        // skip bogus semi colons
        while (scanner.token == SEMICOLON) {
            scanner.scan();
        }

        // checks that we compile module or package compilation unit
        if (scanner.token == Token.EOF) {
            env.traceln("Scanner:  EOF");
            String sourceName = env.getSimpleInputFileName();
            int mod = ACC_INTERFACE | ACC_ABSTRACT;

            // package-info
            if (sourceName.endsWith("package-info.jasm")) {
                env.traceln("Creating \"package-info.jasm\": package: " + pkg + " " + currentCFV.asString());

                if (cd == null) {
                    cd = new ClassData(env, currentCFV.clone());
                    pool = cd.pool;
                } else {
                    cd.cfv = currentCFV.clone();
                }
                ConstCell me = pool.FindCellClassByName(pkgPrefix + "package-info");

                // Interface package-info should be marked synthetic and abstract
                if (currentCFV.major_version() > 49) {
                    mod |= SYNTHETIC_ATTRIBUTE;
                }
                cd.init(mod, me, new ConstCell(0), null);

                if (pkgAnnttns != null) {
                    cd.addAnnotations(pkgAnnttns);
                }

                endClass();
            }
            return;
        }

        if (pkg == null && pkgAnnttns != null) { // RemoveModules
            clsAnnttns = pkgAnnttns;
            pkgAnnttns = null;
        }
    }

    /**
     * Parse an Jasm file.
     */
    void parseFile() {
        try {
            // First, parse any package identifiers (and associated package annotations)
            parseJasmPackages();

            while (scanner.token != Token.EOF) {
                // Second, parse any class identifiers (and associated class annotations)
                try {
                    // Parse annotations
                    if (scanner.token == Token.ANNOTATION) {
                        if (cd == null) {
                            cd = new ClassData(env, currentCFV.clone());
                            pool = cd.pool;
                        } else {
                            cd.cfv = currentCFV.clone();
                        }
                        clsAnnttns = annotParser.scanAnnotations();
                    }

                    // Parse class modifiers
                    int mod = scanModifiers();
                    if (mod == 0) {
                        switch (scanner.token) {
                            case OPEN:
                            case MODULE:
                            case CLASS:
                            case CPINDEX:
                            case STRINGVAL:
                            case IDENT:
                                // this is a class declaration anyway
                                break;
                            case SEMICOLON:
                                // Bogus semi colon
                                scanner.scan();
                                continue;
                            default:
                                // no class declaration found
                                debugScan(" [Parser.parseFile]: ");
                                env.error(scanner.pos, "toplevel.expected");
                                throw new Scanner.SyntaxError();
                        }
                    } else if (Modifiers.isInterface(mod) && (scanner.token != Token.CLASS)) {
                        // rare syntactic sugar:
                        // interface <ident> == abstract interface class <ident>
                        mod |= ACC_ABSTRACT;
                    }
                    if (scanner.token == Token.MODULE || scanner.token == Token.OPEN)
                        parseModule();
                    else
                        parseClass(mod);
                    clsAnnttns = null;

                } catch (Scanner.SyntaxError e) {
                    // KTL
                    env.traceln("^^^^^^^ Syntax Error ^^^^^^^^^^^^");
                    if (scanner.debugFlag)
                        e.printStackTrace();
                    if (e.isFatal()) {
                        break;
                    }
                    recoverFile();
                }
            }
        } catch (IOException e) {
            env.error(scanner.pos, "io.exception", env.getSimpleInputFileName());
        } catch (Error er) {
            er.printStackTrace();
        }
    } //end parseFile

    @FunctionalInterface
    interface NameSupplier {
        String get() throws IOException;
    }

    @FunctionalInterface
    interface Method {
        void call() throws IOException;
    }

    /**
     * The main compile error for the parser
     */
    static class CompilerError extends Error {

        CompilerError(String message) {
            super(message);
        }
    }
}  //end Parser
