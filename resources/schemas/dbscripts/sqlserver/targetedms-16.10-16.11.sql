
/* The run related count values are now calculated by the server in TargetedMSSchema.getTargetedMSRunsTable */
--EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideGroupCount';
--ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideCount';
ALTER TABLE targetedms.Runs DROP COLUMN PeptideCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PrecursorCount';
ALTER TABLE targetedms.Runs DROP COLUMN PrecursorCount;
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'TransitionCount';
ALTER TABLE targetedms.Runs DROP COLUMN TransitionCount;