package jdk.vm.ci.hotspot;

/**
 * The acmp comparison compares two objects for equality. Due to Valhalla, profiling data is now attached to this comparison.
 * This class represents the profiled type information of one operand.
 * Corresponds to {@code ciSingleTypeEntry}
 */
public interface SingleTypeEntry{


    /**
     * @return returns the profiled type, null if multiple types have been seen
     */
    HotSpotResolvedObjectType getValidType();

    /**
     * @return whether the operand was seen to be null, false otherwise
     */
    boolean maybeNull();

    /**
     * @return whether the operand was never null, false otherwise
     */
    boolean neverNull();

    /**
     * @return whether the operand was always null, false otherwise
     */
    boolean alwaysNull();

    /**
     * @return whether the operand was seen to be a value class, false otherwise
     */
    boolean valueClass();

}
