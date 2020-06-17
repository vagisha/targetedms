CREATE VIEW targetedms.AuditLog AS

WITH logTree as (
    SELECT entryId
         , entryHash
         , versionid AS RunId
         , parentEntryHash
    FROM targetedms.AuditLogEntry e
    WHERE versionid IS NOT NULL

    UNION ALL

    SELECT nxt.entryId
         , nxt.entryHash
         , COALESCE(nxt.versionid, prev.RunId) AS RunId
         , nxt.parentEntryHash
    FROM targetedms.AuditLogEntry nxt
             JOIN logTree prev
                  ON prev.parentEntryHash = nxt.entryHash
)
SELECT logTree.entryId
     , e.documentguid
     , e.entryHash
     , logTree.RunId
     , e.createtimestamp
     , e.timezoneoffset
     , e.username
     , e.formatversion
     , e.parentEntryHash
     , e.reason
     , e.extrainfo

FROM (SELECT DISTINCT entryId, RunId FROM logTree) logTree
         INNER JOIN targetedms.AuditLogEntry e ON (logTree.EntryId = e.EntryId);
