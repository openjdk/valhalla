/*
 * @test /nodynamiccopyright/
 * @summary Check inlineness via __inline__ annotation
 * @bug 8222745
 * @compile/fail/ref=InlineAnnotationTest.out -XDrawDiagnostics InlineAnnotationTest.java
 */

@__primitive__
class InlineAnnotationTest01 extends Object { 
}

@__primitive__
class InlineAnnotationTest02 { 
}

@java.lang.__primitive__
class InlineAnnotationTest03  { 
    int x = 10;
    InlineAnnotationTest03() {
        x = 20;
    }
}

@__primitive__
interface InlineAnnotationTest04 { 
}
