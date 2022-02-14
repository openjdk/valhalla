/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.util;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A class that lazily opens a package-specific resource bundle containing localization
 * data for a class.
 */
public class I18NResourceBundle extends ResourceBundle {

    /**
     * Get a package-specific resource bundle for a class containing localization data.
     * The bundle is named i18n.properties in the same package as the given class.
     *
     * @param c the class for which to obtain the resource bundle
     * @return the appropriate resource bundle for the class
     */
    public static I18NResourceBundle getBundleForClass(Class c) {
        String cn = c.getName();
        int dot = cn.lastIndexOf('.');
        String rn = (dot == -1 ? "i18n" : cn.substring(0, dot) + ".i18n");
        return new I18NResourceBundle(rn, c.getClassLoader());
    }

    /**
     * Create a resource bundle for the given name. The actual resource bundle will not be
     * loaded until it is needed.
     *
     * @param name The name of the actual resource bundle to use.
     */
    private I18NResourceBundle(String name, ClassLoader cl) {
        this.name = name;
        this.classLoader = cl;
    }

    /**
     * Get an entry from the resource bundle. If the resource cannot be found, a message
     * is printed to the console and the result will be a string containing the method
     * parameters.
     *
     * @param key the name of the entry to be returned
     * @param arg an argument to be formatted into the result using
     * {@link java.text.MessageFormat#format}
     * @return the formatted string
     */
    public String getString(String key, Object arg) {
        return getString(key, new Object[]{arg});
    }

    /**
     * Get an entry from the resource bundle. If the resource cannot be found, a message
     * is printed to the console and the result will be a string containing the method
     * parameters.
     *
     * @param key the name of the entry to be returned
     * @param args an array of arguments to be formatted into the result using
     * {@link java.text.MessageFormat#format}
     * @return the formatted string
     */
    public String getString(String key, Object... args) {
        try {
            return MessageFormat.format(getString(key), args);
        } catch (MissingResourceException e) {
            System.err.println("WARNING: missing resource: " + key + " for " + name);
            StringBuffer sb = new StringBuffer(key);
            for (int i = 0; i < args.length; i++) {
                sb.append('\n');
                sb.append(args.toString());
            }
            return sb.toString();
        }
    }

    /**
     * Get an entry from the bundle, returning null if it is not found.
     *
     * @param key the name of the entry to be returned
     * @return the value of the entry, or null if it is not found.
     */
    public String getOptionalString(String key) {
        if (delegate == null) {
            delegate = ResourceBundle.getBundle(name, Locale.getDefault(), classLoader);
        }
        try {
            String s = (String) (delegate.getObject(key));
            if (s != null) {
                System.out.println("i18n: " + key);
            }
            return s;
        } catch (MissingResourceException e) {
            return null;
        }
    }

    /**
     * A required internal method for ResourceBundle. Load the actual resource bundle, if
     * it has not yet been loaded, then hand the request off to that bundle. If the
     * resource cannot be found, a message is printed to the console and the result will
     * be the original tag.
     */
    protected Object handleGetObject(String key) throws MissingResourceException {
        try {
            if (delegate == null) {
                delegate = ResourceBundle.getBundle(name, Locale.getDefault(), classLoader);
            }
            return delegate.getObject(key);
        } catch (MissingResourceException e) {
            System.err.println("WARNING: missing resource: " + key + " for " + name);
            return key;
        }
    }

    /**
     * A required internal method for ResourceBundle. Load the actual resource bundle, if
     * it has not yet been loaded, then hand the request off to that bundle.
     */
    public Enumeration getKeys() {
        if (delegate == null) {
            delegate = ResourceBundle.getBundle(name);
        }
        return delegate.getKeys();
    }

    /**
     * Returns the name of this bundle (useful for methods using bundle name instead of
     * instance, such as <code>Logger</code> creation,
     *
     * @return the name of this resource bundle
     */
    public String getName() {
        return name;
    }

    private String name;
    private ResourceBundle delegate;
    private ClassLoader classLoader;
}
