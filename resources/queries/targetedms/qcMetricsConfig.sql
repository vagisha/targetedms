
SELECT
       qmc.id,
       qmc.name,
       qmc.Series1Label,
       qmc.Series1SchemaName,
       qmc.Series1QueryName,
       qmc.Series2Label,
       qmc.Series2SchemaName,
       qmc.Series2QueryName,
       CASE WHEN qem.enabled IS NOT NULL THEN qem.enabled
            ELSE TRUE END AS Enabled,
       CASE WHEN qem.metric IS NULL THEN FALSE
            ELSE TRUE END AS Inserted
FROM
      qcmetricconfiguration qmc
FULL JOIN   qcenabledmetrics qem
       ON   qem.metric=qmc.id