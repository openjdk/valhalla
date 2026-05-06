/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common;

import org.openjdk.asmtools.jdis.Indenter;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Internal presentation of a module
 */
public final class Module extends Indenter {

  //* A module name and module_flags
  public final Header header;
  //* A service dependence's of this module
  public final Set<Uses> uses;
  //* Modules on which the current module has a dependence.
  public final Set<Dependence> requires;
  //* A module exports, may be qualified or unqualified.
  public final Map<Exported, Set<String>> exports;
  //* Packages, to be opened by the current module
  public final Map<Opened, Set<String>> opens;
  //* A service that a module provides one or more implementations of.
  public final Map<Provided, Set<String>> provides;

  private Module(Builder builder) {
    this.header = builder.header;
    this.requires = Collections.unmodifiableSet(builder.requires);
    this.exports = Collections.unmodifiableMap(builder.exports);
    this.opens = Collections.unmodifiableMap(builder.opens);
    this.uses = Collections.unmodifiableSet(builder.uses);
    this.provides = Collections.unmodifiableMap(builder.provides);
  }

  public String getModuleFlags () {
    return Modifier.getModuleModifiers(header.getFlags());
  }
  public String getModuleName () { return header.getModuleName();  }
  public String getModuleVersion()  { return header.getModuleVersion(); };

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    int l = 0;
    requires.stream()
        .sorted()
        .forEach(d -> sb.append(getIndentString()).append(format("requires %s;%s%n",
            d.toString(),
            d.getModuleVersion() == null ? "" : " // @" + d.getModuleVersion())));
    //
    l = newLine(sb,l);
    exports.entrySet().stream()
        .filter(e -> e.getValue().isEmpty())
        .sorted(Map.Entry.comparingByKey())
        .map(e -> format("%sexports %s;%n", getIndentString(), e.getKey().toString()))
        .forEach(sb::append);
    exports.entrySet().stream()
        .filter(e -> !e.getValue().isEmpty())
        .sorted(Map.Entry.comparingByKey())
        .map(e -> format("%sexports %s to%n%s;%n", getIndentString(), e.getKey().toString(),
            e.getValue().stream().sorted()
                .map(mn -> format("%s          %s", getIndentString(), mn))
                .collect(Collectors.joining(",\n"))))
        .forEach(sb::append);
    //
    l = newLine(sb,l);
    opens.entrySet().stream()
        .filter(e -> e.getValue().isEmpty())
        .sorted(Map.Entry.comparingByKey())
        .map(e -> format("%sopens %s;%n", getIndentString(), e.getKey().toString()))
        .forEach(sb::append);
    opens.entrySet().stream()
        .filter(e -> !e.getValue().isEmpty())
        .sorted(Map.Entry.comparingByKey())
        .map(e -> format("%sopens %s to%n%s;%n", getIndentString(), e.getKey().toString(),
            e.getValue().stream().sorted()
                .map(mn -> format("%s          %s", getIndentString(), mn))
                .collect(Collectors.joining(",\n"))))
        .forEach(sb::append);
    //
    l = newLine(sb,l);
    uses.stream().sorted()
        .map(s -> format("%suses %s;%n", getIndentString(), s))
        .forEach(sb::append);
    //
    l = newLine(sb,l);
    provides.entrySet().stream()
        .filter(e -> !e.getValue().isEmpty())
        .sorted(Map.Entry.comparingByKey())
        .map(e -> format("%sprovides %s with%n%s;%n", getIndentString(), e.getKey().toString(),
            e.getValue().stream().sorted()
                .map(mn -> format("%s          %s", getIndentString(), mn))
                .collect(Collectors.joining(",\n"))))
        .forEach(sb::append);
    //
    if( Character.isWhitespace(sb.charAt(sb.length()-1)) )
      sb.deleteCharAt(sb.length()-1);
    return sb.toString();
  }

  private int newLine(StringBuilder sb, int length) {
    if(sb.length() > length) {
      sb.append("\n");
      return sb.length() + 1;
    }
    return length;
  }

  /**
   * Modules flags
   */
  public enum Modifier {
    ACC_NONE(0x0000, "", ""),
    ACC_OPEN(0x0020, "open", "ACC_OPEN"),
    ACC_TRANSITIVE(0x0020, "transitive", "ACC_TRANSITIVE"),
    ACC_STATIC_PHASE(0x0040, "static", "ACC_STATIC_PHASE"),
    ACC_SYNTHETIC(0x1000, "", "ACC_SYNTHETIC"),
    ACC_MANDATED(0x8000, "", "ACC_MANDATED");
    private final int value;
    private final String keyword;
    private final String flag;
    Modifier(int value, String keyword, String flagName) {
      this.value = value;
      this.keyword = keyword;
      this.flag = flagName;
    }

    public int asInt() { return value; }

    public static String getModuleModifiers(int flag) {
      return asString(flag, false, ACC_TRANSITIVE);
    }

    public static String getModuleFlags(int flag) {
      return asString(flag, true, ACC_TRANSITIVE);
    }

    public static String getStatementModifiers(int flag) {
      return asString(flag, false, ACC_OPEN);
    }

    public static String getStatementFlags(int flag) {
      return asString(flag, true, ACC_OPEN);
    }

    private static String asString(int value, boolean flagFormat, Modifier skipped ) {
      String buf = "";
      for(Module.Modifier m : values()) {
        if( m != skipped && (value & m.value) != 0) {
          buf += ((flagFormat) ? m.flag : m.keyword) + " ";
          value ^= m.value;
        }
      }
      if( flagFormat && value != 0 )
        buf += String.format("0x%04X ", value);
      return buf;
    }
  }

  // A module header consists of a module name and module flags
  public final static class Header extends VersionedFlaggedTargetType{
    Header(String typeName, int flag) { this(typeName, flag, null); }
    Header(String typeName, int flag, String moduleVersion) { super(typeName, flag, moduleVersion); }
    public String getModuleName()    { return getTypeName(); }
    public int    getModuleFlags()   { return getFlags();    }
    public String getModuleVersion() { return getVersion();  }
  }

  //* A module on which the current module has a dependence.
  public final static class Dependence extends VersionedFlaggedTargetType {
    public Dependence(String moduleName, int flag) {this(moduleName, flag, null);}
    public Dependence(String moduleName, int flag, String moduleVersion) {super(moduleName, flag, moduleVersion);}
    public Dependence(String moduleName, boolean transitive, boolean staticPhase) { this(moduleName,transitive,staticPhase,null);}
    public Dependence(String moduleName, boolean transitive, boolean staticPhase, String moduleVersion) {
      this(moduleName,
          (transitive ? Modifier.ACC_TRANSITIVE.value : Modifier.ACC_NONE.value) |
          (staticPhase ? Modifier.ACC_STATIC_PHASE.value : Modifier.ACC_NONE.value), moduleVersion);
    }
    public String getModuleVersion()          { return getVersion();  }
  }

  public final static class Uses extends TargetType {
    public Uses(String typeName) { super(typeName); }
  }

  //* A provided type of the current module.
  public final static class Provided extends TargetType {
    public Provided(String typeName) { super(typeName); }
  }

  //* An opened package of the current module.
  public final static class Opened extends FlaggedTargetType {
    public Opened(String typeName) {
      super(typeName, 0);
    }
    public Opened(String typeName, int opensFlags) {
      super(typeName, opensFlags);
    }
  }

  //* An exported package of the current module.
  public final static class Exported extends FlaggedTargetType {
    public Exported(String typeName) {
      super(typeName, 0);
    }

    public Exported(String typeName, int exportsFlags) {
      super(typeName, exportsFlags);
    }
  }

  public static class VersionedFlaggedTargetType extends FlaggedTargetType {
    private String version;

    VersionedFlaggedTargetType(String typeName, int flag) {
      this(typeName,flag, null);
    }

    VersionedFlaggedTargetType(String typeName, int flag, String version) {
      super(typeName, flag);
      this.version = version != null && !version.isEmpty() ? version : null;
    }
    public String getVersion() { return version; }

    @Override
    public int hashCode() {
      int code = version == null ? 0 : version.hashCode();
      return code + super.hashCode();
    }
  }

  public static class FlaggedTargetType extends TargetType {
    private int flag;

    FlaggedTargetType(String typeName, int flag) {
      super(typeName);
      this.flag = flag;
    }

    public boolean isFlagged() {
      return true;
    }

    public int getFlags() {
      return flag;
    }

    public void setFlag(int value) { flag = value; }

    @Override
    public int hashCode() {
      return super.hashCode() + flag;
    }

    @Override
    public boolean equals(Object o) {
      return super.equals(o) && ((FlaggedTargetType) o).flag == this.flag;
    }

    @Override
    public String toString() {
      return Modifier.getStatementModifiers(this.flag)+ super.toString();
    }
  }

  public static class TargetType implements Comparable<TargetType> {
    private String typeName;

    TargetType(String typeName) { this.typeName = typeName; }

    public String getTypeName() {
      return typeName;
    }

    public void setTypeName(String value) { typeName = value; }

    public boolean isFlagged() {
      return false;
    }

    @Override
    public int hashCode() { return typeName.hashCode() * 11; }

    @Override
    public boolean equals(Object o) {
      if (o instanceof TargetType) {
        TargetType t = (TargetType) o;
        return this.typeName.equals(t.getTypeName());
      }
      return false;
    }

    @Override
    public int compareTo(TargetType t) {
      return this.typeName.compareTo(t.getTypeName());
    }

    @Override
    public String toString() {
      return typeName;
    }
  }

  /**
   * The module builder.
   */
  public static final class Builder {
    final Header header;
    final Set<Dependence> requires = new HashSet<>();
    final Map<Exported, Set<String>> exports = new HashMap<>();
    final Map<Opened, Set<String>> opens = new HashMap<>();
    final Set<Uses> uses = new HashSet<>();
    final Map<Provided, Set<String>> provides = new HashMap<>();


    public Builder() {
      this("", Modifier.ACC_NONE.asInt(), null);
    }

    public Builder(String moduleName, int moduleFlags, String moduleVersion) {
      header = new Header( moduleName,moduleFlags, moduleVersion);
    }

    public Builder setModuleFlags(int moduleFlags) {
      header.setFlag(header.getFlags() | moduleFlags);
      return this;
    }

    public Builder setModuleFlags(Modifier... moduleFlags) {
      for (Modifier m : moduleFlags)
        setModuleFlags(m.value);
      return this;
    }

    public Builder setModuleName(String value) {
      header.setTypeName(value);
      return this;
    }

    public Builder require(String d, boolean transitive, boolean staticPhase, String version) {
      requires.add(new Dependence(d, transitive, staticPhase, version));
      return this;
    }

    public Builder require(String d, int requiresFlag, String version) {
      requires.add(new Dependence(d, requiresFlag, version));
      return this;
    }

    public Builder require(String d, int requiresFlag) {
      requires.add(new Dependence(d, requiresFlag, null));
      return this;
    }

    public Builder opens(Opened p, Set<String> ms) {
      return add(opens, p, ms);
    }

    public Builder opens(String packageName, int exportFlags, Set<String> ms) {
      return add(opens, new Opened(packageName, exportFlags), ms);
    }

    public Builder opens(String packageName, int exportFlags) {
      return add(opens, new Opened(packageName, exportFlags), new HashSet<>());
    }


    public Builder exports(Exported p, Set<String> ms) {
      return add(exports, p, ms);
    }

    public Builder exports(String packageName, int exportFlags, Set<String> ms) {
      return add(exports, new Exported(packageName, exportFlags), ms);
    }

    public Builder exports(String packageName, int exportFlags) {
      return add(exports, new Exported(packageName, exportFlags), new HashSet<>());
    }

    public Builder uses(String serviceName) {
      uses.add(new Uses(serviceName));
      return this;
    }


    public Builder uses(Set<String> serviceNames) {
      uses.addAll(serviceNames.stream().map(Uses::new).collect(Collectors.toList()));
      return this;
    }

    public Builder provides(Provided t, Set<String> implementations) {
      return add(provides, t, implementations);
    }

    public Builder provides(String serviceName, Set<String> implementations) {
      return add(provides, new Provided(serviceName), implementations);
    }


    /**
     * @return The new module
     */
    public Module build() {
      return new Module(this);
    }

    private <T extends TargetType> Builder  add( Map<T, Set<String>> collection, T source, Set<String> target) {
      Objects.requireNonNull(source);
      Objects.requireNonNull(target);
      if (!collection.containsKey(source))
        collection.put(source, new HashSet<>());
      collection.get(source).addAll(target);
      return this;
    }
  }
}
