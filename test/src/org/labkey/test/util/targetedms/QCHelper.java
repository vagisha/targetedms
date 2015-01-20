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

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Annotation that = (Annotation) o;

            if (!date.equals(that.date)) return false;
            if (!description.equals(that.description)) return false;
            if (!type.equals(that.type)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = type.hashCode();
            result = 31 * result + description.hashCode();
            result = 31 * result + date.hashCode();
            return result;
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

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AnnotationType that = (AnnotationType) o;

            if (!color.equals(that.color)) return false;
            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = name.hashCode();
            result = 31 * result + color.hashCode();
            return result;
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
    }
}
