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
 */package org.labkey.targetedms.parser;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * List of possible standard types for peptides and molecules in a Skyline document.
 */
public enum StandardType
{
    /**
     * Global Standard. When the normalization method is "Ratio to Global Standards", the normalized area
     * is calculated as the ratio to the sum of the peak areas of all of the molecules & peptides whose StandardType is
     * Normalization.
     */
    Normalization("Normalization"),
    QC("QC"),
    iRT("iRT"),
    /**
     * Surrogate standard. When a molecule's {@link GeneralMolecule#getNormalizationMethod()} is "surrogate_XXX",
     * the normalized area is calculated as the ratio to the sum of the peak areas of all of the molecules & peptides
     * whose StandardType is Surrogate, and whose Name is XXX.
     */
    SurrogateStandard("Surrogate Standard");
    private final String stringValue;
    StandardType(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString()
    {
        return stringValue;
    }

    @Nullable
    public static StandardType parse(@Nullable String stringValue)
    {
        if (StringUtils.isEmpty(stringValue))
        {
            return null;
        }
        if ("Global Standard".equals(stringValue)) {
            return Normalization;
        }
        Optional<StandardType> match = Stream.of(values()).filter(v->v.toString().equals(stringValue)).findFirst();
        if (match.isPresent()) {
            return match.get();
        }
        return null;
    }
}
