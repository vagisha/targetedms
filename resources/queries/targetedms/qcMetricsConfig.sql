
SELECT
       qmc.id,
       qmc.name,
       ifnull(qem.enabled, 1) AS Enabled
FROM
      qcmetricconfiguration qmc
FULL JOIN   qcenabledmetrics qem
       ON   qem.metric=qmc.id