/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.view;

import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.blib.BlibSpectrum;

/**
 * User: vsharma
 * Date: 5/7/12
 * Time: 12:57 PM
 */
public class SpectrumViewBean
{
    private BlibSpectrum _spectrum;
    private String _peptide;
    private String _modifiedSequence;
    private int _charge;
    private PeptideSettings.SpectrumLibrary _library;

    public String getPeptide()
    {
        return _peptide;
    }

    public void setPeptide(String peptide)
    {
        _peptide = peptide;
    }

    public String getModifiedSequence()
    {
        return _modifiedSequence;
    }

    public void setModifiedSequence(String modifiedSequence)
    {
        _modifiedSequence = modifiedSequence;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public BlibSpectrum getSpectrum()
    {
        return _spectrum;
    }

    public void setSpectrum(BlibSpectrum spectrum)
    {
        _spectrum = spectrum;
    }

    public PeptideSettings.SpectrumLibrary getLibrary()
    {
        return _library;
    }

    public void setLibrary(PeptideSettings.SpectrumLibrary library)
    {
        _library = library;
    }
}
