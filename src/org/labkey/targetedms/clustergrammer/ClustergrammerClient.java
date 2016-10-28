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
package org.labkey.targetedms.clustergrammer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.validation.BindException;

import java.util.Map;

import static org.labkey.api.action.SpringActionController.ERROR_MSG;

/**
 * Created by iansigmon on 4/7/16.
 */
public class ClustergrammerClient implements HeatMapService
{
    //JSON based endpoint
    private static final String CLUSTERGRAMMER_JSON_ENDPOINT = "http://amp.pharm.mssm.edu/clustergrammer/vector_upload/";

    private static final String CG_JSON_TEMPLATE = "{\"title\":\"%s\", \"columns\":[%s]}";
    private static final String COLUMN_TEMPLATE = "{\"col_name\":\"%s\", \"data\":[%s]}";
    private static final String VALUE_TEMPLATE = "{\"val\":%E, \"row_name\":\"%s\" }";


    //Example response JSON
    //    {
    //        "id": "570c220b9238d045ff38551f",
    //        "link": "http://amp.pharm.mssm.edu/clustergrammer/viz/570c220b9238d045ff38551f/test"
    //    }

    @Override
    public String generateHeatMap(HeatMap matrix, BindException errors) throws Exception
    {
        try (CloseableHttpClient httpclient = HttpClients.createDefault())
        {
            HttpPost post = new HttpPost(CLUSTERGRAMMER_JSON_ENDPOINT);

            HttpEntity entity = generateHeatMapViaJSON(matrix);
            post.setEntity(entity);

            try (CloseableHttpResponse response = httpclient.execute(post))
            {
                ResponseHandler<String> handler = new BasicResponseHandler();
                StatusLine status = response.getStatusLine();

                if (status.getStatusCode() == HttpStatus.SC_OK || status.getStatusCode() == HttpStatus.SC_CREATED)
                {
                    String resp = handler.handleResponse(response);
                    JSONObject json = new JSONObject(resp);
                    return (String)json.get("link");
                }
                else
                {
                    EntityUtils.consume(response.getEntity());
                    errors.reject(ERROR_MSG, "Request to Clustergrammer failed:\n " + status.getStatusCode() +": " + status.getReasonPhrase());
                }
            }
        }

        return null;
    }

    public HttpEntity generateHeatMapViaJSON(HeatMap matrix) throws  Exception
    {
        return new StringEntity(serializeToJSON(matrix), ContentType.APPLICATION_JSON);
    }

    private String serializeToJSON(HeatMap hm)
    {
        return String.format(CG_JSON_TEMPLATE, hm.getTitle(), serializeMatrix(hm.getMatrix()));
    }

    private String serializeMatrix(Map<String, Map<String, Double>> matrix)
    {
        StringBuilder sb = new StringBuilder();

        String comma = "";
        for (String rowKey : matrix.keySet())
        {
            Map columnMap = matrix.get(rowKey);
            sb.append(comma);
            comma = ",";

            sb.append(String.format(COLUMN_TEMPLATE, rowKey, serializeColumn(columnMap)));
        }

        return sb.toString();
    }

    private String serializeColumn(Map<String, Double> rowMap)
    {
        StringBuilder sb = new StringBuilder();

        String comma = "";
        for (String rowKey : rowMap.keySet())
        {
            Double value = rowMap.get(rowKey);
            if (value == null)
                continue;

            sb.append(comma);
            comma = ",";
            sb.append(String.format(VALUE_TEMPLATE, value, rowKey));
        }

        return sb.toString();
    }

}
