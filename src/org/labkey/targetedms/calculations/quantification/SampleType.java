/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.calculations.quantification;

import java.awt.*;

public enum SampleType {
    unknown(Color.black),
    standard(Color.gray),
    qc(Color.green),
    solvent(new Color(255,226,43)),
    blank(Color.blue),
    double_blank(new Color(255, 230, 216));
    private Color color;
    private SampleType(Color color) {
        this.color = color;
    }
    public Color getColor() {
        return color;
    }
    public static SampleType fromName(String name) {
        if (name == null || 0 == name.length()) {
            return unknown;
        }
        try {
            return SampleType.valueOf(name);
        } catch (IllegalArgumentException iae) {
            return unknown;
        }
    }
}
