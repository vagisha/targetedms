/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.targetedms;

/**
 * Created by Ron on 7/11/2016.
 */
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.targetedms.model.QCMetricConfiguration;

import java.util.Date;

public class TargetMSControllerUnitTest
{
//    @Test
//    public void testGetCandidateContainer(){
//        MockTargetedMSController mockTargetedMSController = new MockTargetedMSController();
//
//        Container rootContainer = new MockContainer(null,"Root", "0",0,0, new  Date(),0,false);
//        Container testContainer = new MockContainer(rootContainer, "Test", "1",1,1,new Date(),0,true);
//
//        Container result = mockTargetedMSController.getCandidateContainer(testContainer,null);
//        Assert.assertEquals(result,testContainer);
//
//    }
    @Test
    public void testQCMetricConfiguration_toJSON(){

        JSONObject expected=new JSONObject();
        String testName = "testName";
        String testSeries1Label = "testSeries1Label";
        String testSeries1SchemaName = "testSeries1SchemaName";
        String testSeries1QueryName = "testSeries1QueryName";
        String testSeries2SchemaName = "testSeries2SchemaName";
        String testSeries2Label = "testSeries2Label";
        String testSeries2QueryName = "testSeries2QueryName";

        expected.put("name", testName);
        expected.put("series1Label", testSeries1Label);
        expected.put("series1SchemaName", testSeries1SchemaName);
        expected.put("series1QueryName", testSeries1QueryName);
        QCMetricConfiguration result = new QCMetricConfiguration();
        result.setName(testName);
        result.setSeries1Label(testSeries1Label);
        result.setSeries1SchemaName(testSeries1SchemaName);
        result.setSeries1QueryName(testSeries1QueryName);

        Assert.assertEquals(expected,result.toJSON());

        expected.put("series2Label", testSeries2Label);
        expected.put("series2SchemaName", testSeries2SchemaName);
        expected.put("series2QueryName", testSeries2QueryName);
        result.setSeries2Label(testSeries2Label);
        result.setSeries2SchemaName(testSeries2SchemaName);
        result.setSeries2QueryName(testSeries2QueryName);

        Assert.assertEquals(expected,result.toJSON());


    }

    private class MockTargetedMSController extends TargetedMSController
    {

        @Override
        protected Container getContainer()
        {
            return super.getContainer();
        }
    }

    private class MockContainer extends Container{

        protected MockContainer(Container dirParent, String name, String id, int rowId, int sortOrder, Date created, int createdBy, boolean searchable)
        {
            super(dirParent, name, id, rowId, sortOrder, created, createdBy, searchable);
        }


    }
}
