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

import org.labkey.api.view.JspView;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.blib.BlibSpectrum;
import org.labkey.targetedms.parser.blib.BlibSpectrumReader;
import org.labkey.targetedms.query.LibraryManager;
import org.labkey.targetedms.query.PrecursorManager;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 5/7/12
 * Time: 12:52 PM
 */
public class PeptideSpectrumView extends JspView<SpectrumViewBean>
{

    public PeptideSpectrumView(Peptide peptide, BindException errors)
    {
        super("/org/labkey/targetedms/view/spectrumView.jsp", getBean(peptide), errors);
        if(hasSpectrum())
        {
            setTitle(String.format("Spectrum Library %s - %s, Charge %d",
                                   getModelBean().getLibrary().getName(),
                                   getModelBean().getPeptide(),
                                   getModelBean().getCharge()));
        }
    }

    public boolean hasSpectrum()
    {
        return getModelBean().getSpectrum() != null;
    }

    private static SpectrumViewBean getBean(Peptide peptide)
    {
        // Get the precursor of this peptide, sorted by label type and charge.
        List<Precursor> precursors = PrecursorManager.getPrecursorsForPeptide(peptide.getId());

        TargetedMSRun run = TargetedMSManager.getRunForPeptide(peptide.getId());

        // Get the spectrum libraries for this run
        List<PeptideSettings.SpectrumLibrary> libraries = LibraryManager.getLibraries(run.getId());
        Map<PeptideSettings.SpectrumLibrary, String> libraryFilePathsMap = LibraryManager.getLibraryFilePaths(run.getId(), libraries);

        // Precursors are sorted by label type (light label first) and charge
        BlibSpectrum spectrum = null;
        Precursor matchedPrecursor = null;
        PeptideSettings.SpectrumLibrary matchedLib = null;
        for(Precursor precursor: precursors)
        {
            for(PeptideSettings.SpectrumLibrary library: libraryFilePathsMap.keySet())
            {
                spectrum = BlibSpectrumReader.getSpectrum(libraryFilePathsMap.get(library),
                                        precursor.getModifiedSequence(),
                                        precursor.getCharge());

                if(spectrum != null)
                {
                    matchedLib = library;
                    break;
                }

            }
            // Return the first match
            if(spectrum != null) {
                matchedPrecursor = precursor;
                break;
            }
        }

        SpectrumViewBean bean = new SpectrumViewBean();
        bean.setPeptide(peptide.getSequence());
        if(spectrum != null)
        {
            bean.setCharge(matchedPrecursor.getCharge());
            bean.setSpectrum(spectrum);
            bean.setModifiedSequence(matchedPrecursor.getModifiedSequence());
            bean.setLibrary(matchedLib);
        }

        return bean;
    }
}
