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
package org.labkey.test.components.targetedms;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.targetedms.QCHelper;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QCPlot
{
    WebElement plot;
    String precursor;
    Map<String, QCHelper.AnnotationType> annotationTypes;

    QCPlot(WebElement plot)
    {
        this.plot = plot;
        this.precursor = elements().precursor.findElement(plot).getText().trim();
    }

    public String getPrecursor()
    {
        return precursor;
    }

    public List<QCHelper.Annotation> getAnnotations()
    {
        List<WebElement> annotationEls = elements().annotation.findElements(plot);
        List<QCHelper.Annotation> annotations = new ArrayList<>();

        for (WebElement annotationEl : annotationEls)
        {
            annotations.add(parseAnnotation(annotationEl));
        }

        return annotations;
    }

    private QCHelper.Annotation parseAnnotation(WebElement annotationEl)
    {
        String annotationString = annotationEl.getText();
        String annotationRegex = "Created By: (.+)\\s*, " +
                "Date: (\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d)\\s*, " +
                "Description: (.+)";
        Pattern annotationPattern = Pattern.compile(annotationRegex, Pattern.MULTILINE);
        Matcher annotationMatcher = annotationPattern.matcher(annotationString);

        Assert.assertTrue(annotationString, annotationMatcher.find());
        String date = annotationMatcher.group(2);
        String description = annotationMatcher.group(3);
        String color = annotationEl.getCssValue("fill");
        QCHelper.AnnotationType type = getAnnotationTypes().get(color);

        return new QCHelper.Annotation(type.getName(), description);
    }

    private Map<String, QCHelper.AnnotationType> getAnnotationTypes()
    {
        if (annotationTypes == null)
        {
            annotationTypes = new HashMap<>();

            List<WebElement> legendItems = elements().legendItem.findElements(plot);

            for (WebElement legendItem : legendItems)
            {
                String annotationType = Locator.css("tspan").findElement(legendItem).getText();
                String color = Locator.css("path").findElement(legendItem).getCssValue("fill");

                annotationTypes.put(color, new QCHelper.AnnotationType(annotationType, color));
            }
        }

        return annotationTypes;
    }

    public String getSvgText()
    {
        WebElement svg = elements().svg.findElement(plot);
        return svg.getText();
    }

    private Elements elements()
    {
        return new Elements();
    }

    private class Elements
    {
        Locator precursor = Locator.css(".labkey-wp-title-text");
        Locator.CssLocator svg = Locator.css("svg");
        Locator annotation = svg.append(Locator.css(".annotation"));
        Locator legendItem = svg.append(Locator.css(".legend-item"));
    }
}
