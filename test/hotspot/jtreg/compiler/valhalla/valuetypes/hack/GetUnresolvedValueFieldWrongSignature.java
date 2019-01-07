class TestUnloadedValueTypeField {
    static class MyValue3 {
        int foo;
    }

    static class MyValue3Holder {
        MyValue3 v;
    }
}

class GetUnresolvedValueFieldWrongSignature {
    static int test3(TestUnloadedValueTypeField.MyValue3Holder holder3) {
        if (holder3 != null) {
            return holder3.v.foo + 3;
        } else {
            return 0;
        }
    }
}