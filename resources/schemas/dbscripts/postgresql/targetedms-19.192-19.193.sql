/*
 * Copyright (c) 2019 LabKey Corporation
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

-- ----------------------------------------------------------------------------
-- Updates to MeasuredDriftTime
-- ----------------------------------------------------------------------------
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN DriftTime DROP NOT NULL;
-- From Brian Pratt about the charge field: either a simple number or an addition description-
-- 1, -4, [M+H]. But no hard limit to adduct string. Typically short though.
-- Longest one there seems to be [M+IsoProp+Na+H] (17 characters) though most come in below 10
ALTER TABLE targetedms.MeasuredDriftTime ALTER COLUMN Charge TYPE VARCHAR(30);
ALTER TABLE targetedms.MeasuredDriftTime ADD Ccs DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobility DOUBLE PRECISION;
ALTER TABLE targetedms.MeasuredDriftTime ADD HighEnergyIonMobilityOffset DOUBLE PRECISION;
-- From Brian Pratt about the ion_mobility_units field: Worst case is 23 characters, for Bruker:  inverse_K0_Vsec_per_cm2
ALTER TABLE targetedms.MeasuredDriftTime ADD IonMobilityUnits VARCHAR(30);

