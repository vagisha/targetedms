
/* The run related count values are now calculated by the server in TargetedMSSchema.getTargetedMSRunsTable */
ALTER TABLE targetedms.Runs DROP COLUMN PeptideGroupCount;
ALTER TABLE targetedms.Runs DROP COLUMN PeptideCount;
ALTER TABLE targetedms.Runs DROP COLUMN PrecursorCount;
ALTER TABLE targetedms.Runs DROP COLUMN TransitionCount;

/* FK from targetedms.Peptide to targetedms.GeneralMolecule wasn't applied (issue 25789) */
ALTER TABLE targetedms.Peptide ADD CONSTRAINT FK_Id_GMId FOREIGN KEY (Id) REFERENCES targetedms.GeneralMolecule (Id);