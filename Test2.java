
public class Test2 {

    static value class ByteValue {
        byte value;

        static final byte[] EDGE_CASES = {
                (byte)0, (byte)-1, (byte)1,
                Byte.MIN_VALUE, Byte.MAX_VALUE
        };

        public ByteValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "ByteValue(" + value +
                   ", bits=0x" + Integer.toHexString(value & 0xFF) + ")";
        }
    }

    static value class BooleanValue {
        boolean value;

        static final boolean[] EDGE_CASES = {
                false, true
        };

        public BooleanValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "BooleanValue(" + value +
                   ", bits=" + (value ? "1" : "0") + ")";
        }
    }

    static value class CharValue {
        char value;

        static final char[] EDGE_CASES = {
                (char)0,
                (char)1,
                (char)0xFFFF,
                Character.MIN_VALUE,
                Character.MAX_VALUE
        };

        public CharValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "CharValue(" + (int)value +
                   ", bits=0x" + Integer.toHexString(value) + ")";
        }
    }

    static value class ShortValue {
        short value;

        static final short[] EDGE_CASES = {
                (short)0, (short)-1, (short)1,
                Short.MIN_VALUE, Short.MAX_VALUE
        };

        public ShortValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "ShortValue(" + value +
                   ", bits=0x" + Integer.toHexString(value & 0xFFFF) + ")";
        }
    }

    static value class IntValue {
        int value;

        static final int[] EDGE_CASES = {
                0, -1, 1,
                Integer.MIN_VALUE, Integer.MAX_VALUE
        };

        public IntValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "IntValue(" + value +
                   ", bits=0x" + Integer.toHexString(value) + ")";
        }
    }

    static value class LongValue {
        long value;

        static final long[] EDGE_CASES = {
                0L, -1L, 1L,
                Long.MIN_VALUE, Long.MAX_VALUE
        };

        public LongValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            return "LongValue(" + value +
                   ", bits=0x" + Long.toHexString(value) + ")";
        }
    }

    static value class FloatValue {
        float value;

        static final float[] EDGE_CASES = {
                +0.0f, -0.0f,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.NaN,
                Float.MIN_VALUE, -Float.MIN_VALUE,
                Float.MAX_VALUE, -Float.MAX_VALUE,
                Float.intBitsToFloat(0x00800000), // smallest normal
                Float.intBitsToFloat(0x80800000),
                Float.intBitsToFloat(0x007FFFFF), // largest subnormal
                Float.intBitsToFloat(0x807FFFFF),
                Float.intBitsToFloat(0x7FC00000), // quiet NaN
                Float.intBitsToFloat(0x7F800001), // signaling NaN
                Float.intBitsToFloat(0x7FFFFFFF), // max payload
                Float.intBitsToFloat(0xFFC00000), // negative quiet
                Float.intBitsToFloat(0xFF800001), // negative signaling
                Float.intBitsToFloat(0xFFFFFFFF)  // negative max payload
        };

        public FloatValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            int bits = Float.floatToRawIntBits(value);
            return "FloatValue(" + value +
                   ", bits=0x" + Integer.toHexString(bits) + ")";
        }
    }

    static value class DoubleValue {
        double value;

        static final double[] EDGE_CASES = {
                +0.0, -0.0,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NaN,
                Double.MIN_VALUE, -Double.MIN_VALUE,
                Double.MAX_VALUE, -Double.MAX_VALUE,
                Double.longBitsToDouble(0x0010000000000000L), // smallest normal
                Double.longBitsToDouble(0x8010000000000000L),
                Double.longBitsToDouble(0x000FFFFFFFFFFFFFL), // largest subnormal
                Double.longBitsToDouble(0x800FFFFFFFFFFFFFL),
                Double.longBitsToDouble(0x7FF8000000000000L), // quiet NaN
                Double.longBitsToDouble(0x7FF0000000000001L), // signaling NaN
                Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL), // max payload
                Double.longBitsToDouble(0xFFF8000000000000L), // negative quiet NaN
                Double.longBitsToDouble(0xFFF0000000000001L), // negative signaling
                Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL)  // negative max payload
        };

        public DoubleValue(int index) {
            value = EDGE_CASES[index];
        }

        public String toString() {
            long bits = Double.doubleToRawLongBits(value);
            return "DoubleValue(" + value +
                   ", bits=0x" + Long.toHexString(bits) + ")";
        }
    }

    public static boolean testByteValue(ByteValue v1, ByteValue v2) {
        return v1 == v2;
    }

    public static boolean testBooleanValue(BooleanValue v1, BooleanValue v2) {
        return v1 == v2;
    }

    public static boolean testCharValue(CharValue v1, CharValue v2) {
        return v1 == v2;
    }

    public static boolean testShortValue(ShortValue v1, ShortValue v2) {
        return v1 == v2;
    }

    public static boolean testIntValue(IntValue v1, IntValue v2) {
        return v1 == v2;
    }

    public static boolean testLongValue(LongValue v1, LongValue v2) {
        return v1 == v2;
    }

    public static boolean testFloatValue(FloatValue v1, FloatValue v2) {
        return v1 == v2;
    }

    public static boolean testDoubleValue(DoubleValue v1, DoubleValue v2) {
        return v1 == v2;
    }

    public static void main(String[] args) {

        for (int k = 0; k < 10_000; ++k) {
            // ByteValue
            for (int i = 0; i < ByteValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < ByteValue.EDGE_CASES.length; ++j) {
                    ByteValue val1 = new ByteValue(i);
                    ByteValue val2 = new ByteValue(j);
                    boolean res = testByteValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }

            // BooleanValue
            for (int i = 0; i < BooleanValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < BooleanValue.EDGE_CASES.length; ++j) {
                    BooleanValue val1 = new BooleanValue(i);
                    BooleanValue val2 = new BooleanValue(j);
                    boolean res = testBooleanValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }

            // CharValue
            for (int i = 0; i < CharValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < CharValue.EDGE_CASES.length; ++j) {
                    CharValue val1 = new CharValue(i);
                    CharValue val2 = new CharValue(j);
                    boolean res = testCharValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }

            // ShortValue
            for (int i = 0; i < ShortValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < ShortValue.EDGE_CASES.length; ++j) {
                    ShortValue val1 = new ShortValue(i);
                    ShortValue val2 = new ShortValue(j);
                    boolean res = testShortValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }

            // IntValue
            for (int i = 0; i < IntValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < IntValue.EDGE_CASES.length; ++j) {
                    IntValue val1 = new IntValue(i);
                    IntValue val2 = new IntValue(j);
                    boolean res = testIntValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }

            // LongValue
            for (int i = 0; i < LongValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < LongValue.EDGE_CASES.length; ++j) {
                    LongValue val1 = new LongValue(i);
                    LongValue val2 = new LongValue(j);
                    boolean res = testLongValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }

            // FloatValue
            for (int i = 0; i < FloatValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < FloatValue.EDGE_CASES.length; ++j) {
                    FloatValue val1 = new FloatValue(i);
                    FloatValue val2 = new FloatValue(j);
                    boolean res = testFloatValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }

            // DoubleValue
            for (int i = 0; i < DoubleValue.EDGE_CASES.length; ++i) {
                for (int j = 0; j < DoubleValue.EDGE_CASES.length; ++j) {
                    DoubleValue val1 = new DoubleValue(i);
                    DoubleValue val2 = new DoubleValue(j);
                    boolean res = testDoubleValue(val1, val2);
                    if (res != (val1 == val2)) {
                        throw new RuntimeException("Incorrect result for " + val1 + " == " + val2);
                    }
                }
            }
        }
    }
}
