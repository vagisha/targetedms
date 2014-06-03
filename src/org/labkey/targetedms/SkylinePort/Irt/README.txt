Most of the code in package org.labkey.targetedms.SkylinePort.Irt was ported in 5/2014 from the trunk of
the Skyline C# code base:
https://svn.code.sf.net/p/proteowizard/code/trunk/pwiz/pwiz_tools/Skyline/

Individual classes were broken out for ease of implementation; otherwise this is as close a direct port from the C# syntax used to Java 1.7 as was possible. As such, the naming standards
may appear a bit odd (initial caps on method names, etc).

Examples of syntax which did not translate directly:
implicit typing (substituted real types)
LINQ (substituted iteration to populate Maps & Lists)
lambdas (substituted hard values in the few cases necessary)

In a few cases the iterator instance used in a for-each was renamed for clarity.
Most XML serialization methods and annotations have been removed (or at least commented out).

Original comments have been preserved.

RetentionTimeProviderImpl, IncompleteStandardException, and RefObject were introduced as helper/conversion/implementation classes to wire the converted C#
code into the existing Skyline import java code.

This is not a complete port of the Skyline iRT code; it is a superset of the bare minimum needed to support applying regression calculations to imported retention time
data. Further cleanup could be done to remove unused methods/codepaths.