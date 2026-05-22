import excluded.ExcludedValueClass;

public class InlineFieldExclusionApp {

    ExcludedValueClass v;

    InlineFieldExclusionApp() {
        v = new ExcludedValueClass();
    }

    public static void main(String args[]) {
        InlineFieldExclusionApp h = new InlineFieldExclusionApp();
        ExcludedValueClass test = new ExcludedValueClass();
        if (h.v != test) {
            throw new RuntimeException("Should be flattened");
        }
        System.out.println("Hello from InlineFieldExclusionApp!");
    }
}
