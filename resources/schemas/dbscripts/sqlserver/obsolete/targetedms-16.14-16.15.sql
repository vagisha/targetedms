/*
 * Copyright (c) 2016 LabKey Corporation
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
 * See the License for the specifict language governing permissions and
 * limitations under the License.
 */

-- Skyline-daily 3.5.1.9426 (and patch release of Skyline 3.5) changed the format of the modified_sequence attribute
-- of the <precursor> element to always have a decimal place in the modification mass string.
-- Example: [+80.0] instead of [+80].
-- Replace strings like [+80] in the modified sequence with [+80.0].
-- Example: K[+96.2]VN[-17]K[+34.1]TES[+80]K[+62.1] -> K[+96.2]VN[-17.0]K[+34.1]TES[+80.0]K[+62.1]
EXEC core.executeJavaUpgradeCode 'updatePrecursorModifiedSequence';
