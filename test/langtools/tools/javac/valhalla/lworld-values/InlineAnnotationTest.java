/*
 * @test /nodynamiccopyright/
 * @summary Check inlineness via __inline__ annotation
 * @bug 8222745
 * @compile/fail/ref=InlineAnnotationTest.out -XDrawDiagnostics InlineAnnotationTest.java
 */

@__inline__
class InlineAnnotationTest01 extends Object { 
}

@__inline__
class InlineAnnotationTest02 { 
}

@java.lang.__inline__
class InlineAnnotationTest03  { 
    int x = 10;
    InlineAnnotationTest03() {
        x = 20;
    }
}

@__inline__
interface InlineAnnotationTest04 { 
}
