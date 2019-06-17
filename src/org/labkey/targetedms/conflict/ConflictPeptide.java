/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.targetedms.conflict;

import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.Precursor;

/**
 * User: vsharma
 * Date: 9/20/12
 * Time: 2:34 PM
 */
public class ConflictPeptide
{
    private Peptide _newPeptide;
    private Precursor _newPeptidePrecursor;
    private int _newPeptideRank = Integer.MAX_VALUE;
    private Peptide _oldPeptide;
    private Precursor _oldPeptidePrecursor;
    private int _oldPeptideRank = Integer.MAX_VALUE;

    public Peptide getNewPeptide()
    {
        return _newPeptide;
    }

    public void setNewPeptide(Peptide newPeptide)
    {
        _newPeptide = newPeptide;
    }

    public Precursor getNewPeptidePrecursor()
    {
        return _newPeptidePrecursor;
    }

    public void setNewPeptidePrecursor(Precursor newPeptidePrecursor)
    {
        _newPeptidePrecursor = newPeptidePrecursor;
    }

    public int getNewPeptideRank()
    {
        return _newPeptideRank;
    }

    public void setNewPeptideRank(int newPeptideRank)
    {
        _newPeptideRank = newPeptideRank;
    }

    public Peptide getOldPeptide()
    {
        return _oldPeptide;
    }

    public void setOldPeptide(Peptide oldPeptide)
    {
        _oldPeptide = oldPeptide;
    }

    public Precursor getOldPeptidePrecursor()
    {
        return _oldPeptidePrecursor;
    }

    public void setOldPeptidePrecursor(Precursor oldPeptidePrecursor)
    {
        _oldPeptidePrecursor = oldPeptidePrecursor;
    }

    public int getOldPeptideRank()
    {
        return _oldPeptideRank;
    }

    public void setOldPeptideRank(int oldPeptideRank)
    {
        _oldPeptideRank = oldPeptideRank;
    }
}
