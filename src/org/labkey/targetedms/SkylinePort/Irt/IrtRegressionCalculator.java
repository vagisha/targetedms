package org.labkey.targetedms.SkylinePort.Irt;

import org.apache.log4j.Logger;
import org.labkey.targetedms.IrtPeptide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 5/23/2014
 *
 * Ported from the Skyline C# code:
 * https://svn.code.sf.net/p/proteowizard/code/trunk/pwiz/pwiz_tools/Skyline/
 * See README.txt for more info.
 * "TPG" comments are tgaluhn's; others are the comments from the original source code.
 *
 * The methods and classes in this file were pulled from Skyline EditIrtCalcDlg.cs
 * Original license info:
 *
 *
 // * Original author: John Chilton <jchilton .at. uw.edu>,</jchilton>
 //                    Brendan MacLean <brendanx .at. u.washington.edu>,
 // *                  MacCoss Lab, Department of Genome Sciences, UW
 // *
 // * Copyright 2011 University of Washington - Seattle, WA
 // *
 // * Licensed under the Apache License, Version 2.0 (the "License");
 // * you may not use this file except in compliance with the License.
 // * You may obtain a copy of the License at
 // *
 // *     http://www.apache.org/licenses/LICENSE-2.0
 // *
 // * Unless required by applicable law or agreed to in writing, software
 // * distributed under the License is distributed on an "AS IS" BASIS,
 // * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // * See the License for the specific language governing permissions and
 // * limitations under the License.
 //
 */
public class IrtRegressionCalculator
{
    private static final double MIN_IRT_TO_TIME_CORRELATION = 0.99;
    private static final int MIN_IRT_TO_TIME_POINT_COUNT = 20;

    public static IRegressionFunction calcRegressionLine(IRetentionTimeProvider retentionTimes, ArrayList<IrtPeptide> standardPeptideList, ArrayList<IrtPeptide> existingPeptideList, Logger pipelineLog)
    {
        // TPG NOTE: standardPeptides to be sorted by irt times ?

        ArrayList<Double> listTimes = new ArrayList<>();
        ArrayList<Double> listIrts = new ArrayList<>(); // TPG declaration & population mod fromLINQ syntax in C#
        for (IrtPeptide standardPeptide : standardPeptideList)
        {
            // listIrts.add(standardPeptide.getiRTValue()); // TPG declaration & population mod fromLINQ syntax in C#

            Double time = retentionTimes.GetRetentionTime(standardPeptide.getModifiedSequence());
            if (time == null)
            {
                continue;
            }
            listTimes.add(time);
            listIrts.add(standardPeptide.getiRTValue()); // TPG declaration & population mod fromLINQ syntax in C#
        }

        if (CurrentCalculator.IsAcceptableStandardCount(standardPeptideList.size(), listTimes.size()) /*listTimes.size() == standardPeptideList.size()*/)
        {
            Statistics statTimes = new Statistics(listTimes);
            Statistics statIrts = new Statistics(listIrts);
            double correlation = statIrts.R(statTimes);
            // If the correlation is not good enough, try removing one value to
            // fix the problem.)
            if (correlation < MIN_IRT_TO_TIME_CORRELATION)
            {
                Double time = null, irt = null;
                for (int i = 0; i < listTimes.size(); i++)
                {
                    RefObject<Double> tempRef_time = new RefObject<>(time);
                    statTimes = GetTrial(listTimes, i, tempRef_time);
                    time = tempRef_time.get();
                    RefObject<Double> tempRef_irt = new RefObject<>(irt);
                    statIrts = GetTrial(listIrts, i, tempRef_irt);
                    irt = tempRef_irt.get();
                    correlation = statIrts.R(statTimes);
                    if (correlation >= MIN_IRT_TO_TIME_CORRELATION)
                    {
                        pipelineLog.info("Calculated iRT regression line by ignoring import value for standard: " + standardPeptideList.get(i).getModifiedSequence());
                        break;
                    }
                }
            }
            else
                pipelineLog.info("Calculated iRT regression line from full standard list.");
            if (correlation >= MIN_IRT_TO_TIME_CORRELATION)
            {
                return new RegressionLine(statIrts.Slope(statTimes), statIrts.Intercept(statTimes));
            }
        }

        if (existingPeptideList.size() > 0)
        {
            pipelineLog.info("Attempting to calculate iRT regression line from shared peptides.");
            // Attempt to get a regression based on shared peptides
            CurrentCalculator calculator = new CurrentCalculator(standardPeptideList, existingPeptideList);
            ArrayList<MeasuredRetentionTime> peptidesTimes = retentionTimes.getPeptideRetentionTimes();
            //var peptidesTimes = retentionTimes.getPeptideRetentionTimes().toArray();
            RetentionTimeRegression regression = RetentionTimeRegression.FindThreshold(MIN_IRT_TO_TIME_CORRELATION, RetentionTimeRegression.getThresholdPrecision(), peptidesTimes, new ArrayList<MeasuredRetentionTime>(), peptidesTimes, calculator, false);

            if (regression != null && regression.getPeptideTimes().size() >= MIN_IRT_TO_TIME_POINT_COUNT)
            {
                // Finally must recalculate the regression, because it is transposed from what
                // we want.
                ArrayList<Double> regressionTimes = new ArrayList<>();
                ArrayList<Double> regressionScores = new ArrayList<>();
                for (MeasuredRetentionTime mrt : regression.getPeptideTimes())
                {
                    regressionTimes.add(mrt.getRetentionTime());
                    Double tempVar = calculator.ScoreSequence(mrt.getPeptideSequence());
                    regressionScores.add(tempVar != null ? tempVar : calculator.getUnknownScore());
                }
                Statistics statTimes = new Statistics(regressionTimes);
                Statistics statIrts = new Statistics(regressionScores);
                pipelineLog.info("Successfully calculated iRT regression line from " + Integer.toString(regression.getPeptideTimes().size()) + " shared peptides.");
                return new RegressionLine(statIrts.Slope(statTimes), statIrts.Intercept(statTimes));
            }
        }

        return null;
    }

    private static Statistics GetTrial(ArrayList<Double> listValues, int i, RefObject<Double> valueReplace)
    {
        if (valueReplace.get() != null)
        {
            listValues.add(i-1, valueReplace.get());
        }
        valueReplace.set(listValues.get(i));
        listValues.remove(i);
        return new Statistics(listValues);
    }

    private final static class CurrentCalculator extends RetentionScoreCalculatorSpec
    {
        private LinkedHashMap<String, Double> _dictStandards = new LinkedHashMap<>();
        private LinkedHashMap<String, Double> _dictLibrary = new LinkedHashMap<>();

        private double _unknownScore;

        public CurrentCalculator(Iterable<IrtPeptide> standardPeptides, Iterable<IrtPeptide> libraryPeptides)
        {
            for (IrtPeptide pep : standardPeptides)
            {
                _dictStandards.put(pep.getModifiedSequence(), pep.getiRTValue());
            }
            for (IrtPeptide pep : libraryPeptides)
            {
                _dictLibrary.put(pep.getModifiedSequence(), pep.getiRTValue());
            }

            double minStandard = Collections.min(_dictStandards.values());
            double minLibrary = Collections.min(_dictLibrary.values());

            // Come up with a value lower than the lowest value, but still within the scale
            // of the measurements.
            _unknownScore = Math.min(minStandard, minLibrary) - Math.abs(minStandard - minLibrary);
        }

        @Override
        public String getName()
        {
            return "__INTERNAL__";
        }

        @Override
        public double getUnknownScore()
        {
            return _unknownScore;
        }

        @Override
        public Double ScoreSequence(String sequence)
        {
            Double irt = _dictStandards.get(sequence);
            if (irt == null)
            {
                irt = _dictLibrary.get(sequence);
            }
            return irt;
        }

        // Merge in changes from https://svn.code.sf.net/p/proteowizard/code/trunk/pwiz/pwiz_tools/Skyline revision 9282
        public static final double MIN_PEPTIDES_PERCENT = 0.80;
        public static final int MIN_PEPTIDES_COUNT = 8;

        public static int MinStandardCount(int expectedCount)
        {
            return expectedCount <= MIN_PEPTIDES_COUNT
                    ? expectedCount
                    : Math.max(MIN_PEPTIDES_COUNT, (int) (expectedCount * MIN_PEPTIDES_PERCENT));
        }

        public static boolean IsAcceptableStandardCount(int expectedCount, int actualCount)
        {
            return actualCount >= MinStandardCount(expectedCount);
        }

        @Override
        public ArrayList<String> ChooseRegressionPeptides(Iterable<String> peptides)
        {
            ArrayList<String> returnStandard = new ArrayList<>();

            for (String sequence : peptides)
            {
                if (_dictStandards.containsKey(sequence))
                    returnStandard.add(sequence);
            }

            int returnCount = returnStandard.size();
            int databaseCount = _dictLibrary.size();

            if (!IsAcceptableStandardCount(databaseCount, returnCount))
                throw new IncompleteStandardException(this);

            return returnStandard;
        }

        @Override
        public ArrayList<String> GetStandardPeptides(Iterable<String> peptides)
        {
            return new ArrayList(_dictStandards.keySet());
        }
    }

}
