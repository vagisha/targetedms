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

public final class MeasuredRetentionTime
{
	/** 
	 To support using iRT values, which can be negative, in place of measured retention times
	 
	*/
	private boolean _allowNegative;

    public MeasuredRetentionTime(String peptideSequence, double retentionTime)
    {
        this(peptideSequence, retentionTime, false);
    }

	public MeasuredRetentionTime(String peptideSequence, double retentionTime, boolean allowNegative)
	{
		setPeptideSequence(peptideSequence);
		setRetentionTime(retentionTime);
		_allowNegative = allowNegative;

		Validate();
	}

	private String privatePeptideSequence;
	public String getPeptideSequence()
	{
		return privatePeptideSequence;
	}
	private void setPeptideSequence(String value)
	{
		privatePeptideSequence = value;
	}
	private double privateRetentionTime;
	public double getRetentionTime()
	{
		return privateRetentionTime;
	}
	private void setRetentionTime(double value)
	{
		privateRetentionTime = value;
	}

	/**
	 For serialization
	 
	*/
	private MeasuredRetentionTime()
	{
	}

	private enum ATTR
	{
		peptide,
		time;

		public int getValue()
		{
			return this.ordinal();
		}

		public static ATTR forValue(int value)
		{
			return values()[value];
		}
	}

	private void Validate()        // TPG TODO: May not need Validate()
	{
//		if (!FastaSequence.IsExSequence(getPeptideSequence()))
//		{
//			throw new InvalidDataException(String.format(Resources.getMeasuredRetentionTime_Validate_The_sequence__0__is_not_a_valid_peptide(), getPeptideSequence()));
//		}
//		if (!_allowNegative && getRetentionTime() < 0)
//		{
//			throw new InvalidDataException(Resources.getMeasuredRetentionTime_Validate_Measured_retention_times_must_be_positive_values());
//		}
	}

	public boolean equals(MeasuredRetentionTime obj)
	{
		if (obj == null)
			return false;
        else
            return getPeptideSequence().equals(obj.getPeptideSequence()) && obj.getRetentionTime() == getRetentionTime();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof MeasuredRetentionTime))
		{
			return false;
		}
		return equals((MeasuredRetentionTime)obj);
	}

	@Override
	public int hashCode()
	{
    	return (getPeptideSequence().hashCode() * 397) ^ (new Double(getRetentionTime())).hashCode();
	}

}