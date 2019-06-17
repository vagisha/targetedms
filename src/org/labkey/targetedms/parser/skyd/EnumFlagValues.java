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
package org.labkey.targetedms.parser.skyd;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Converts between an Enum and a long using the ordinals of the enum values.
 */
public class EnumFlagValues
{
    public static <T extends Enum<T>> EnumSet<T> enumSetFromFlagValues(Class<T> enumClass, long longValue) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(enumValue->0 != (longValue & (1L << enumValue.ordinal())))
                .collect(Collectors.toCollection(()->EnumSet.noneOf(enumClass)));
    }

    public static <T extends Enum<T>> long enumSetToFlagValues(EnumSet<T> enumSet) {
        long result = 0;
        for (T enumValue : enumSet) {
            result |= 1L << enumValue.ordinal();
        }
        return result;
    }
}
