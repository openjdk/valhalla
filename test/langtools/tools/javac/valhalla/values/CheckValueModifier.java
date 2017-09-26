/*
 * @test /nodynamiccopyright/
 * @summary Compiler should reject value modifier when it features in illegal contexts.
 *
 * @compile/fail/ref=CheckValueModifier.out -XDenableValueTypes -XDrawDiagnostics CheckValueModifier.java
 */

/* Note: __ByValue as a modifier will be rejected by the parser if it features as a
   modifier of a (a) catch parameter, (b) resource variable, (c) for loop's init section
   declarators and (d) formal parameters. We test here only for the other illegal places.

   All uses of __ByValue below should trigger errors.
*/
class CheckValueModifier {
   __ByValue int x;
   __ByValue int foo() {
       __ByValue String local;
   }
   __ByValue interface IFace {}
   __ByValue @interface Annot {}
   __ByValue enum Enum {}
   __ByValue abstract class Inner {}
}
