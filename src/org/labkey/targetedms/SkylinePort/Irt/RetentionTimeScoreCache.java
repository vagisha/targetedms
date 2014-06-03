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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RetentionTimeScoreCache
{
	private final LinkedHashMap<String, LinkedHashMap<String, Double>> _cache = new LinkedHashMap<>();

	public RetentionTimeScoreCache(Iterable<RetentionScoreCalculatorSpec> calculators, ArrayList<MeasuredRetentionTime> peptidesTimes, RetentionTimeScoreCache cachePrevious)
	{
		for (IRetentionScoreCalculator calculator : calculators)
		{
			LinkedHashMap<String, Double> cacheCalc = new LinkedHashMap<>();
			_cache.put(calculator.getName(), cacheCalc);
			LinkedHashMap<String, Double> cacheCalcPrevious;
			if (cachePrevious == null || !((cacheCalcPrevious = cachePrevious._cache.get(calculator.getName())) != null))
			{
				cacheCalcPrevious = null;
			}

			for (MeasuredRetentionTime mrt : peptidesTimes)
			{
				String seq = mrt.getPeptideSequence();
				if (!cacheCalc.containsKey(seq))
				{
					cacheCalc.put(seq, CalcScore(calculator, seq, cacheCalcPrevious));
				}
			}
		}
	}

	public void RecalculateCalcCache(RetentionScoreCalculatorSpec calculator)
	{
		Map<String, Double> calcCache = _cache.get(calculator.getName());
		if(calcCache != null)
		{
			LinkedHashMap<String, Double> newCalcCache = new LinkedHashMap<String, Double>();

			for (String key : calcCache.keySet())
			{
				//force recalculation
				newCalcCache.put(key, CalcScore(calculator, key, null));
			}

			_cache.put(calculator.getName(), newCalcCache);
		}
	}

	public double CalcScore(IRetentionScoreCalculator calculator, String peptide)
	{
		LinkedHashMap<String, Double> cacheCalc = null;
		cacheCalc = _cache.get(calculator.getName());
		return CalcScore(calculator, peptide, cacheCalc);
	}

	public static ArrayList<Double> CalcScores(IRetentionScoreCalculator calculator, ArrayList<String> peptides, RetentionTimeScoreCache scoreCache)
	{
		HashMap<String, Double> cacheCalc;
        if (scoreCache == null || (cacheCalc = scoreCache._cache.get(calculator.getName())) == null)
		{
			cacheCalc = null;
		}

        ArrayList<Double> scores = new ArrayList<>();
        for (String sequence : peptides)
        {
            scores.add(CalcScore(calculator, sequence, cacheCalc));
        }
        return scores;
//		return peptides.ConvertAll(pep => CalcScore(calculator, pep, cacheCalc));
	}

	private static double CalcScore(IRetentionScoreCalculator calculator, String peptide, java.util.Map<String, Double> cacheCalc)
	{
		Double score;
		if (cacheCalc == null || (score = cacheCalc.get(peptide)) == null)
		{
			Double tempVar = calculator.ScoreSequence(peptide);
			score = (tempVar != null) ? tempVar : calculator.getUnknownScore();
		}
		return score;
	}
}