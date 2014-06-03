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

//[XmlRoot("retention_score_calculator")]
public class RetentionScoreCalculator extends RetentionScoreCalculatorSpec
{
	private IRetentionScoreCalculator _impl;

	public RetentionScoreCalculator(String name)
	{
//		super(name);
		Validate();
	}

    @Override
    public String getName()
    {
        return "retention_score_calculator";
    }

    @Override
	public Double ScoreSequence(String sequence)
	{
		return _impl.ScoreSequence(sequence);
	}

	@Override
	public double getUnknownScore()
	{
		return _impl.getUnknownScore();
	}

	@Override
	public ArrayList<String> GetStandardPeptides(Iterable<String> peptides)
	{
		return _impl.GetStandardPeptides(peptides);
	}

	private RetentionScoreCalculator()
	{
	}

	@Override
	public ArrayList<String> ChooseRegressionPeptides(Iterable<String> peptides)
    {
		return _impl.ChooseRegressionPeptides(peptides);
	}

	private void Validate()
	{
//		_impl = RetentionTimeRegression.GetCalculatorByName(getName());
//		if (_impl == null)
//		{
//			throw new InvalidDataException(String.format(Resources.getRetentionScoreCalculator_Validate_The_retention_time_calculator__0__is_not_valid(), getName()));
//		}
	}

//	@Override
//	public void ReadXml(XmlReader reader)
//	{
//		super.ReadXml(reader);
//		// Consume tag
//		reader.Read();
//
//		Validate();
//	}
//
//	public static RetentionScoreCalculator Deserialize(XmlReader reader)
//	{
//		return reader.Deserialize(new RetentionScoreCalculator());
//	}
}