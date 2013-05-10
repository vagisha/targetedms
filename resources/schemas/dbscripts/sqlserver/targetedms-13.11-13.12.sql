UPDATE core.PortalWebParts
SET Permanent = 0
WHERE name = 'Protein Search' AND Container IN
(SELECT EntityId FROM core.Containers WHERE type = 'TargetedMS');
