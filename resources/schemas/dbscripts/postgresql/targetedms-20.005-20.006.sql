-- Issue 38773: Remove PanoramaPublic tables from the targetedms schema
DROP TABLE IF EXISTS targetedms.JournalExperiment;
DROP TABLE IF EXISTS targetedms.Journal;
DROP TABLE IF EXISTS targetedms.ExperimentAnnotations;