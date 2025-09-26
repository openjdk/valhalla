package jdk.vm.ci.hotspot;

/**
 * The acmp comparison compares two objects for equality. Due to Valhalla, profiling data is now attached to this comparison.
 * This class represents the profiled type information of both operands.
 * Corresponds to {@code ciACmpData}
 */
public interface ACmpDataAccessor {
    /**
     * @return the profiled type information of the left operand
     */
    SingleTypeEntry getLeft();

    /**
     * @return the profiled type information of the right operand
     */
    SingleTypeEntry getRight();

}
