/*
 * @test /nodynamiccopyright/
 * @summary Compiler should reject value modifier when it features in illegal contexts.
 *
 * @compile/fail/ref=CheckValueModifier.out -XDrawDiagnostics CheckValueModifier.java
 */

/* Note: value as a modifier will be rejected by the parser if it features as a
   modifier of a (a) catch parameter, (b) resource variable, (c) for loop's init section
   declarators and (d) formal parameters. We test here only for the other illegal places.

   All uses of value below should trigger errors.
*/
class CheckValueModifier {
   primitive int x;
   primitive int foo() {
   }
   primitive interface IFace {}
   primitive @interface Annot {}
   primitive enum Enum {}
   primitive abstract class Inner {}
}
