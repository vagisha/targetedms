UPDATE core.PortalWebParts
SET Permanent = false
WHERE Name = 'Protein Search' AND Container IN
(SELECT ObjectId FROM prop.PropertySets ps JOIN prop.Properties p ON ps.Set = p.Set
WHERE ps.Category = 'folderType' AND p.Value = 'Targeted MS');
