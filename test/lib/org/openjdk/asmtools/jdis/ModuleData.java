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
package org.openjdk.asmtools.jdis;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.openjdk.asmtools.common.Module;
import org.openjdk.asmtools.common.Tool;
import org.openjdk.asmtools.jasm.JasmTokens;

import static org.openjdk.asmtools.jdis.Main.i18n;

/**
 *  The module attribute data.
 */
public class ModuleData {

  // internal references
  private final Tool tool;

  private ConstantPool pool;
  private PrintWriter out;
  private Module module;

  public ModuleData(ClassData clsData) {
    this.tool = clsData.tool;
    this.pool = clsData.pool;
    this.out = clsData.out;
  }

  public String getModuleName() {
    return module == null ? "N/A" : module.getModuleName();
  }

  public String getModuleVersion() { return module.getModuleVersion();  }

  public String getModuleHeader() {
    if ( module == null ) {
      return "N/A";
    } else {
      StringBuilder sb = new StringBuilder(module.getModuleFlags());
      sb.append(JasmTokens.Token.MODULE.parseKey()).append(" ");
      sb.append(module.getModuleName());
      if (module.getModuleVersion() != null)
        sb.append("// @").append(module.getModuleVersion());
      return sb.toString();
    }
  }

  /**
   * Reads and resolve the method's attribute data called from ClassData.
   */
  public void read(DataInputStream in) throws IOException {
    int index, moduleFlags, versionIndex;
    String moduleName, version;
    Module.Builder builder;
    try {
    // u2 module_name_index;
    index = in.readUnsignedShort();
    moduleName = pool.getModule(index);
    // u2 module_flags;
    moduleFlags = in.readUnsignedShort();
    // u2 module_version_index;
    versionIndex = in.readUnsignedShort();
    version = pool.getString(versionIndex);
    builder = new Module.Builder(moduleName, moduleFlags, version);
    } catch (IOException ioe) {
      tool.error(i18n.getString("jdis.error.invalid_header"));
      throw ioe;
    }

    try {
      int requires_count = in.readUnsignedShort();
      for (int i = 0; i < requires_count; i++) {
        index = in.readUnsignedShort();
        int requiresFlags = in.readUnsignedShort();
        versionIndex = in.readUnsignedShort();

        moduleName = pool.getModule(index);
        version = pool.getString(versionIndex);
        builder.require(moduleName, requiresFlags, version);
      }
    } catch (IOException ioe) {
      tool.error(i18n.getString("jdis.error.invalid_requires"));
      throw ioe;
    }

    try {
      int exports_count = in.readUnsignedShort();
      if (exports_count > 0) {
        for (int i = 0; i < exports_count; i++) {
          index = in.readUnsignedShort();
          String packageName = pool.getPackage(index);
          int exportsFlags = in.readUnsignedShort();
          int exports_to_count = in.readUnsignedShort();
          if (exports_to_count > 0) {
            Set<String> targets = new HashSet<>(exports_to_count);
            for (int j = 0; j < exports_to_count; j++) {
              int exports_to_index = in.readUnsignedShort();
              targets.add(pool.getModule(exports_to_index));
            }
            builder.exports(packageName, exportsFlags, targets);
          } else {
            builder.exports(packageName, exportsFlags);
          }
        }
      }
    } catch (IOException ioe) {
      tool.error(i18n.getString("jdis.error.invalid_exports"));
      throw ioe;
    }

    try {
      int opens_count = in.readUnsignedShort();
      if (opens_count > 0) {
        for (int i = 0; i < opens_count; i++) {
          index = in.readUnsignedShort();
          String packageName = pool.getPackage(index);
          int opensFlags = in.readUnsignedShort();
          int opens_to_count = in.readUnsignedShort();
          if (opens_to_count > 0) {
            Set<String> targets = new HashSet<>(opens_to_count);
            for (int j = 0; j < opens_to_count; j++) {
              int opens_to_index = in.readUnsignedShort();
              targets.add(pool.getModule(opens_to_index));
            }
            builder.opens(packageName, opensFlags, targets);
          } else {
            builder.opens(packageName, opensFlags);
          }
        }
      }
    } catch (IOException ioe) {
      tool.error(i18n.getString("jdis.error.invalid_opens"));
      throw ioe;
    }

    try {
      int uses_count = in.readUnsignedShort();
      if (uses_count > 0) {
        for (int i = 0; i < uses_count; i++) {
          index = in.readUnsignedShort();
          String serviceName = pool.getClassName(index);
          builder.uses(serviceName);
        }
      }
    } catch (IOException ioe) {
      tool.error(i18n.getString("jdis.error.invalid_uses"));
      throw ioe;
    }

    try {
      int provides_count = in.readUnsignedShort();
      if (provides_count > 0) {
        for (int i = 0; i < provides_count; i++) {
          index = in.readUnsignedShort();
          String serviceName = pool.getClassName(index);
          int provides_with_count = in.readUnsignedShort();
          Set<String> implNames = new HashSet<>(provides_with_count);
          for (int j = 0; j < provides_with_count; j++) {
            int provides_with_index = in.readUnsignedShort();
            implNames.add(pool.getClassName(provides_with_index));
          }
          builder.provides(serviceName, implNames);
        }
      }
    } catch (IOException ioe) {
      tool.error(i18n.getString("jdis.error.invalid_provides"));
      throw ioe;
    }
    module = builder.build();
  }

  /* Print Methods */
  public void print() {
    if (module != null)
      out.println(module.toString());
  }
}
