package org.labkey.targetedms.SkylinePort.Irt;

//
// * Original author: Brendan MacLean <brendanx .at. u.washington.edu>,
// *                  MacCoss Lab, Department of Genome Sciences, UW
// *
// * Copyright 2009 University of Washington - Seattle, WA
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

/*
 * Ported from the Skyline C# code:
 * https://svn.code.sf.net/p/proteowizard/code/trunk/pwiz/pwiz_tools/Skyline/
 * See README.txt for more info.
 * "TPG" comments are tgaluhn's; others are the comments from the original source code.
 */

import com.google.common.collect.Lists;
import org.apache.commons.math3.util.Precision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 
 Describes a slope and intercept for converting from a
 hydrophobicity factor to a predicted retention time in minutes.
 
*/
//[XmlRoot("predict_retention_time")]
public final class RetentionTimeRegression
{
	public static Double GetRetentionTimeDisplay(Double rt)
	{
		if (rt == null)
		{
			return null;
		}

		return Precision.round(rt, 2);
	}

	private RetentionScoreCalculatorSpec _calculator;

//	// Support for auto-calculate regression
//	private ImmutableSortedList<Integer, RegressionLine> _listFileIdToConversion;
//	// Peptide standards used to derive the above conversions
//	private ImmutableDictionary<Integer, PeptideDocNode> _dictStandardPeptides;
	// May be true when the dictionary is null, because peptides were missing
	private boolean _isMissingStandardPeptides;

	private List<MeasuredRetentionTime> _peptidesTimes = new ArrayList<>();

    public String getName()
    {
        return "predict_retention_time";
    }

	public RetentionTimeRegression(String name, RetentionScoreCalculatorSpec calculator, Double slope, Double intercept, double window, ArrayList<MeasuredRetentionTime> peptidesTimes)
	{
//		super(name);
		setTimeWindow(window);
		if (slope != null && intercept != null)
		{
			setConversion(new RegressionLineElement(slope, intercept));
		}
		else if (slope != null || intercept != null)
		{
		//	throw new RuntimeException("RetentionTimeRegression_Slope_and_intercept_must_both_have_values_or_both_not_have_values");
            // TPG Was C# InvalidDataException
		}
		setPeptideTimes(peptidesTimes);

		_calculator = calculator;

		Validate();
	}

	public RetentionScoreCalculatorSpec getCalculator()
	{
		return _calculator;
	}
	private void setCalculator(RetentionScoreCalculatorSpec value)
	{
		_calculator = value;
	}

	private double privateTimeWindow;
	public double getTimeWindow()
	{
		return privateTimeWindow;
	}
	private void setTimeWindow(double value)
	{
		privateTimeWindow = value;
	}

	private RegressionLineElement privateConversion;
	public RegressionLineElement getConversion()
	{
		return privateConversion;
	}
	private void setConversion(RegressionLineElement value)
	{
		privateConversion = value;
	}

	public boolean IsUsable()
	{
		return getConversion() != null && getCalculator().IsUsable();
	}

//	public boolean getIsAutoCalculated()
//	{
//		return getConversion() == null || _listFileIdToConversion != null;
//	}

//	public boolean IsStandardPeptide(PeptideDocNode nodePep)
//	{
//		return _dictStandardPeptides != null && _dictStandardPeptides.ContainsKey(nodePep.getPeptide().getGlobalIndex());
//	}

	public List<MeasuredRetentionTime> getPeptideTimes()
	{
		return _peptidesTimes;
	}
	private void setPeptideTimes(ArrayList<MeasuredRetentionTime> value)
	{
		_peptidesTimes = Collections.unmodifiableList(value);
	}

	public Double GetRetentionTime(String seq)
	{
		return GetRetentionTime(seq, getConversion());
	}

//	public Double GetRetentionTime(String seq, ChromFileInfoId fileId)
//	{
//		return GetRetentionTime(seq, GetConversion(fileId));
//	}

	public Double GetRetentionTime(String seq, IRegressionFunction conversion)
	{
		Double score = getCalculator().ScoreSequence(seq);
		if (score != null)
		{
			return GetRetentionTime(score, conversion);
		}
		return null;
	}

	public Double GetRetentionTime(double score)
	{
		return GetRetentionTime(score, getConversion());
	}

//	public Double GetRetentionTime(double score, ChromFileInfoId fileId)
//	{
//		return GetRetentionTime(score, GetConversion(fileId));
//	}

	private static Double GetRetentionTime(double score, IRegressionFunction conversion)
	{
		// CONSIDER: Return the full value?
		return conversion != null ? GetRetentionTimeDisplay(conversion.GetY(score)) : null;
	}

//	public IRegressionFunction GetConversion(ChromFileInfoId fileId)
//	{
//		RegressionLine tempVar = GetRegressionFunction(fileId);
//		return (tempVar != null) ? tempVar : (IRegressionFunction) getConversion();
//	}
//
//	public IRegressionFunction GetUnconversion(ChromFileInfoId fileId)
//	{
//		double slope, intercept;
//		RegressionLine regressionLine = GetRegressionFunction(fileId);
//		if (null != regressionLine)
//		{
//			slope = regressionLine.getSlope();
//			intercept = regressionLine.getIntercept();
//		}
//		else if (null != getConversion())
//		{
//			slope = getConversion().getSlope();
//			intercept = getConversion().getIntercept();
//		}
//		else
//		{
//			return null;
//		}
//		return new RegressionLine(1.0/slope, -intercept/slope);
//	}
//
//	private RegressionLine GetRegressionFunction(ChromFileInfoId fileId)
//	{
//		RegressionLine conversion = null;
//		if (fileId != null && _listFileIdToConversion != null)
//		{
//			RefObject<RegressionLine> tempRef_conversion = new RefObject<RegressionLine>(conversion);
//			_listFileIdToConversion.TryGetValue(fileId.getGlobalIndex(), tempRef_conversion);
//			conversion = tempRef_conversion.argvalue;
//		}
//		return conversion;
//	}
//
//	public boolean IsAutoCalcRequired(SrmDocument document, SrmDocument previous)
//	{
//		// Any time there is no regression information, an auto-calc is required
//		// unless the document has no results
//		if (getConversion() == null)
//		{
//			if (!document.getSettings().getHasResults() && _dictStandardPeptides != null)
//			{
//				return false;
//			}
//
//			// If prediction settings have change, then do auto-recalc
//			if (previous == null || !ReferenceEquals(this, previous.getSettings().getPeptideSettings().getPrediction().getRetentionTime()))
//			{
//				// If it has already been determined that standard peptides are missing
//				// and no previous document is given, then no auto-recalc is required
//				if (previous == null && _isMissingStandardPeptides)
//				{
//					return document.HasAllRetentionTimeStandards(); // Recalc if all standards are now present
//				}
//				return true;
//			}
//
//			// Otherwise, only if any of the transition groups or their results
//			// have changed.  This is important to avoid an infinite loop when
//			// not enough information is present to actually calculate the Conversion
//			// parameter.
//			var enumPrevious = previous.getTransitionGroups().iterator();
//			for (var nodeGroup : document.getTransitionGroups())
//			{
//				if (!enumPrevious.MoveNext())
//				{
//					return true;
//				}
//				var nodeGroupPrevious = enumPrevious.Current;
//				if (nodeGroupPrevious == null)
//				{
//					return true;
//				}
//				if (!ReferenceEquals(nodeGroup.Id, nodeGroupPrevious.Id) || !ReferenceEquals(nodeGroup.Results, nodeGroupPrevious.Results))
//				{
//					return true;
//				}
//			}
//			return enumPrevious.MoveNext();
//		}
//
//		// If there is a documentwide regression, but no per-file information
//		// then no auto-calc is required.
//		if (_listFileIdToConversion == null || _dictStandardPeptides == null)
//		{
//			return false;
//		}
//
//		// If any of the standard peptides do not match exactly, then auto-calc
//		// is required.
//		int countMatching = 0;
//		for (var nodePep : document.getPeptides())
//		{
//			PeptideDocNode nodePepStandard = null;
//			RefObject<PeptideDocNode> tempRef_nodePepStandard = new RefObject<PeptideDocNode>(nodePepStandard);
//			boolean tempVar = !_dictStandardPeptides.TryGetValue(nodePep.Peptide.GlobalIndex, tempRef_nodePepStandard);
//				nodePepStandard = tempRef_nodePepStandard.argvalue;
//			if (tempVar)
//			{
//				continue;
//			}
//			if (!ReferenceEquals(nodePep, nodePepStandard))
//			{
//				return true;
//			}
//			countMatching++;
//		}
//		// Or any are missing.
//		return countMatching != _dictStandardPeptides.size();
//	}

	/** 
	 Calculate the correlation statistics for this regression with a set
	 of peptide measurements.
	 
	 @param peptidesTimes List of peptide-time pairs
	 @param scoreCache Cached pre-calculated scores for these peptides
	 @return Calculated values for the peptides using this regression
	*/
	public RetentionTimeStatistics CalcStatistics(ArrayList<MeasuredRetentionTime> peptidesTimes, java.util.Map<String, Double> scoreCache)
	{
		java.util.ArrayList<String> listPeptides = new java.util.ArrayList<String>();
		java.util.ArrayList<Double> listHydroScores = new java.util.ArrayList<Double>();
		java.util.ArrayList<Double> listPredictions = new java.util.ArrayList<Double>();
		java.util.ArrayList<Double> listRetentionTimes = new java.util.ArrayList<Double>();

		boolean usableCalc = getCalculator().IsUsable();

		for (MeasuredRetentionTime peptideTime : peptidesTimes)
		{
			String seq = peptideTime.getPeptideSequence();
			double score = usableCalc ? ScoreSequence(getCalculator(), scoreCache, seq) : 0;
			listPeptides.add(seq);
			listHydroScores.add(score);
			Double tempVar = GetRetentionTime(score);
			listPredictions.add((tempVar != null) ? tempVar : 0);
			listRetentionTimes.add(peptideTime.getRetentionTime());
		}

		Statistics statRT = new Statistics(listRetentionTimes);
		Statistics statScores = new Statistics(listHydroScores);
		double r = statRT.R(statScores);

		return new RetentionTimeStatistics(r, listPeptides, listHydroScores, listPredictions, listRetentionTimes);
	}

//	// Support for serializing multiple calculator types
//	private static final IXmlElementHelper<RetentionScoreCalculatorSpec>[] CALCULATOR_HELPERS = { new XmlElementHelperSuper<RetentionScoreCalculator, RetentionScoreCalculatorSpec>(), new XmlElementHelperSuper<RCalcIrt, RetentionScoreCalculatorSpec>() };
//
//	public static IXmlElementHelper<RetentionScoreCalculatorSpec>[] getCalculatorXmlHelpers()
//	{
//		return CALCULATOR_HELPERS;
//	}

//
//	/**
//	 For serialization
//
//	*/
//	private RetentionTimeRegression()
//	{
//	}
//
//	private enum ATTR
//	{
//		calculator,
//		time_window;
//
//		public int getValue()
//		{
//			return this.ordinal();
//		}
//
//		public static ATTR forValue(int value)
//		{
//			return values()[value];
//		}
//	}
//
//	private enum EL
//	{
//		regression_rt;
//
//		public int getValue()
//		{
//			return this.ordinal();
//		}
//
//		public static EL forValue(int value)
//		{
//			return values()[value];
//		}
//	}

	private void Validate()
	{
//		// TODO: Fix this hacky way of dealing with the default value.
//		if (getConversion() == null || getTimeWindow() + getConversion().getSlope() + getConversion().getIntercept() != 0 || getCalculator() != null)
//		{
//			if (getCalculator() == null)
//			{
//				throw new InvalidDataException(Resources.getRetentionTimeRegression_Validate_Retention_time_regression_must_specify_a_sequence_to_score_calculator());
//			}
//			if (getTimeWindow() <= 0)
//			{
//				throw new InvalidDataException(String.format(Resources.getRetentionTimeRegression_Validate_Invalid_negative_retention_time_window__0__(), getTimeWindow()));
//			}
//		}
	}

//	public static RetentionTimeRegression Deserialize(XmlReader reader)
//	{
//		return reader.Deserialize(new RetentionTimeRegression());
//	}

//	@Override
//	public void ReadXml(XmlReader reader)
//	{
//		// Read start tag attributes
//		super.ReadXml(reader);
//		String calculatorName = reader.GetAttribute(ATTR.calculator);
//		setTimeWindow(reader.GetDoubleAttribute(ATTR.time_window));
//		// Consume start tag
//		reader.ReadStartElement();
//
//		if (!DotNetToJavaStringHelper.isNullOrEmpty(calculatorName))
//		{
//			_calculator = new RetentionScoreCalculator(calculatorName);
//		}
//		// TODO: Fix this hacky way of dealing with the default value.
//		else if (reader.IsStartElement("irt_calculator")) // Not L10N
//		{
//			_calculator = RCalcIrt.Deserialize(reader);
//		}
//
//		setConversion(reader.<RegressionLineElement>DeserializeElement(EL.regression_rt));
//
//		// Read all measured retention times
//		java.util.ArrayList<MeasuredRetentionTime> list = new java.util.ArrayList<MeasuredRetentionTime>();
//		reader.ReadElements(list);
//		setPeptideTimes(list.toArray(new MeasuredRetentionTime[]{}));
//
//		// Consume end tag
//		reader.ReadEndElement();
//
//		Validate();
//	}

//	@Override
//	public void WriteXml(XmlWriter writer)
//	{
//		// Write attributes
//		super.WriteXml(writer);
//		writer.WriteAttribute(ATTR.time_window, getTimeWindow());
//
//		if (_calculator != null)
//		{
//			var irtCalc = (RCalcIrt)((_calculator instanceof RCalcIrt) ? _calculator : null);
//			if (irtCalc != null)
//			{
//				writer.WriteElement(irtCalc);
//			}
//			else
//			{
//				writer.WriteAttributeString(ATTR.calculator, _calculator.getName());
//			}
//		}
//
//		// Write conversion inner-tag, if not auto-convert
//		if (!getIsAutoCalculated())
//		{
//			writer.WriteElement(EL.regression_rt, getConversion());
//		}
//
//		// Write all measured retention times
//		writer.WriteElements(getPeptideTimes());
//	}

	public boolean equals(RetentionTimeRegression obj)
	{
		if (null == obj)
		{
			return false;
		}
		return getCalculator().equals(obj.getCalculator()) && getConversion().equals(obj.getConversion()) && obj.getTimeWindow() == getTimeWindow() && getPeptideTimes().equals(obj.getPeptideTimes()); // TPG TODO: order same on 2 peptideTimes lists?
	}

	@Override
	public boolean equals(Object obj)
	{
		if (null == obj || !(obj instanceof RetentionTimeRegression))
		{
			return false;
		}
		return equals((RetentionTimeRegression)obj);
	}

	@Override
	public int hashCode()
	{
        int result = super.hashCode();
        result = (result*397) ^ getCalculator().hashCode();
        result = (result*397) ^ (getConversion() != null ? getConversion().hashCode() : 0);
        result = (result*397) ^ (new Double(getTimeWindow())).hashCode();
        result = (result*397) ^ getPeptideTimes().hashCode();
        //result = (result*397) ^ getPeptideTimes().GetHashCodeDeep();
        //result = (result*397) ^ (_listFileIdToConversion != null ? _listFileIdToConversion.hashCode() : 0);
        return result;
	}

	public static RetentionTimeRegression CalcRegression(String name, ArrayList<RetentionScoreCalculatorSpec> calculators, ArrayList<MeasuredRetentionTime> measuredPeptides, RefObject<RetentionTimeStatistics> statistics)
	{
		RetentionScoreCalculatorSpec s = null;
		RefObject<RetentionScoreCalculatorSpec> tempRef_s = new RefObject<>(s);
		RetentionTimeRegression tempVar = CalcRegression(name, calculators, measuredPeptides, null, false, statistics, tempRef_s);
		s = tempRef_s.get();
		return tempVar;
	}

	/** 
	 This function chooses the best calculator (by r value) and returns a regression based on that calculator.
	 
	 @param name Name of the regression
	 @param calculators An IEnumerable of calculators to choose from (cannot be null)
	 @param measuredPeptides A List of MeasuredRetentionTime objects to build the regression from
	 @param scoreCache A RetentionTimeScoreCache to try getting scores from before calculating them
	 @param allPeptides If true, do not let the calculator pick which peptides to use in the regression
	 @param statistics Statistics from the regression of the best calculator
	 @param calculatorSpec The best calculator
	 @return 
	*/
	public static RetentionTimeRegression CalcRegression(String name, ArrayList<RetentionScoreCalculatorSpec> calculators, ArrayList<MeasuredRetentionTime> measuredPeptides, RetentionTimeScoreCache scoreCache, boolean allPeptides, RefObject<RetentionTimeStatistics> statistics, RefObject<RetentionScoreCalculatorSpec> calculatorSpec)
	{
		// Get a list of peptide names for use by the calculators to choose their regression peptides

        ArrayList<String> listPeptides = new ArrayList<>();

        for (MeasuredRetentionTime mrt : measuredPeptides)
            listPeptides.add(mrt.getPeptideSequence());

		// Set these now so that we can return null on some conditions
		calculatorSpec.set(calculators.get(0));
		statistics.set(null);

		int count = listPeptides.size();
		if (count == 0)
		{
			return null;
		}

		RetentionScoreCalculatorSpec[] calculatorCandidates = calculators == null ? new RetentionScoreCalculatorSpec[0] : calculators.toArray(new RetentionScoreCalculatorSpec[]{});
		int calcs = calculatorCandidates.length;

		// An arraylist, indexed by calculator, of scores of peptides by each calculator
		List<ArrayList<Double>> peptideScoresByCalc = new ArrayList<>(calcs);
		// An arraylist, indexed by calculator, of the peptides each calculator will use
		List<ArrayList<String>> calcPeptides = new ArrayList<>(calcs);
		// An arraylist, indexed by calculator, of actual retention times for the peptides in peptideScoresByCalc
		List<ArrayList<Double>> listRTs = new ArrayList<>(calcs);

		LinkedHashMap<String, Double> dictMeasuredPeptides = new LinkedHashMap<String, Double>();

		for (MeasuredRetentionTime measured : measuredPeptides)
		{
			if (!dictMeasuredPeptides.containsKey(measured.getPeptideSequence()))
			{
				dictMeasuredPeptides.put(measured.getPeptideSequence(), measured.getRetentionTime());
			}
		}
		HashSet<Integer> setExcludeCalcs = new LinkedHashSet<>();
		for (int i = 0; i < calcs; i++)
		{
			listRTs.add(i, null);
            calcPeptides.add(i, null);
            peptideScoresByCalc.add(i, null);

            if (setExcludeCalcs.contains(i))
			{
				continue;
			}

			RetentionScoreCalculatorSpec calc = calculatorCandidates[i];
			if(!calc.IsUsable())
			{
				setExcludeCalcs.add(i);
				continue;
			}

			try
			{
				listRTs.set(i, new ArrayList<Double>());
				calcPeptides.set(i, allPeptides ? listPeptides : calc.ChooseRegressionPeptides(listPeptides));
				peptideScoresByCalc.set(i, RetentionTimeScoreCache.CalcScores(calc, calcPeptides.get(i), scoreCache));
			}
			catch (RuntimeException e)
			{
				setExcludeCalcs.add(i);
				listRTs.set(i, null);
				calcPeptides.set(i, null);
				peptideScoresByCalc.set(i, null);
				continue;
			}

			for(String calcPeptide : calcPeptides.get(i))
			{
				listRTs.get(i).add(dictMeasuredPeptides.get(calcPeptide));
			}
		}
		Statistics[] aStatValues = new Statistics[calcs];
		for (int i = 0; i < calcs; i++)
		{
			if(setExcludeCalcs.contains(i))
			{
				continue;
			}

			aStatValues[i] = new Statistics(peptideScoresByCalc.get(i));
		}
		double r = Double.MIN_VALUE;
		RetentionScoreCalculatorSpec calcBest = null;
		Statistics statBest = null;
		java.util.ArrayList<Double> listBest = null;
		int bestCalcIndex = 0;
		Statistics bestCalcStatRT = null;
		for (int i = 0; i < calcs; i++)
		{
			if(setExcludeCalcs.contains(i))
			{
				continue;
			}

			Statistics statRT = new Statistics(listRTs.get(i));
			Statistics stat = aStatValues[i];
			double rVal = statRT.R(stat);

			// Make sure sets containing unknown scores have very low correlations to keep
			// such scores from ending up in the final regression.
			rVal = !peptideScoresByCalc.get(i).contains(calculatorCandidates[i].getUnknownScore()) ? rVal : 0;
			if (r < rVal)
			{
				bestCalcIndex = i;
				r = rVal;
				statBest = stat;
				listBest = peptideScoresByCalc.get(i);
				calcBest = calculatorCandidates[i];
				bestCalcStatRT = statRT;
			}
		}

		if (calcBest == null)
		{
			return null;     // TPG: TODO: need a NULL check in FindThreshold for this case.
		}

		calculatorSpec.set(calcBest);

		double slope = bestCalcStatRT.Slope(statBest);
		double intercept = bestCalcStatRT.Intercept(statBest);

		// Suggest a time window of 4*StdDev (or 2 StdDev on either side of
		// the mean == ~95% of training data).
		Statistics residuals = bestCalcStatRT.Residuals(statBest);
		double window = residuals.StdDev() * 4;
		// At minimum suggest a 0.5 minute window, in case of something wacky
		// like only 2 data points.  The RetentionTimeRegression class will
		// throw on a window of zero.
		if (window < 0.5)
		{
			window = 0.5;
		}

		// Save statistics
		RegressionLine rlBest = new RegressionLine(slope, intercept);

		ArrayList<Double> listPredicted = new ArrayList<>(listBest.size());
        for (Double x : listBest)
            listPredicted.add(rlBest.GetY(x));

		statistics.set(new RetentionTimeStatistics(r, calcPeptides.get(bestCalcIndex), listBest, listPredicted, listRTs.get(bestCalcIndex)));

		// Get MeasuredRetentionTimes for only those peptides chosen by the calculator
		HashSet<String> setBestPeptides = new LinkedHashSet<>();
		for (String pep : calcPeptides.get(bestCalcIndex))
		{
			setBestPeptides.add(pep);
		}

		ArrayList<MeasuredRetentionTime> calcMeasuredRts = new ArrayList<>();
        for (MeasuredRetentionTime mrt : measuredPeptides)
        {
            if (setBestPeptides.contains(mrt.getPeptideSequence()))
                calcMeasuredRts.add(mrt);
        }
//        measuredPeptides.Where(pep => setBestPeptides.contains(pep.PeptideSequence)).toArray();
		return new RetentionTimeRegression(name, calcBest, slope, intercept, window, calcMeasuredRts);
	}

	private static double ScoreSequence(IRetentionScoreCalculator calculator, Map<String, Double> scoreCache, String sequence)
	{
		Double score;
		if (scoreCache == null || (score = scoreCache.get(sequence)) == null)
		{
			Double tempVar = calculator.ScoreSequence(sequence);
			score = (tempVar != null) ? tempVar : calculator.getUnknownScore();
		}
		return score;
	}

	public static RetentionTimeRegression FindThreshold(double threshold, Integer precision, ArrayList<MeasuredRetentionTime> measuredPeptides, ArrayList<MeasuredRetentionTime> standardPeptides, ArrayList<MeasuredRetentionTime> variablePeptides, RetentionScoreCalculatorSpec calculator, Boolean isCanceled)
	{
		ArrayList<RetentionScoreCalculatorSpec> calculators = Lists.newArrayList(calculator);
		RetentionTimeScoreCache scoreCache = new RetentionTimeScoreCache(calculators, measuredPeptides, null);
		RetentionTimeStatistics statisticsAll = null;
		RefObject<RetentionTimeStatistics> tempRef_statisticsAll = new RefObject<>(statisticsAll);
		RefObject<RetentionScoreCalculatorSpec> tempRef_calculator = new RefObject<>(calculator);
		RetentionTimeRegression regressionInitial = CalcRegression("__internal__", calculators, measuredPeptides, scoreCache, true, tempRef_statisticsAll, tempRef_calculator);

        // TPG addition to Skyline C# code: adding null check on return.
        if (regressionInitial == null)
            return null;

        statisticsAll = tempRef_statisticsAll.get();
		calculator = tempRef_calculator.get();

		LinkedHashSet<Integer> outIndexes = new LinkedHashSet<>();
		RetentionTimeStatistics statisticsRefined = null;
		RefObject<RetentionTimeStatistics> tempRef_statisticsRefined = new RefObject<>(statisticsRefined);
		RefObject<LinkedHashSet<Integer>> tempRef_outIndexes = new RefObject<>(outIndexes);
		RetentionTimeRegression tempVar = regressionInitial.FindThreshold(threshold, precision, 0, measuredPeptides.size(), standardPeptides, variablePeptides, statisticsAll, calculator, scoreCache, isCanceled, tempRef_statisticsRefined, tempRef_outIndexes);
		statisticsRefined = tempRef_statisticsRefined.get();  // TPG: TODO Proper handling of the ref conversion
		outIndexes = tempRef_outIndexes.get();
		return tempVar;

	}

	public RetentionTimeRegression FindThreshold(double threshold, Integer precision, int left, int right, ArrayList<MeasuredRetentionTime> standardPeptides, ArrayList<MeasuredRetentionTime> variablePeptides, RetentionTimeStatistics statistics, RetentionScoreCalculatorSpec calculator, RetentionTimeScoreCache scoreCache, Boolean isCanceled, RefObject<RetentionTimeStatistics> statisticsResult, RefObject<LinkedHashSet<Integer>> outIndexes)
	{
		if (left > right)
		{
			int worstIn = right;
			int bestOut = left;
			if (IsAboveThreshold(statisticsResult.get().getR(), threshold, precision))
			{
				// Add back outliers until below the threshold
				for (;;)
				{
//					if (isCanceled)
//					{
//						throw new OperationCanceledException();
//					}
					RecalcRegression(bestOut, standardPeptides, variablePeptides, statisticsResult.get(), calculator, scoreCache, statisticsResult, outIndexes);
					if (bestOut >= variablePeptides.size() || !IsAboveThreshold(statisticsResult.get().getR(), threshold, precision))
					{
						break;
					}
					bestOut++;
				}
				worstIn = bestOut;
			}

			// Remove values until above the threshold
			for (;;)
			{
//				if (isCanceled)
//				{
//					throw new OperationCanceledException();
//				}
				RetentionTimeRegression regression = RecalcRegression(worstIn, standardPeptides, variablePeptides, statisticsResult.get(), calculator, scoreCache, statisticsResult, outIndexes);
				// If there are only 2 left, then this is the best we can do and still have
				// a linear equation.
				if (worstIn <= 2 || IsAboveThreshold(statisticsResult.get().getR(), threshold, precision))
				{
					return regression;
				}
				worstIn--;
			}
		}

//		// Check for cancelation
//		if (isCanceled())
//		{
//			throw new OperationCanceledException();
//		}

		int mid = (left + right) / 2;

		LinkedHashSet<Integer> outIndexesNew = outIndexes.get();
		RetentionTimeStatistics statisticsNew = null;
		// Rerun the regression
		RefObject<RetentionTimeStatistics> tempRef_statisticsNew = new RefObject<>(statisticsNew);
		RefObject<LinkedHashSet<Integer>> tempRef_outIndexesNew = new RefObject<>(outIndexesNew);
		RetentionTimeRegression regressionNew = RecalcRegression(mid, standardPeptides, variablePeptides, statistics, calculator, scoreCache, tempRef_statisticsNew, tempRef_outIndexesNew);
		statisticsNew = tempRef_statisticsNew.get();
		outIndexesNew = tempRef_outIndexesNew.get();
		// If no regression could be calculated, give up to avoid infinite recursion.
		if (regressionNew == null)
		{
			return this;
		}

		statisticsResult.set(statisticsNew);
		outIndexes.set(outIndexesNew);

		if (IsAboveThreshold(statisticsResult.get().getR(), threshold, precision))
		{
			return regressionNew.FindThreshold(threshold, precision, mid + 1, right, standardPeptides, variablePeptides, statisticsResult.get(), calculator, scoreCache, isCanceled, statisticsResult, outIndexes);
		}

		return regressionNew.FindThreshold(threshold, precision, left, mid - 1, standardPeptides, variablePeptides, statisticsResult.get(), calculator, scoreCache, isCanceled, statisticsResult, outIndexes);
	}

	private RetentionTimeRegression RecalcRegression(int mid, List<MeasuredRetentionTime> requiredPeptides, ArrayList<MeasuredRetentionTime> variablePeptides, RetentionTimeStatistics statistics, RetentionScoreCalculatorSpec calculator, RetentionTimeScoreCache scoreCache, RefObject<RetentionTimeStatistics> statisticsResult, RefObject<LinkedHashSet<Integer>> outIndexes)
	{
		// Create list of deltas between predicted and measured times
		ArrayList<Double> listTimes = statistics.getListRetentionTimes();
		ArrayList<Double> listPredictions = statistics.getListPredictions();
		ArrayList<Double> listHydroScores = statistics.getListHydroScores();
		ArrayList<DeltaIndex> listDeltas = new ArrayList<>();
		int iNextStat = 0;
		double unknownScore = getCalculator().getUnknownScore();
		for (int i = 0; i < variablePeptides.size(); i++)
		{
			double delta;
			if (variablePeptides.get(i).getRetentionTime() == 0)
			{
				delta = Double.MAX_VALUE; // Make sure zero times are always outliers
			}
			else if (!outIndexes.get().contains(i) && iNextStat < listPredictions.size())
			{
				delta = listHydroScores.get(iNextStat) != unknownScore ? Math.abs(listPredictions.get(iNextStat) - listTimes.get(iNextStat)) : Double.MAX_VALUE;
				iNextStat++;
			}
			else
			{
				// Recalculate values for the indexes that were not used to generate
				// the current regression.
				MeasuredRetentionTime peptideTime = variablePeptides.get(i);
				double score = scoreCache.CalcScore(getCalculator(), peptideTime.getPeptideSequence());
				delta = Double.MAX_VALUE;
				if (score != unknownScore)
				{
					Double predictedTime = GetRetentionTime(score);
					if (predictedTime != null)
					{
						delta = Math.abs(predictedTime - peptideTime.getRetentionTime());
					}
				}
			}
			listDeltas.add(new DeltaIndex(delta, i));
		}

		// Sort descending
		Collections.sort(listDeltas);

		// Remove points with the highest deltas above mid
		outIndexes.set(new LinkedHashSet<Integer>());
		int countOut = variablePeptides.size() - mid - 1;
		for (int i = 0; i < countOut; i++)
		{
			outIndexes.get().add(listDeltas.get(i).getIndex());
		}
		ArrayList<MeasuredRetentionTime> peptidesTimesTry = new ArrayList<>(variablePeptides.size());
		for (int i = 0; i < variablePeptides.size(); i++)
		{
			if (outIndexes.get().contains(i))
			{
				continue;
			}
			peptidesTimesTry.add(variablePeptides.get(i));
		}

		peptidesTimesTry.addAll(requiredPeptides);

		RetentionScoreCalculatorSpec s = null;
		RefObject<RetentionScoreCalculatorSpec> tempRef_s = new RefObject<RetentionScoreCalculatorSpec>(s);
		RetentionTimeRegression tempVar = CalcRegression(getName(), Lists.newArrayList(calculator), peptidesTimesTry, scoreCache, true, statisticsResult, tempRef_s);
		s = tempRef_s.get();  // TPG TODO: By ref handled correctly?
		return tempVar;
	}

	public static int getThresholdPrecision()
	{
		return 4;
	}

	public static boolean IsAboveThreshold(double value, double threshold)
	{
		return IsAboveThreshold(value, threshold, null);
	}

	public static boolean IsAboveThreshold(double value, double threshold, Integer precision)
	{
		return Precision.round(value, (precision != null) ? precision : getThresholdPrecision()) >= threshold;
	}

	public static final String SSRCALC_300_A = "SSRCalc 3.0 (300A)"; // Not L10N
	public static final String SSRCALC_100_A = "SSRCalc 3.0 (100A)"; // Not L10N

//	public static IRetentionScoreCalculator GetCalculatorByName(String calcName)
//	{
////		switch (calcName)
////ORIGINAL LINE: case SSRCALC_300_A:
//		if (SSRCALC_300_A.equals(calcName))
//		{
//				return new SSRCalc3(SSRCALC_300_A, SSRCalc3.Column.A300);
//		}
////ORIGINAL LINE: case SSRCALC_100_A:
//		else if (SSRCALC_100_A.equals(calcName))
//		{
//				return new SSRCalc3(SSRCALC_100_A, SSRCalc3.Column.A100);
//		}
//		return null;
//	}

//	public boolean SamePeptides(RetentionTimeRegression rtRegressionNew)
//	{
//		if (_dictStandardPeptides == null && rtRegressionNew._dictStandardPeptides == null)
//		{
//			return true;
//		}
//		if (_dictStandardPeptides == null || rtRegressionNew._dictStandardPeptides == null)
//		{
//			return false;
//		}
//		if (_dictStandardPeptides.size() != rtRegressionNew._dictStandardPeptides.size())
//		{
//			return false;
//		}
//		for (var idPeptide : _dictStandardPeptides)
//		{
//			PeptideDocNode nodePep = null;
//			RefObject<PeptideDocNode> tempRef_nodePep = new RefObject<PeptideDocNode>(nodePep);
//			boolean tempVar = !rtRegressionNew._dictStandardPeptides.TryGetValue(idPeptide.getKey(), tempRef_nodePep);
//				nodePep = tempRef_nodePep.argvalue;
//			if (tempVar)
//			{
//				return false;
//			}
//			if (!ReferenceEquals(nodePep, idPeptide.getValue()))
//			{
//				return false;
//			}
//		}
//		return true;
//	}

	private final static class DeltaIndex implements Comparable<DeltaIndex>
	{
		public DeltaIndex(double delta, int index)
		{
			setDelta(delta);
			setIndex(index);
		}

		private double privateDelta;
		private double getDelta()
		{
			return privateDelta;
		}
		private void setDelta(double value)
		{
			privateDelta = value;
		}
		private int privateIndex;
		public int getIndex()
		{
			return privateIndex;
		}
		private void setIndex(int value)
		{
			privateIndex = value;
		}
		public int compareTo(DeltaIndex other)
		{
			if (getDelta() > other.getDelta())
			{
				return -1;
			}
			if (getDelta() == other.getDelta())
			{
				return 0;
			}
			return 1;
		}
	}
}