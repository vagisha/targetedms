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

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableSelector;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.calculations.ReplicateDataSet;
import org.labkey.targetedms.calculations.RunQuantifier;
import org.labkey.targetedms.calculations.quantification.CalibrationCurve;
import org.labkey.targetedms.calculations.quantification.SampleType;
import org.labkey.targetedms.parser.CalibrationCurveEntity;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.QuantificationSettings;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.PeptideManager;

import java.util.ArrayList;
import java.util.List;

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

    @Nullable
    public JSONObject getCalibrationCurveData()
    {
        TargetedMSRun run = TargetedMSManager.getRun(_form.getId());
        JSONObject json = null;

        if(null != run)
        {
            RunQuantifier runQuantifier = new RunQuantifier(run, _user, _container);
            CalibrationCurveEntity calibrationCurve = new TableSelector(TargetedMSManager.getTableInfoCalibrationCurve())
                    .getObject(_form.getCalibrationCurveId(), CalibrationCurveEntity.class);

            if (null != calibrationCurve)
            {
                QuantificationSettings quantificationSettings = new TableSelector(TargetedMSManager.getTableInfoQuantificationSettings())
                        .getObject(_container, calibrationCurve.getQuantificationSettingsId(), QuantificationSettings.class);

                GeneralMolecule generalMolecule = PeptideManager.getPeptide(_container, calibrationCurve.getGeneralMoleculeId());
                if (generalMolecule == null)
                {
                    generalMolecule = MoleculeManager.getMolecule(_container, calibrationCurve.getGeneralMoleculeId());
                }
                List<GeneralMoleculeChromInfo> chromInfos = new ArrayList<>();
                CalibrationCurve recalcedCalibrationCurve
                        = runQuantifier.calculateCalibrationCurve(quantificationSettings, generalMolecule, chromInfos);

                json = processCalibrationCurveJson(generalMolecule, runQuantifier.getReplicateDataSet(), recalcedCalibrationCurve, chromInfos);
            }
        }

        return json;
    }

    private JSONObject processCalibrationCurveJson(GeneralMolecule molecule, ReplicateDataSet replicateDataSet, CalibrationCurve calibrationCurve, Iterable<GeneralMoleculeChromInfo> chromInfos)
    {
        JSONObject json = new JSONObject();
        Double maxX = null, maxY = null, minY = null;

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

            if (maxY == null || y > maxY)
                maxY = y;

            if (maxX == null || x > maxX)
                maxX = x;

            if (minY == null || y < minY)
                minY = y;

            point.put("x", x);
            point.put("y", y);
            point.put("type", sampleType.toString());
            point.put("name", replicate.getName());

            jsonPoints.put(point);
        }

        jsonCurve.put("maxX", maxX);
        jsonCurve.put("maxY", maxY);
        jsonCurve.put("minY", minY);

        json.put("molecule", jsonMolecule);
        json.put("calibrationCurve", jsonCurve);
        json.put("dataPoints", jsonPoints);

        return json;
    }
}
