
/* missed in sqlserver version of targetedms-16.10-16.11.sql */
EXEC core.fn_dropifexists 'Runs', 'targetedms', 'DEFAULT', 'PeptideGroupCount';
ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;