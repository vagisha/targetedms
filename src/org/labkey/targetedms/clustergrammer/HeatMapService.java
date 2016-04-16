package org.labkey.targetedms.clustergrammer;


import org.springframework.validation.BindException;

/**
 * Created by iansigmon on 4/7/16.
 */
public interface HeatMapService
{
    String generateHeatMap(HeatMap matrix, BindException errors) throws Exception;
}
