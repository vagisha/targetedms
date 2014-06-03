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
 Slope and intercept pair used to calculate a y-value from
 a given x based on a linear regression.
 
 The class can read its properties from the attributes on
 an XML element, but does not itself represent a full XML
 element.  Use one of the wrapper classes for full XML
 serialization.
 
*/
public final class RegressionLine implements IRegressionFunction
{
	public RegressionLine(double slope, double intercept)
	{
		setSlope(slope);
		setIntercept(intercept);
	}

	private double privateSlope;
	public double getSlope()
	{
		return privateSlope;
	}
	private void setSlope(double value)
	{
		privateSlope = value;
	}

	private double privateIntercept;
	public double getIntercept()
	{
		return privateIntercept;
	}
	private void setIntercept(double value)
	{
		privateIntercept = value;
	}

	/** 
	 Use the y = m*x + b formula to calculate the desired y
	 for a given x.
	 
	 @param x Value in x dimension
	 @return 
	*/
	public double GetY(double x)
	{
		return getSlope() * x + getIntercept();
	}

	/** 
	 Use the y = m*x + b formula to calculate the desired x
	 for a given y.
	 
	 @param y Value in y dimension
	 @return 
	*/
	public double GetX(double y)
	{
		return (y - getIntercept()) / getSlope();
	}

	private enum ATTR
	{
		slope,
		intercept;

		public int getValue()
		{
			return this.ordinal();
		}

		public static ATTR forValue(int value)
		{
			return values()[value];
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (null == obj || ! (obj instanceof RegressionLine))
		{
			return false;
		}
        return ((RegressionLine)obj).getSlope() == getSlope() && ((RegressionLine)obj).getIntercept() == getIntercept();
	}

	@Override
	public int hashCode()
	{
        return ((new Double(getSlope())).hashCode()*397) ^ (new Double(getIntercept())).hashCode();
	}

}