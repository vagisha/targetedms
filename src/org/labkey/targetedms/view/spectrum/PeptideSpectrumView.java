/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

package org.labkey.targetedms.view.spectrum;

import org.labkey.api.view.JspView;
import org.springframework.validation.BindException;

/**
 * User: vsharma
 * Date: 5/7/12
 * Time: 12:52 PM
 */
public class PeptideSpectrumView extends JspView<LibrarySpectrumMatch>
{

    public PeptideSpectrumView(LibrarySpectrumMatch specMatch, BindException errors)
    {
        super("/org/labkey/targetedms/view/spectrum/spectrumView.jsp", specMatch, errors);
        setTitle(String.format("%s, Charge %d",
                getModelBean().getModifiedSequence(),
                getModelBean().getCharge()));
    }
}
