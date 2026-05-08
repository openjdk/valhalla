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
package org.openjdk.asmtools.jcoder;

import static org.openjdk.asmtools.jcoder.JcodTokens.*;

import java.io.IOException;
import java.util.HashMap;

/**
 * A Scanner for Jcoder tokens. Errors are reported to the environment object.<p>
 *
 * The scanner keeps track of the current token, the value of the current token (if any),
 * and the start position of the current token.<p>
 *
 * The scan() method advances the scanner to the next token in the input.<p>
 *
 * The match() method is used to quickly match opening brackets (ie: '(', '{', or '[')
 * with their closing counter part. This is useful during error recovery.<p>
 *
 * The compiler treats either "\n", "\r" or "\r\n" as the end of a line.<p>
 */
public class Scanner {
    /*-------------------------------------------------------- */
    /* Scanner Fields */

    /**
     * End of input
     */
    public static final int EOF = -1;
    public static final int LBRACE = 123; // "{"
    private boolean debugCP = false;
    private int numCPentrs = 0;

    /**
     * Where errors are reported
     */
    protected SourceFile env;

    /**
     * Input stream
     */
    protected SourceFile in;
    HashMap<String, String> macros;

    /**
     * The current character
     */
    protected int ch, prevCh = -1;
    protected String macro;
    protected int indexMacro;

    /**
     * Current token
     */
    protected Token token;

    /**
     * The position of the current token
     */
    protected int pos;

    /**
     * The position of the previous token
     */
    protected int prevPos;

    /*  Token values. */
    protected long longValue;
    protected int intValue;
    protected int intSize;
    protected String stringValue;
    protected ByteBuffer longStringValue;
    protected int sign; // sign, when reading number

    /*  A doc comment preceding the most recent token */
    protected String docComment;

    /**
     * A growable character buffer.
     */
    private int count;
    private char[] buffer = new char[32];

    /*-------------------------------------------------------- */
    /**
     * Create a scanner to scan an input stream.
     */
    protected Scanner(SourceFile sf, HashMap<String, String> macros)
            throws IOException {
        this.env = sf;
        this.in = sf;
        this.macros = macros;

        ch = sf.read();
        prevPos = sf.pos;

        scan();
    }

    /**
     * for use in jcfront.
     */
    protected Scanner(SourceFile sf)
            throws IOException {
        this.env = sf;
        this.in = sf;
        this.macros = new HashMap<>();

        ch = sf.read();
        prevPos = sf.pos;

        scan();
    }

    /* *********************************************** */
    void setDebugCP(boolean enable) {
        if (enable) {
            numCPentrs = 0;
        }
        debugCP = enable;

    }

    void addConstDebug(ConstType ct) {
        numCPentrs += 1;
        env.traceln("\n Const[" + numCPentrs + "] = " + ct.printval());
    }

    void setMacro(String macro) {
        this.macro = macro;
        indexMacro = 0;
        prevCh = ch;
    }

    void readCh() throws IOException {
        if (macro != null) {
            if (indexMacro < macro.length()) {
                ch = macro.charAt(indexMacro);
            }
            macro = null;
        }
        if (prevCh >= 0) {
            ch = prevCh;
            prevCh = -1;
        } else {
            ch = in.read();
        }
    }

    private void putc(int ch) {
        if (count == buffer.length) {
            char[] newBuffer = new char[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
        buffer[count++] = (char) ch;
    }

    private String bufferString() {
        char[] buf = new char[count];
        System.arraycopy(buffer, 0, buf, 0, count);
        return new String(buf);
    }

    /**
     * Scan a comment. This method should be called once the initial /, * and the next
     * character have been read.
     */
    private void skipComment() throws IOException {
        while (true) {
            switch (ch) {
                case EOF:
                    env.error(pos, "eof.in.comment");
                    return;

                case '*':
                    readCh();
                    if (ch == '/') {
                        readCh();
                        return;
                    }
                    break;

                default:
                    readCh();
                    break;
            }
        }
    }

    /**
     * Scan a doc comment. This method should be called once the initial /, * and * have
     * been read. It gathers the content of the comment (witout leading spaces and '*'s)
     * in the string buffer.
     */
    private String scanDocComment() throws IOException {
        count = 0;

        if (ch == '*') {
            do {
                readCh();
            } while (ch == '*');
            if (ch == '/') {
                readCh();
                return "";
            }
        }
        switch (ch) {
            case '\n':
            case ' ':
                readCh();
                break;
        }

        boolean seenstar = false;
        int c = count;
        while (true) {
            switch (ch) {
                case EOF:
                    env.error(pos, "eof.in.comment");
                    return bufferString();

                case '\n':
                    putc('\n');
                    readCh();
                    seenstar = false;
                    c = count;
                    break;

                case ' ':
                case '\t':
                    putc(ch);
                    readCh();
                    break;

                case '*':
                    if (seenstar) {
                        readCh();
                        if (ch == '/') {
                            readCh();
                            count = c;
                            return bufferString();
                        }
                        putc('*');
                    } else {
                        seenstar = true;
                        count = c;
                        do {
                            readCh();
                        } while (ch == '*');
                        switch (ch) {
                            case ' ':
                                readCh();
                                break;

                            case '/':
                                readCh();
                                count = c;
                                return bufferString();
                        }
                    }
                    break;

                default:
                    if (!seenstar) {
                        seenstar = true;
                    }
                    putc(ch);
                    readCh();
                    c = count;
                    break;
            }
        }
    }

    /**
     * Scan a decimal number
     */
    private void scanDecNumber() throws IOException {
        boolean overflow = false;
        long value = ch - '0';
        count = 0;
        token = Token.INTVAL;
        intSize = 2; // default
        putc(ch);    // save character in buffer
numberLoop:
        for (;;) {
            readCh();
            switch (ch) {
                case '8':
                case '9':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    putc(ch);
                    overflow = overflow || (value * 10) / 10 != value;
                    value = (value * 10) + (ch - '0');
                    overflow = overflow || (value - 1 < -1);
                    break;
                case 'b':
                    readCh();
                    intSize = 1;
                    break numberLoop;
                case 's':
                    readCh();
                    intSize = 2;
                    break numberLoop;
                case 'i':
                    readCh();
                    intSize = 4;
                    break numberLoop;
                case 'l':
                    readCh();
                    intSize = 8;
                    break numberLoop;
                default:
                    break numberLoop;
            }
        }
        longValue = value;
        intValue = (int) value;
        // we have just finished reading the number.  The next thing better
        // not be a letter or digit.
        if (Character.isJavaIdentifierPart((char) ch) || ch == '.') {
            env.error(in.pos, "invalid.number", Character.toString((char)ch));
            do {
                readCh();
            } while (Character.isJavaIdentifierPart((char) ch) || ch == '.');
            return;
        }
        if (overflow) {
            env.error(pos, "overflow");
        }
    } // scanNumber()

    /**
     * Scan a hex number.
     */
    private void scanHexNumber() throws IOException {
        boolean overflow = false;
        long value = 0;
        int cypher;
        count = 0;
        token = Token.INTVAL;
        intSize = 2; // default
        putc(ch);    // save character in buffer
numberLoop:
        for (int k = 0;; k++) {
            readCh();
            switch (ch) {
                case '8':
                case '9':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    cypher = (char) ch - '0';
                    break;
                case 'd':
                case 'D':
                case 'e':
                case 'E':
                case 'f':
                case 'F':
                case 'a':
                case 'A':
                case 'b':
                case 'B':
                case 'c':
                case 'C':
                    cypher = 10 + Character.toLowerCase((char) ch) - 'a';
                    break;

                default:
                    break numberLoop;
            }
            putc(ch);
            overflow = overflow || ((value >>> 60) != 0);
            value = (value << 4) + cypher;
            intSize = (k + 1) / 2;
        }
        longValue = value;
        intValue = (int) value;
        // we have just finished reading the number.  The next thing better
        // not be a letter or digit.
        if (Character.isJavaIdentifierPart((char) ch) || ch == '.') {
            env.error(in.pos, "invalid.number", Character.toString((char)ch));
            do {
                readCh();
            } while (Character.isJavaIdentifierPart((char) ch) || ch == '.');
            intValue = 0;
//        } else if ( overflow || (intValue - 1 < -1) ) {
        } else if (overflow) {
            intValue = 0;   // so we don't get second overflow in Parser
            env.error(pos, "overflow");
        }
    } // scanNumber()

    /**
     * Scan an escape character.
     *
     * @return the character or -1 if it escaped an end-of-line.
     */
    private int scanEscapeChar() throws IOException {
        int p = in.pos;

        readCh();
        switch (ch) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7': {
                int n = ch - '0';
                for (int i = 2; i > 0; i--) {
                    readCh();
                    switch (ch) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                            n = (n << 3) + ch - '0';
                            break;

                        default:
                            if (n > 0xFF) {
                                env.error(p, "invalid.escape.char");
                            }
                            return n;
                    }
                }
                readCh();
                if (n > 0xFF) {
                    env.error(p, "invalid.escape.char");
                }
                return n;
            }
            case 'r':
                readCh();
                return '\r';
            case 'n':
                readCh();
                return '\n';
            case 'f':
                readCh();
                return '\f';
            case 'b':
                readCh();
                return '\b';
            case 't':
                readCh();
                return '\t';
            case '\\':
                readCh();
                return '\\';
            case '\"':
                readCh();
                return '\"';
            case '\'':
                readCh();
                return '\'';
        }

        env.error(p, "invalid.escape.char");
        readCh();
        return -1;
    }

    /**
     * Scan a string. The current character should be the opening " of the string.
     */
    private void scanString() throws IOException {
        token = Token.STRINGVAL;
        count = 0;
        readCh();

loop:
        for (;;) {
            switch (ch) {
                case EOF:
                    env.error(pos, "eof.in.string");
                    break loop;

                case '\n':
                    readCh();
                    env.error(pos, "newline.in.string");
                    break loop;

                case '"':
                    readCh();
                    break loop;

                case '\\': {
                    int c = scanEscapeChar();
                    if (c >= 0) {
                        putc((char)c);
                    }
                    break;
                }

                default:
                    putc(ch);
                    readCh();
                    break;
            }
        }
        stringValue = bufferString();
    }

    /**
     * Scan a character array. The current character should be the opening ' of the array.
     */
    private void scanCharArray() throws IOException {
        token = Token.LONGSTRINGVAL;
        ByteBuffer buf = new ByteBuffer();
        count = 0;
        readCh();

loop:
        for (;;) {
            int c = ch;
            switch (ch) {
                case EOF:
                    env.error(pos, "eof.in.string");
                    break loop;

                case '\n':
                    readCh();
                    env.error(pos, "newline.in.string");
                    break loop;

                case '\'':
                    readCh();
                    break loop;

                case '\\':
                    c = scanEscapeChar();
                    if (c < 0) {
                        break;
                    }
                // no break - continue
                default:
                    // see  description of java.io.DataOutput.writeUTF()
                    if ((c > 0) && (c <= 0x7F)) {
                        buf.write(c);
                    } else if ((c == 0) || ((c >= 0x80) && (c <= 0x7FF))) {
                        buf.write(0xC0 | (0x1F & (c >> 6)));
                        buf.write(0x80 | (0x3f & c));
                    } else {
                        buf.write(0xc0 | (0x0f & (c >> 12)));
                        buf.write(0x80 | (0x3f & (c >> 6)));
                        buf.write(0x80 | (0x3f & c));
                    }
                    readCh();
            }
        }
        longStringValue = buf;
    }

    /**
     * Scan an Identifier. The current character should be the first character of the
     * identifier.
     */
    private void scanIdentifier() throws IOException {
        count = 0;
        boolean compound = false;
        for (;;) {
            putc(ch);
            readCh();
            if ((ch == '/') || (ch == '.') || (ch == '-')) {
                compound = true;
            } else if (!Character.isJavaIdentifierPart((char) ch)) {
                break;
            }
        }
        stringValue = bufferString();
        if (compound) {
            token = Token.IDENT;
        } else {
            token = keyword_token_ident(stringValue);
            if (token == Token.IDENT) {
                intValue = constValue(stringValue);
                if (intValue != -1) {
                    // this is a constant
                    if (debugCP) {
                        ConstType ct = constType(stringValue);
                        if (ct != null) {
                            addConstDebug(ct);
                        }
                    }
                    token = Token.INTVAL;
                    intSize = 1;
                    longValue = intValue;
                }
            }
        }
    } // end scanIdentifier

    // skip till symbol
    protected void skipTill(int sym) throws IOException {
        while (true) {
            if( ch == EOF ) {
                env.error(pos, "eof.in.comment");
                return;
            } else if (ch == sym) {
                return;
            }
            readCh();
        }
    }

    protected int xscan() throws IOException {
        int retPos = pos;
        prevPos = in.pos;
        docComment = null;
        sign = 1;
        for (;;) {
            pos = in.pos;

            switch (ch) {
                case EOF:
                    token = Token.EOF;
                    return retPos;

                case '\n':
                case ' ':
                case '\t':
                case '\f':
                    readCh();
                    break;

                case '/':
                    readCh();
                    switch (ch) {
                        case '/':
                            // Parse a // comment
                            do {
                                readCh();
                            } while ((ch != EOF) && (ch != '\n'));
                            break;

                        case '*':
                            readCh();
                            if (ch == '*') {
                                docComment = scanDocComment();
                            } else {
                                skipComment();
                            }
                            break;

                        default:
                            token = Token.DIV;
                            return retPos;
                    }
                    break;

                case '"':
                    scanString();
                    return retPos;

                case '\'':
                    scanCharArray();
                    return retPos;

                case '-':
                    sign = -sign; // hack: no check that numbers only are allowed after
                case '+':
                    readCh();
                    break;

                case '0':
                    readCh();
                    token = Token.INTVAL;
                    longValue = intValue = 0;
                    switch (ch) {
                        case 'x':
                        case 'X':
                            scanHexNumber();
                            break;
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
                            scanDecNumber();
                            break;
                        case 'b':
                            readCh();
                            intSize = 1;
                            break;
                        case 's':
                            readCh();
                            intSize = 2;
                            break;
                        case 'i':
                            readCh();
                            intSize = 4;
                            break;
                        case 'l':
                            readCh();
                            intSize = 8;
                            break;
                        default:
                            intSize = 2;
                    }
                    return retPos;

                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    scanDecNumber();
                    return retPos;

                case '{':
                    readCh();
                    token = Token.LBRACE;
                    return retPos;

                case '}':
                    readCh();
                    token = Token.RBRACE;
                    return retPos;

                case '(':
                    readCh();
                    token = Token.LPAREN;
                    return retPos;

                case ')':
                    readCh();
                    token = Token.RPAREN;
                    return retPos;

                case '[':
                    readCh();
                    token = Token.LSQBRACKET;
                    return retPos;

                case ']':
                    readCh();
                    token = Token.RSQBRACKET;
                    return retPos;

                case ',':
                    readCh();
                    token = Token.COMMA;
                    return retPos;

                case ';':
                    readCh();
                    token = Token.SEMICOLON;
                    return retPos;

                case ':':
                    readCh();
                    token = Token.COLON;
                    return retPos;

                case '=':
                    readCh();
                    if (ch == '=') {
                        readCh();
                        token = Token.EQ;
                        return retPos;
                    }
                    token = Token.ASSIGN;
                    return retPos;

                case '\u001a':
                    // Our one concession to DOS.
                    readCh();
                    if (ch == EOF) {
                        token = Token.EOF;
                        return retPos;
                    }
                    env.error(pos, "funny.char");
                    readCh();
                    break;

                case '#':
                    readCh();
                    scanDecNumber();
                    return retPos;

                case '&': {
                    readCh();
                    retPos = pos;
                    if (!Character.isJavaIdentifierStart((char) ch)) {
                        env.error(pos, "identifier.expected");
                    }
                    scanIdentifier();
                    String macroId = stringValue;
                    String macro = (String) macros.get(macroId);
                    if (macro == null) {
                        env.error(pos, "macro.undecl", macroId);
                        throw new SyntaxError();
                    }
                    setMacro(macro);
                    readCh();
                }
                break;

                default:
                    if (Character.isJavaIdentifierStart((char) ch)) {
                        scanIdentifier();
                        return retPos;
                    }
                    env.error(pos, "funny.char");
                    readCh();
                    break;
            }
        }
    }

    /**
     * Scan to a matching '}', ']' or ')'. The current token must be a '{', '[' or '(';
     */
    protected void match(Token open, Token close) throws IOException {
        int depth = 1;

        while (true) {
            scan();
            if (token == open) {
                depth++;
            } else if (token == close) {
                if (--depth == 0) {
                    return;
                }
            } else if (token == Token.EOF) {
                env.error(pos, "unbalanced.paren");
                return;
            }
        }
    }

    /**
     * Scan the next token.
     *
     * @return the position of the previous token.
     */
    protected int scan() throws IOException {
        int retPos = xscan();
//env.traceln("scanned:"+token+" ("+keywordName(token)+")");
        return retPos;
    }

    /**
     * Scan the next token.
     *
     * @return the position of the previous token.
     */
    protected int scanMacro() throws IOException {
        int retPos = xscan();
//env.traceln("scanned:"+token+" ("+keywordName(token)+")");
        return retPos;
    }
}
