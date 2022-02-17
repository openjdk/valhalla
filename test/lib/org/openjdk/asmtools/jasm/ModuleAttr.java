/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The module attribute
 */
class ModuleAttr extends AttrData {
  // shared data
  private Module.Builder builder;
  private final ClassData clsData;
  private final Function<String, ConstantPool.ConstCell> findCellAsciz;
  private final Function<String, ConstantPool.ConstCell> findCellClassByName;
  private final Function<String, ConstantPool.ConstCell> findCellModuleByName;
  private final Function<String, ConstantPool.ConstCell> findCellPackageByName;

  // entries to populate tables of the module attribute
  BiConsumer<String, Integer> requires       = (mn, f) -> this.builder.require(mn, f);
  BiConsumer<String, Set<String>> exports    = (pn, ms) -> this.builder.exports(new Module.Exported(pn), ms);
  BiConsumer<String, Set<String>> opens      = (pn, ms) -> this.builder.opens(new Module.Opened(pn), ms);
  BiConsumer<String, Set<String>> provides   = (tn, ts) -> this.builder.provides(new Module.Provided(tn), ts);
  Consumer<Set<String>>           uses       = (ts) -> this.builder.uses(ts);

  ModuleAttr(ClassData cdata) {
    super(cdata, Tables.AttrTag.ATT_Module.parsekey());
    builder = new Module.Builder();
    clsData = cdata;
    findCellAsciz = (name) -> clsData.pool.FindCellAsciz(name);
    findCellClassByName = (name) -> clsData.pool.FindCellClassByName(name);
    findCellModuleByName = (name) -> clsData.pool.FindCellModuleByName(name);
    findCellPackageByName = (name) -> clsData.pool.FindCellPackageByName(name);
  }

  void openModule() {
    builder.setModuleFlags(Module.Modifier.ACC_OPEN);
  }
  void setModuleName(String value) { builder.setModuleName(value);}

  ModuleAttr build() {
    Module module = builder.build();
    Content.instance.header = new HeaderStruct(module.header, findCellModuleByName, findCellAsciz);
    Content.instance.requiresStruct = new SetStruct<>(module.requires, findCellModuleByName, findCellAsciz);
    Content.instance.exportsMapStruct = new MapStruct<>(module.exports, findCellPackageByName, findCellModuleByName );
    Content.instance.opensMapStruct = new MapStruct<>(module.opens,findCellPackageByName, findCellModuleByName );
    Content.instance.usesStruct = new SetStruct<>(module.uses, findCellClassByName, null);
    Content.instance.providesMapStruct = new MapStruct<>(module.provides, findCellClassByName, findCellClassByName);
    return this;
  }

  @Override
  public int attrLength() {
    return Content.instance.getLength();
  }

  @Override
  public void write(CheckedDataOutputStream out) throws IOException {
    super.write(out);
    Content.instance.write(out);
  }

  private enum Content implements Data {
    instance {
      @Override
      public int getLength() {
        return header.getLength() +
            requiresStruct.getLength() +
            exportsMapStruct.getLength() +
            opensMapStruct.getLength() +
            usesStruct.getLength() +
            providesMapStruct.getLength();
      }

      @Override
      public void write(CheckedDataOutputStream out) throws IOException {
        // keep order!
        header.write(out);
        requiresStruct.write(out);
        exportsMapStruct.write(out);
        opensMapStruct.write(out);
        usesStruct.write(out);
        providesMapStruct.write(out);
      }
    };

    HeaderStruct header ;
    SetStruct<Module.Dependence>  requiresStruct;
    MapStruct<Module.Exported>    exportsMapStruct;
    MapStruct<Module.Opened>      opensMapStruct;
    SetStruct<Module.Uses>        usesStruct;
    MapStruct<Module.Provided>    providesMapStruct;
  }

  /**
   * u2 {exports|opens}_count;
   * {  u2 {exports|opens}_index;
   * u2 {exports|opens}_flags;
   * u2 {exports|opens}_to_count;
   * u2 {exports|opens}_to_index[{exports|opens}_to_count];
   * } {exports|opens}[{exports|opens}_count];
   * or
   * u2 provides_count;
   * {  u2 provides_index;
   * u2 provides_with_count;
   * u2 provides_with_index[provides_with_count];
   * } provides[provides_count];
   */
  private class MapStruct<T extends Module.TargetType> implements Data {
    final List<Triplet<ConstantPool.ConstCell, Integer, List<ConstantPool.ConstCell>>> exportsOpensList = new ArrayList<>();
    final List<Pair<ConstantPool.ConstCell, List<ConstantPool.ConstCell>>> providesList = new ArrayList<>();

    MapStruct(Map<T, Set<String>> source,
              Function<String,ConstantPool.ConstCell> nameFinder,
              Function<String,ConstantPool.ConstCell> targetFinder) {
      Objects.requireNonNull(source);
      source.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> {
                ArrayList<ConstantPool.ConstCell> to = new ArrayList<>();
                e.getValue().forEach(mn -> to.add(targetFinder.apply(mn)));
                if (e.getKey().isFlagged()) {
                  exportsOpensList.add(new Triplet<>
                      ( nameFinder.apply(e.getKey().getTypeName()),
                        ((Module.FlaggedTargetType) e.getKey()).getFlags(),
                        to));
                } else {
                  providesList.add(new Pair<>(nameFinder.apply(e.getKey().getTypeName()),
                      to));
                }
              }
          );
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
      if (providesList.isEmpty()) {
        out.writeShort(exportsOpensList.size());          // u2 {exports|opens}_count;
        for (Triplet<ConstantPool.ConstCell, Integer, List<ConstantPool.ConstCell>> triplet : exportsOpensList) {
          out.writeShort(triplet.first.arg);              // {  u2 {exports|opens}_index;
          out.writeShort(triplet.second);                 //    u2 {exports|opens}_flags;
          out.writeShort(triplet.third.size());           //    u2 {exports|opens}_to_count;
          for (ConstantPool.ConstCell to : triplet.third)
            out.writeShort(to.arg);                       // u2 {exports|opens}_to_index[{exports|opens}_to_count]; }
        }
      } else {
        out.writeShort(providesList.size());              // u2 provides_count;
        for (Pair<ConstantPool.ConstCell, List<ConstantPool.ConstCell>> pair : providesList) {
          out.writeShort(pair.first.arg);                 // {  u2 provides_index;
          out.writeShort(pair.second.size());             //    u2 provides_with_count;
          for (ConstantPool.ConstCell to : pair.second)
            out.writeShort(to.arg);                       // u2 provides_with_index[provides_with_count]; }
        }
      }
    }

    @Override
    public int getLength() {
      if (providesList.isEmpty()) {
        // (u2:{exports|opens}_count) + (u2:{exports|opens}_index + u2:{exports|opens}_flags u2:{exports|opens}_to_count) * {exports|opens}_count +
        return 2 + 6 * exportsOpensList.size() +
        //  (u2:{exports|opens}_to_index) * {exports|opens}_to_count
            exportsOpensList.stream().mapToInt(p -> p.third.size()).filter(s -> s > 0).sum() * 2;
      } else {
        // (u2 : provides_count) + (u2:provides_index + u2:provides_with_count) * provides_count +
        return 2 + 4 * providesList.size() +
        // (u2:provides_with_index) * provides_with_count
            providesList.stream().mapToInt(p -> p.second.size()).filter(s -> s > 0).sum() * 2;
      }
    }
  }

  private class HeaderStruct implements Data {
    final ConstantPool.ConstCell index;
    final int flags;
    final ConstantPool.ConstCell versionIndex;

    HeaderStruct(Module.Header source,
                 Function<String,ConstantPool.ConstCell> nameFinder,
                 Function<String,ConstantPool.ConstCell> versionFinder) {
      index = nameFinder.apply(source.getModuleName());
      versionIndex = (source.getModuleVersion() == null ) ? null : versionFinder.apply(source.getModuleVersion());
      flags = source.getModuleFlags();
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
      out.writeShort(index.arg);                                    // u2 module_name_index;
      out.writeShort(flags);                                        // u2 module_flags;
      out.writeShort(versionIndex == null ? 0 : versionIndex.arg);  // u2 module_version_index;
    }

    @Override
    public int getLength() {
      // u2:module_name_index) +  u2:module_flags +u2:module_version_index
      return 6;
    }
  }

  /**
   * u2 uses_count;
   * u2 uses_index[uses_count];
   * or
   * u2 requires_count;
   * {  u2 requires_index;
   *    u2 requires_flags;
   *    u2 requires_version_index;
   * } requires[requires_count];
   */
  private class SetStruct<T extends Module.TargetType> implements Data {
    final List<ConstantPool.ConstCell> usesList = new ArrayList<>();
    final List<Triplet<ConstantPool.ConstCell, Integer, ConstantPool.ConstCell>> requiresList = new ArrayList<>();

    SetStruct(Set<T> source,
              Function<String,ConstantPool.ConstCell> nameFinder,
              Function<String,ConstantPool.ConstCell> versionFinder) {
      Objects.requireNonNull(source);
      source.forEach(e -> {
        if (e.isFlagged()) {
          requiresList.add(new Triplet<>(
              nameFinder.apply(e.getTypeName()),
              ((Module.FlaggedTargetType) e).getFlags(),
              (((Module.VersionedFlaggedTargetType) e).getVersion() == null) ?
                  null :
                  versionFinder.apply(((Module.VersionedFlaggedTargetType) e).getVersion())));
        } else {
          usesList.add(nameFinder.apply((e.getTypeName())));
        }
      });
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
      if (usesList.isEmpty()) {
        out.writeShort(requiresList.size());                  // u2 requires_count;
        for (Triplet<ConstantPool.ConstCell, Integer, ConstantPool.ConstCell> r : requiresList) {
          out.writeShort(r.first.arg);                        // u2 requires_index;
          out.writeShort(r.second);                           // u2 requires_flags;
          out.writeShort(r.third == null ? 0 : r.third.arg);  // u2 requires_version_index;
        }
      } else {
        out.writeShort(usesList.size());                      // u2 uses_count;
        for (ConstantPool.ConstCell u : usesList)
          out.writeShort(u.arg);                              // u2 uses_index[uses_count];
      }
    }

    @Override
    public int getLength() {
      return usesList.isEmpty() ?
          // (u2:requires_count) + (u2:requires_index + u2:requires_flags + u2:requires_version_index) * requires_count
          2 + 6 * requiresList.size() :
          // (u2:uses_count) + (u2:uses_index) * uses_count
          2 + 2 * usesList.size();
    }
  }

  // Helper classes
  private class Pair<F, S> {
    final F first;
    final S second;

    Pair(F first, S second) {
      this.first = first;
      this.second = second;
    }
  }

  public class Triplet<F, S, T>  extends Pair<F,S> {
    private final T third;
    Triplet(F first, S second, T third) {
      super(first,second);
      this.third = third;
    }
  }

}
