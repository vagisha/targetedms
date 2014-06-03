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

public abstract class RetentionScoreCalculatorSpec implements IRetentionScoreCalculator
{
	public abstract Double ScoreSequence(String sequence);

	public abstract double getUnknownScore();

	public abstract ArrayList<String> ChooseRegressionPeptides(Iterable<String> peptides);

	public abstract ArrayList<String> GetStandardPeptides(Iterable<String> peptides);

	public boolean IsUsable()
	{
		return true;
	}

//	public RetentionScoreCalculatorSpec Initialize(IProgressMonitor loadMonitor)
//	{
//		return this;
//	}

//	public String getPersistencePath()
//	{
//		return null;
//	}
//
//	public String PersistMinimized(String pathDestDir, SrmDocument document)
//	{
//		return null;
//	}
//
//		///#region Implementation of IXmlSerializable
//
//	/**
//	 For XML serialization
//
//	*/
//	protected RetentionScoreCalculatorSpec()
//	{
//	}
//
//		///#endregion
}