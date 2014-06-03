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

/**
 The simplest XML element wrapper for an unnamed <see cref="RegressionLine"/>.
 
*/
public final class RegressionLineElement implements IRegressionFunction
{
	private RegressionLine _regressionLine;

    public RegressionLine getRegressionLine()
    {
        return _regressionLine;
    }

    public RegressionLineElement(double slope, double intercept)
	{
		_regressionLine = new RegressionLine(slope, intercept);
	}

	public RegressionLineElement(RegressionLine regressionLine)
	{
		_regressionLine = regressionLine;
	}

	public double getSlope()
	{
		return _regressionLine.getSlope();
	}

	public double getIntercept()
	{
		return _regressionLine.getIntercept();
	}

	public double GetY(double x)
	{
		return _regressionLine.GetY(x);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (null == obj || !(obj instanceof RegressionLineElement))
		{
			return false;
		}
		return _regressionLine.equals(((RegressionLineElement) obj).getRegressionLine());
	}

	@Override
	public int hashCode()
	{
		return _regressionLine.hashCode();
	}

}