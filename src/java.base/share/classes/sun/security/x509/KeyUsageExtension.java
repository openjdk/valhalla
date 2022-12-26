/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.x509;

import java.io.IOException;

import sun.security.util.*;

/**
 * Represent the Key Usage Extension.
 *
 * <p>This extension, if present, defines the purpose (e.g., encipherment,
 * signature, certificate signing) of the key contained in the certificate.
 * The usage restriction might be employed when a multipurpose key is to be
 * restricted (e.g., when an RSA key should be used only for signing or only
 * for key encipherment).
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see Extension
 */
public class KeyUsageExtension extends Extension {

    public static final String NAME = "KeyUsage";
    public static final String DIGITAL_SIGNATURE = "digital_signature";
    public static final String NON_REPUDIATION = "non_repudiation";
    public static final String KEY_ENCIPHERMENT = "key_encipherment";
    public static final String DATA_ENCIPHERMENT = "data_encipherment";
    public static final String KEY_AGREEMENT = "key_agreement";
    public static final String KEY_CERTSIGN = "key_certsign";
    public static final String CRL_SIGN = "crl_sign";
    public static final String ENCIPHER_ONLY = "encipher_only";
    public static final String DECIPHER_ONLY = "decipher_only";

    // Private data members
    private boolean[] bitString;

    // Encode this extension value
    private void encodeThis() {
        DerOutputStream os = new DerOutputStream();
        os.putTruncatedUnalignedBitString(new BitArray(this.bitString));
        this.extensionValue = os.toByteArray();
    }

    /**
     * Check if bit is set.
     *
     * @param position the position in the bit string to check.
     */
    private boolean isSet(int position) {
        return (position < bitString.length) &&
                bitString[position];
    }

    /**
     * Set the bit at the specified position.
     */
    private void set(int position, boolean val) {
        // enlarge bitString if necessary
        if (position >= bitString.length) {
            boolean[] tmp = new boolean[position+1];
            System.arraycopy(bitString, 0, tmp, 0, bitString.length);
            bitString = tmp;
        }
        bitString[position] = val;
    }

    /**
     * Create a KeyUsageExtension with the passed bit settings. The criticality
     * is set to true.
     *
     * @param bitString the bits to be set for the extension.
     */
    public KeyUsageExtension(byte[] bitString) {
        this.bitString =
            new BitArray(bitString.length*8,bitString).toBooleanArray();
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = true;
        encodeThis();
    }

    /**
     * Create a KeyUsageExtension with the passed bit settings. The criticality
     * is set to true.
     *
     * @param bitString the bits to be set for the extension.
     */
    public KeyUsageExtension(boolean[] bitString) {
        this.bitString = bitString;
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = true;
        encodeThis();
    }

    /**
     * Create a KeyUsageExtension with the passed bit settings. The criticality
     * is set to true.
     *
     * @param bitString the bits to be set for the extension.
     */
    public KeyUsageExtension(BitArray bitString) {
        this.bitString = bitString.toBooleanArray();
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = true;
        encodeThis();
    }

    /**
     * Create the extension from the passed DER encoded value of the same.
     * The DER encoded value may be wrapped in an OCTET STRING.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param value an array of DER encoded bytes of the actual value (possibly
     * wrapped in an OCTET STRING).
     * @exception ClassCastException if value is not an array of bytes
     * @exception IOException on error.
     */
    public KeyUsageExtension(Boolean critical, Object value)
    throws IOException {
        this.extensionId = PKIXExtensions.KeyUsage_Id;
        this.critical = critical.booleanValue();
        /*
         * The following check should be activated again after
         * the PKIX profiling work becomes standard and the check
         * is not a barrier to interoperability !
         * if (!this.critical) {
         *   throw new IOException("KeyUsageExtension not marked critical,"
         *                         + " invalid profile.");
         * }
         */
        byte[] extValue = (byte[]) value;
        if (extValue[0] == DerValue.tag_OctetString) {
            this.extensionValue = new DerValue(extValue).getOctetString();
        } else {
            this.extensionValue = extValue;
        }
        DerValue val = new DerValue(this.extensionValue);
        this.bitString = val.getUnalignedBitString().toBooleanArray();
    }

    /**
     * Create a default key usage.
     */
    public KeyUsageExtension() {
        extensionId = PKIXExtensions.KeyUsage_Id;
        critical = true;
        bitString = new boolean[0];
    }

    /**
     * Set the attribute value.
     */
    public void set(String name, boolean val) throws IOException {
        if (name.equalsIgnoreCase(DIGITAL_SIGNATURE)) {
            set(0,val);
        } else if (name.equalsIgnoreCase(NON_REPUDIATION)) {
            set(1,val);
        } else if (name.equalsIgnoreCase(KEY_ENCIPHERMENT)) {
            set(2,val);
        } else if (name.equalsIgnoreCase(DATA_ENCIPHERMENT)) {
            set(3,val);
        } else if (name.equalsIgnoreCase(KEY_AGREEMENT)) {
            set(4,val);
        } else if (name.equalsIgnoreCase(KEY_CERTSIGN)) {
            set(5,val);
        } else if (name.equalsIgnoreCase(CRL_SIGN)) {
            set(6,val);
        } else if (name.equalsIgnoreCase(ENCIPHER_ONLY)) {
            set(7,val);
        } else if (name.equalsIgnoreCase(DECIPHER_ONLY)) {
            set(8,val);
        } else {
          throw new IOException("Attribute name not recognized by"
                                + " KeyUsage.");
        }
        encodeThis();
    }

    /**
     * Get the attribute value.
     */
    public boolean get(String name) throws IOException {
        if (name.equalsIgnoreCase(DIGITAL_SIGNATURE)) {
            return isSet(0);
        } else if (name.equalsIgnoreCase(NON_REPUDIATION)) {
            return isSet(1);
        } else if (name.equalsIgnoreCase(KEY_ENCIPHERMENT)) {
            return isSet(2);
        } else if (name.equalsIgnoreCase(DATA_ENCIPHERMENT)) {
            return isSet(3);
        } else if (name.equalsIgnoreCase(KEY_AGREEMENT)) {
            return isSet(4);
        } else if (name.equalsIgnoreCase(KEY_CERTSIGN)) {
            return isSet(5);
        } else if (name.equalsIgnoreCase(CRL_SIGN)) {
            return isSet(6);
        } else if (name.equalsIgnoreCase(ENCIPHER_ONLY)) {
            return isSet(7);
        } else if (name.equalsIgnoreCase(DECIPHER_ONLY)) {
            return isSet(8);
        } else {
          throw new IOException("Attribute name not recognized by"
                                + " KeyUsage.");
        }
    }


    /**
     * Returns a printable representation of the KeyUsage.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("KeyUsage [\n");

        if (isSet(0)) {
            sb.append("  DigitalSignature\n");
        }
        if (isSet(1)) {
            sb.append("  Non_repudiation\n");
        }
        if (isSet(2)) {
            sb.append("  Key_Encipherment\n");
        }
        if (isSet(3)) {
            sb.append("  Data_Encipherment\n");
        }
        if (isSet(4)) {
            sb.append("  Key_Agreement\n");
        }
        if (isSet(5)) {
            sb.append("  Key_CertSign\n");
        }
        if (isSet(6)) {
            sb.append("  Crl_Sign\n");
        }
        if (isSet(7)) {
            sb.append("  Encipher_Only\n");
        }
        if (isSet(8)) {
            sb.append("  Decipher_Only\n");
        }
        sb.append("]\n");

        return sb.toString();
    }

    /**
     * Write the extension to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     */
    @Override
    public void encode(DerOutputStream out) {
       if (this.extensionValue == null) {
           this.extensionId = PKIXExtensions.KeyUsage_Id;
           this.critical = true;
           encodeThis();
       }
       super.encode(out);
    }



    public boolean[] getBits() {
        return bitString.clone();
    }

    /**
     * Return the name of this extension.
     */
    @Override
    public String getName() {
        return NAME;
    }
}
