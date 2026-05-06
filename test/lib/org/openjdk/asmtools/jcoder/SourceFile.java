/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jcoder;

import java.io.*;
import java.util.Hashtable;

import org.openjdk.asmtools.common.Tool;
import org.openjdk.asmtools.util.I18NResourceBundle;

/**
 * An input stream for java programs. The stream treats either "\n", "\r" or "\r\n" as the
 * end of a line, it always returns \n. It also parses UNICODE characters expressed as
 * \uffff. However, if it sees "\\", the second slash cannot begin a unicode sequence. It
 * keeps track of the current position in the input stream.
 *
 * An position consists of: ((linenr &lt;&lt; OFFSETBITS) | offset) this means that both
 * the line number and the exact offset into the file are encoded in each postion
 * value.<p>
 */
public class SourceFile implements org.openjdk.asmtools.jasm.Constants {

    Tool tool;

    boolean traceFlag = false;
    boolean debugInfoFlag = false;
    /**
     * The increment for each character.
     */
    static final int OFFSETINC = 1;
    /**
     * The increment for each line.
     */
    static final int LINEINC = 1 << OFFSETBITS;
    String inputFileName;
    InputStream in;
    PrintWriter out;
    int pos;
    private int chpos;
    private int pushBack = -1;

    public SourceFile(Tool tool, DataInputStream dataInputStream, String inputFileName, PrintWriter out) {
        this.tool = tool;
        this.inputFileName = inputFileName;
        this.in = new BufferedInputStream(dataInputStream);
        chpos = LINEINC;
        this.out = out;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public void closeInp() {
        try {
            in.close();
        } catch (IOException e) {
        }
        flushErrors();
    }

    public int read() throws IOException {
        pos = chpos;
        chpos += OFFSETINC;

        int c = pushBack;
        if (c == -1) {
            c = in.read();
        } else {
            pushBack = -1;
        }

        // parse special characters
        switch (c) {
            case -2:
                // -2 is a special code indicating a pushback of a backslash that
                // definitely isn't the start of a unicode sequence.
                return '\\';

            case '\\':
                if ((c = in.read()) != 'u') {
                    pushBack = (c == '\\' ? -2 : c);
                    return '\\';
                }
                // we have a unicode sequence
                chpos += OFFSETINC;
                while ((c = in.read()) == 'u') {
                    chpos += OFFSETINC;
                }

                // unicode escape sequence
                int d = 0;
                for (int i = 0; i < 4; i++, chpos += OFFSETINC, c = in.read()) {
                    switch (c) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            d = (d << 4) + c - '0';
                            break;

                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                            d = (d << 4) + 10 + c - 'a';
                            break;

                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
                            d = (d << 4) + 10 + c - 'A';
                            break;

                        default:
                            error(pos, "invalid.escape.char");
                            pushBack = c;
                            return d;
                    }
                }
                pushBack = c;
                return d;

            case '\n':
                chpos += LINEINC;
                return '\n';

            case '\r':
                if ((c = in.read()) != '\n') {
                    pushBack = c;
                } else {
                    chpos += OFFSETINC;
                }
                chpos += LINEINC;
                return '\n';

            default:
                return c;
        }
    }

    public int lineNumber(int pos) {
        return pos >>> OFFSETBITS;
    }

    public int lineNumber() {
        return pos >>> OFFSETBITS;
    }

    /*==============================================================  Environment */
    /**
     * The number of errors and warnings
     */
    public int nerrors;
    public int nwarnings;

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    /*
     *  Until place for jasm.properties is defind,
     *  I have to keep them right here
     *
     static Hashtable properties = new Hashtable(40);

     static {
     // Scanner:
     properties.put("err.eof.in.comment", "Comment not terminated at end of input.");
     properties.put("err.invalid.number", "Invalid character \'%s\' in number.");
     properties.put("err.invalid.octal.number", "Invalid character in octal number.");
     properties.put("err.overflow", "Numeric overflow.");
     properties.put("err.float.format", "Invalid floating point format.");
     properties.put("err.eof.in.string", "String not terminated at end of input.");
     properties.put("err.newline.in.string", "String not terminated at end of line.");
     properties.put("err.funny.char", "Invalid character in input.");
     properties.put("err.unbalanced.paren", "Unbalanced parentheses.");
     // Parser:
     properties.put("err.toplevel.expected", "Class or interface declaration expected.");
     properties.put("err.token.expected", "'%s' expected.");
     properties.put("err.identifier.expected", "Identifier expected.");
     properties.put("err.name.expected", "Name expected.");
     properties.put("err.io.exception", "I/O error in %s.");
     properties.put("err.cannot.write", "Cannot write to %s.");
     properties.put("warn.array.wronglength", "expected array length %s do not match real length %s; expected length written");
     properties.put("warn.attr.wronglength", "expected attribute length %s do not match real length %s; expected length written");
     properties.put("attrname.notfound", "Cannot find \"%s\" in constant pool");
     properties.put("err.attrname.expected", "Attribute's name or index expected.");
     properties.put("err.element.expected", "Primary data item expected.");
     properties.put("err.struct.expected", "Structured data item expected.");
     properties.put("err.macro.undecl", "Macro %s undefined.");
     }
     static String getProperty(String nm) {
     return (String) properties.get(nm);
     }
     */
    /**
     * Error String
     */
    String errorString(String err, Object arg1, Object arg2, Object arg3) {
        String str = null;

        if (!err.startsWith("warn.")) {
            err = "err." + err;
        }
        //str = getProperty(err);
        str = i18n.getString(err);

        if (str == null) {
            return "error message '" + err + "' not found";
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c == '%') && (i + 1 < str.length())) {
                switch (str.charAt(++i)) {
                    case 's':
                        String arg = arg1.toString();
                        for (int j = 0; j < arg.length(); j++) {
                            switch (c = arg.charAt(j)) {
                                case ' ':
                                case '\t':
                                case '\n':
                                case '\r':
                                    buf.append((char) c);
                                    break;

                                default:
                                    if ((c > ' ') && (c <= 255)) {
                                        buf.append((char) c);
                                    } else {
                                        buf.append('\\');
                                        buf.append('u');
                                        buf.append(Integer.toString(c, 16));
                                    }
                            }
                        }
                        arg1 = arg2;
                        arg2 = arg3;
                        break;

                    case '%':
                        buf.append('%');
                        break;

                    default:
                        buf.append('?');
                        break;
                }
            } else {
                buf.append((char) c);
            }
        }
        return buf.toString();
    }

    /**
     * List of outstanding error messages
     */
    ErrorMessage errors;

    /**
     * Insert an error message in the list of outstanding error messages. The list is
     * sorted on input position.
     */
    void insertError(int where, String message) {
        //output("ERR = " + message);
        ErrorMessage msg = new ErrorMessage(where, message);
        if (errors == null) {
            errors = msg;
        } else if (errors.where > where) {
            msg.next = errors;
            errors = msg;
        } else {
            ErrorMessage m = errors;
            for (; (m.next != null) && (m.next.where <= where); m = m.next) {
                ;
            }
            msg.next = m.next;
            m.next = msg;
        }
    }

    /**
     * Flush outstanding errors
     */
    public void flushErrors() {
        if (errors == null) {
            return;
        }

        try {
            // Read the file
            DataInputStream dataInputStream = tool.getDataInputStream(inputFileName);
            if (dataInputStream == null)
                return;

            byte data[] = new byte[dataInputStream.available()];
            dataInputStream.read(data);
            dataInputStream.close();

            // Report the errors
            for (ErrorMessage msg = errors; msg != null; msg = msg.next) {
                int ln = msg.where >>> OFFSETBITS;
                int off = msg.where & ((1 << OFFSETBITS) - 1);

                int i, j;
                for (i = off; (i > 0) && (data[i - 1] != '\n') && (data[i - 1] != '\r'); i--) {
                    ;
                }
                for (j = off; (j < data.length) && (data[j] != '\n') && (data[j] != '\r'); j++) {
                    ;
                }

                String prefix = inputFileName + ":" + ln + ":";
                outputln(prefix + " " + msg.message);
                outputln(new String(data, i, j - i));

                char strdata[] = new char[(off - i) + 1];
                for (j = i; j < off; j++) {
                    strdata[j - i] = (data[j] == '\t') ? '\t' : ' ';
                }
                strdata[off - i] = '^';
                outputln(new String(strdata));
            }
        } catch (IOException e) {
            outputln("I/O exception");
        }
        errors = null;
    }

    /**
     * Output a string. This can either be an error message or something for debugging.
     * This should be used instead of print.
     */
    public void output(String msg) {
        int len = msg.length();
        for (int i = 0; i < len; i++) {
            out.write(msg.charAt(i));
        }
        out.flush();
    }

    /**
     * Output a string. This can either be an error message or something for debugging.
     * This should be used instead of println.
     */
    public void outputln(String msg) {
        output(msg);
        out.write('\n');
        out.flush();
    }

    /**
     * Issue an error.
     * @param where Offset in the source for the error
     * @param err Error number (as defined in this interface)
     * @param arg1 Optional argument to the error (null if not applicable)
     * @param arg2 Optional argument to the error (null if not applicable)
     * @param arg3 Optional argument to the error (null if not applicable)
     */
    /**
     * Issue an error
     */
    public void error(int where, String err, Object arg1, Object arg2, Object arg3) {
        String msg = errorString(err, arg1, arg2, arg3);
        if (err.startsWith("warn.")) {
            nwarnings++;
        } else {
            nerrors++;
        }
        traceln("error:" + msg);
        insertError(where, msg);
    }

    public final void error(int where, String err, Object arg1, Object arg2) {
        error(where, err, arg1, arg2, null);
    }

    public final void error(int where, String err, Object arg1) {
        error(where, err, arg1, null, null);
    }

    public final void error(int where, String err) {
        error(where, err, null, null, null);
    }

    public final void error(String err) {
        error(pos, err, null, null, null);
    }

    public final void error(String err, Object arg1) {
        error(pos, err, arg1, null, null);
    }

    /*==============================================================  trace */
    public void trace(String message) {
        if (traceFlag) {
            output(message);
        }
    }

    public void traceln(String message) {
        if (traceFlag) {
            outputln(message);
        }
    }
} // end SourceFile

