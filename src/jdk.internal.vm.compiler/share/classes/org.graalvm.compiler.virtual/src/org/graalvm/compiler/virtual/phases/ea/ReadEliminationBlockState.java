/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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


package org.graalvm.compiler.virtual.phases.ea;

import java.util.Iterator;

import jdk.internal.vm.compiler.collections.EconomicMap;
import jdk.internal.vm.compiler.collections.Equivalence;
import org.graalvm.compiler.nodes.ValueNode;
import jdk.internal.vm.compiler.word.LocationIdentity;

/**
 * This class maintains a set of known values, identified by base object, locations and offset.
 */
public class ReadEliminationBlockState extends EffectsBlockState<ReadEliminationBlockState> {

    final EconomicMap<CacheEntry<?>, ValueNode> readCache;

    public abstract static class CacheEntry<T> {

        public final ValueNode object;
        public final T identity;

        protected CacheEntry(ValueNode object, T identity) {
            this.object = object;
            this.identity = identity;
        }

        public abstract CacheEntry<T> duplicateWithObject(ValueNode newObject);

        @Override
        public int hashCode() {
            int result = 31 + ((identity == null) ? 0 : identity.hashCode());
            // we need to use the identity hash code for the object since the node may not yet have
            // a valid id and thus not have a stable hash code
            return 31 * result + ((object == null) ? 0 : System.identityHashCode(object));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CacheEntry<?>)) {
                return false;
            }
            CacheEntry<?> other = (CacheEntry<?>) obj;
            return identity.equals(other.identity) && object == other.object;
        }

        @Override
        public String toString() {
            return object + ":" + identity;
        }

        public abstract boolean conflicts(LocationIdentity other, ValueNode index, ValueNode array);

        public abstract LocationIdentity getIdentity();
    }

    public static final class LoadCacheEntry extends CacheEntry<LocationIdentity> {

        public LoadCacheEntry(ValueNode object, LocationIdentity identity) {
            super(object, identity);
        }

        @Override
        public CacheEntry<LocationIdentity> duplicateWithObject(ValueNode newObject) {
            return new LoadCacheEntry(newObject, identity);
        }

        @Override
        public boolean conflicts(LocationIdentity other, ValueNode index, ValueNode array) {
            return identity.equals(other);
        }

        @Override
        public LocationIdentity getIdentity() {
            return identity;
        }
    }

    /**
     * CacheEntry describing an Unsafe memory reference. The memory location and the location
     * identity are separate so both must be considered when looking for optimizable memory
     * accesses.
     */
    public static final class UnsafeLoadCacheEntry extends CacheEntry<ValueNode> {

        private final LocationIdentity locationIdentity;

        public UnsafeLoadCacheEntry(ValueNode object, ValueNode location, LocationIdentity locationIdentity) {
            super(object, location);
            assert locationIdentity != null;
            this.locationIdentity = locationIdentity;
        }

        @Override
        public CacheEntry<ValueNode> duplicateWithObject(ValueNode newObject) {
            return new UnsafeLoadCacheEntry(newObject, identity, locationIdentity);
        }

        @Override
        public boolean conflicts(LocationIdentity other, ValueNode index, ValueNode array) {
            return locationIdentity.equals(other);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + locationIdentity.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UnsafeLoadCacheEntry) {
                UnsafeLoadCacheEntry other = (UnsafeLoadCacheEntry) obj;
                return super.equals(other) && locationIdentity.equals(other.locationIdentity);
            }
            return false;
        }

        @Override
        public LocationIdentity getIdentity() {
            return locationIdentity;
        }

        @Override
        public String toString() {
            return "UNSAFE:" + super.toString() + " location:" + locationIdentity;
        }
    }

    public ReadEliminationBlockState() {
        readCache = EconomicMap.create(Equivalence.DEFAULT);
    }

    public ReadEliminationBlockState(ReadEliminationBlockState other) {
        super(other);
        readCache = EconomicMap.create(Equivalence.DEFAULT, other.readCache);
    }

    @Override
    public String toString() {
        return super.toString() + " " + readCache;
    }

    @Override
    public boolean equivalentTo(ReadEliminationBlockState other) {
        return isSubMapOf(readCache, other.readCache);
    }

    public void addCacheEntry(CacheEntry<?> identifier, ValueNode value) {
        readCache.put(identifier, value);
    }

    public ValueNode getCacheEntry(CacheEntry<?> identifier) {
        return readCache.get(identifier);
    }

    public void killReadCache(LocationIdentity identity, ValueNode index, ValueNode array) {
        if (identity.isAny()) {
            // ANY aliases with every other location
            readCache.clear();
            return;
        }
        Iterator<CacheEntry<?>> iterator = readCache.getKeys().iterator();
        while (iterator.hasNext()) {
            CacheEntry<?> entry = iterator.next();
            /*
             * We cover multiple cases here but in general index and array can only be !=null for
             * indexed nodes thus the location identity of other accesses (field and object
             * locations) will never be the same and will never alias with array accesses.
             *
             * Unsafe accesses will alias if they are writing to any location.
             */
            if (entry.conflicts(identity, index, array)) {
                iterator.remove();
            }
        }
    }

    public EconomicMap<CacheEntry<?>, ValueNode> getReadCache() {
        return readCache;
    }
}
