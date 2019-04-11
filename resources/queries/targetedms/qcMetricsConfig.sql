
SELECT
       qmc.id,
       qmc.name,
       ifnull(qem.enabled, 1) AS Enabled,
       CASE WHEN qem.metric IS NULL THEN FALSE
            ELSE TRUE END AS Inserted
FROM
      qcmetricconfiguration qmc
FULL JOIN   qcenabledmetrics qem
       ON   qem.metric=qmc.id