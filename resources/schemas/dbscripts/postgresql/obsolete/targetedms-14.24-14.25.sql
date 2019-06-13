/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

--TransitionChromInfo's Identified column can now be one of 'true', 'false' or 'aligned'
ALTER TABLE targetedms.TransitionChromInfo ADD Identified_temp VARCHAR(10);
UPDATE targetedms.TransitionChromInfo SET Identified_temp =(CASE WHEN Identified THEN 'true'
                                                              WHEN Identified IS FALSE THEN 'false'
                                                              ELSE NULL END);
ALTER TABLE targetedms.TransitionChromInfo DROP COLUMN Identified;
ALTER TABLE targetedms.TransitionChromInfo RENAME Identified_temp TO Identified;



--PrecursorChromInfo's Identified column can now be one of 'true', 'false' or 'aligned'
ALTER TABLE targetedms.PrecursorChromInfo ADD Identified_temp VARCHAR(10);
UPDATE targetedms.PrecursorChromInfo SET Identified_temp =(CASE WHEN Identified THEN 'true'
                                                         WHEN Identified IS FALSE THEN 'false'
                                                         ELSE NULL END);
ALTER TABLE targetedms.PrecursorChromInfo DROP COLUMN Identified;
ALTER TABLE targetedms.PrecursorChromInfo RENAME Identified_temp TO Identified;
