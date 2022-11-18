/*
 * @test /nodynamiccopyright/
 * @summary Check inlineness via __inline__ annotation
 * @bug 8222745
 * @compile/fail/ref=InlineAnnotationTest.out -XDrawDiagnostics -XDenablePrimitiveClasses InlineAnnotationTest.java
 */

primitive
class InlineAnnotationTest01 extends Object { 
}

primitive
class InlineAnnotationTest02 { 
}

primitive
class InlineAnnotationTest03  { 
    int x = 10;
    InlineAnnotationTest03() {
        x = 20;
    }
}

primitive
interface InlineAnnotationTest04 { 
}
