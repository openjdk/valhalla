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

import sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import sun.reflect.generics.reflectiveObjects.WildcardTypeImpl;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class TypeContext {
    private final Map<TypeVariable<?>, Type> inferredVars = new HashMap<>();

    public boolean checkBounds() {
        for (Map.Entry<TypeVariable<?>, Type> inferredTypes : inferredVars.entrySet()) {
            for (Type bound : upper(inferredTypes.getKey())) {
                if (!isSubType(inferredTypes.getValue(), subst(bound))) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isSameType(Type t1, Type t2) {
        if (isTypeVar(t1) || isTypeVar(t2)) {
            // inference!
            TypeVariable<?> tv = isTypeVar(t1) ?
                    (TypeVariable<?>)t1 : (TypeVariable<?>)t2;
            Type inferred = isTypeVar(t1) ? t2 : t1;
            Type prevInferred = inferredVars.get(tv);
            if (prevInferred != null && !isSameType(inferred, prevInferred)) {
                // incompatible constraints
                return false;
            }
            inferredVars.put(tv, inferred);
            return true;
        }
        if (isArray(t1) && isArray(t2)) {
            return isSameType(arrayComponent(t1), arrayComponent(t2));
        } else if (isClass(t1) && isClass(t2)) {
            return toClass(t1).equals(toClass(t2)) &&
                    isSameTypes(typeArguments(t1), typeArguments(t2));
        } else if (isWildcard(t1) && isWildcard(t2)) {
            return isSameTypes(upper(t1), upper(t2)) &&
                    isSameTypes(lower(t2), lower(t2));
        } else {
            return t1.equals(t2);
        }
    }

    public boolean isSameTypes(Type[] ts1, Type[] ts2) {
        if (ts1.length != ts2.length) return false;
        for (int i = 0 ; i < ts1.length ; i++) {
            if (!isSameType(ts1[i], ts2[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean isSubType(Type s, Type t) {
        if (isPrimitive(s) && isPrimitive(t)) {
            return ((Class<?>)t).isAssignableFrom(((Class<?>)s));
        } else if (isArray(s)) {
            if (isArray(t)) {
                Type cs = arrayComponent(s);
                Type ct = arrayComponent(t);
                return isSubType(cs, ct);
            } else {
                return isSameType(t, Object.class) ||
                        isSameType(t, Serializable.class) ||
                        isSameType(t, Cloneable.class);
            }
        } else if (isClass(s) && isClass(t)) {
            // Lift subtype to supertype
            Type sup = lift(s, toClass(t));
            return sup != null &&
                    (typeArguments(t).length == 0 ||
                    isSameTypes(typeArguments(sup), typeArguments(t)));
        }
        return false;
    }

    public Type subst(Type t) {
        if (inferredVars.isEmpty()) return t; // fast-path
        return switch (t) {
            case TypeVariable<?> tv -> inferredVars.get(tv);
            case WildcardType wt -> WildcardTypeImpl.make(subst(wt.getUpperBounds()), subst(wt.getLowerBounds()));
            case GenericArrayType at -> GenericArrayTypeImpl.make(subst(at.getGenericComponentType()));
            case ParameterizedType pt -> ParameterizedTypeImpl.make((Class<?>)pt.getRawType(), subst(pt.getActualTypeArguments()), pt.getOwnerType());
            default -> t;
        };
    }

    public Type[] subst(Type[] ts) {
        return Stream.of(ts).map(this::subst).toArray(Type[]::new);
    }

    Type lift(Type type, Class<?> target) {
        Class<?> baseType = type instanceof ParameterizedType pt ?
                (Class<?>)pt.getRawType() : (Class<?>)type;
        if (baseType.equals(target)) {
            return type;
        } else if (target.equals(Object.class)) {
            return Object.class;
        }
        TypeContext tc = new TypeContext();
        if (type instanceof ParameterizedType pt) {
            ParameterizedType formal = ParameterizedTypeImpl.make(baseType, baseType.getTypeParameters(), pt.getOwnerType());
            tc.isSameType(pt, formal); // generate substitutions
        }
        if (!tc.checkBounds()) {
            return null;
        }
        if (baseType.getGenericSuperclass() != null) {
            Type res = lift(tc.subst(baseType.getGenericSuperclass()), target);
            if (res != null) {
                return res;
            }
        }
        for (Type intf : baseType.getGenericInterfaces()) {
            Type res = lift(tc.subst(intf), target);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    static Type[] typeArguments(Type t) {
        return switch (t) {
            case ParameterizedType pt -> pt.getActualTypeArguments();
            case Class<?> _ -> new Type[0];
            default -> throw unsupported(t);
        };
    }

    static Class<?> toClass(Type t) {
        return switch (t) {
            case ParameterizedType pt -> (Class<?>)pt.getRawType();
            case Class<?> c -> c;
            default -> throw unsupported(t);
        };
    }

    static boolean isPrimitive(Type t) {
        return t instanceof Class<?> c && c.isPrimitive();
    }

    static boolean isClass(Type t) {
        return switch (t) {
            case Class<?> c -> !c.isPrimitive() && !c.isArray();
            case ParameterizedType _ -> true;
            default -> false;
        };
    }

    static boolean isArray(Type t) {
        return switch (t) {
            case Class<?> c -> c.isArray();
            case GenericArrayType _ -> true;
            default -> false;
        };
    }

    static boolean isTypeVar(Type t) {
        return t instanceof TypeVariable<?>;
    }

    static boolean isWildcard(Type t) {
        return t instanceof WildcardType;
    }

    static Type[] upper(Type t) {
        if (isWildcard(t)) {
            return ((WildcardType)t).getUpperBounds();
        } else if (isTypeVar(t)) {
            return ((TypeVariable<?>)t).getBounds();
        } else {
            throw unsupported(t);
        }
    }

    static Type[] lower(Type t) {
        if (isWildcard(t)) {
            return ((WildcardType) t).getLowerBounds();
        } else {
            throw unsupported(t);
        }
    }

    static Type arrayComponent(Type t) {
        return switch (t) {
            case GenericArrayType gat -> gat.getGenericComponentType();
            case Class<?> c when c.isArray() -> c.componentType();
            default -> throw unsupported(t);
        };
    }

    static UnsupportedOperationException unsupported(Type t) {
        return new UnsupportedOperationException(t.getTypeName());
    }
}
