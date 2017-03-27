/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.targetedms.view;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.targetedms.calculations.quantification.CalibrationCurve;
import org.labkey.targetedms.calculations.quantification.SampleType;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableSelector;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.calculations.ReplicateDataSet;
import org.labkey.targetedms.calculations.RunQuantifier;
import org.labkey.targetedms.parser.CalibrationCurveEntity;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.QuantificationSettings;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Creates a calibration curve chart. In the future, the chart should be displayed alongside controls which allow
 *  users to customize which {@link SampleType}s are displayed.
 */
public class CalibrationCurveChart
{
    User _user;
    Container _container;
    TargetedMSController.CalibrationCurveForm _form;
    public CalibrationCurveChart(User user, Container container, TargetedMSController.CalibrationCurveForm form) {
        _user = user;
        _container = container;
        _form = form;
    }

    public JSONObject getCalibrationCurveData()
    {
        TargetedMSRun run = TargetedMSManager.getRun(_form.getId());
        RunQuantifier runQuantifier = new RunQuantifier(run, _user, _container);
        CalibrationCurveEntity calibrationCurve = new TableSelector(TargetedMSManager.getTableInfoCalibrationCurve())
                .getObject(_form.getCalibrationCurveId(), CalibrationCurveEntity.class);
        QuantificationSettings quantificationSettings = new TableSelector(TargetedMSManager.getTableInfoQuantificationSettings())
                .getObject(_container, calibrationCurve.getQuantificationSettingsId(), QuantificationSettings.class);

        GeneralMolecule generalMolecule = PeptideManager.getPeptide(_container, calibrationCurve.getGeneralMoleculeId());
        if (generalMolecule == null) {
            generalMolecule = MoleculeManager.getMolecule(_container, calibrationCurve.getGeneralMoleculeId());
        }
        List<GeneralMoleculeChromInfo> chromInfos = new ArrayList<>();
        CalibrationCurve recalcedCalibrationCurve
                = runQuantifier.calculateCalibrationCurve(quantificationSettings, generalMolecule, chromInfos);

        return processCalibrationCurveJson(generalMolecule, runQuantifier.getReplicateDataSet(), recalcedCalibrationCurve, chromInfos);

    }

    private JSONObject processCalibrationCurveJson(GeneralMolecule molecule, ReplicateDataSet replicateDataSet, CalibrationCurve calibrationCurve, Iterable<GeneralMoleculeChromInfo> chromInfos)
    {
        JSONObject json = new JSONObject();
        Double maxX = 0.0, maxY = 0.0, minX = 0.0, minY = 0.0;

        // Molecule data
        JSONObject jsonMolecule = new JSONObject();
        jsonMolecule.put("name", molecule.getTextId());


        // Get calibration curve data
        JSONObject jsonCurve = new JSONObject();
        jsonCurve.put("slope", calibrationCurve.getSlope());
        jsonCurve.put("intercept", calibrationCurve.getIntercept());
        jsonCurve.put("count", calibrationCurve.getPointCount());
        jsonCurve.put("rSquared", calibrationCurve.getRSquared());
        jsonCurve.put("quadraticCoefficient", calibrationCurve.getQuadraticCoefficient());


        // Get data points
        JSONArray jsonPoints = new JSONArray();
        for (GeneralMoleculeChromInfo chromInfo : chromInfos)
        {
            Replicate replicate = replicateDataSet.getReplicateFromSampleFileId(chromInfo.getSampleFileId());
            if (replicate == null)
                continue;

            SampleType sampleType = SampleType.fromName(replicate.getSampleType());
            if (sampleType == SampleType.blank || sampleType == SampleType.solvent || sampleType == SampleType.double_blank)
                continue;

            JSONObject point = new JSONObject();
            Double y = calibrationCurve.getY(chromInfo.getCalculatedConcentration());
            Double x = replicate.getAnalyteConcentration();
            if (x != null)
            {
                if (molecule.getConcentrationMultiplier() != null)
                {
                    x *= molecule.getConcentrationMultiplier();
                }
            }
            else
            {
                x = chromInfo.getCalculatedConcentration();
            }

            if( y > maxY)
                maxY = y;

            if( x > maxX )
                maxX = x;

            if( x < minX )
                minX = x;

            if( y < minY )
                minY = y;

            point.put("x", x);
            point.put("y", y);
            point.put("type", sampleType.toString());
            point.put("name", replicate.getName());

            jsonPoints.put(point);
        }

        jsonCurve.put("maxX", maxX);
        jsonCurve.put("maxY", maxY);

        json.put("molecule", jsonMolecule);
        json.put("calibrationCurve", jsonCurve);
        json.put("dataPoints", jsonPoints);

        return json;
    }

    public JFreeChart getChart() {
        TargetedMSRun run = TargetedMSManager.getRun(_form.getId());
        RunQuantifier runQuantifier = new RunQuantifier(run, _user, _container);
        CalibrationCurveEntity calibrationCurve = new TableSelector(TargetedMSManager.getTableInfoCalibrationCurve())
                .getObject(_form.getCalibrationCurveId(), CalibrationCurveEntity.class);
        QuantificationSettings quantificationSettings = new TableSelector(TargetedMSManager.getTableInfoQuantificationSettings())
            .getObject(_container, calibrationCurve.getQuantificationSettingsId(), QuantificationSettings.class);

        GeneralMolecule generalMolecule = PeptideManager.getPeptide(_container, calibrationCurve.getGeneralMoleculeId());
        if (generalMolecule == null) {
            generalMolecule = MoleculeManager.getMolecule(_container, calibrationCurve.getGeneralMoleculeId());
        }
        List<GeneralMoleculeChromInfo> chromInfos = new ArrayList<>();
        CalibrationCurve recalcedCalibrationCurve
                = runQuantifier.calculateCalibrationCurve(quantificationSettings, generalMolecule, chromInfos);
        XYDataset xyDataset = makeXYSeriesCollection(generalMolecule, runQuantifier.getReplicateDataSet(), recalcedCalibrationCurve, chromInfos);
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setDotHeight(5);
        renderer.setDotWidth(5);
        for (int iSeries = 0; iSeries < xyDataset.getSeriesCount(); iSeries++) {
            SampleType sampleType = (SampleType) xyDataset.getSeriesKey(iSeries);
            renderer.setSeriesPaint(iSeries, sampleType.getColor());
        }
        XYPlot xyPlot = new XYPlot(xyDataset, new NumberAxis("Calculated Concentration"), new NumberAxis("Normalized Area"), renderer);
        JFreeChart chart = new JFreeChart("Calibration Curve: " + generalMolecule.getTextId(), xyPlot);
        return chart;
    }

    private XYSeriesCollection makeXYSeriesCollection(GeneralMolecule molecule, ReplicateDataSet replicateDataSet, CalibrationCurve calibrationCurve, Iterable<GeneralMoleculeChromInfo> chromInfos) {
        Map<SampleType, XYSeries> seriesMap = new HashMap<>();
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        for (GeneralMoleculeChromInfo chromInfo : chromInfos) {
            Replicate replicate = replicateDataSet.getReplicateFromSampleFileId(chromInfo.getSampleFileId());
            if (replicate == null) {
                continue;
            }
            SampleType sampleType = SampleType.fromName(replicate.getSampleType());
            if (sampleType == SampleType.blank || sampleType == SampleType.solvent || sampleType == SampleType.double_blank) {
                continue;
            }
            XYSeries xySeries = seriesMap.get(sampleType);
            if (xySeries == null) {
                xySeries = new XYSeries(sampleType);
                xySeriesCollection.addSeries(xySeries);
                seriesMap.put(sampleType, xySeries);
            }
            Double y = calibrationCurve.getY(chromInfo.getCalculatedConcentration());
            Double x = replicate.getAnalyteConcentration();
            if (x != null) {
                if (molecule.getConcentrationMultiplier() != null) {
                    x *= molecule.getConcentrationMultiplier();
                }
            } else {
                x = chromInfo.getCalculatedConcentration();
            }
            xySeries.add(x, y);
        }
        return xySeriesCollection;
    }
}
