/*
 * @test /nodynamiccopyright/
 * @summary Check valueness via __value__ annotation
 * @bug 8221699
 * @compile/fail/ref=ValueAnnotationTest.out -XDrawDiagnostics -XDenablePrimitiveClasses ValueAnnotationTest.java
 */

primitive
class ValueAnnotationTest01 extends Object { 
}

primitive
class ValueAnnotationTest02 { 
}

primitive
class ValueAnnotationTest03  { 
    int x = 10;
    ValueAnnotationTest03() {
        x = 20;
    }
}

primitive
interface ValueAnnotationTest04 { 
}
