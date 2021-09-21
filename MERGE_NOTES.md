
** Incomplete merge ! **

Merge from master up until JDK-8273655 (https://github.com/openjdk/valhalla/commit/65ed0a742e65fe7e661e8da2adea6cd41992869e)

The following files contain dubious conflict resolutions:

---

- Commit:  https://github.com/openjdk/valhalla/commit/1d2458db34ed6acdd20bb8c165b7619cdbc32f47
- Bug: https://bugs.openjdk.java.net/browse/JDK-8266550
  - src/hotspot/share/opto/type.cpp
  - src/hotspot/share/opto/type.hpp

Completely unresolved, conflict markers are of the form:

  `<<<<<<< ours`  // i.e. lworld
  `||||||| base`  // i.e. master original
  `=======`
  `>>>>>>> theirs` // i.e. master today

---

- Commit: https://github.com/openjdk/valhalla/commit/02822e1398d6015f0ed26edd440db8e0d50bf152#diff-03f7ae3cf79ff61be6e4f0590b7809a87825b073341fdbfcf36143b99c304474L729
- Bug: https://bugs.openjdk.java.net/browse/JDK-8272377
  - src/hotspot/share/opto/escape.cpp

Not sure of the correctness.

