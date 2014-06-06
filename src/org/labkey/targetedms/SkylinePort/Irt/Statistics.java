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

import java.util.List;

/** 
 Simple statistics utility class based on the CodeProject article:
 http: //www.codeproject.com/KB/cs/csstatistics.aspx
 
*/
public class Statistics
{
	private Double[] _list;

	/** 
	 Constructor for statistics on a set of numbers.
	 
	 @param list The set of numbers
	*/
	public Statistics(Double... list)
	{
		_list = list;
	}

	public Statistics(List<Double> list)
	{
		if (list != null)
        {
            _list = new Double[list.size()];
            _list = list.toArray(_list);
        }
	}     // TPG TODO: Decide on best internal storage for _list

    public Double[] getList()
    {
        return _list;
    }

    //	/**
//	 Change the set of numbers for which statistics are to be computed.
//
//	 @param list New set of numbers
//	*/
//	public final void Update(Double... list)
//	{
//		_list = list;
//	}

	/**
	 Count of the numbers in the set.

	*/
	public final int getLength()
	{
		return _list.length;
	}

//	/**
//	 Creates a copy of the internal list and sorts it.  O(n*log(n)) operation.
//
//	 @return A sorted copy of the list of numbers in this object
//	*/
//	private double[] OrderedList()
//	{
//		double[] ordered = new double[_list.length];
//		_list.CopyTo(ordered, 0);
//		Arrays.sort(ordered);
//		return ordered;
//	}
//

	/**
	 Sum of all the values in a set of numbers.

	*/
	public final double Sum()
	{
		// ReSharper disable LoopCanBePartlyConvertedToQuery
		double sum = 0;
		for (double d : _list) // using LINQ Sum() is about 10x slower than foreach
		{
			sum += d;
		}
		// ReSharper restore LoopCanBePartlyConvertedToQuery
		return sum;
	}

	/**
	 Calculates the mean average of the set of numbers.

	 @return Mean
	*/
	public final double Mean()
	{
		try
		{
			return Sum()/getLength();
		}
		catch (RuntimeException e)
		{
			return Double.NaN;
		}
	}

//	/**
//	 Calculates a weighted mean average of the set of numbers.
//	 See:
//	 http://en.wikipedia.org/wiki/Weighted_mean
//
//	 @param weights The weights
//	 @return Weighted mean
//	*/
//	public final double Mean(Statistics weights)
//	{
//		try
//		{
//			double sum = 0;
//			for (int i = 0; i < _list.length; i++)
//			{
//				sum += _list[i] * weights._list[i];
//			}
//			return sum / weights.Sum();
//		}
//		catch (RuntimeException e)
//		{
//			return Double.NaN;
//		}
//	}
//
//	/**
//	 Calculates range (max - min) of the set of numbers.
//
//	 @return Range
//	*/
//	public final double Range()
//	{
//		double minimum = Min();
//		double maximum = Max();
//		return (maximum - minimum);
//	}
//
//	/**
//	 Calculates the inter-quartile range (Q3 - Q1) of the set of numbers.
//
//	 @return Inter-quartile range
//	*/
//	public final double IQ()
//	{
//		return Q3() - Q1();
//	}
//
//	/**
//	 Calculates the mid-point of the range (min + max) / 2 of the
//	 set of numbers.
//
//	 @return Mid-point of range
//	*/
//	public final double MiddleOfRange()
//	{
//		double minimum = Min();
//		double maximum = Max();
//		return (minimum + maximum)/2;
//	}
//
//	/**
//	 Normalizes a the set of numbers to a unit vector.
//
//	 @return Normalized numbers
//	*/
//	public final Statistics NormalizeUnit()
//	{
//		double[] normalized = new double[getLength()];
//		try
//		{
//			double sum = Sum();
//			for (int i = 0; i < normalized.length; i++)
//			{
//				normalized[i] = _list[i]/sum;
//			}
//		}
//		catch (RuntimeException e)
//		{
//			for (int i = 0; i < normalized.length; i++)
//			{
//				normalized[i] = Double.NaN;
//			}
//		}
//		return new Statistics(normalized);
//	}
//
//	/**
//	 Base statistical value used in calculating variance and standard deviations.
//
//	*/
//	private double SumOfSquares()
//	{
//		double s = 0;
//		for (double value : _list)
//		{
//			s += Math.pow(value, 2);
//		}
//		return s;
//	}

	/**
	 Base statistical value used in calculating variance and standard deviations.
	 <p>
	 Simple "Naive" algorithm has inherent numeric instability.  See:
	 http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
	 </p>

	*/
	private double VarianceTotal()
	{
//            return (SumOfSquares() - _list.Length * Math.Pow(Mean(), 2));
		double mean = Mean();

		double s = 0;
		double sc = 0;
		for (double value : _list)
		{
			double diff = value - mean;
			s += diff*diff;
			sc += diff;
		}
		return (s - (sc*sc)/_list.length);
	}

	/**
	 Calculates the variance of the set of numbers.

	 @return Variance
	*/
	public final double Variance()
	{
		try
		{
			return VarianceTotal() / (_list.length - 1);
		}
		catch (RuntimeException e)
		{
			return Double.NaN;
		}
	}

//	/**
//	 Calculates the variance of the set of numbers as a
//	 sample of a larger population.
//
//	 @return Variance estimate for population
//	*/
//	public final double VarianceS()
//	{
//		return Variance();
//	}
//
//	/**
//	 Calculates the variance of the set of numbers as the
//	 entire population.
//
//	 @return Variance of population
//	*/
//	public final double VarianceP()
//	{
//		try
//		{
//			return VarianceTotal() / _list.length;
//		}
//		catch (RuntimeException e)
//		{
//			return Double.NaN;
//		}
//	}
//
//	/**
//	 Calculates the variance for a set of numbers from a weighted mean.
//	 See:
//	 http://en.wikipedia.org/wiki/Weighted_mean
//
//	 @param weights The weights
//	 @return Variance from weighted mean
//	*/
//	public final double Variance(Statistics weights)
//	{
//		if (_list.length < 2)
//		{
//			return 0;
//		}
//
//		try
//		{
//			double s = 0;
//			for (int i = 0; i < _list.length; i++)
//			{
//				s += weights._list[i] * Math.pow(_list[i], 2);
//			}
//			return (s/weights.Mean() - _list.length*Math.pow(Mean(weights), 2)) / (_list.length - 1);
//		}
//		catch (RuntimeException e)
//		{
//			return Double.NaN;
//		}
//	}

	/**
	 Calculates the stadard deviation (sqrt(variance)) of the set
	 of numbers.

	 @return Standard deviation
	*/
	public final double StdDev()
	{
		return Math.sqrt(Variance());
	}

	/**
	 Calculates the covariance between this and another set of numbers.

	 @param s Second set of numbers
	 @return Covariance
	*/
	public final double Covariance(Statistics s)
	{
		return Covariance(this, s);
	}

	/**
	 Calculates the covariance between two sets of numbers.

	 @param s1 First set of numbers
	 @param s2 Second set of numbers
	 @return
	*/
	public static double Covariance(Statistics s1, Statistics s2)
	{
		try
		{
			if (s1.getLength() != s2.getLength())
			{
				return Double.NaN;
			}

			int len = s1.getLength();
			double sumMul = 0;
			for (int i = 0; i < len; i++)
			{
				sumMul += (s1._list[i]*s2._list[i]);
			}
			return (sumMul - len*s1.Mean()*s2.Mean())/(len - 1);
		}
		catch (RuntimeException e)
		{
			return Double.NaN;
		}
	}

	/**
	 Calculates the correlation coefficient between this and
	 another set of numbers.

	 @param s Second set of numbers
	 @return Correlation coefficient
	*/
	public final double R(Statistics s)
	{
		return R(this, s);
	}

	/**
	 Calculates the correlation coefficient between two sets
	 of numbers.

	 @param s1 First set of numbers
	 @param s2 Second set of numbers
	 @return Correlation coefficient
	*/
	public static double R(Statistics s1, Statistics s2)
	{
		try
		{
//			SimpleRegression regression = new SimpleRegression(s1.get, s2);
            return Covariance(s1, s2)/(s1.StdDev()*s2.StdDev());
		}
		catch (RuntimeException e)
		{
			return Double.NaN;
		}
	}

	/**
	 Calculates the b term (y-intercept) of the linear
	 regression function (y = a*x + b) using the current set of numbers as Y values
	 and another set as X values.

	 @param x X values
	 @return The b coefficient of y = a*x + b
	*/
	public final double BTerm2(Statistics x)
	{
		return BTerm2(this, x);
	}

	/**
	 Calculates the b term (y-intercept) of the linear
	 regression function (y = a*x + b) given the Y and X values.

	 @param y Y values
	 @param x X values
	 @return The b coefficient of y = a*x + b
	*/
	public static double BTerm2(Statistics y, Statistics x)
	{
		return y.Mean() - ATerm2(y, x)*x.Mean();
	}

	/**
	 Calculates the y-intercept (b term) of the linear
	 regression function (y = a*x + b) using the current set of numbers as Y values
	 and another set as X values.

	 @param x X values
	 @return The y-intercept
	*/
	public final double Intercept(Statistics x)
	{
		return BTerm2(x);
	}

//	/**
//	 Calculates the y-intercept (Beta coefficient) of the linear
//	 regression function (y = a*x + b) given the Y and X values.
//
//	 @param y Y values
//	 @param x X values
//	 @return The y-intercept
//	*/
//	public static double Intercept(Statistics y, Statistics x)
//	{
//		return BTerm2(y, x);
//	}

	/**
	 Calculates the a term (slope) of the linear regression function (y = a*x + b)
	 using the current set of numbers as Y values and another set
	 as X values.

	 @param x X values
	 @return The a term of y = a*x + b
	*/
	public final double ATerm2(Statistics x)
	{
		return ATerm2(this, x);
	}

	/**
	 Calculates the a term (slope) of the linear regression function (y = a*x + b)
	 given the Y and X values.

	 @param y Y values
	 @param x X values
	 @return The a term of y = a*x + b
	*/
	public static double ATerm2(Statistics y, Statistics x)
	{
		try
		{
			return Covariance(y, x) / (Math.pow(x.StdDev(), 2));
		}
		catch (RuntimeException e)
		{
			return Double.NaN;
		}
	}

	/**
	 Calculates the slope (a term) of the linear regression function (y = a*x + b)
	 using the current set of numbers as Y values and another set
	 as X values.

	 @param x X values
	 @return The slope
	*/
	public final double Slope(Statistics x)
	{
		return ATerm2(x);
	}

//	/**
//	 Calculates the slope (a term) of the linear regression function (y = a*x + b)
//	 given the Y and X values.
//
//	 @param y Y values
//	 @param x X values
//	 @return The slope
//	*/
//	public static double Slope(Statistics y, Statistics x)
//	{
//		return ATerm2(y, x);
//	}

	/**
	 Calculates the residuals of the linear regression function
	 using the current set of numbers as Y values and another set
	 as X values.

	 @param x X values
	 @return A set of residuals
	*/
	public final Statistics Residuals(Statistics x)
	{
		return Residuals(this, x);
	}

	/**
	 Calculates the residuals of the linear regression function
	 given the Y and X values.

	 @param y Y values
	 @param x X values
	 @return A set of residuals
	*/
	public static Statistics Residuals(Statistics y, Statistics x)
	{
		double a = ATerm2(y, x);
		double b = BTerm2(y, x);

		java.util.ArrayList<Double> residuals = new java.util.ArrayList<Double>();
		for (int i = 0; i < x.getLength(); i++)
		{
			residuals.add(y._list[i] - (a*x._list[i] + b));
		}
		return new Statistics(residuals);
	}
}