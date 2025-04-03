/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

/**
 * A Context class, that can keep useful information about unset fields.
 * This information will be produced during flow analysis and used during
 * code generation.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class UnsetFieldsInfo {
    protected static final Context.Key<UnsetFieldsInfo> unsetFieldsInfoKey = new Context.Key<>();

    public static UnsetFieldsInfo instance(Context context) {
        UnsetFieldsInfo instance = context.get(unsetFieldsInfoKey);
        if (instance == null)
            instance = new UnsetFieldsInfo(context);
        return instance;
    }

    @SuppressWarnings("this-escape")
    protected UnsetFieldsInfo(Context context) {
        context.put(unsetFieldsInfoKey, this);
    }

    public UnsetFieldsInfo() {}

    /* this record will be the key of map unsetFieldsMap there could be cases where we need to look for the tree
     * in other cases we need to look for the symbol, the symbol could be null sometimes and in other occasions will
     * be the VarSymbol to the left side of an assignment statement like if we have:
     *     i = 1;
     * the symbol will be `i` the tree will be `i = 1` there could be cases when we need to remove all assignments to
     * a given symbol if at the end of an statement the associated strict field is not DA for example for code like:
     *
     *     if (cond) {
     *         i = 1;
     *     } else {
     *         j = 2;
     *     }
     *
     * at the end of this `if` statement either `i` nor `j` will be DA and thus we need to remove any entry associated
     * to them, remember that when we assign to a field we enter what other fields are still unassigned so for tree:
     *     `i = 1`
     * there will be an entry for symbol `i` and the tree above indicating that field `j` is still unassigned and
     * vice versa for tree `j = 2`, for the example above, both entries should be removed, and to remove them we need to
     * search in the map using the corresponding VarSymbol `i` or `j` depending on the case
     */
    private record SymPlusTreeKey(Symbol sym, JCTree tree) {}

    private WeakHashMap<ClassSymbol, Map<SymPlusTreeKey, Set<VarSymbol>>> unsetFieldsMap = new WeakHashMap<>();
    // as we use a record with two components as the key of the map above, we need a way to relate both components to be
    // able to generate a key if only the tree is available for some searches
    private WeakHashMap<JCTree, Symbol> treeToSymbolMap = new WeakHashMap<>();

    public void addUnsetFieldsInfo(ClassSymbol csym, Symbol sym, JCTree tree, Set<VarSymbol> unsetFields) {
        Map<SymPlusTreeKey, Set<VarSymbol>> treeToFieldsMap = unsetFieldsMap.get(csym);
        if (treeToFieldsMap == null) {
            treeToFieldsMap = new HashMap<>();
            treeToFieldsMap.put(new SymPlusTreeKey(sym, tree), unsetFields);
            unsetFieldsMap.put(csym, treeToFieldsMap);
            treeToSymbolMap.put(tree, sym);
        } else {
            if (!treeToFieldsMap.containsKey(tree)) {
                // only add if there is no info for the given tree
                treeToFieldsMap.put(new SymPlusTreeKey(sym, tree), unsetFields);
                treeToSymbolMap.put(tree, sym);
            }
        }
    }

    public void removeAssigmentToSym(ClassSymbol csym, Symbol sym) {
        Map<SymPlusTreeKey, Set<VarSymbol>> treeToFieldsMap = unsetFieldsMap.get(csym);
        if (treeToFieldsMap != null) {
            java.util.List<SymPlusTreeKey> treesToRemove = new ArrayList<>();
            for (SymPlusTreeKey symtree : treeToFieldsMap.keySet()) {
                if (symtree.sym() == sym) {
                    treesToRemove.add(symtree);
                }
            }
            for (SymPlusTreeKey symTree : treesToRemove) {
                treeToFieldsMap.remove(symTree);
                treeToSymbolMap.remove(symTree.tree());
            }
            unsetFieldsMap.put(csym, treeToFieldsMap);
        }
    }

    public void addAll(UnsetFieldsInfo unsetFieldsInfo) {
        for (ClassSymbol cs : unsetFieldsInfo.unsetFieldsMap.keySet()) {
            Map<SymPlusTreeKey, Set<VarSymbol>> treeSetMap = unsetFieldsInfo.unsetFieldsMap.get(cs);
            for (SymPlusTreeKey symTree: treeSetMap.keySet()) {
                addUnsetFieldsInfo(cs, symTree.sym(), symTree.tree(), treeSetMap.get(symTree));
            }
        }
    }

    public boolean isEmpty() {
        return unsetFieldsMap.isEmpty();
    }

    public Set<VarSymbol> getUnsetFields(ClassSymbol csym, JCTree tree) {
        Map<SymPlusTreeKey, Set<VarSymbol>> treeToFieldsMap = unsetFieldsMap.get(csym);
        if (treeToFieldsMap != null) {
            Symbol sym = treeToSymbolMap.get(tree);
            Set<VarSymbol> result = treeToFieldsMap.get(new SymPlusTreeKey(sym, tree));
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public void removeUnsetFieldInfo(ClassSymbol csym, JCTree tree) {
        Map<JCTree, Set<VarSymbol>> treeToFieldsMap = unsetFieldsMap.get(csym);
        if (treeToFieldsMap != null) {
            treeToFieldsMap.remove(tree);
        }
    }
}
