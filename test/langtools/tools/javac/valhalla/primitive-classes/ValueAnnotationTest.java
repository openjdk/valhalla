/*
 * @test /nodynamiccopyright/
 * @summary Check valueness via __value__ annotation
 * @bug 8221699
 * @compile/fail/ref=ValueAnnotationTest.out -XDrawDiagnostics ValueAnnotationTest.java
 */

@__primitive__
class ValueAnnotationTest01 extends Object { 
}

@__primitive__
class ValueAnnotationTest02 { 
}

@java.lang.__primitive__
class ValueAnnotationTest03  { 
    int x = 10;
    ValueAnnotationTest03() {
        x = 20;
    }
}

@__primitive__
interface ValueAnnotationTest04 { 
}
