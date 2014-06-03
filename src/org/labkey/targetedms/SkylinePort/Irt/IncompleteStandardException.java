package org.labkey.targetedms.SkylinePort.Irt;

/*
 * Java equivalent to support port from the Skyline C# code:
 * https://svn.code.sf.net/p/proteowizard/code/trunk/pwiz/pwiz_tools/Skyline/
 * See README.txt for more info.
 * "TPG" comments are tgaluhn's; others are the comments from the original source code.
 */
public class IncompleteStandardException extends RuntimeException
{
	// TPG: This will only be thrown by ChooseRegressionPeptides so it is OK to have an error specific to regressions.
	private static final String ERROR = "The calculator requires all of its standard peptides in order to determine a regression";

	private RetentionScoreCalculatorSpec privateCalculator;
	public final RetentionScoreCalculatorSpec getCalculator()
	{
		return privateCalculator;
	}
	private void setCalculator(RetentionScoreCalculatorSpec value)
	{
		privateCalculator = value;
	}

	public IncompleteStandardException(RetentionScoreCalculatorSpec calc)
	{
		super(ERROR);
		setCalculator(calc);
	}
}