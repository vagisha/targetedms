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

import java.util.LinkedHashMap;

public final class RetentionTimeStatistics
{
	public RetentionTimeStatistics(double r, java.util.ArrayList<String> peptides, java.util.ArrayList<Double> listHydroScores, java.util.ArrayList<Double> listPredictions, java.util.ArrayList<Double> listRetentionTimes)
	{
		setR(r);
		setPeptides(peptides);
		setListHydroScores(listHydroScores);
		setListPredictions(listPredictions);
		setListRetentionTimes(listRetentionTimes);
	}

	private double privateR;
	public double getR()
	{
		return privateR;
	}
	private void setR(double value)
	{
		privateR = value;
	}
	private java.util.ArrayList<String> privatePeptides;
	public java.util.ArrayList<String> getPeptides()
	{
		return privatePeptides;
	}
	private void setPeptides(java.util.ArrayList<String> value)
	{
		privatePeptides = value;
	}
	private java.util.ArrayList<Double> privateListHydroScores;
	public java.util.ArrayList<Double> getListHydroScores()
	{
		return privateListHydroScores;
	}
	private void setListHydroScores(java.util.ArrayList<Double> value)
	{
		privateListHydroScores = value;
	}
	private java.util.ArrayList<Double> privateListPredictions;
	public java.util.ArrayList<Double> getListPredictions()
	{
		return privateListPredictions;
	}
	private void setListPredictions(java.util.ArrayList<Double> value)
	{
		privateListPredictions = value;
	}
	private java.util.ArrayList<Double> privateListRetentionTimes;
	public java.util.ArrayList<Double> getListRetentionTimes()
	{
		return privateListRetentionTimes;
	}
	private void setListRetentionTimes(java.util.ArrayList<Double> value)
	{
		privateListRetentionTimes = value;
	}

	public java.util.Map<String, Double> getScoreCache()
	{
		LinkedHashMap<String, Double> scoreCache = new LinkedHashMap<String, Double>();
		for (int i = 0; i < getPeptides().size(); i++)
		{
			String sequence = getPeptides().get(i);
			if (!scoreCache.containsKey(sequence))
			{
				scoreCache.put(sequence, getListHydroScores().get(i));
			}
		}
		return scoreCache;
	}
}