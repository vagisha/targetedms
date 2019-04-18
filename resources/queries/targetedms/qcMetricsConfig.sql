
SELECT
       qmc.id,
       qmc.name,
       CASE WHEN qem.enabled IS NOT NULL THEN qem.enabled
            ELSE TRUE END AS Enabled,
       CASE WHEN qem.metric IS NULL THEN FALSE
            ELSE TRUE END AS Inserted
FROM
      qcmetricconfiguration qmc
FULL JOIN   qcenabledmetrics qem
       ON   qem.metric=qmc.id