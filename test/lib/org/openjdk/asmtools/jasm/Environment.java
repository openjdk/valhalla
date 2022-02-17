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

import static org.openjdk.asmtools.jasm.Constants.EOF;
import static org.openjdk.asmtools.jasm.Constants.OFFSETBITS;
import org.openjdk.asmtools.util.I18NResourceBundle;

import java.io.*;
import java.nio.file.Paths;
import java.text.MessageFormat;

/**
 * An input stream for java programs. The stream treats either "\n", "\r" or "\r\n" as the
 * end of a line, it always returns \n. It also parses UNICODE characters expressed as
 * \uffff. However, if it sees "\\", the second slash cannot begin a unicode sequence. It
 * keeps track of the current position in the input stream.
 *
 * An position consists of: ((linenr &lt;&lt; OFFSETBITS) | offset) this means that both
 * the line number and the exact offset into the file are encoded in each position
 * value.<p>
 */
public class Environment {

    /*-------------------------------------------------------- */
    /* Environment Inner Classes */
    /**
     * A sorted list of error messages
     */
    final class ErrorMessage {

        int where;
        String message;
        ErrorMessage next;

        /**
         * Constructor
         */
        ErrorMessage(int where, String message) {
            this.where = where;
            this.message = message;
        }
    }

    /*-------------------------------------------------------- */
    /* Environment Fields */
    static boolean traceFlag = false;
    boolean debugInfoFlag = false;

    private String inputFileName;
    private String simpleInputFileName;
    public PrintWriter out;
    private boolean nowarn;
    private byte[] data;
    private int bytepos;
    private int linepos;
    public int pos;
    /*-------------------------------------------------------- */

    public Environment(DataInputStream dis, String inputFileName, PrintWriter out, boolean nowarn) throws IOException {
        this.out = out;
        this.inputFileName = inputFileName;
        this.nowarn = nowarn;
        // Read the file
        data = new byte[dis.available()];
        dis.read(data);
        dis.close();
        bytepos = 0;
        linepos = 1;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public String getSimpleInputFileName() {
        if( simpleInputFileName == null ) {
            simpleInputFileName = Paths.get(inputFileName).getFileName().toString();
        }
        return simpleInputFileName;
    }

    int lookForward() {
        try {
            return data[bytepos];
        } catch (ArrayIndexOutOfBoundsException e) {
            return EOF;
        }
    }

    int convertUnicode() {
        int c;
        try {
            while ((c = data[bytepos]) == 'u') {
                bytepos++;
            }
            int d = 0;
            for (int i = 0; i < 4; i++) {
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
                        return d;
                }
                ++bytepos;
                c = data[bytepos];
            }
            return d;
        } catch (ArrayIndexOutOfBoundsException e) {
            error(pos, "invalid.escape.char");
            return EOF;
        }
    }

    public int read() {
        int c;
        pos = (linepos << OFFSETBITS) | bytepos;
        try {
            c = data[bytepos];
        } catch (ArrayIndexOutOfBoundsException e) {
            return EOF;
        }
        bytepos++;

        // parse special characters
        switch (c) {
            /*            case '\\':
             if (lookForward() != 'u') {
             return '\\';
             }
             // we have a unicode sequence
             return convertUnicode();*/
            case '\n':
                linepos++;
                return '\n';

            case '\r':
                if (lookForward() == '\n') {
                    bytepos++;
                }
                linepos++;
                return '\n';

            default:
                return c;
        }
    }

    int lineNumber(int lcpos) {
        return lcpos >>> OFFSETBITS;
    }

    int lineNumber() {
        return lineNumber(pos);
    }

    int lineOffset(int lcpos) {
        return lcpos & ((1 << OFFSETBITS) - 1);
    }

    int lineOffset() {
        return lineOffset(pos);
    }

    /*==============================================================  Environment */
    /**
     * The number of errors and warnings
     */
    public int nerrors;
    public int nwarnings;
    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    /**
     * Error String
     */
    String errorString(String err, Object arg1, Object arg2, Object arg3) {
        String str = null;

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
        // KTL
        // Need to do message format to substitute args
        String msg = buf.toString();
        MessageFormat form = new MessageFormat(msg);
        Object args[] = {arg1, arg2, arg3};
        msg = form.format(args);

        return msg;

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
            for (; (m.next != null) && (m.next.where <= where); m = m.next);
            msg.next = m.next;
            m.next = msg;
        }
    }

    /**
     * Flush outstanding errors
     */
    public void flushErrors() {
        if (errors == null) {
            traceln("flushErrors: errors == null");
            return;
        }

        // Report the errors
        for (ErrorMessage msg = errors; msg != null; msg = msg.next) {
            int off = lineOffset(msg.where);

            int i, j;
            for (i = off; (i > 0) && (data[i - 1] != '\n') && (data[i - 1] != '\r'); i--);
            for (j = off; (j < data.length) && (data[j] != '\n') && (data[j] != '\r'); j++);

            outputln( String.format( "%s (%d:%d) %s", getSimpleInputFileName(), lineNumber(msg.where), off - i, msg.message));
            outputln(new String(data, i, j - i));

            char strdata[] = new char[(off - i) + 1];
            for (j = i; j < off; j++) {
                strdata[j - i] = (data[j] == '\t') ? '\t' : ' ';
            }
            strdata[off - i] = '^';
            outputln(new String(strdata));
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
        output((msg == null ? "" : msg) + "\n");
    }

    /**
     * Issue an error. source - the input source, usually a file name string offset - the
     * offset in the source of the error err - the error number (as defined in this
     * interface) arg1 - an optional argument to the error (null if not applicable) arg2 -
     * a second optional argument to the error (null if not applicable) arg3 - a third
     * optional argument to the error (null if not applicable)
     */
    /**
     * Issue an error
     */
    public void error(int where, String err, Object arg1, Object arg2, Object arg3) {
        String msg;
        if (err.startsWith("warn.")) {
            if (nowarn) {
                return;
            }
            nwarnings++;
            msg = "Warning: ";
        } else {
            err = "err." + err;
            nerrors++;
            msg = "Error: ";
        }
        msg = msg + errorString(err, arg1, arg2, arg3);
        traceln(msg);
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

    public final void error(String err, Object arg1, Object arg2, Object arg3) {
        error(pos, err, arg1, arg2, arg3);
    }

    public final void error(String err, Object arg1, Object arg2) {
        error(pos, err, arg1, arg2, null);
    }

    public final void error(String err, Object arg1) {
        error(pos, err, arg1, null, null);
    }

    public final void error(String err) {
        error(pos, err, null, null, null);
    }

    public final String errorStr(String err, Object arg1, Object arg2, Object arg3) {
        return errorString(err, arg1, arg2, arg3);
    }

    public final String errorStr(String err, Object arg1, Object arg2) {
        return errorStr(err, arg1, arg2, null);
    }

    public final String errorStr(String err, Object arg1) {
        return errorStr(err, arg1, null, null);
    }

    public final String errorStr(String err) {
        return errorStr(err, null, null, null);
    }

    /*==============================================================  trace */
    public boolean isTraceEnabled() {
        return traceFlag;
    }

    public boolean isDebugEnabled() {
        return debugInfoFlag;
    }

    void trace(String message) {
        if (traceFlag) {
            output(message);
        }
    }

    void traceln(String message) {
        if (traceFlag) {
            outputln(message);
        }
    }

} // end Environment
