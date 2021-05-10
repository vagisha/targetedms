package org.labkey.targetedms.view;

import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.view.JspView;

public class CalibrationCurveView extends JspView<JSONObject>
{
    private final CalibrationCurveChart _chart;

    public CalibrationCurveView(User user, Container container, long calibrationCurveId)
    {
        super("/org/labkey/targetedms/view/calibrationCurve.jsp");
        _chart = new CalibrationCurveChart(user, container, calibrationCurveId);
        setModelBean(_chart.getCalibrationCurveData());
        setTitle("Calibration Curve");
    }

    public CalibrationCurveChart getChart()
    {
        return _chart;
    }
}
