/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.parser;

import com.sun.tools.javac.code.Lint;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.Preview;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.parser.Tokens.*;
import static com.sun.tools.javac.util.LayoutCharacters.*;

/** The lexical analyzer maps an input stream consisting of
 *  ASCII characters and Unicode escapes into a token sequence.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavaTokenizer {

    private static final boolean scannerDebug = false;

    /** The source language setting.
     */
    private Source source;

    /** The preview language setting. */
    private Preview preview;

    /** The log to be used for error reporting.
     */
    private final Log log;

    /** The token factory. */
    private final Tokens tokens;

    /** The token kind, set by nextToken().
     */
    protected TokenKind tk;

    /** The token's radix, set by nextToken().
     */
    protected int radix;

    /** The token's name, set by nextToken().
     */
    protected Name name;

    /** The position where a lexical error occurred;
     */
    protected int errPos = Position.NOPOS;

    /** The Unicode reader (low-level stream reader).
     */
    protected UnicodeReader reader;

    /** If is a text block
     */
    protected boolean isTextBlock;

    /** If contains escape sequences
     */
    protected boolean hasEscapeSequences;

    protected ScannerFactory fac;

    // The set of lint options currently in effect. It is initialized
    // from the context, and then is set/reset as needed by Attr as it
    // visits all the various parts of the trees during attribution.
    protected Lint lint;

    private static final boolean hexFloatsWork = hexFloatsWork();
    private static boolean hexFloatsWork() {
        try {
            Float.valueOf("0x1.0p1");
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Create a scanner from the input array.  This method might
     * modify the array.  To avoid copying the input array, ensure
     * that {@code inputLength < input.length} or
     * {@code input[input.length -1]} is a white space character.
     *
     * @param fac the factory which created this Scanner
     * @param buf the input, might be modified
     * Must be positive and less than or equal to input.length.
     */
    protected JavaTokenizer(ScannerFactory fac, CharBuffer buf) {
        this(fac, new UnicodeReader(fac, buf));
    }

    protected JavaTokenizer(ScannerFactory fac, char[] buf, int inputLength) {
        this(fac, new UnicodeReader(fac, buf, inputLength));
    }

    protected JavaTokenizer(ScannerFactory fac, UnicodeReader reader) {
        this.fac = fac;
        this.log = fac.log;
        this.tokens = fac.tokens;
        this.source = fac.source;
        this.preview = fac.preview;
        this.reader = reader;
        this.lint = fac.lint;
    }

    protected void checkSourceLevel(int pos, Feature feature) {
        if (preview.isPreview(feature) && !preview.isEnabled()) {
            //preview feature without --preview flag, error
            lexError(DiagnosticFlag.SOURCE_LEVEL, pos, preview.disabledError(feature));
        } else if (!feature.allowedInSource(source)) {
            //incompatible source level, error
            lexError(DiagnosticFlag.SOURCE_LEVEL, pos, feature.error(source.name));
        } else if (preview.isPreview(feature)) {
            //use of preview feature, warn
            preview.warnPreview(pos, feature);
        }
    }

    /** Report an error at the given position using the provided arguments.
     */
    protected void lexError(int pos, JCDiagnostic.Error key) {
        log.error(pos, key);
        tk = TokenKind.ERROR;
        errPos = pos;
    }

    protected void lexError(DiagnosticFlag flags, int pos, JCDiagnostic.Error key) {
        log.error(flags, pos, key);
        tk = TokenKind.ERROR;
        errPos = pos;
    }

    protected void lexWarning(LintCategory lc, int pos, JCDiagnostic.Warning key) {
        DiagnosticPosition dp = new SimpleDiagnosticPosition(pos) ;
        log.warning(lc, dp, key);
    }

    /** Read next character in character or string literal and copy into sbuf.
     *      pos - start of literal offset
     *      translateEscapesNow - true if String::translateEscapes is not available
     *                            in the java.base libs. Occurs during bootstrapping.
     *      multiline - true if scanning a text block. Allows newlines to be embedded
     *                  in the result.
     */
    private void scanLitChar(int pos, boolean translateEscapesNow, boolean multiline) {
         if (reader.ch == '\\') {
            if (reader.peekChar() == '\\' && !reader.isUnicode()) {
                reader.skipChar();
                if (!translateEscapesNow) {
                    reader.putChar(false);
                }
                reader.putChar(true);
            } else {
                reader.nextChar(translateEscapesNow);
                switch (reader.ch) {
                case '0': case '1': case '2': case '3':
                case '4': case '5': case '6': case '7':
                    char leadch = reader.ch;
                    int oct = reader.digit(pos, 8);
                    reader.nextChar(translateEscapesNow);
                    if ('0' <= reader.ch && reader.ch <= '7') {
                        oct = oct * 8 + reader.digit(pos, 8);
                        reader.nextChar(translateEscapesNow);
                        if (leadch <= '3' && '0' <= reader.ch && reader.ch <= '7') {
                            oct = oct * 8 + reader.digit(pos, 8);
                            reader.nextChar(translateEscapesNow);
                        }
                    }
                    if (translateEscapesNow) {
                        reader.putChar((char)oct);
                    }
                    break;
                case 'b':
                    reader.putChar(translateEscapesNow ? '\b' : 'b', true); break;
                case 't':
                    reader.putChar(translateEscapesNow ? '\t' : 't', true); break;
                case 'n':
                    reader.putChar(translateEscapesNow ? '\n' : 'n', true); break;
                case 'f':
                    reader.putChar(translateEscapesNow ? '\f' : 'f', true); break;
                case 'r':
                    reader.putChar(translateEscapesNow ? '\r' : 'r', true); break;
                case '\'':
                case '\"':
                case '\\':
                    reader.putChar(true); break;
                case 's':
                    checkSourceLevel(reader.bp, Feature.TEXT_BLOCKS);
                    reader.putChar(translateEscapesNow ? ' ' : 's', true); break;
                case '\n':
                case '\r':
                    if (!multiline) {
                        lexError(reader.bp, Errors.IllegalEscChar);
                    } else {
                        checkSourceLevel(reader.bp, Feature.TEXT_BLOCKS);
                        int start = reader.bp;
                        if (reader.ch == '\r' && reader.peekChar() == '\n') {
                           reader.nextChar(translateEscapesNow);
                        }
                        reader.nextChar(translateEscapesNow);
                        processLineTerminator(start, reader.bp);
                    }
                    break;
                default:
                    lexError(reader.bp, Errors.IllegalEscChar);
                }
            }
        } else if (reader.bp != reader.buflen) {
            reader.putChar(true);
        }
    }

    /** Interim access to String methods used to support text blocks.
     *  Required to handle bootstrapping with pre-text block jdks.
     *  Should be replaced with direct calls in the 'next' jdk.
     */
    static class TextBlockSupport {
        /** Reflection method to remove incidental indentation.
         */
        private static final Method stripIndent;

        /** Reflection method to translate escape sequences.
         */
        private static final Method translateEscapes;

        /** true if stripIndent and translateEscapes are available in the bootstrap jdk.
         */
        private static final boolean hasSupport;

        /** Get a string method via refection or null if not available.
         */
        private static Method getStringMethodOrNull(String name) {
            try {
                return String.class.getMethod(name);
            } catch (Exception ex) {
                // Method not available, return null.
            }
            return null;
        }

        static {
            // Get text block string methods.
            stripIndent = getStringMethodOrNull("stripIndent");
            translateEscapes = getStringMethodOrNull("translateEscapes");
            // true if stripIndent and translateEscapes are available in the bootstrap jdk.
            hasSupport = stripIndent != null && translateEscapes != null;
        }

        /** Return true if stripIndent and translateEscapes are available in the bootstrap jdk.
         */
        static boolean hasSupport() {
            return hasSupport;
        }

        /** Return the leading whitespace count (indentation) of the line.
         */
        private static int indent(String line) {
            return line.length() - line.stripLeading().length();
        }

        enum WhitespaceChecks {
            INCONSISTENT,
            TRAILING
        };

        /** Check that the use of white space in content is not problematic.
         */
        static Set<WhitespaceChecks> checkWhitespace(String string) {
            // Start with empty result set.
            Set<WhitespaceChecks> checks = new HashSet<>();
            // No need to check empty strings.
            if (string.isEmpty()) {
                return checks;
            }
            // Maximum common indentation.
            int outdent = 0;
            // No need to check indentation if opting out (last line is empty.)
            char lastChar = string.charAt(string.length() - 1);
            boolean optOut = lastChar == '\n' || lastChar == '\r';
            // Split string based at line terminators.
            String[] lines = string.split("\\R");
            int length = lines.length;
            // Extract last line.
            String lastLine = length == 0 ? "" : lines[length - 1];
             if (!optOut) {
                // Prime with the last line indentation (may be blank.)
                outdent = indent(lastLine);
                for (String line : lines) {
                    // Blanks lines have no influence (last line accounted for.)
                    if (!line.isBlank()) {
                        outdent = Integer.min(outdent, indent(line));
                        if (outdent == 0) {
                            break;
                        }
                    }
                }
            }
            // Last line is representative.
            String start = lastLine.substring(0, outdent);
            for (String line : lines) {
                // Fail if a line does not have the same indentation.
                if (!line.isBlank() && !line.startsWith(start)) {
                    // Mix of different white space
                    checks.add(WhitespaceChecks.INCONSISTENT);
                }
                // Line has content even after indent is removed.
                if (outdent < line.length()) {
                    // Is the last character a white space.
                    lastChar = line.charAt(line.length() - 1);
                    if (Character.isWhitespace(lastChar)) {
                        // Has trailing white space.
                        checks.add(WhitespaceChecks.TRAILING);
                    }
                }
            }
            return checks;
        }

        /** Invoke String::stripIndent through reflection.
         */
        static String stripIndent(String string) {
            try {
                string = (String)stripIndent.invoke(string);
            } catch (InvocationTargetException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            return string;
        }

        /** Invoke String::translateEscapes through reflection.
         */
        static String translateEscapes(String string) {
            try {
                string = (String)translateEscapes.invoke(string);
            } catch (InvocationTargetException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            return string;
        }
    }

    /** Test for EOLN.
     */
    private boolean isEOLN() {
        return reader.ch == LF || reader.ch == CR;
    }

    /** Test for CRLF.
     */
    private boolean isCRLF() {
        return reader.ch == CR && reader.peekChar() == LF;
    }

    /** Count and skip repeated occurrences of the specified character.
     */
    private int countChar(char ch, int max) {
        int count = 0;
        for ( ; count < max && reader.bp < reader.buflen && reader.ch == ch; count++) {
            reader.scanChar();
        }
        return count;
    }

    /** Skip and process a line terminator.
     */
    private void skipLineTerminator() {
        int start = reader.bp;
        if (isCRLF()) {
            reader.scanChar();
        }
        reader.scanChar();
        processLineTerminator(start, reader.bp);
    }

    /** Scan a string literal or text block.
     */
    private void scanString(int pos) {
        // Clear flags.
        isTextBlock = false;
        hasEscapeSequences = false;
        // Track the end of first line for error recovery.
        int firstEOLN = -1;
        // Attempt to scan for up to 3 double quotes.
        int openCount = countChar('\"', 3);
        switch (openCount) {
        case 1: // Starting a string literal.
            break;
        case 2: // Starting an empty string literal.
            tk = Tokens.TokenKind.STRINGLITERAL;
            return;
        case 3: // Starting a text block.
            // Check if preview feature is enabled for text blocks.
            checkSourceLevel(pos, Feature.TEXT_BLOCKS);
            isTextBlock = true;
            // Verify the open delimiter sequence.
            while (reader.bp < reader.buflen) {
                char ch = reader.ch;
                if (ch != ' ' && ch != '\t' && ch != FF) {
                    break;
                }
                reader.scanChar();
            }
            if (isEOLN()) {
                skipLineTerminator();
            } else {
                // Error if the open delimiter sequence is not
                //     """<white space>*<LineTerminator>.
                lexError(reader.bp, Errors.IllegalTextBlockOpen);
                return;
            }
            break;
        }
        // While characters are available.
        while (reader.bp < reader.buflen) {
            // If possible close delimiter sequence.
            if (reader.ch == '\"') {
                // Check to see if enough double quotes are present.
                int closeCount = countChar('\"', openCount);
                if (openCount == closeCount) {
                    // Good result.
                    tk = Tokens.TokenKind.STRINGLITERAL;
                    return;
                }
                // False alarm, add double quotes to string buffer.
                reader.repeat('\"', closeCount);
            } else if (isEOLN()) {
                // Line terminator in string literal is an error.
                // Fall out to unclosed string literal error.
                if (openCount == 1) {
                    break;
                }
                skipLineTerminator();
                // Add line terminator to string buffer.
                reader.putChar('\n', false);
                // Record first line terminator for error recovery.
                if (firstEOLN == -1) {
                    firstEOLN = reader.bp;
                }
            } else if (reader.ch == '\\') {
                // Handle escape sequences.
                hasEscapeSequences = true;
                // Translate escapes immediately if TextBlockSupport is not available
                // during bootstrapping.
                boolean translateEscapesNow = !TextBlockSupport.hasSupport();
                scanLitChar(pos, translateEscapesNow, openCount != 1);
            } else {
                // Add character to string buffer.
                reader.putChar(true);
            }
        }
        // String ended without close delimiter sequence.
        lexError(pos, openCount == 1 ? Errors.UnclosedStrLit : Errors.UnclosedTextBlock);
        if (firstEOLN  != -1) {
            // Reset recovery position to point after open delimiter sequence.
            reader.reset(firstEOLN);
        }
    }

    private void scanDigits(int pos, int digitRadix) {
        char saveCh;
        int savePos;
        do {
            if (reader.ch != '_') {
                reader.putChar(false);
            }
            saveCh = reader.ch;
            savePos = reader.bp;
            reader.scanChar();
        } while (reader.digit(pos, digitRadix) >= 0 || reader.ch == '_');
        if (saveCh == '_')
            lexError(savePos, Errors.IllegalUnderscore);
    }

    /** Read fractional part of hexadecimal floating point number.
     */
    private void scanHexExponentAndSuffix(int pos) {
        if (reader.ch == 'p' || reader.ch == 'P') {
            reader.putChar(true);
            skipIllegalUnderscores();
            if (reader.ch == '+' || reader.ch == '-') {
                reader.putChar(true);
            }
            skipIllegalUnderscores();
            if (reader.digit(pos, 10) >= 0) {
                scanDigits(pos, 10);
                if (!hexFloatsWork)
                    lexError(pos, Errors.UnsupportedCrossFpLit);
            } else
                lexError(pos, Errors.MalformedFpLit);
        } else {
            lexError(pos, Errors.MalformedFpLit);
        }
        if (reader.ch == 'f' || reader.ch == 'F') {
            reader.putChar(true);
            tk = TokenKind.FLOATLITERAL;
            radix = 16;
        } else {
            if (reader.ch == 'd' || reader.ch == 'D') {
                reader.putChar(true);
            }
            tk = TokenKind.DOUBLELITERAL;
            radix = 16;
        }
    }

    /** Read fractional part of floating point number.
     */
    private void scanFraction(int pos) {
        skipIllegalUnderscores();
        if (reader.digit(pos, 10) >= 0) {
            scanDigits(pos, 10);
        }
        int sp1 = reader.sp;
        if (reader.ch == 'e' || reader.ch == 'E') {
            reader.putChar(true);
            skipIllegalUnderscores();
            if (reader.ch == '+' || reader.ch == '-') {
                reader.putChar(true);
            }
            skipIllegalUnderscores();
            if (reader.digit(pos, 10) >= 0) {
                scanDigits(pos, 10);
                return;
            }
            lexError(pos, Errors.MalformedFpLit);
            reader.sp = sp1;
        }
    }

    /** Read fractional part and 'd' or 'f' suffix of floating point number.
     */
    private void scanFractionAndSuffix(int pos) {
        radix = 10;
        scanFraction(pos);
        if (reader.ch == 'f' || reader.ch == 'F') {
            reader.putChar(true);
            tk = TokenKind.FLOATLITERAL;
        } else {
            if (reader.ch == 'd' || reader.ch == 'D') {
                reader.putChar(true);
            }
            tk = TokenKind.DOUBLELITERAL;
        }
    }

    /** Read fractional part and 'd' or 'f' suffix of floating point number.
     */
    private void scanHexFractionAndSuffix(int pos, boolean seendigit) {
        radix = 16;
        Assert.check(reader.ch == '.');
        reader.putChar(true);
        skipIllegalUnderscores();
        if (reader.digit(pos, 16) >= 0) {
            seendigit = true;
            scanDigits(pos, 16);
        }
        if (!seendigit)
            lexError(pos, Errors.InvalidHexNumber);
        else
            scanHexExponentAndSuffix(pos);
    }

    private void skipIllegalUnderscores() {
        if (reader.ch == '_') {
            lexError(reader.bp, Errors.IllegalUnderscore);
            while (reader.ch == '_')
                reader.scanChar();
        }
    }

    /** Read a number.
     *  @param radix  The radix of the number; one of 2, 8, 10, 16.
     */
    private void scanNumber(int pos, int radix) {
        // for octal, allow base-10 digit in case it's a float literal
        this.radix = radix;
        int digitRadix = (radix == 8 ? 10 : radix);
        int firstDigit = reader.digit(pos, Math.max(10, digitRadix));
        boolean seendigit = firstDigit >= 0;
        boolean seenValidDigit = firstDigit >= 0 && firstDigit < digitRadix;
        if (seendigit) {
            scanDigits(pos, digitRadix);
        }
        if (radix == 16 && reader.ch == '.') {
            scanHexFractionAndSuffix(pos, seendigit);
        } else if (seendigit && radix == 16 && (reader.ch == 'p' || reader.ch == 'P')) {
            scanHexExponentAndSuffix(pos);
        } else if (digitRadix == 10 && reader.ch == '.') {
            reader.putChar(true);
            scanFractionAndSuffix(pos);
        } else if (digitRadix == 10 &&
                   (reader.ch == 'e' || reader.ch == 'E' ||
                    reader.ch == 'f' || reader.ch == 'F' ||
                    reader.ch == 'd' || reader.ch == 'D')) {
            scanFractionAndSuffix(pos);
        } else {
            if (!seenValidDigit) {
                switch (radix) {
                case 2:
                    lexError(pos, Errors.InvalidBinaryNumber);
                    break;
                case 16:
                    lexError(pos, Errors.InvalidHexNumber);
                    break;
                }
            }
            if (reader.ch == 'l' || reader.ch == 'L') {
                reader.scanChar();
                tk = TokenKind.LONGLITERAL;
            } else {
                tk = TokenKind.INTLITERAL;
            }
        }
    }

    /** Read an identifier.
     */
    private void scanIdent() {
        boolean isJavaIdentifierPart;
        char high;
        reader.putChar(true);
        do {
            switch (reader.ch) {
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z':
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case '$': case '_':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                break;
            case '\u0000': case '\u0001': case '\u0002': case '\u0003':
            case '\u0004': case '\u0005': case '\u0006': case '\u0007':
            case '\u0008': case '\u000E': case '\u000F': case '\u0010':
            case '\u0011': case '\u0012': case '\u0013': case '\u0014':
            case '\u0015': case '\u0016': case '\u0017':
            case '\u0018': case '\u0019': case '\u001B':
            case '\u007F':
                reader.scanChar();
                continue;
            case '\u001A': // EOI is also a legal identifier part
                if (reader.bp >= reader.buflen) {
                    name = reader.name();
                    tk = tokens.lookupKind(name);
                    return;
                }
                reader.scanChar();
                continue;
            default:
                if (reader.ch < '\u0080') {
                    // all ASCII range chars already handled, above
                    isJavaIdentifierPart = false;
                } else {
                    if (Character.isIdentifierIgnorable(reader.ch)) {
                        reader.scanChar();
                        continue;
                    } else {
                        int codePoint = reader.peekSurrogates();
                        if (codePoint >= 0) {
                            if (isJavaIdentifierPart = Character.isJavaIdentifierPart(codePoint)) {
                                reader.putChar(true);
                            }
                        } else {
                            isJavaIdentifierPart = Character.isJavaIdentifierPart(reader.ch);
                        }
                    }
                }
                if (!isJavaIdentifierPart) {
                    name = reader.name();
                    tk = tokens.lookupKind(name);
                    return;
                }
            }
            reader.putChar(true);
        } while (true);
    }

    /** Return true if reader.ch can be part of an operator.
     */
    private boolean isSpecial(char ch) {
        switch (ch) {
        case '!': case '%': case '&': case '*': case '?':
        case '+': case '-': case ':': case '<': case '=':
        case '>': case '^': case '|': case '~':
        case '@':
            return true;
        default:
            return false;
        }
    }

    /** Read longest possible sequence of special characters and convert
     *  to token.
     */
    private void scanOperator() {
        while (true) {
            reader.putChar(false);
            Name newname = reader.name();
            TokenKind tk1 = tokens.lookupKind(newname);
            if (tk1 == TokenKind.IDENTIFIER) {
                reader.sp--;
                break;
            }
            tk = tk1;
            reader.scanChar();
            if (!isSpecial(reader.ch)) break;
        }
    }

    /** Read token.
     */
    public Token readToken() {

        reader.sp = 0;
        name = null;
        radix = 0;

        int pos = 0;
        int endPos = 0;
        List<Comment> comments = null;

        try {
            loop: while (true) {
                pos = reader.bp;
                switch (reader.ch) {
                case ' ': // (Spec 3.6)
                case '\t': // (Spec 3.6)
                case FF: // (Spec 3.6)
                    do {
                        reader.scanChar();
                    } while (reader.ch == ' ' || reader.ch == '\t' || reader.ch == FF);
                    processWhiteSpace(pos, reader.bp);
                    break;
                case LF: // (Spec 3.4)
                    reader.scanChar();
                    processLineTerminator(pos, reader.bp);
                    break;
                case CR: // (Spec 3.4)
                    reader.scanChar();
                    if (reader.ch == LF) {
                        reader.scanChar();
                    }
                    processLineTerminator(pos, reader.bp);
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z':
                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z':
                case '$': case '_':
                    scanIdent();
                    break loop;
                case '0':
                    reader.scanChar();
                    if (reader.ch == 'x' || reader.ch == 'X') {
                        reader.scanChar();
                        skipIllegalUnderscores();
                        scanNumber(pos, 16);
                    } else if (reader.ch == 'b' || reader.ch == 'B') {
                        reader.scanChar();
                        skipIllegalUnderscores();
                        scanNumber(pos, 2);
                    } else {
                        reader.putChar('0');
                        if (reader.ch == '_') {
                            int savePos = reader.bp;
                            do {
                                reader.scanChar();
                            } while (reader.ch == '_');
                            if (reader.digit(pos, 10) < 0) {
                                lexError(savePos, Errors.IllegalUnderscore);
                            }
                        }
                        scanNumber(pos, 8);
                    }
                    break loop;
                case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    scanNumber(pos, 10);
                    break loop;
                case '.':
                    reader.scanChar();
                    if (reader.digit(pos, 10) >= 0) {
                        reader.putChar('.');
                        scanFractionAndSuffix(pos);
                    } else if (reader.ch == '.') {
                        int savePos = reader.bp;
                        reader.putChar('.'); reader.putChar('.', true);
                        if (reader.ch == '.') {
                            reader.scanChar();
                            reader.putChar('.');
                            tk = TokenKind.ELLIPSIS;
                        } else {
                            lexError(savePos, Errors.IllegalDot);
                        }
                    } else {
                        tk = TokenKind.DOT;
                    }
                    break loop;
                case ',':
                    reader.scanChar(); tk = TokenKind.COMMA; break loop;
                case ';':
                    reader.scanChar(); tk = TokenKind.SEMI; break loop;
                case '(':
                    reader.scanChar(); tk = TokenKind.LPAREN; break loop;
                case ')':
                    reader.scanChar(); tk = TokenKind.RPAREN; break loop;
                case '[':
                    reader.scanChar(); tk = TokenKind.LBRACKET; break loop;
                case ']':
                    reader.scanChar(); tk = TokenKind.RBRACKET; break loop;
                case '{':
                    reader.scanChar(); tk = TokenKind.LBRACE; break loop;
                case '}':
                    reader.scanChar(); tk = TokenKind.RBRACE; break loop;
                case '/':
                    reader.scanChar();
                    if (reader.ch == '/') {
                        do {
                            reader.scanCommentChar();
                        } while (reader.ch != CR && reader.ch != LF && reader.bp < reader.buflen);
                        if (reader.bp < reader.buflen) {
                            comments = addComment(comments, processComment(pos, reader.bp, CommentStyle.LINE));
                        }
                        break;
                    } else if (reader.ch == '*') {
                        boolean isEmpty = false;
                        reader.scanChar();
                        CommentStyle style;
                        if (reader.ch == '*') {
                            style = CommentStyle.JAVADOC;
                            reader.scanCommentChar();
                            if (reader.ch == '/') {
                                isEmpty = true;
                            }
                        } else {
                            style = CommentStyle.BLOCK;
                        }
                        while (!isEmpty && reader.bp < reader.buflen) {
                            if (reader.ch == '*') {
                                reader.scanChar();
                                if (reader.ch == '/') break;
                            } else {
                                reader.scanCommentChar();
                            }
                        }
                        if (reader.ch == '/') {
                            reader.scanChar();
                            comments = addComment(comments, processComment(pos, reader.bp, style));
                            break;
                        } else {
                            lexError(pos, Errors.UnclosedComment);
                            break loop;
                        }
                    } else if (reader.ch == '=') {
                        tk = TokenKind.SLASHEQ;
                        reader.scanChar();
                    } else {
                        tk = TokenKind.SLASH;
                    }
                    break loop;
                case '\'':
                    reader.scanChar();
                    if (reader.ch == '\'') {
                        lexError(pos, Errors.EmptyCharLit);
                        reader.scanChar();
                    } else {
                        if (isEOLN())
                            lexError(pos, Errors.IllegalLineEndInCharLit);
                        scanLitChar(pos, true, false);
                        if (reader.ch == '\'') {
                            reader.scanChar();
                            tk = TokenKind.CHARLITERAL;
                        } else {
                            lexError(pos, Errors.UnclosedCharLit);
                        }
                    }
                    break loop;
                case '\"':
                    scanString(pos);
                    break loop;
                default:
                    if (isSpecial(reader.ch)) {
                        scanOperator();
                    } else {
                        boolean isJavaIdentifierStart;
                        int codePoint = -1;
                        if (reader.ch < '\u0080') {
                            // all ASCII range chars already handled, above
                            isJavaIdentifierStart = false;
                        } else {
                            codePoint = reader.peekSurrogates();
                            if (codePoint >= 0) {
                                if (isJavaIdentifierStart = Character.isJavaIdentifierStart(codePoint)) {
                                    reader.putChar(true);
                                }
                            } else {
                                isJavaIdentifierStart = Character.isJavaIdentifierStart(reader.ch);
                            }
                        }
                        if (isJavaIdentifierStart) {
                            scanIdent();
                        } else if (reader.digit(pos, 10) >= 0) {
                            scanNumber(pos, 10);
                        } else if (reader.bp == reader.buflen || reader.ch == EOI && reader.bp + 1 == reader.buflen) { // JLS 3.5
                            tk = TokenKind.EOF;
                            pos = reader.realLength;
                        } else {
                            String arg;

                            if (codePoint >= 0) {
                                char high = reader.ch;
                                reader.scanChar();
                                arg = String.format("\\u%04x\\u%04x", (int) high, (int)reader.ch);
                            } else {
                                arg = (32 < reader.ch && reader.ch < 127) ?
                                                String.format("%s", reader.ch) :
                                                String.format("\\u%04x", (int)reader.ch);
                            }
                            lexError(pos, Errors.IllegalChar(arg));
                            reader.scanChar();
                        }
                    }
                    break loop;
                }
            }
            endPos = reader.bp;
            switch (tk.tag) {
                case DEFAULT: return new Token(tk, pos, endPos, comments);
                case NAMED: return new NamedToken(tk, pos, endPos, name, comments);
                case STRING: {
                    // Get characters from string buffer.
                    String string = reader.chars();
                    // If a text block.
                    if (isTextBlock && TextBlockSupport.hasSupport()) {
                        // Verify that the incidental indentation is consistent.
                        if (lint.isEnabled(LintCategory.TEXT_BLOCKS)) {
                            Set<TextBlockSupport.WhitespaceChecks> checks =
                                    TextBlockSupport.checkWhitespace(string);
                            if (checks.contains(TextBlockSupport.WhitespaceChecks.INCONSISTENT)) {
                                lexWarning(LintCategory.TEXT_BLOCKS, pos,
                                        Warnings.InconsistentWhiteSpaceIndentation);
                            }
                            if (checks.contains(TextBlockSupport.WhitespaceChecks.TRAILING)) {
                                lexWarning(LintCategory.TEXT_BLOCKS, pos,
                                        Warnings.TrailingWhiteSpaceWillBeRemoved);
                            }
                        }
                        // Remove incidental indentation.
                        try {
                            string = TextBlockSupport.stripIndent(string);
                        } catch (Exception ex) {
                            // Error already reported, just use unstripped string.
                        }
                    }
                    // Translate escape sequences if present.
                    if (hasEscapeSequences && TextBlockSupport.hasSupport()) {
                        try {
                            string = TextBlockSupport.translateEscapes(string);
                        } catch (Exception ex) {
                            // Error already reported, just use untranslated string.
                        }
                    }
                    // Build string token.
                    return new StringToken(tk, pos, endPos, string, comments);
                }
                case NUMERIC: return new NumericToken(tk, pos, endPos, reader.chars(), radix, comments);
                default: throw new AssertionError();
            }
        }
        finally {
            if (scannerDebug) {
                    System.out.println("nextToken(" + pos
                                       + "," + endPos + ")=|" +
                                       new String(reader.getRawCharacters(pos, endPos))
                                       + "|");
            }
        }
    }
    //where
        List<Comment> addComment(List<Comment> comments, Comment comment) {
            return comments == null ?
                    List.of(comment) :
                    comments.prepend(comment);
        }

    /** Return the position where a lexical error occurred;
     */
    public int errPos() {
        return errPos;
    }

    /** Set the position where a lexical error occurred;
     */
    public void errPos(int pos) {
        errPos = pos;
    }

    /**
     * Called when a complete comment has been scanned. pos and endPos
     * will mark the comment boundary.
     */
    protected Tokens.Comment processComment(int pos, int endPos, CommentStyle style) {
        if (scannerDebug)
            System.out.println("processComment(" + pos
                               + "," + endPos + "," + style + ")=|"
                               + new String(reader.getRawCharacters(pos, endPos))
                               + "|");
        char[] buf = reader.getRawCharacters(pos, endPos);
        return new BasicComment<>(new UnicodeReader(fac, buf, buf.length), style);
    }

    /**
     * Called when a complete whitespace run has been scanned. pos and endPos
     * will mark the whitespace boundary.
     */
    protected void processWhiteSpace(int pos, int endPos) {
        if (scannerDebug)
            System.out.println("processWhitespace(" + pos
                               + "," + endPos + ")=|" +
                               new String(reader.getRawCharacters(pos, endPos))
                               + "|");
    }

    /**
     * Called when a line terminator has been processed.
     */
    protected void processLineTerminator(int pos, int endPos) {
        if (scannerDebug)
            System.out.println("processTerminator(" + pos
                               + "," + endPos + ")=|" +
                               new String(reader.getRawCharacters(pos, endPos))
                               + "|");
    }

    /** Build a map for translating between line numbers and
     * positions in the input.
     *
     * @return a LineMap */
    public Position.LineMap getLineMap() {
        return Position.makeLineMap(reader.getRawCharacters(), reader.buflen, false);
    }


    /**
    * Scan a documentation comment; determine if a deprecated tag is present.
    * Called once the initial /, * have been skipped, positioned at the second *
    * (which is treated as the beginning of the first line).
    * Stops positioned at the closing '/'.
    */
    protected static class BasicComment<U extends UnicodeReader> implements Comment {

        CommentStyle cs;
        U comment_reader;

        protected boolean deprecatedFlag = false;
        protected boolean scanned = false;

        protected BasicComment(U comment_reader, CommentStyle cs) {
            this.comment_reader = comment_reader;
            this.cs = cs;
        }

        public String getText() {
            return null;
        }

        public int getSourcePos(int pos) {
            return -1;
        }

        public CommentStyle getStyle() {
            return cs;
        }

        public boolean isDeprecated() {
            if (!scanned && cs == CommentStyle.JAVADOC) {
                scanDocComment();
            }
            return deprecatedFlag;
        }

        @SuppressWarnings("fallthrough")
        protected void scanDocComment() {
            try {
                boolean deprecatedPrefix = false;

                comment_reader.bp += 3; // '/**'
                comment_reader.ch = comment_reader.buf[comment_reader.bp];

                forEachLine:
                while (comment_reader.bp < comment_reader.buflen) {

                    // Skip optional WhiteSpace at beginning of line
                    while (comment_reader.bp < comment_reader.buflen && (comment_reader.ch == ' ' || comment_reader.ch == '\t' || comment_reader.ch == FF)) {
                        comment_reader.scanCommentChar();
                    }

                    // Skip optional consecutive Stars
                    while (comment_reader.bp < comment_reader.buflen && comment_reader.ch == '*') {
                        comment_reader.scanCommentChar();
                        if (comment_reader.ch == '/') {
                            return;
                        }
                    }

                    // Skip optional WhiteSpace after Stars
                    while (comment_reader.bp < comment_reader.buflen && (comment_reader.ch == ' ' || comment_reader.ch == '\t' || comment_reader.ch == FF)) {
                        comment_reader.scanCommentChar();
                    }

                    deprecatedPrefix = false;
                    // At beginning of line in the JavaDoc sense.
                    if (!deprecatedFlag) {
                        String deprecated = "@deprecated";
                        int i = 0;
                        while (comment_reader.bp < comment_reader.buflen && comment_reader.ch == deprecated.charAt(i)) {
                            comment_reader.scanCommentChar();
                            i++;
                            if (i == deprecated.length()) {
                                deprecatedPrefix = true;
                                break;
                            }
                        }
                    }

                    if (deprecatedPrefix && comment_reader.bp < comment_reader.buflen) {
                        if (Character.isWhitespace(comment_reader.ch)) {
                            deprecatedFlag = true;
                        } else if (comment_reader.ch == '*') {
                            comment_reader.scanCommentChar();
                            if (comment_reader.ch == '/') {
                                deprecatedFlag = true;
                                return;
                            }
                        }
                    }

                    // Skip rest of line
                    while (comment_reader.bp < comment_reader.buflen) {
                        switch (comment_reader.ch) {
                            case '*':
                                comment_reader.scanCommentChar();
                                if (comment_reader.ch == '/') {
                                    return;
                                }
                                break;
                            case CR: // (Spec 3.4)
                                comment_reader.scanCommentChar();
                                if (comment_reader.ch != LF) {
                                    continue forEachLine;
                                }
                            /* fall through to LF case */
                            case LF: // (Spec 3.4)
                                comment_reader.scanCommentChar();
                                continue forEachLine;
                            default:
                                comment_reader.scanCommentChar();
                        }
                    } // rest of line
                } // forEachLine
                return;
            } finally {
                scanned = true;
            }
        }
    }
}
