/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jasm.Tables.*;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 *
 * JcodTokens
 *
 * This class contains tokens specific to parsing JCOD syntax.
 *
 * The classes in JcodTokens are following a Singleton Pattern. These classes are Enums,
 * and they are contained in private hash maps (lookup tables and reverse lookup tables).
 * These hash maps all have public accessors, which clients use to look-up enums.
 *
 * Tokens in this table carry no external state, and are typically treated as constants.
 * They do not need to be reset.
 */
public class JcodTokens {

    /*-------------------------------------------------------- */
    /* Marker: describes the type of Keyword */
    static public enum KeywordType {

        TOKEN(0, "TOKEN"),
        KEYWORD(3, "KEYWORD");

        private final Integer value;
        private final String printval;

        KeywordType(Integer val, String print) {
            value = val;
            printval = print;
        }

        public String printval() {
            return printval;
        }
    }

    /*-------------------------------------------------------- */
    /* Marker - describes the type of token */
    /*    this is rather cosmetic, no function currently. */
    static public enum TokenType {
        VALUE               (0, "Value"),
        KEYWORDS            (1, "Keywords"),
        PUNCTUATION         (2, "Punctuation"),
        JDEC                (3, "JDec"),
        STACKMAP            (4, "StackMap"),
        MISC                (5, "Misc");

        private final Integer value;
        private final String printval;

        TokenType(Integer val, String print) {
            value = val;
            printval = print;
        }

        public String printval() {
            return printval;
        }
    }

  /*-------------------------------------------------------- */
  /** Scanner Tokens (Definitive List) */
    static public enum Token {
        EOF                 (-1, "EOF",             "EOF",          TokenType.MISC),
        IDENT               (60, "IDENT",           "IDENT",        TokenType.VALUE),
        LONGSTRINGVAL       (61, "LONGSTRINGVAL",   "LONGSTRING",   TokenType.VALUE),
        INTVAL              (65, "INTVAL",          "INT",          TokenType.VALUE),
        LONGVAL             (66, "LONGVAL",         "LONG",         TokenType.VALUE),
        STRINGVAL           (69, "STRINGVAL",       "STRING",       TokenType.VALUE),

        CLASS               (70, "CLASS",           "class",        TokenType.KEYWORDS, KeywordType.KEYWORD),
        INTERFACE           (71, "INTERFACE",       "interface",    TokenType.KEYWORDS, KeywordType.KEYWORD),
        DIV                 (72, "DIV",             "div",          TokenType.KEYWORDS),
        EQ                  (73, "EQ",              "eq",           TokenType.KEYWORDS),
        ASSIGN              (74, "ASSIGN",          "assign",       TokenType.KEYWORDS),
        MODULE              (75, "MODULE",          "module",       TokenType.KEYWORDS, KeywordType.KEYWORD),

        COLON               (134, "COLON",        ":",    TokenType.PUNCTUATION),
        SEMICOLON           (135, "SEMICOLON",    ";",    TokenType.PUNCTUATION, KeywordType.KEYWORD),
        COMMA               (0,   "COMMA",        ",",    TokenType.PUNCTUATION, KeywordType.KEYWORD),
        LBRACE              (138, "LBRACE",       "{",    TokenType.PUNCTUATION, KeywordType.KEYWORD),
        RBRACE              (139, "RBRACE",       "}",    TokenType.PUNCTUATION, KeywordType.KEYWORD),
        LPAREN              (140, "LPAREN",       "(",    TokenType.PUNCTUATION, KeywordType.KEYWORD),
        RPAREN              (141, "RPAREN",       ")",    TokenType.PUNCTUATION, KeywordType.KEYWORD),
        LSQBRACKET          (142, "LSQBRACKET",   "[",    TokenType.PUNCTUATION, KeywordType.KEYWORD),
        RSQBRACKET          (143, "RSQBRACKET",   "]",    TokenType.PUNCTUATION, KeywordType.KEYWORD),


        BYTEINDEX           (156, "BYTEINDEX",   "b",       TokenType.JDEC, KeywordType.KEYWORD),
        SHORTINDEX          (157, "SHORTINDEX",  "s",       TokenType.JDEC, KeywordType.KEYWORD),
        ATTR                (158, "ATTR",        "Attr",    TokenType.JDEC, KeywordType.KEYWORD),
        BYTES               (159, "BYTES",       "Bytes",   TokenType.JDEC, KeywordType.KEYWORD),
        MACRO               (160, "MACRO",        "Attr",    TokenType.JDEC),
        COMP                (161, "COMP",        "Component", TokenType.JDEC, KeywordType.KEYWORD),
        FILE                (162, "FILE",        "file",    TokenType.JDEC, KeywordType.KEYWORD),

        ZEROINDEX           (163, "ZEROINDEX",   "z",       TokenType.STACKMAP, KeywordType.KEYWORD);

        private Integer value;
        private String printval;
        private String parsekey;
        private TokenType tk_type;
        private KeywordType key_type;

        // By default, if a KeywordType is not specified, it has the value 'TOKEN'
        Token(Integer val, String print, String op) {
            init(val, print, op, TokenType.VALUE, KeywordType.TOKEN);
        }

        Token(Integer val, String print, String op, TokenType tt) {
            init(val, print, op, tt, KeywordType.TOKEN);
        }

        Token(Integer val, String print, String op, TokenType tt, KeywordType kt) {
            init(val, print, op, tt, kt);
        }

        private void init(Integer val, String print, String op, TokenType tt, KeywordType kt) {
            value = val;
            printval = print;
            parsekey = op;
            tk_type = tt;
            key_type = kt;
        }

        public String printval() {
            return printval;
        }

        public String parsekey() {
            return parsekey;
        }

        public int value() {
            return value;
        }

        @Override
        public String toString() {
            return "<" + printval + "> [" + value + "]";
        }

    }

    /**
     * Initialized keyword and token Hash Maps (and Reverse Tables)
     */
    protected static final int MaxTokens = 172;
    private static HashMap<Integer, Token> TagToTokens = new HashMap<>(MaxTokens);
    private static HashMap<String, Token> SymbolToTokens = new HashMap<>(MaxTokens);
    private static HashMap<String, Token> ParsekeyToTokens = new HashMap<>(MaxTokens);

    protected static final int MaxKeywords = 40;
    private static HashMap<Integer, Token> TagToKeywords = new HashMap<>(MaxKeywords);
    private static HashMap<String, Token> SymbolToKeywords = new HashMap<>(MaxKeywords);
    private static HashMap<String, Token> ParsekeyToKeywords = new HashMap<>(MaxKeywords);

    static {

        // register all of the tokens
        for (Token tk : Token.values()) {
            registerToken(tk);
        }

        SymbolToKeywords.put(Token.INTVAL.printval(), Token.INTVAL);
        ParsekeyToKeywords.put(Token.INTVAL.parsekey(), Token.INTVAL);
        SymbolToKeywords.put(Token.STRINGVAL.printval(), Token.STRINGVAL);
        ParsekeyToKeywords.put(Token.STRINGVAL.parsekey(), Token.STRINGVAL);
    }

    private static void registerToken(Token tk) {
        // Tag is a keyword
        if (tk.key_type == KeywordType.KEYWORD) {
            TagToKeywords.put(tk.value, tk);
            SymbolToKeywords.put(tk.printval, tk);
            if (tk.parsekey != null) {
                ParsekeyToKeywords.put(tk.parsekey, tk);
            }
        }

        // Finally, register all tokens
        TagToTokens.put(tk.value, tk);
        SymbolToTokens.put(tk.printval, tk);
        ParsekeyToTokens.put(tk.printval, tk);
    }

    /* Token accessors */
    public static Token token(int tk) {
        return TagToTokens.get(tk);
    }

    public static Token keyword_token(int tk) {
        return TagToKeywords.get(tk);
    }

    /* Reverse lookup accessors */
    public static Token token(String parsekey) {
        return ParsekeyToTokens.get(parsekey);
    }

    public static Token keyword_token(String parsekey) {
        return ParsekeyToKeywords.get(parsekey);
    }

    /* Reverse lookup by ID accessors */
    public static Token token_ID(String ID) {
        return ParsekeyToTokens.get(ID);
    }

    public static Token keyword_token_ID(String ID) {
        return ParsekeyToKeywords.get(ID);
    }

    public static String keywordName(int token) {
        String retval = "";
        if (token > TagToTokens.size()) {
            retval = null;
        } else {
            Token tk = keyword_token(token);
            if (tk != null) {
                retval = tk.parsekey;
            }
        }
        return retval;
    }

    public static Token keyword_token_ident(String idValue) {
        Token kwd = keyword_token(idValue);

        if (kwd == null) {
            kwd = Token.IDENT;
        }
        return kwd;
    }

    public static int keyword_token_int(String idValue) {
        return keyword_token_ident(idValue).value();
    }

    private static HashMap<String, ConstType> NameToConstantType = new HashMap<>(ConstType.maxTag);
    private static HashMap<Integer, ConstType> ConstantTypes = new HashMap<>(ConstType.maxTag);

    static {
        // register all of the tokens
        for (ConstType ct : ConstType.values()) {
            registerConstantType(ct);
        }
    }

    /**
     * ConstType
     *
     * A (typed) tag (constant) representing the type of Constant in the Constant Pool.
     *
     * This is more-or-less a copy of jasm.ConstType. Unfortunately, there's no way to
     * sub-class (or slightly alter) the members of an enum. This enum set is slightly
     * modified from the Jasm one.
     */
    static public enum ConstType {
//        CONSTANT_ZERO                       (-3, "CONSTANT_ZERO", ""),
        CONSTANT_UTF8                       (1, "CONSTANT_UTF8", "Asciz", "Utf8"),
        CONSTANT_UNICODE                    (2, "CONSTANT_UNICODE", ""),
        CONSTANT_INTEGER                    (3, "CONSTANT_INTEGER", "int", "u4"),
        CONSTANT_FLOAT                      (4, "CONSTANT_FLOAT", "float"),
        CONSTANT_LONG                       (5, "CONSTANT_LONG", "long"),
        CONSTANT_DOUBLE                     (6, "CONSTANT_DOUBLE", "double"),
        // Class is removed for JavaCard (???)
        CONSTANT_CLASS                      (7, "CONSTANT_CLASS", "class"),
        CONSTANT_STRING                     (8, "CONSTANT_STRING", "String"),
        CONSTANT_FIELD                      (9, "CONSTANT_FIELD", "Field"),
        CONSTANT_METHOD                     (10, "CONSTANT_METHOD", "Method"),
        CONSTANT_INTERFACEMETHOD            (11, "CONSTANT_INTERFACEMETHOD", "InterfaceMethod"),
        CONSTANT_NAMEANDTYPE                (12, "CONSTANT_NAMEANDTYPE", "NameAndType"),
        // added for JavaCard
        CONSTANT_JAVACARD_PACKAGE           (13, "CONSTANT_PACKAGE", "package"),  // in javacard export file
        // Constant 14 reserved
        CONSTANT_METHODHANDLE               (15, "CONSTANT_METHODHANDLE", "MethodHandle"),
        CONSTANT_METHODTYPE                 (16, "CONSTANT_METHODTYPE", "MethodType"),
        CONSTANT_DYNAMIC                    (17, "CONSTANT_DYNAMIC", "Dynamic"),
        CONSTANT_INVOKEDYNAMIC              (18, "CONSTANT_INVOKEDYNAMIC", "InvokeDynamic"),
        CONSTANT_MODULE                     (19, "CONSTANT_MODULE",  "Module"),
        CONSTANT_MODULE_PACKAGE             (20, "CONSTANT_PACKAGE", "Package");

        public static final int maxTag = 20;

        private final int value;
        private final String parseKey;
        private final String printval;
        private final String alias;

        ConstType(int val, String print, String parse) {
            value = val;
            parseKey = parse;
            printval = print;
            alias = null;
        }

        ConstType(int val, String print, String parse, String als) {
            value = val;
            parseKey = parse;
            printval = print;
            alias = als;
        }

        public int value() {
            return value;
        }

        public String parseKey() {
            return parseKey;
        }

        public String printval() {
            return printval;
        }

        public void print(PrintWriter out) {
            out.print(parseKey);
        }

        @Override
        public String toString() {
            return "<" + printval + "> [" + Integer.toString(value) + "]";
        }
    };

    static public ConstType constType(int i) {
        return ConstantTypes.get(i);
    }

    static public ConstType constType(String parsekey) {
        return NameToConstantType.get(parsekey);
    }

    private static void registerConstantType(ConstType tt) {
        NameToConstantType.put(tt.parseKey, tt);
        if (tt.alias != null) {
            NameToConstantType.put(tt.alias, tt);
        }
        ConstantTypes.put(tt.value, tt);
    }

    public static int constValue(String stringValue) {
        ConstType Val = constType(stringValue);
        int val = -1;

        if (Val != null) {
            val = Val.value();
        } else {
            StackMapType smt = stackMapTypeKey(stringValue);

            if (smt != null) {
                val = smt.value();
            }
        }
        return val;
    }

}
