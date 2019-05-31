** Overall structure ----------------------------------------------------------

At the current moment all key implemented benchmarks cover inline types 
containing only primitive types (1, 2 and 8 int values), that allows to 
understand scalability with increasing of fields in inline type. Combinations 
of int and long were checked, no differences were found and such benchmarks 
were omitted for simplicity.

On top level all benchmarks are split into two packages 
- org.openjdk.bench.valhalla.baseline      (pre-valhalla benchmarks)
- org.openjdk.bench.valhalla.lworld        (inline types benchmarks)
(such separation simplify compilation and execution baseline benchmarks with non-lworld jdk)

Comparable benchmarks (same/similar functionality) has the follwoing naming conventions:
* primitive - manual layout of primitive values into enclosing entity (array, class, method args, etc).
* reverence - using immutable pre-valhalla "old" style Java classes
* value     - using non-nullable inline types
* boxed     - using nullable inline types
* covariance - in baseline world it's when interface arrays contains references; 
               in lworld it's when interface array refers to flattened(non-nullable_) array of inline type 

Packages:

acmp:      covers acmp performance in baseline and inline types equality/substitutability in lworld
arrays:    elementary operations for arrays 
fields:    elementary operations for fields
callconv:  calling convention benchmarks (passing inline types as arguments and return value)
invoke:    inline types methods invocations
matrix:    Complex numbers matrix multiplication.
traversal: covers cost of random access readings from array. Targeting data locality.
           Different working set sizes are used to check all CPU caches hits impact.

