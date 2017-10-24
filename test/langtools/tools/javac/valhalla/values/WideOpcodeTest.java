/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Check that javap disassembles wide vopcodes properly
 * @modules jdk.compiler jdk.jdeps/com.sun.tools.javap
 * @compile -XDenableValueTypes WideOpcodeTest.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla WideOpcodeTest
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class WideOpcodeTest {

    public static __ByValue final class V {
        final int v = 0;
        __ValueFactory static V makeV() {
            return __MakeDefault V();
        }
    }

    static void foo(V v) {}
    
    public static void main(String[] args) {

        V v0, v1, v2, v3, v4, v5, v6, v7, v8, v9, v10;
        V v11, v12, v13, v14, v15, v16, v17, v18, v19, v20;
        V v21, v22, v23, v24, v25, v26, v27, v28, v29, v30;
        V v31, v32, v33, v34, v35, v36, v37, v38, v39, v40;
        V v41, v42, v43, v44, v45, v46, v47, v48, v49, v50;
        V v51, v52, v53, v54, v55, v56, v57, v58, v59, v60;
        V v61, v62, v63, v64, v65, v66, v67, v68, v69, v70;
        V v71, v72, v73, v74, v75, v76, v77, v78, v79, v80;
        V v81, v82, v83, v84, v85, v86, v87, v88, v89, v90;
        V v91, v92, v93, v94, v95, v96, v97, v98, v99, v100;
        V v101, v102, v103, v104, v105, v106, v107, v108, v109, v110;
        V v111, v112, v113, v114, v115, v116, v117, v118, v119, v120;
        V v121, v122, v123, v124, v125, v126, v127, v128, v129, v130;
        V v131, v132, v133, v134, v135, v136, v137, v138, v139, v140;
        V v141, v142, v143, v144, v145, v146, v147, v148, v149, v150;
        V v151, v152, v153, v154, v155, v156, v157, v158, v159, v160;
        V v161, v162, v163, v164, v165, v166, v167, v168, v169, v170;
        V v171, v172, v173, v174, v175, v176, v177, v178, v179, v180;
        V v181, v182, v183, v184, v185, v186, v187, v188, v189, v190;
        V v191, v192, v193, v194, v195, v196, v197, v198, v199, v200;
        V v201, v202, v203, v204, v205, v206, v207, v208, v209, v210;
        V v211, v212, v213, v214, v215, v216, v217, v218, v219, v220;
        V v221, v222, v223, v224, v225, v226, v227, v228, v229, v230;
        V v231, v232, v233, v234, v235, v236, v237, v238, v239, v240;
        V v241, v242, v243, v244, v245, v246, v247, v248, v249, v250;
        V v251, v252, v253, v254, v255, v256, v257, v258, v259, v260;
        V v261, v262, v263, v264, v265, v266, v267, v268, v269, v270;
        V v271, v272, v273, v274, v275, v276, v277, v278, v279, v280;
        V v281, v282, v283, v284, v285, v286, v287, v288, v289, v290;
        V v291, v292, v293, v294, v295, v296, v297, v298, v299, v300;

        v300 = V.makeV();
        foo(v300);
        new WideOpcodeTest().run();
    }

    void run() {
        String [] params =
                new String [] { "-v",
                                Paths.get(System.getProperty("test.classes"),
                               "WideOpcodeTest.class").toString() };

        runCheck(params, new String [] {
               "0: invokestatic  #2                  // Method \";QWideOpcodeTest$V;\".makeV:()QWideOpcodeTest$V;",
               "3: vstore_w      301",
               "7: vload_w       301",
               "11: invokestatic  #3                  // Method foo:(QWideOpcodeTest$V;)V",
                         });
     }

     void runCheck(String [] params, String [] expectedOut) {
        StringWriter s;
        String out;

        try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
            com.sun.tools.javap.Main.run(params, pw);
            out = s.toString();
        }
        for (String eo: expectedOut) {
            if (!out.contains(eo))
                throw new AssertionError("Unexpected output: " + eo + " \n in: " + out);
        }
    }
}
