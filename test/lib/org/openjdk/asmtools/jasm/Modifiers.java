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

import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.RuntimeConstants.*;
import static org.openjdk.asmtools.jasm.Tables.CF_Context;

/**
 *
 *
 */
public class Modifiers {

    /*
     * Modifier masks
     */
    public static final int MM_ATTR        = SYNTHETIC_ATTRIBUTE | DEPRECATED_ATTRIBUTE;

    public static final int MM_ACCESS      = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED;

    public static final int MM_INTRF       = MM_ACCESS  | ACC_ABSTRACT  | ACC_INTERFACE | MM_ATTR | ACC_ANNOTATION;

    public static final int MM_CLASS       = MM_ACCESS  | ACC_FINAL     |  ACC_SUPER    | ACC_ABSTRACT | ACC_ENUM |
                                             MM_ATTR    |  ACC_MODULE |
                                             ACC_VALUE | ACC_PERMITS_VALUE | ACC_PRIMITIVE;

    public static final int MM_FIELD       = MM_ACCESS    | ACC_STATIC | ACC_FINAL    |  ACC_VOLATILE | ACC_TRANSIENT |
                                            ACC_SYNTHETIC | ACC_ENUM   |
                                            ACC_MANDATED |         // JEP 359 Record
                                            MM_ATTR;

    public static final int MM_I_METHOD    = ACC_ABSTRACT | ACC_PUBLIC | ACC_PRIVATE | ACC_STATIC | ACC_VARARGS |
                                            ACC_BRIDGE    | ACC_SYNTHETIC ; // interface method

    public static final int MM_A_METHOD    = MM_ACCESS | ACC_ABSTRACT | MM_ATTR;

    public static final int MM_N_METHOD    = MM_ACCESS | ACC_STRICT | ACC_VARARGS | ACC_SYNTHETIC | MM_ATTR;  // <init>

    public static final int MM_METHOD      = MM_ACCESS    | ACC_STATIC | ACC_FINAL    | ACC_SYNCHRONIZED |  ACC_BRIDGE |
                                             ACC_VARARGS  | ACC_NATIVE | ACC_ABSTRACT |  ACC_STRICT      | ACC_SYNTHETIC |
                                             ACC_MANDATED |         // JEP 359 Record
                                             MM_ATTR ;

    public static final int MM_INNERCLASS  = MM_ACCESS    | ACC_STATIC    | ACC_FINAL      | ACC_SUPER | ACC_INTERFACE |
                                             ACC_ABSTRACT | ACC_SYNTHETIC | ACC_ANNOTATION | ACC_ENUM  | MM_ATTR |
                                             ACC_VALUE | ACC_PERMITS_VALUE | ACC_PRIMITIVE;

    public static final int MM_REQUIRES    = ACC_TRANSITIVE | ACC_STATIC_PHASE  | ACC_SYNTHETIC | ACC_MANDATED ;

    public static final int MM_EXPORTS     = ACC_SYNTHETIC | ACC_MANDATED ;

    private Modifiers() {
    }

    public static boolean validRequires(int mod) {
        return (mod & ~MM_REQUIRES) == 0;
    }

    public static boolean validExports(int mod) { return (mod & ~MM_EXPORTS) == 0; }

    public static boolean validInnerClass(int mod) {
        return (mod & ~MM_INNERCLASS) == 0;
    }

    public static boolean validField(int mod) {
        return (mod & ~MM_FIELD) == 0;
    }

    public static boolean validMethod(int mod) {
        return (mod & ~MM_METHOD) == 0;
    }

    public static boolean validInterface(int mod) {
        return (mod & ~MM_INTRF) == 0;
    }

    public static int getInvalidModifiers4Interface(int mod) {
        return  mod & ~MM_INTRF;
    }

    public static boolean validClass(int mod) {
        return (mod & ~MM_CLASS) == 0;
    }

    public static int getInvalidModifiers4Class(int mod) {
        return (mod & ~MM_CLASS);
    }

    public static boolean validAbstractMethod(int mod) {
        return (mod & ~MM_A_METHOD) == 0;
    }

    public static boolean validInitMethod(int mod) {
        return (mod & ~MM_N_METHOD) == 0;
    }

    public static boolean validInterfaceMethod(int mod, ClassData cd) {
        return ((mod & ~MM_I_METHOD) == 0) &&
            (cd.cfv.major_version() >= 52 || isPublic(mod) && isAbstract(mod) && !isStatic(mod));
    }

    public static boolean validInterfaceField(int mod) {
        return mod == (ACC_STATIC | ACC_PUBLIC | ACC_FINAL);
    }

    public static boolean isPublic(int mod) {
        return (mod & ACC_PUBLIC) != 0;
    }

    public static boolean isPrivate(int mod) {
        return (mod & ACC_PRIVATE) != 0;
    }

    public static boolean isProtected(int mod) {
        return (mod & ACC_PROTECTED) != 0;
    }

    public static boolean isInterface(int mod) {
        return (mod & ACC_INTERFACE) != 0;
    }

    public static boolean isAbstract(int mod) {
        return (mod & ACC_ABSTRACT) != 0;
    }

    public static boolean isFinal(int mod) {
        return (mod & ACC_FINAL) != 0;
    }

    public static boolean isStatic(int mod) {
        return (mod & ACC_STATIC) != 0;
    }

    public static boolean isSynthetic(int mod) {
        return (mod & ACC_SYNTHETIC) != 0;
    }

    public static boolean isDeprecated(int mod) {
        return (mod & DEPRECATED_ATTRIBUTE) != 0;
    }

    public static boolean isTransient(int mod) {
        return (mod & ACC_TRANSIENT) != 0;
    }

    public static boolean isAnnotation(int mod) {
        return (mod & ACC_ANNOTATION) != 0;
    }

    public static boolean isNative(int mod) {
        return (mod & ACC_NATIVE) != 0;
    }

    public static boolean isStrict(int mod) {
        return (mod & ACC_STRICT) != 0;
    }

    public static boolean isEnum(int mod) {
        return (mod & ACC_ENUM) != 0;
    }

    public static boolean isSuper(int mod) {
        return (mod & ACC_SUPER) != 0;
    }

    public static boolean isModule(int mod) { return (mod & ACC_MODULE)!=0; }

    public static boolean isMandated(int mod) { return (mod & ACC_MANDATED) != 0; }

    public static boolean isSynchronized(int mod) {
        return (mod & ACC_SYNCHRONIZED) != 0;
    }

    public static boolean isBridge(int mod) {
        return (mod & ACC_BRIDGE) != 0;
    }

    public static boolean isVolatile(int mod) {
        return (mod & ACC_VOLATILE) != 0;
    }

    public static boolean isVarArgs(int mod) {
        return (mod & ACC_VARARGS) != 0;
    }

    public static boolean isSyntheticPseudoMod(int mod) {
        return (mod & SYNTHETIC_ATTRIBUTE) != 0;
    }

    public static boolean isDeprecatedPseudoMod(int mod) {
        return (mod & DEPRECATED_ATTRIBUTE) != 0;
    }

    public static boolean hasPseudoMod(int mod) {
        return isSyntheticPseudoMod(mod) || isDeprecatedPseudoMod(mod);
    }

    public static boolean isTransitive(int mod) { return (mod & ACC_TRANSITIVE) != 0;  }

    public static boolean isStaticPhase(int mod) { return (mod & ACC_STATIC_PHASE) != 0;  }

    public static boolean isValue(int mod) {
        return (mod & ACC_VALUE) != 0;
    }

    public static boolean isPermitsValue(int mod) {
        return (mod & ACC_PERMITS_VALUE) != 0;
    }

    public static boolean isPrimitive(int mod) {
        return (mod & ACC_PRIMITIVE) != 0;
    }

    /*
     * Checks that only one (or none) of the Access flags are set.
     */
    public static boolean validAccess(int mod) {
        boolean retval = true;
        switch (mod & MM_ACCESS) {
            case 0:
            case ACC_PUBLIC:
            case ACC_PRIVATE:
            case ACC_PROTECTED:
                break;
            default:
                retval = false;
        }
        return retval;
    }

    /*
     * Are both flags set
     *
     */
    public static boolean both(int mod, int flagA, int flagB) {
        return (mod & (flagA | flagB)) == (flagA | flagB);
    }

    /**
     * Check the modifier flags for the class
     *
     * @param env       The error reporting environment.
     * @param mod       The modifier flags being checked
     * @param scanner   The file parser
     */
    public static void checkClassModifiers(Environment env, int mod, Scanner scanner) {
        if (isInterface(mod)) {
            if( isEnum(mod) ) {
                env.error(scanner.pos, "warn.invalid.modifier.class.intenum");
            } else if ( !validInterface(mod) ) {
                env.error(scanner.pos, "warn.invalid.modifier.int",
                        toString(mod & ~MM_INTRF, CF_Context.CTX_CLASS));
            }
            if (!isAbstract(mod)) {
                env.error(scanner.pos, "warn.invalid.modifier.int.abs");
            }
        } else {
            if ( scanner.token != Token.CLASS && !isEnum(mod) && scanner.token != Token.ANNOTATION) {
                env.error(scanner.pos, "warn.missing.modifier.class");
            }
            if (! validClass(mod)) {
                env.error(scanner.pos, "warn.invalid.modifier.class",
                            toString(mod & ~MM_CLASS, CF_Context.CTX_CLASS));
            }
            if (isAbstract(mod) && Modifiers.isFinal(mod)) {
                env.error(scanner.pos, "warn.invalid.modifier.class.finabs");
            }
        }
    }

    /**
     * Check the modifier flags for the field
     *
     * @param cd The ClassData for the current class
     * @param mod The modifier flags being checked
     * @param pos the position of the parser in the file
     */
    public static void checkFieldModifiers(ClassData cd, int mod, int pos) {
        Environment env = cd.env;
        if (cd.isInterface()) {
            // For interfaces
            if (!validInterfaceField(mod)) {
                env.error(pos, "warn.invalid.modifier.intfield");
            }
        } else {
            // For non-interfaces
            if (!validField(mod)) {
                env.error(pos, "warn.invalid.modifier.field",
                        toString(mod & ~MM_FIELD, CF_Context.CTX_METHOD));
            }
            if (both(mod, ACC_FINAL, ACC_VOLATILE)) {
                env.error(pos, "warn.invalid.modifier.fiva");
            }
            if (!validAccess(mod)) {
                env.error(pos, "warn.invalid.modifier.acc");
            }
        }

    }

    /**
     * Check the modifier flags for the method
     *
     * @param cd The ClassData for the current class
     * @param mod The modifier flags being checked
     * @param pos the position of the parser in the file
     */
    public static void checkMethodModifiers(ClassData cd, int mod, int pos, boolean is_init, boolean is_clinit) {
        Environment env = cd.env;
        if (!is_clinit) {
            if (cd.isInterface()) {
                if (is_init) {
                    env.error(pos, "warn.init.in_int");
                } else if (!validInterfaceMethod(mod, cd)) {
                    int badflags = (mod & ~MM_I_METHOD);
                    env.error(pos, "warn.invalid.modifier.intmth", toString(badflags, CF_Context.CTX_METHOD)
                            + "   *****" + toString(mod, CF_Context.CTX_METHOD) + "*****");
                }
            } else {
                if (is_init && !validInitMethod(mod)) {
                    int badflags = (mod & ~MM_N_METHOD);
                    env.error(pos, "warn.invalid.modifier.init", toString(badflags, CF_Context.CTX_METHOD)
                            + "   *****" + toString(mod, CF_Context.CTX_METHOD) + "*****");
                } else if (isAbstract(mod)) {
                    if (!validAbstractMethod(mod)) {
                        int badflags = (mod & ~MM_A_METHOD);
                        env.error(pos, "warn.invalid.modifier.abst", toString(badflags, CF_Context.CTX_METHOD)
                                + "   *****" + toString(mod, CF_Context.CTX_METHOD) + "*****");
                    }
                } else {
                    if (!validMethod(mod)) {
                        env.error(pos, "warn.invalid.modifier.mth",
                                toString(mod & ~MM_METHOD, CF_Context.CTX_METHOD));
                    }
                }
                if (!validAccess(mod)) {
                    env.error(pos, "warn.invalid.modifier.acc");
                }
            }
        }
    }

    /**
     * Check the modifier flags for the inner-class
     *
     * @param cd The ClassData for the current class
     * @param mod The modifier flags being checked
     * @param pos the position of the parser in the file
     */
    public static void checkInnerClassModifiers(ClassData cd, int mod, int pos) {
        Environment env = cd.env;

        if (!validInnerClass(mod)) {
            int badflags = (mod & ~MM_INNERCLASS);
            env.error(pos, "warn.invalid.modifier.innerclass",
                    toString(badflags, CF_Context.CTX_INNERCLASS)
                    + "   *****" + toString(mod, CF_Context.CTX_INNERCLASS) + "*****");
        }

    }

    private static StringBuffer _accessString(int mod, CF_Context context) {
        StringBuffer sb = new StringBuffer();
        if (context == CF_Context.CTX_CLASS && isModule(mod)) {
            sb.append(Token.MODULE.parseKey() + " ");
        }
        if (isPublic(mod)) {
            sb.append(Token.PUBLIC.parseKey() + " ");
        }
        if (isPrivate(mod)) {
            sb.append(Token.PRIVATE.parseKey() + " ");
        }
        if (isProtected(mod)) {
            sb.append(Token.PROTECTED.parseKey() + " ");
        }
        if (isStatic(mod)) {
            sb.append(Token.STATIC.parseKey() + " ");
        }
        if (context == CF_Context.CTX_METHOD && isFinal(mod)) {
            sb.append(Token.FINAL.parseKey() + " ");
        }
        if (context == CF_Context.CTX_FIELD && isTransient(mod)) {
            sb.append(Token.TRANSIENT.parseKey() + " ");
        }
        if (context == CF_Context.CTX_CLASS && isSuper(mod)) {
            sb.append(Token.SUPER.parseKey() + " ");
        }
        if (context == CF_Context.CTX_METHOD && isSynchronized(mod)) {
            sb.append(Token.SYNCHRONIZED.parseKey() + " ");
        }
        if (context == CF_Context.CTX_METHOD) {
            if (isBridge(mod)) {
                sb.append(Token.BRIDGE.parseKey() + " ");
            }
            if (isVarArgs(mod)) {
                sb.append(Token.VARARGS.parseKey() + " ");
            }
            if (isNative(mod)) {
                sb.append(Token.NATIVE.parseKey() + " ");
            }
            if (isStrict(mod)) {
                sb.append(Token.STRICT.parseKey() + " ");
            }
        }
        if (isAbstract(mod)) {
            if ((context != CF_Context.CTX_CLASS) || !isInterface(mod)) {
                sb.append(Token.ABSTRACT.parseKey() + " ");
            }
        }
        if (context.isOneOf(CF_Context.CTX_CLASS, CF_Context.CTX_INNERCLASS) && isPermitsValue(mod)) {
            sb.append(Token.PERMITS_VALUE.parseKey() + " ");
        }
        if (  context.isOneOf(CF_Context.CTX_CLASS, CF_Context.CTX_INNERCLASS, CF_Context.CTX_FIELD) && isFinal(mod)) {
            sb.append(Token.FINAL.parseKey() + " ");
        }
        if (context.isOneOf(CF_Context.CTX_CLASS, CF_Context.CTX_INNERCLASS) && isInterface(mod)) {
            if (isAnnotation(mod)) {
                sb.append(Token.ANNOTATION_ACCESS.parseKey() + " ");
            }
            sb.append(Token.INTERFACE.parseKey() + " ");
        }
        if (isSynthetic(mod)) {
            sb.append(Token.SYNTHETIC.parseKey() + " ");
        }
        if (context == CF_Context.CTX_FIELD && isVolatile(mod)) {
            sb.append(Token.VOLATILE.parseKey() + " ");
        }
        if (isEnum(mod)) {
            sb.append(Token.ENUM.parseKey() + " ");
        }
        if (context.isOneOf(CF_Context.CTX_METHOD, CF_Context.CTX_FIELD) && isMandated(mod)) {
            sb.append(Token.MANDATED.parseKey() + " ");
        }
        if (context.isOneOf(CF_Context.CTX_CLASS, CF_Context.CTX_INNERCLASS) && isPrimitive(mod)) {
            sb.append(Token.PRIMITIVE.parseKey() + " ");
        }
        if (context.isOneOf(CF_Context.CTX_CLASS, CF_Context.CTX_INNERCLASS) && isValue(mod)) {
            sb.append(Token.VALUE.parseKey() + " ");
        }

        return sb;
    }

    public static String toString(int mod, CF_Context context) {
        StringBuffer sb = _accessString(mod, context);

        if (isSyntheticPseudoMod(mod)) {
            sb.append("Synthetic(Pseudo) ");
        }
        if (isDeprecatedPseudoMod(mod)) {
            sb.append("Deprecated(Pseudo) ");
        }

        return sb.toString().trim();
    }

    public static String moduleFlags( int flags ) {
        return "";
    }

    public static String accessString(int mod, CF_Context context) {
        return (context == CF_Context.CTX_MODULE) ?
            moduleFlags(mod) :
            _accessString(mod, context).toString();
    }

}
