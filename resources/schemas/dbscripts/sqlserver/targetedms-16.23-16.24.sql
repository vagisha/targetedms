/* IX_Runs_ExperimentRunLSID */

EXEC core.fn_dropifexists 'Runs','targetedms','INDEX','IX_Runs_ExperimentRunLSID';
CREATE INDEX IX_Runs_ExperimentRunLSID ON targetedms.Runs(ExperimentRunLSID, Id);

/* precursorchrominfo.Container */

ALTER TABLE targetedms.precursorchrominfo ADD container ENTITYID;
GO

UPDATE targetedms.precursorchrominfo
SET container =
  (SELECT R.container
   FROM targetedms.samplefile sfile
   INNER JOIN targetedms.replicate rep  ON ( rep.id = sfile.ReplicateId )
   INNER JOIN targetedms.runs r ON ( r.id = rep.RunId )
 WHERE sfile.id = SampleFileId );

ALTER TABLE targetedms.precursorchrominfo ALTER COLUMN container ENTITYID NOT NULL;

CREATE INDEX idx_precursorchrominfo_container ON targetedms.precursorchrominfo (container, id);

GO