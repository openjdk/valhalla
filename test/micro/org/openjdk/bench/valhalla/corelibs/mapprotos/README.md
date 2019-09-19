# Prototype implementations of HashMaps using inline classes for the table entries.

   **NOTE: The implementations have NOT been optimized or tuned.**

## YHashMap uses Open Addressing to store all entries in a table of inline classes
The hash of the key is used as the first index into the table. 
If there is a collision, double hashing (with a static offset) is used to probe subsequent locations for available storage.
The Robin Hood hashing variation on insertion is used to reduce worst case lookup times.
On key removal, the subsequent double-hashed entries are compressed into the entry being removed.

### YHashMap Storage requirements
Typical storage usage for a table near its load factor is 22 bytes per entry.

Inserting entries into the YHashMap may resize the table but otherwise does
not use any additional memory on each get or put.

## XHashMap stores the initial entry in a table of inline classes
The hash of the key is used as the first index into the table. 
If there is a collision, subsequent entries add the familiar link list of Nodes.
On key removal, direct entries in the table are replaced by the first linked node;
for Nodes in the link list, the Node is unlinked.

### XHashMap Storage requirements:
Typical storage usage for a table near its load factor is 32 bytes per entry.

## java.util.HashMap (the original)
HashMap uses a table of references to the initial Node entries, collisions are handled by 
linked Nodes.

### HashMap Storage requirements:
Typical storage usage for a table near its load factor is 37 bytes per entry.


## Sample JHM test results for Get, Put, Iteration, and Replacement

    Benchmark                                                   (mapType)  (seed)   (size)  Mode  Cnt    Score     Error  Units
    GetX.getHit              org.openjdk.bench.valhalla.corelibs.YHashMap      42      500  avgt    5    0.007 ±   0.001  ms/op
    GetX.getHit              org.openjdk.bench.valhalla.corelibs.XHashMap      42      500  avgt    5    0.006 ±   0.001  ms/op
    GetX.getHit                                         java.util.HashMap      42      500  avgt    5    0.004 ±   0.001  ms/op
    GetX.getHit              org.openjdk.bench.valhalla.corelibs.YHashMap      42  1000000  avgt    5   91.906 ±   3.417  ms/op
    GetX.getHit              org.openjdk.bench.valhalla.corelibs.XHashMap      42  1000000  avgt    5   77.905 ±   8.267  ms/op
    GetX.getHit                                         java.util.HashMap      42  1000000  avgt    5   79.900 ±   7.679  ms/op

    GetX.getMix              org.openjdk.bench.valhalla.corelibs.YHashMap      42      500  avgt    5    0.006 ±   0.001  ms/op
    GetX.getMix              org.openjdk.bench.valhalla.corelibs.XHashMap      42      500  avgt    5    0.004 ±   0.001  ms/op
    GetX.getMix                                         java.util.HashMap      42      500  avgt    5    0.004 ±   0.001  ms/op
    GetX.getMix              org.openjdk.bench.valhalla.corelibs.YHashMap      42  1000000  avgt    5  174.739 ±  25.464  ms/op
    GetX.getMix              org.openjdk.bench.valhalla.corelibs.XHashMap      42  1000000  avgt    5   77.241 ±   4.472  ms/op
    GetX.getMix                                         java.util.HashMap      42  1000000  avgt    5  122.827 ±   8.779  ms/op

    PutX.put                 org.openjdk.bench.valhalla.corelibs.YHashMap      42      500  avgt    5    0.034 ±   0.005  ms/op
    PutX.put                 org.openjdk.bench.valhalla.corelibs.XHashMap      42      500  avgt    5    0.025 ±   0.003  ms/op
    PutX.put                                            java.util.HashMap      42      500  avgt    5    0.013 ±   0.001  ms/op
    PutX.put                 org.openjdk.bench.valhalla.corelibs.YHashMap      42  1000000  avgt    5  459.951 ± 345.698  ms/op
    PutX.put                 org.openjdk.bench.valhalla.corelibs.XHashMap      42  1000000  avgt    5  409.430 ± 146.610  ms/op
    PutX.put                                            java.util.HashMap      42  1000000  avgt    5  336.873 ± 122.706  ms/op

    PutX.putSized            org.openjdk.bench.valhalla.corelibs.YHashMap      42      500  avgt    5    0.014 ±   0.002  ms/op
    PutX.putSized            org.openjdk.bench.valhalla.corelibs.XHashMap      42      500  avgt    5    0.012 ±   0.002  ms/op
    PutX.putSized                                       java.util.HashMap      42      500  avgt    5    0.010 ±   0.002  ms/op
    PutX.putSized            org.openjdk.bench.valhalla.corelibs.YHashMap      42  1000000  avgt    5  263.196 ±  68.516  ms/op
    PutX.putSized            org.openjdk.bench.valhalla.corelibs.XHashMap      42  1000000  avgt    5  292.726 ± 165.881  ms/op
    PutX.putSized                                       java.util.HashMap      42  1000000  avgt    5  310.345 ± 176.412  ms/op

    ReplX.replace            org.openjdk.bench.valhalla.corelibs.YHashMap      42      500  avgt    5    0.023 ±   0.001  ms/op
    ReplX.replace            org.openjdk.bench.valhalla.corelibs.XHashMap      42      500  avgt    5    0.011 ±   0.001  ms/op
    ReplX.replace                                       java.util.HashMap      42      500  avgt    5    0.012 ±   0.001  ms/op
    ReplX.replace            org.openjdk.bench.valhalla.corelibs.YHashMap      42  1000000  avgt    5  302.525 ±  13.322  ms/op
    ReplX.replace            org.openjdk.bench.valhalla.corelibs.XHashMap      42  1000000  avgt    5  206.582 ±  33.568  ms/op
    ReplX.replace                                       java.util.HashMap      42  1000000  avgt    5  231.924 ±  40.715  ms/op

    WalkX.sumIterator        org.openjdk.bench.valhalla.corelibs.YHashMap      42      500  avgt    5    0.003 ±   0.001  ms/op
    WalkX.sumIterator        org.openjdk.bench.valhalla.corelibs.XHashMap      42      500  avgt    5    0.010 ±   0.001  ms/op
    WalkX.sumIterator                                   java.util.HashMap      42      500  avgt    5    0.002 ±   0.001  ms/op
    WalkX.sumIterator        org.openjdk.bench.valhalla.corelibs.YHashMap      42  1000000  avgt    5   43.471 ±   3.634  ms/op
    WalkX.sumIterator        org.openjdk.bench.valhalla.corelibs.XHashMap      42  1000000  avgt    5   35.960 ±   2.046  ms/op
    WalkX.sumIterator                                   java.util.HashMap      42  1000000  avgt    5   58.106 ±   3.852  ms/op

    WalkX.sumIteratorHidden  org.openjdk.bench.valhalla.corelibs.YHashMap      42      500  avgt    5    0.002 ±   0.001  ms/op
    WalkX.sumIteratorHidden  org.openjdk.bench.valhalla.corelibs.XHashMap      42      500  avgt    5    0.007 ±   0.001  ms/op
    WalkX.sumIteratorHidden                             java.util.HashMap      42      500  avgt    5    0.005 ±   0.001  ms/op
    WalkX.sumIteratorHidden  org.openjdk.bench.valhalla.corelibs.YHashMap      42  1000000  avgt    5   33.189 ±   2.212  ms/op
    WalkX.sumIteratorHidden  org.openjdk.bench.valhalla.corelibs.XHashMap      42  1000000  avgt    5   43.003 ±   1.183  ms/op
    WalkX.sumIteratorHidden                             java.util.HashMap      42  1000000  avgt    5  102.566 ±   5.276  ms/op
