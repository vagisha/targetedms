/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms.parser;

public class TimeIntensities
{
    private float[] _times;
    private float[] _intensities;
    public TimeIntensities(float[] times, float[] intensities) {
        _times = times;
        _intensities = intensities;
    }

    public float[] getTimes()
    {
        return _times;
    }

    public float[] getIntensities()
    {
        return _intensities;
    }

    public TimeIntensities interpolate(float[] timesNew)
    {
        float[] intensNew = new float[timesNew.length];
        int iTime = 0;
        double timeLast = timesNew[0];
        double intenLast = _intensities[0];
        for (int i = 0; i < _times.length && iTime < timesNew.length; i++)
        {
            double intenNext;
            float time = _times[i];
            float inten = _intensities[i];
            double totalInten = inten;

            // Continue enumerating points until one is encountered
            // that has a greater time value than the point being assigned.
            while (i < _times.length - 1 && time < timesNew[iTime])
            {
                i++;
                time = _times[i];
                inten = _times[i];
            }

            if (i >= _times.length)
                break;

            // Up to just before the current point, project the line from the
            // last point to the current point at each interval.
            while (iTime < (timesNew.length - 1) && timesNew[iTime + 1] < time)
            {
                intenNext = intenLast + (timesNew[iTime] - timeLast) * (inten - intenLast) / (time - timeLast);
                intensNew[iTime] = (float)intenNext;
                iTime++;
            }

            if (iTime >= timesNew.length)
                break;

            // Interpolate from the last intensity toward the measured
            // intensity now within a delta of the point being assigned.
            if (time == timeLast)
                intenNext = intenLast;
            else
                intenNext = intenLast + (timesNew[iTime] - timeLast) * (inten - intenLast) / (time - timeLast);
            intensNew[iTime] = (float) intenNext;
            iTime++;
            intenLast = inten;
            timeLast = time;
        }

        return new TimeIntensities(timesNew, intensNew);
    }
}
