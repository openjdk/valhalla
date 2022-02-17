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

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.JasmTokens.*;
import static org.openjdk.asmtools.jasm.Constants.EOF;
import static org.openjdk.asmtools.jasm.Constants.OFFSETBITS;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * A Scanner for Jasm tokens. Errors are reported to the environment object.<p>
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
public class Scanner extends ParseBase {

    /**
     * SyntaxError is the generic error thrown for parsing problems.
     */
    protected static class SyntaxError extends Error {
        boolean fatalError = false;
        SyntaxError Fatal() { fatalError = true; return this; }
        boolean isFatal() {return fatalError;}
    }

    /**
     * Input stream
     */
    protected Environment in;

    /**
     * The current character
     */
    protected int ch;

    /**
     * Current token
     */
//    protected int token;
    protected Token token;

    /**
     * The position of the current token
     */
    protected int pos;

    /*
     * Token values.
     */
    protected char charValue;
    protected int intValue;
    protected long longValue;
    protected float floatValue;
    protected double doubleValue;
    protected String stringValue;
    protected String idValue;
    protected int radix;        // Radix, when reading int or long

    /*   doc comment preceding the most recent token  */
    protected String docComment;

    /* A growable character buffer. */
    private int count;
    private char buffer[] = new char[32];
    //
    private Predicate<Integer> escapingAllowed;
    /**
     * The position of the previous token
     */
    protected int prevPos;
    protected int sign;              // sign, when reading number
    protected boolean inBits;        // inBits prefix, when reading number

    /**
     * main constructor.
     *
     * Create a scanner to scan an input stream.
     */
    protected Scanner(Environment env) throws IOException {
        super.init(this, null, env);
        escapingAllowed = noFunc;
        this.in = env;
        ch = env.read();
        xscan();
    }

    protected void scanModuleStatement() throws IOException {
        try {
            escapingAllowed = yesAndProcessFunc;
            scan();
        } finally {
            escapingAllowed = noFunc;
        }
    }

    /**
     * scan
     *
     * Scan the next token.
     *
     * @throws IOException
     */
    protected void scan() throws IOException {
        int signloc = 1, cnt = 0;
        prevPos = pos;
prefix:
        for (;;) {
            xscan();
            switch (token) {
                case SIGN:
                    signloc = signloc * intValue;
                    break;
                default:
                    break prefix;
            }
            cnt++;
        }
        switch (token) {
            case INTVAL:
            case LONGVAL:
            case FLOATVAL:
            case DOUBLEVAL:
            case INF:
            case NAN:
                sign = signloc;
                break;
            default:
        }
    }

    /**
     * Check the token may be identifier
     */
    protected final boolean checkTokenIdent() {
        return token.possibleJasmIdentifier();
    }

    static String readableConstant(int t) {
        return "<" + Tables.tag(t) + "> [" + t + "]";
    }

    /**
     * Expects a token, scans the next token or throws an exception.
     */
    protected final void expect(Token t) throws SyntaxError, IOException {
        check(t);
        scan();
    }

    /**
     * Checks a token, throws an exception if not the same
     */
    protected final void check(Token t) throws SyntaxError, IOException {
        if (token != t) {
            if ((t != Token.IDENT) || !checkTokenIdent()) {
                env.traceln("expect: " + t + " instead of " + token);
                switch (t) {
                    case IDENT:
                        env.error(pos, "identifier.expected");
                        break;
                    default:
                        env.error(pos, "token.expected", "<" + t.printValue() + ">");
                        break;
                }

                if (debugFlag) {
                    debugStr("<<<<<PROBLEM>>>>>>>: ");
                    throw new Error("<<<<<PROBLEM>>>>>>>");
                } else {
                    throw new SyntaxError();
                }
            }
        }
    }

    private void putCh(int ch) {
        if (count == buffer.length) {
            char newBuffer[] = new char[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
        buffer[count++] = (char) ch;
    }

    private String bufferString() {
        char buf[] = new char[count];
        System.arraycopy(buffer, 0, buf, 0, count);
        return new String(buf);
    }

    /**
     * Returns true if the character is a unicode digit.
     *
     * @param ch the character to be checked
     */
    public static boolean isUCDigit(int ch) {
        if ((ch >= '0') && (ch <= '9')) {
            return true;
        }
        switch (ch >> 8) {
            case 0x06:
                return ((ch >= 0x0660) && (ch <= 0x0669)) ||        // Arabic-Indic
                        ((ch >= 0x06f0) && (ch <= 0x06f9));         // Eastern Arabic-Indic
            case 0x07:
            case 0x08:
            default:
                return false;
            case 0x09:
                return ((ch >= 0x0966) && (ch <= 0x096f)) ||        // Devanagari
                        ((ch >= 0x09e6) && (ch <= 0x09ef));         // Bengali
            case 0x0a:
                return ((ch >= 0x0a66) && (ch <= 0x0a6f)) ||        // Gurmukhi
                        ((ch >= 0x0ae6) && (ch <= 0x0aef));         // Gujarati
            case 0x0b:
                return ((ch >= 0x0b66) && (ch <= 0x0b6f)) ||        // Oriya
                        ((ch >= 0x0be7) && (ch <= 0x0bef));         // Tamil
            case 0x0c:
                return ((ch >= 0x0c66) && (ch <= 0x0c6f)) ||        // Telugu
                        ((ch >= 0x0ce6) && (ch <= 0x0cef));         // Kannada
            case 0x0d:
                return ((ch >= 0x0d66) && (ch <= 0x0d6f));          // Malayalam
            case 0x0e:
                return ((ch >= 0x0e50) && (ch <= 0x0e59)) ||        // Thai
                        ((ch >= 0x0ed0) && (ch <= 0x0ed9));         // Lao
            case 0x0f:
                return false;
            case 0x10:
                return ((ch >= 0x1040) && (ch <= 0x1049));         // Tibetan
        }
    }

    /**
     * Returns true if the character is a Unicode letter.
     *
     * @param ch the character to be checked
     */
    public static boolean isUCLetter(int ch) {
        // fast check for Latin capitals and small letters
        if (((ch >= 'A') && (ch <= 'Z'))
                || ((ch >= 'a') && (ch <= 'z'))) {
            return true;
        }
        // rest of ISO-LATIN-1
        if (ch < 0x0100) {
            // fast check
            if (ch < 0x00c0) {
                return (ch == '_') || (ch == '$');
            }
            // various latin letters and diacritics,
            // but *not* the multiplication and division symbols
            return ((ch >= 0x00c0) && (ch <= 0x00d6))
                    || ((ch >= 0x00d8) && (ch <= 0x00f6))
                    || ((ch >= 0x00f8) && (ch <= 0x00ff));
        }
        // other non CJK alphabets and symbols, but not digits
        if (ch <= 0x1fff) {
            return !isUCDigit(ch);
        }
        // rest are letters only in five ranges:
        //        Hiragana, Katakana, Bopomofo and Hangul
        //        CJK Squared Words
        //        Korean Hangul Symbols
        //        Han (Chinese, Japanese, Korean)
        //        Han compatibility
        return ((ch >= 0x3040) && (ch <= 0x318f))
                || ((ch >= 0x3300) && (ch <= 0x337f))
                || ((ch >= 0x3400) && (ch <= 0x3d2d))
                || ((ch >= 0x4e00) && (ch <= 0x9fff))
                || ((ch >= 0xf900) && (ch <= 0xfaff));
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
                    if ((ch = in.read()) == '/') {
                        ch = in.read();
                        return;
                    }
                    break;
                default:
                    ch = in.read();
                    break;
            }
        }
    }

    /**
     * Scan a doc comment. This method should be called once the initial /, * and * have
     * been read. It gathers the content of the comment (without leading spaces and '*'s)
     * in the string buffer.
     */
    @SuppressWarnings("empty-statement")
    private String scanDocComment() throws IOException {
        count = 0;

        if (ch == '*') {
            do {
                ch = in.read();
            } while (ch == '*');
            if (ch == '/') {
                ch = in.read();
                return "";
            }
        }
        switch (ch) {
            case '\n':
            case ' ':
                ch = in.read();
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
                    putCh('\n');
                    ch = in.read();
                    seenstar = false;
                    c = count;
                    break;
                case ' ':
                case '\t':
                    putCh(ch);
                    ch = in.read();
                    break;
                case '*':
                    if (seenstar) {
                        if ((ch = in.read()) == '/') {
                            ch = in.read();
                            count = c;
                            return bufferString();
                        }
                        putCh('*');
                    } else {
                        seenstar = true;
                        count = c;
                        while ((ch = in.read()) == '*');
                        switch (ch) {
                            case ' ':
                                ch = in.read();
                                break;
                            case '/':
                                ch = in.read();
                                count = c;
                                return bufferString();
                        }
                    }
                    break;
                default:
                    if (!seenstar) {
                        seenstar = true;
                    }
                    putCh(ch);
                    ch = in.read();
                    c = count;
                    break;
            }
        }
    }

    /**
     * Scan a decimal at this point
     */
    private void scanCPRef() throws IOException {
        switch (ch = in.read()) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9': {
                boolean overflow = false;
                long value = ch - '0';
                count = 0;
                putCh(ch);                // save character in buffer
numberLoop:
                for (;;) {
                    switch (ch = in.read()) {
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
                            putCh(ch);
                            if (overflow) {
                                break;
                            }
                            value = (value * 10) + (ch - '0');
                            overflow = (value > 0xFFFF);
                            break;
                        default:
                            break numberLoop;
                    }
                } // while true
                intValue = (int) value;
                stringValue = bufferString();
                token = Token.CPINDEX;
                if (overflow) {
                    env.error(pos, "overflow");
                }
                break;
            }
            default:
                stringValue = Character.toString((char)ch);
                env.error(in.pos, "invalid.number", stringValue);
                intValue = 0;
                token = Token.CPINDEX;
                ch = in.read();
        }
    } // scanCPRef()

    /**
     * Scan a number. The first digit of the number should be the current character. We
     * may be scanning hex, decimal, or octal at this point
     */
    private void scanNumber() throws IOException {
        boolean seenNonOctal = false;
        boolean overflow = false;
        radix = (ch == '0' ? 8 : 10);
        long value = ch - '0';
        count = 0;
        putCh(ch);                // save character in buffer
numberLoop:
        for (;;) {
            switch (ch = in.read()) {
                case '.':
                    if (radix == 16) {
                        break numberLoop; // an illegal character
                    }
                    scanReal();
                    return;

                case '8':
                case '9':
                    // We can't yet throw an error if reading an octal.  We might
                    // discover we're really reading a real.
                    seenNonOctal = true;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    putCh(ch);
                    if (radix == 10) {
                        overflow = overflow || (value * 10) / 10 != value;
                        value = (value * 10) + (ch - '0');
                        overflow = overflow || (value - 1 < -1);
                    } else if (radix == 8) {
                        overflow = overflow || (value >>> 61) != 0;
                        value = (value << 3) + (ch - '0');
                    } else {
                        overflow = overflow || (value >>> 60) != 0;
                        value = (value << 4) + (ch - '0');
                    }
                    break;
                case 'd':
                case 'D':
                case 'e':
                case 'E':
                case 'f':
                case 'F':
                    if (radix != 16) {
                        scanReal();
                        return;
                    }
                // fall through
                case 'a':
                case 'A':
                case 'b':
                case 'B':
                case 'c':
                case 'C':
                    putCh(ch);
                    if (radix != 16) {
                        break numberLoop; // an illegal character
                    }
                    overflow = overflow || (value >>> 60) != 0;
                    value = (value << 4) + 10
                            + Character.toLowerCase((char) ch) - 'a';
                    break;
                case 'l':
                case 'L':
                    ch = in.read();        // skip over 'l'
                    longValue = value;
                    token = Token.LONGVAL;
                    break numberLoop;
                case 'x':
                case 'X':
                   // if the first character is a '0' and this is the second
                    // letter, then read in a hexadecimal number.  Otherwise, error.
                    if (count == 1 && radix == 8) {
                        radix = 16;
                        break;
                    } else {
                        // we'll get an illegal character error
                        break numberLoop;
                    }
                default:
                    intValue = (int) value;
                    token = Token.INTVAL;
                    break numberLoop;
            }
        } // while true
        // we have just finished reading the number.  The next thing better
        // not be a letter or digit.
        if (isUCDigit(ch) || isUCLetter(ch) || ch == '.') {
            env.error(in.pos, "invalid.number", Character.toString((char)ch));
            do {
                ch = in.read();
            } while (isUCDigit(ch) || isUCLetter(ch) || ch == '.');
            intValue = 0;
            token = Token.INTVAL;
        } else if (radix == 8 && seenNonOctal) {
            intValue = 0;
            token = Token.INTVAL;
            env.error(in.pos, "invalid.octal.number");
        } else if (overflow
                || (token == Token.INTVAL
                && ((radix == 10) ? (intValue - 1 < -1)
                        : ((value & 0xFFFFFFFF00000000L) != 0)))) {
            intValue = 0;        // so we don't get second overflow in Parser
            longValue = 0;
            env.error(pos, "overflow");
        }
    } // scanNumber()

    /**
     * Scan a float. We are either looking at the decimal, or we have already seen it and
     * put it into the buffer. We haven't seen an exponent. Scan a float. Should be called
     * with the current character is either the 'e', 'E' or '.'
     */
    private void scanReal() throws IOException {
        boolean seenExponent = false;
        boolean isSingleFloat = false;
        char lastChar;
        if (ch == '.') {
            putCh(ch);
            ch = in.read();
        }

numberLoop:
        for (;; ch = in.read()) {
            switch (ch) {
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
                    putCh(ch);
                    break;
                case 'e':
                case 'E':
                    if (seenExponent) {
                        break numberLoop; // we'll get a format error
                    }
                    putCh(ch);
                    seenExponent = true;
                    break;
                case '+':
                case '-':
                    lastChar = buffer[count - 1];
                    if (lastChar != 'e' && lastChar != 'E') {
                        break numberLoop; // this isn't an error, though!
                    }
                    putCh(ch);
                    break;
                case 'f':
                case 'F':
                    ch = in.read(); // skip over 'f'
                    isSingleFloat = true;
                    break numberLoop;
                case 'd':
                case 'D':
                    ch = in.read(); // skip over 'd'
                // fall through
                default:
                    break numberLoop;
            } // sswitch
        } // loop

        // we have just finished reading the number.  The next thing better
        // not be a letter or digit.
        if (isUCDigit(ch) || isUCLetter(ch) || ch == '.') {
            env.error(in.pos, "invalid.number", Character.toString((char)ch));
            do {
                ch = in.read();
            } while (isUCDigit(ch) || isUCLetter(ch) || ch == '.');
            doubleValue = 0;
            token = Token.DOUBLEVAL;
        } else {
            token = isSingleFloat ? Token.FLOATVAL : Token.DOUBLEVAL;
            try {
                lastChar = buffer[count - 1];
                if (lastChar == 'e' || lastChar == 'E'
                        || lastChar == '+' || lastChar == '-') {
                    env.error(in.pos - 1, "float.format");
                } else if (isSingleFloat) {
                    floatValue = Float.valueOf(bufferString());
                    if (Float.isInfinite(floatValue)) {
                        env.error(pos, "overflow");
                    }
                } else {
                    doubleValue = Double.valueOf(bufferString());
                    if (Double.isInfinite(doubleValue)) {
                        env.error(pos, "overflow");
                        env.error(pos, "overflow");
                    }
                }
            } catch (NumberFormatException ee) {
                env.error(pos, "float.format");
                doubleValue = 0;
                floatValue = 0;
            }
        }
    } // scanReal

    /**
     * Scan an escape character.
     *
     * @return the character or '\\'
     */
    private int scanEscapeChar() throws IOException {
        int p = in.pos;

        switch (ch = in.read()) {
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
                    switch (ch = in.read()) {
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
                ch = in.read();
                if (n > 0xFF) {
                    env.error(p, "invalid.escape.char");
                }
                return n;
            }
            case 'r':
                ch = in.read();
                return '\r';
            case 'n':
                ch = in.read();
                return '\n';
            case 'f':
                ch = in.read();
                return '\f';
            case 'b':
                ch = in.read();
                return '\b';
            case 't':
                ch = in.read();
                return '\t';
            case '\\':
                ch = in.read();
                return '\\';
            case '\"':
                ch = in.read();
                return '\"';
            case '\'':
                ch = in.read();
                return '\'';
            case 'u':
                int unich = in.convertUnicode();
                ch = in.read();
                return unich;
        }
        return '\\';
    }

    /**
     * Scan a string. The current character should be the opening " of the string.
     */
    private void scanString() throws IOException {
        token = Token.STRINGVAL;
        count = 0;
        ch = in.read();

        // Scan a String
        while (true) {
            switch (ch) {
                case EOF:
                    env.error(pos, "eof.in.string");
                    stringValue = bufferString();
                    return;
                case '\n':
                    ch = in.read();
                    env.error(pos, "newline.in.string");
                    stringValue = bufferString();
                    return;
                case '"':
                    ch = in.read();
                    stringValue = bufferString();
                    return;
                case '\\': {
                    int c = scanEscapeChar();
                    if (c >= 0) {
                        putCh((char) c);
                    }
                    break;
                }
                default:
                    putCh(ch);
                    ch = in.read();
                    break;
            }
        }
    }


    /**
     * Scan an Identifier. The current character should be the first character of the
     * identifier.
     */
    private void scanIdentifier(char[] prefix) throws IOException {
        int firstChar;
        count = 0;
        if(prefix != null) {
            for(;;) {
                for (int i = 0; i < prefix.length; i++)
                    putCh(prefix[i]);
                ch = in.read();
                if (ch == '\\') {
                    ch = in.read();
                    if (ch == 'u') {
                        ch = in.convertUnicode();
                        if (!isUCLetter(ch) && !isUCDigit(ch)) {
                            prefix = new char[]{(char)ch};
                            continue;
                        }
                    } else if (escapingAllowed.test(ch)) {
                        prefix = new char[]{(char)ch};
                        continue;
                    }
                    int p = in.pos;
                    env.error(p, "invalid.escape.char");
                }
                break;
            }
        }
        firstChar = ch;
        boolean firstIteration = true;
scanloop:
        while (true) {
            putCh(ch);
            ch = in.read();

            // Check to see if the annotation marker is at
            // the front of the identifier.
            if (firstIteration && firstChar == '@') {
                // May be a type annotation
                if (ch == 'T') {  // type annotation
                    putCh(ch);
                    ch = in.read();
                }

                // is either a runtime visible or invisible annotation
                if (ch == '+' || ch == '-') {  // regular annotation
                    // possible annotation -
                    // need to eat up the '@+' or '@-'
                    putCh(ch);
                    ch = in.read();
                }
                idValue = bufferString();
                stringValue = idValue;
                token = Token.ANNOTATION;
                return;
            }

            firstIteration = false;
            switch (ch) {
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
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
                case '$':
                case '_':
                case '-':
                case '[':
                case ']':
                case '(':
                case ')':
                case '<':
                case '>':
                    break;
                case '/': {// may be comment right after identifier
                    int c = in.lookForward();
                    if ((c == '*') || (c == '/')) {
                        break scanloop; // yes, comment
                    }
                    break; // no, continue to parse identifier
                }
                case '\\':
                    ch = in.read();
                    if ( ch == 'u') {
                        ch = in.convertUnicode();
                        if (isUCLetter(ch) || isUCDigit(ch)) {
                            break;
                        }
                    } else if( escapingAllowed.test(ch)) {
                        break;
                    }
                    int p = in.pos;
                    env.error(p, "invalid.escape.char");
                default:
//                    if ((!isUCDigit(ch)) && (!isUCLetter(ch))) {
                    break scanloop;
//                    }
            } // end switch
        } // end scanloop
        idValue = bufferString();
        stringValue = idValue;
        token = keyword_token_ident(idValue);
        debugStr(format("##### SCANNER (scanIdent) ######## token = %s value = \"%s\"\n", token, idValue));
    } // end scanIdentifier

//==============================
    @SuppressWarnings("empty-statement")
    protected final void xscan() throws IOException {
        docComment = null;
loop:
        for (;;) {
            pos = in.pos;
            switch (ch) {
                case EOF:
                    token = Token.EOF;
                    break loop;
                case '\n':
                case ' ':
                case '\t':
                case '\f':
                    ch = in.read();
                    break;
                case '/':
                    switch (ch = in.read()) {
                        case '/':
                            // Parse a // comment
                            while (((ch = in.read()) != EOF) && (ch != '\n'));
                            break;
                        case '*':
                            ch = in.read();
                            if (ch == '*') {
                                docComment = scanDocComment();
                            } else {
                                skipComment();
                            }
                            break;
                        default:
                            token = Token.DIV;
                            break loop;
                    }
                    break;
                case '"':
                    scanString();
                    break loop;
                case '-':
                    intValue = -1;
                    token = Token.SIGN;
                    ch = in.read();
                    break loop;
                case '+':
                    intValue = +1;
                    ch = in.read();
                    token = Token.SIGN;
                    break loop;
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
                    scanNumber();
                    break loop;
                case '.':
                    switch (ch = in.read()) {
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
                            count = 0;
                            putCh('.');
                            scanReal();
                            break;
                        default:
                            token = Token.FIELD;
                    }
                    break loop;
                case '{':
                    ch = in.read();
                    token = Token.LBRACE;
                    break loop;
                case '}':
                    ch = in.read();
                    token = Token.RBRACE;
                    break loop;
                case ',':
                    ch = in.read();
                    token = Token.COMMA;
                    break loop;
                case ';':
                    ch = in.read();
                    token = Token.SEMICOLON;
                    break loop;
                case ':':
                    ch = in.read();
                    token = Token.COLON;
                    break loop;
                case '=':
                    if ((ch = in.read()) == '=') {
                        ch = in.read();
                        token = Token.EQ;
                        break loop;
                    }
                    token = Token.ASSIGN;
                    break loop;
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case '$':
                case '_':
                case '@':
                case '[':
                case ']':
                case '(':
                case ')':
                case '<':
                case '>':
                    scanIdentifier(null);
                    break loop;
                case '\u001a':
                    // Our one concession to DOS.
                    if ((ch = in.read()) == EOF) {
                        token = Token.EOF;
                        break loop;
                    }
                    env.error(pos, "funny.char");
                    ch = in.read();
                    break;
                case '#':
                    int c = in.lookForward();
                    if (c == '{') {
                        // '#' char denotes a "paramMethod name" token
                        ch = in.read();
                        token = Token.PARAM_NAME;
                        break loop;
                    }
                    // otherwise, it is a normal cpref
                    scanCPRef();
                    break loop;
                case '\\':
                    ch = in.read();
                    if ( ch == 'u') {
                        ch = in.convertUnicode();
                        if (isUCLetter(ch)) {
                            scanIdentifier(null);
                            break loop;
                        }
                    } else if( escapingAllowed.test(ch)) {
                        scanIdentifier(new char[]{'\\', (char)ch});
                        break loop;
                    }
//                    if ((ch = in.read()) == 'u') {
//                        ch = in.convertUnicode();
//                        if (isUCLetter(ch)) {
//                            scanIdentifier();
//                            break loop;
//                        }
//                    }
                default:
                    env.out.println("funny.char:" + env.lineNumber(pos) + "/" + (pos & ((1 << OFFSETBITS) - 1)));
                    env.error(pos, "funny.char");
                    ch = in.read();
            }
        }
    }

    @Override
    protected void debugScan(String dbstr) {
        if (token == null) {
            env.traceln(dbstr + "<<<NULL TOKEN>>>");
            return;
        }
        env.trace(dbstr + token);
        switch (token) {
            case IDENT:
                env.traceln(" = '" + stringValue + "' {idValue = '" + idValue + "'}");
                break;
            case STRINGVAL:
                env.traceln(" = {stringValue}: \"" + stringValue + "\"");
                break;
            case INTVAL:
                env.traceln(" = {intValue}: " + intValue + "}");
                break;
            case FLOATVAL:
                env.traceln(" = {floatValue}: " + floatValue);
                break;
            case DOUBLEVAL:
                env.traceln(" = {doubleValue}: " + doubleValue);
                break;
            default:
                env.traceln("");
        }
    }

    private Predicate<Integer> noFunc = (ch)-> false;
    private Predicate<Integer> yesAndProcessFunc = (ch) -> {
        boolean res = ((ch == '\\') || (ch == ':') || (ch == '@'));
        if (res)
            putCh('\\');
        return res;
    };
}
