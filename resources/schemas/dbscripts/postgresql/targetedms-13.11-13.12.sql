UPDATE core.portalwebparts 
SET permanent = false
WHERE name = 'Protein Search' AND container IN
(SELECT entityid FROM core.containers WHERE type = 'TargetedMS');
