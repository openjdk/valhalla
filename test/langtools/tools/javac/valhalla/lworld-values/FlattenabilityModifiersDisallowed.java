/*
 * @test /nodynamiccopyright/
 * @summary Verify that flattenability modifiers are not exposed by default.
 * @compile/fail/ref=FlattenabilityModifiersDisallowed.out -XDrawDiagnostics -XDdev FlattenabilityModifiersDisallowed.java
 * @compile -XDrawDiagnostics -XDdev -XDallowFlattenabilityModifiers FlattenabilityModifiersDisallowed.java
 */

class FlattenabilityModifiersDisallowed {
    __Flattenable int x;
    __NotFlattened int y;
}
