/*
 * Copyright (c) 2008, 2023, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.filter;

import com.sun.hotspot.igv.graph.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class CombineFilter extends AbstractFilter {

    private List<CombineRule> rules;
    private String name;

    public CombineFilter(String name) {
        this.name = name;
        rules = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void apply(Diagram diagram) {

        for (CombineRule r : rules) {

            List<Figure> first = r.getFirstSelector().selected(diagram);
            List<Figure> second = r.getSecondSelector().selected(diagram);
            for (Figure f : first) {

                List<Figure> successors = new ArrayList<>(f.getSuccessors());
                if (r.isReversed()) {
                    if (successors.size() == 1) {
                        Figure succ = successors.get(0);
                        InputSlot slot = null;

                        for (InputSlot s : succ.getInputSlots()) {
                            for (FigureConnection c : s.getConnections()) {
                                if (c.getOutputSlot().getFigure() == f) {
                                    slot = s;
                                }
                            }
                        }

                        slot.getSource().addSourceNode(f.getInputNode());
                        if (r.getPropertyNames() != null && r.getPropertyNames().length > 0) {
                            String s = r.getFirstMatchingProperty(f);
                            if (s != null && s.length() > 0) {
                                slot.setShortName(s);
                                slot.setText(s);
                                slot.setColor(f.getColor());
                            }
                        } else {
                            assert slot != null;
                            slot.setText(f.getProperties().get("dump_spec"));
                            slot.setColor(f.getColor());
                            if (f.getProperties().get("short_name") != null) {
                                slot.setShortName(f.getProperties().get("short_name"));
                            } else {
                                String s = f.getProperties().get("dump_spec");
                                if (s != null && s.length() <= 5) {
                                    slot.setShortName(s);
                                }
                            }
                        }

                        for (InputSlot s : f.getInputSlots()) {
                            for (FigureConnection c : s.getConnections()) {
                                FigureConnection newConn = diagram.createConnection(slot, c.getOutputSlot(), c.getLabel());
                                newConn.setColor(c.getColor());
                                newConn.setStyle(c.getStyle());
                            }
                        }
                    }
                } else {

                    for (Figure succ : successors) {
                        if (succ.getPredecessors().size() == 1) {
                            if (second.contains(succ) && succ.getOutputSlots().size() <= 1) {

                                OutputSlot oldSlot = null;
                                for (OutputSlot s : f.getOutputSlots()) {
                                    for (FigureConnection c : s.getConnections()) {
                                        if (c.getInputSlot().getFigure() == succ) {
                                            oldSlot = s;
                                        }
                                    }
                                }

                                assert oldSlot != null;

                                OutputSlot nextSlot = null;
                                if (succ.getOutputSlots().size() == 1) {
                                    nextSlot = succ.getOutputSlots().get(0);
                                }

                                int pos = 0;
                                if (succ.getProperties().get("con") != null) {
                                    pos = Integer.parseInt(succ.getProperties().get("con"));
                                }
                                OutputSlot slot = f.createOutputSlot(pos);
                                slot.getSource().addSourceNode(succ.getInputNode());
                                if (r.getPropertyNames() != null && r.getPropertyNames().length > 0) {
                                    String s = r.getFirstMatchingProperty(succ);
                                    if (s != null && s.length() > 0) {
                                        slot.setShortName(s);
                                        slot.setText(s);
                                        slot.setColor(succ.getColor());
                                    }
                                } else {
                                    slot.setText(succ.getProperties().get("dump_spec"));
                                    slot.setColor(succ.getColor());
                                    if (succ.getProperties().get("short_name") != null) {
                                        slot.setShortName(succ.getProperties().get("short_name"));
                                    } else {
                                        String s = succ.getProperties().get("dump_spec");
                                        if (s != null && s.length() <= 2) {
                                            slot.setShortName(s);
                                        } else {
                                            String tmpName = succ.getProperties().get("name");
                                            if (tmpName != null && tmpName.length() > 0) {
                                                slot.setShortName(tmpName.substring(0, 1));
                                            }
                                        }
                                    }
                                }
                                if (nextSlot != null) {
                                    for (FigureConnection c : nextSlot.getConnections()) {
                                        FigureConnection newConn = diagram.createConnection(c.getInputSlot(), slot, c.getLabel());
                                        newConn.setColor(c.getColor());
                                        newConn.setStyle(c.getStyle());
                                    }
                                }

                                diagram.removeFigure(succ);

                                if (oldSlot.getConnections().size() == 0) {
                                    f.removeSlot(oldSlot);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void addRule(CombineRule combineRule) {
        rules.add(combineRule);
    }

    public static class CombineRule {

        private Selector first;
        private Selector second;
        private boolean reversed;
        private String[] propertyNames;

        public CombineRule(Selector first, Selector second, boolean reversed, String[] propertyNames) {
            this.first = first;
            this.second = second;
            this.reversed = reversed;
            this.propertyNames = propertyNames;
        }

        public boolean isReversed() {
            return reversed;
        }

        public Selector getFirstSelector() {
            return first;
        }

        public Selector getSecondSelector() {
            return second;
        }

        public String[] getPropertyNames() {
            return propertyNames;
        }

        public String getFirstMatchingProperty(Figure figure) {
            return AbstractFilter.getFirstMatchingProperty(figure, propertyNames);
        }
    }
}
