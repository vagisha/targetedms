/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
PARAMETERS(RUN_ID INTEGER)
WITH logTree as (
  SELECT entryId
       , entryHash
       , parentEntryHash
  FROM targetedms.AuditLogEntry e
  WHERE e.versionId = RUN_ID
  UNION ALL
  SELECT nxt.entryId
       , nxt.entryHash
       , nxt.parentEntryHash
  FROM targetedms.AuditLogEntry nxt
         JOIN logTree prev
              ON prev.parentEntryHash = nxt.entryHash
)
SELECT tree.entryId
     , CASE when msg.orderNumber = 0 THEN ent.createTimestamp END	create_timestamp
     , CASE when msg.orderNumber = 0 THEN ent.documentGUID END		document_guid
     , CASE when msg.orderNumber = 0 THEN ent.reason END			reason
     , CASE when msg.orderNumber = 0 THEN (ent.timezoneOffset/60) END timezone_offset
     , CASE when msg.orderNumber = 0 THEN ent.userName END			user_name
     , CASE WHEN msg.orderNumber = 0 AND ent.extraInfo IS NOT NULL THEN '(info)' END has_extra_info
     , msg.orderNumber
     , CASE msg.orderNumber
         WHEN 0 THEN 'UndoRedo'
         WHEN 1 THEN 'Summary'
         ELSE 'All Info'
  END					message_type
     , msg.messageType		message_info_type
     , msg.enText			message_text
FROM logTree tree
       JOIN targetedms.AuditLogEntry ent
            ON tree.entryId = ent.entryId
       JOIN targetedms.AuditLogMessage msg
            ON ent.entryId = msg.entryId
ORDER BY ent.createTimestamp, msg.orderNumber