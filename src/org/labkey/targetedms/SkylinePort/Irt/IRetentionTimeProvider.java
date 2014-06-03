package org.labkey.targetedms.SkylinePort.Irt;

import java.util.ArrayList;

/*
 * Ported from the Skyline C# code:
 * https://svn.code.sf.net/p/proteowizard/code/trunk/pwiz/pwiz_tools/Skyline/
 * See README.txt for more info.
 * "TPG" comments are tgaluhn's; others are the comments from the original source code.
 */

public interface IRetentionTimeProvider
{
	String getName();

	Double GetRetentionTime(String sequence);

	Integer GetTimeSource(String sequence);

	ArrayList<MeasuredRetentionTime> getPeptideRetentionTimes();
}