package org.labkey.targetedms.clustergrammer;

import com.google.common.io.Files;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.labkey.api.util.FileUtil;
import org.springframework.validation.BindException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by iansigmon on 4/7/16.
 */
public class ClustergrammerClient implements HeatMapService
{
    //File based endpoint
    private static final String CLUSTERGRAMMER_ENDPOINT = "http://amp.pharm.mssm.edu/clustergrammer/matrix_upload/";
    //JSON based endpoint
    private static final String CLUSTERGRAMMER_JSON_ENDPOINT = "http://amp.pharm.mssm.edu/clustergrammer/vector_upload/";

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
                    errors.addSuppressed(new HttpResponseException(status.getStatusCode(), status.getReasonPhrase()));
                }
            }
        }

        return null;
    }

    public HttpEntity generateHeatMapViaJSON(HeatMap matrix) throws  Exception
    {
        return new StringEntity(serializeToJSON(matrix), ContentType.APPLICATION_JSON);
    }

    public HttpEntity generateHeatMapViaFile(HeatMap matrix) throws Exception
    {
        File file = serializeToFile(matrix);
        return new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM);
    }

    //Assumes row maps contain columns in same order
    private File serializeToFile(HeatMap hm) throws FileNotFoundException
    {
        File streamFile = new File(FileUtil.getTempDirectory(), FileUtil.makeFileNameWithTimestamp("clustergrammer", "tsv"));

        Map<String, Map<String, Double>> matrix = hm.getMatrix();

        BufferedWriter writer = Files.newWriter(streamFile, StandardCharsets.UTF_8);

        //TODO: serialize matrix to TSV
        throw new NotImplementedException("");
    }

    private String serializeToJSON(HeatMap hm)
    {
        Map<String, Map<String, Double>> matrix = hm.getMatrix();
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("\"title\": \"" + hm.getTitle() + "\",");
        sb.append("\"columns\":[");

        String comma = "";
        for (String rowKey : matrix.keySet())
        {
            Map<String, Double> rowMap = matrix.get(rowKey);
            sb.append(comma);
            comma = ",";

            sb.append("{");
            sb.append("\"col_name\": \"" + rowKey + "\"");
            sb.append(comma);
            sb.append("\"data\":[");

            serializeRow(sb, rowMap);
            sb.append("]}");
        }

        sb.append("]}\n");

        return sb.toString();
    }

    private void serializeRow(StringBuilder sb, Map<String, Double> rowMap)
    {
        String comma = "";
        for (String rowKey : rowMap.keySet())
        {
            Double value = rowMap.get(rowKey);
            if (value == null)
                continue;

            sb.append(comma);
            comma = ",";

            sb.append("{ ");
            sb.append("\"val\":" + value);
            sb.append(comma);
            sb.append("\"row_name\": \"" + rowKey + "\"");
            sb.append("} ");
        }
    }

}
