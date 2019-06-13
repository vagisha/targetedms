/*
 * Copyright (c) 2015-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util.targetedms;

import org.labkey.test.BaseWebDriverTest;

public class QCHelper
{
    BaseWebDriverTest _test;

    QCHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public static class Annotation
    {
        private String type;
        private String description;
        private String date;

        public Annotation(String type, String description, String date)
        {
            this.type = type;
            this.description = description;
            this.date = date;
        }

        public Annotation(String type, String description)
        {
            this.type = type;
            this.description = description;
        }

        public String getType()
        {
            return type;
        }

        public String getDescription()
        {
            return description;
        }

        public String getDate()
        {
            return date;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Annotation that = (Annotation) o;

            if (!description.equals(that.description)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return description.hashCode();
        }

        @Override
        public String toString()
        {
            return getDescription();
        }
    }

    public static class AnnotationType
    {
        private String name;
        private String description;
        private String color;

        public AnnotationType(String name, String description, String color)
        {
            this.name = name;
            this.description = description;
            this.color = color;
        }

        public AnnotationType(String name, String color)
        {
            this.name = name;
            this.color = color;
        }

        public String getName()
        {
            return name;
        }

        public String getDescription()
        {
            return description;
        }

        public String getColor()
        {
            return color;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AnnotationType that = (AnnotationType) o;

            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }
}
