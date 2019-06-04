/*
 * @test /nodynamiccopyright/
 * @summary Check valueness via __value__ annotation
 * @bug 8221699
 * @compile/fail/ref=ValueAnnotationTest.out -XDrawDiagnostics ValueAnnotationTest.java
 */

@__inline__
class ValueAnnotationTest01 extends Object { 
}

@__inline__
class ValueAnnotationTest02 { 
}

@java.lang.__inline__
class ValueAnnotationTest03  { 
    int x = 10;
    ValueAnnotationTest03() {
        x = 20;
    }
}

@__inline__
interface ValueAnnotationTest04 { 
}
