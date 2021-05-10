/*
 * Copyright (c) 2013 LabKey Corporation
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

package org.labkey.targetedms.passport;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.WebPartView;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.model.passport.IFile;
import org.labkey.targetedms.model.passport.IPeptide;
import org.labkey.targetedms.model.passport.IProtein;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.query.PeptideGroupManager;
import org.labkey.targetedms.view.passport.ProteinListView;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.labkey.targetedms.TargetedMSManager.getSqlDialect;

public class PassportController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(PassportController.class);
    private static final Logger LOG = Logger.getLogger(PassportController.class);

    public PassportController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            ProteinListView runListView = ProteinListView.createView(getViewContext());
            VBox vbox = new VBox();
            vbox.addView(runListView);
            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    public static class ProteinForm
    {
        private Integer _proteinId;
        private boolean _new = false;

        public Integer getProteinId()
        {
            return _proteinId;
        }

        public void setProteinId(Integer proteinId)
        {
            _proteinId = proteinId;
        }

        public boolean isNew()
        {
            return _new;
        }

        public void setNew(boolean aNew)
        {
            _new = aNew;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ProteinAction extends SimpleViewAction<ProteinForm>
    {
        private String _title;
        private IProtein _protein;

        @Override
        public ModelAndView getView(ProteinForm form, BindException errors) throws IOException, SAXException, ParserConfigurationException
        {
            if (form.getProteinId() == null)
            {
                throw new NotFoundException("No valid protein ID specified");
            }
            _protein = getProtein(form.getProteinId());

            if (null == _protein)
            {
                throw new NotFoundException("Protein not found for id: " + form.getProteinId());
            }
            boolean beforeAfter = _protein.getPep().stream().anyMatch(pep -> pep.getReplicateInfo().stream().anyMatch(rep -> "BeforeIncubation".equalsIgnoreCase(rep.getReplicate())));
            _title = (beforeAfter ? "Passport Protein View" : "Reproducibility Report") + ": " + _protein.getName();

            VBox result = new VBox();
            PeptideGroup group = PeptideGroupManager.getPeptideGroup(getContainer(), _protein.getPepGroupId());
            TargetedMSRun run = TargetedMSManager.getRun(group.getRunId());
            TargetedMSController.addProteinSummaryViews(result, group, run, getUser(), getContainer());

            if (beforeAfter)
            {
                result.addView(new JspView<>("/org/labkey/targetedms/view/passport/beforeAfterReport.jsp", _protein));
            }
            else
            {
                JspView<?> filterSection = new JspView<>("/org/labkey/targetedms/view/passport/MxNReport.jsp", _protein);
                filterSection.setTitle("Precursors");
                filterSection.setFrame(WebPartView.FrameType.PORTAL);
                result.addView(filterSection);

                JspView<?> chartSection = new JspView<>("/org/labkey/targetedms/view/passport/charts.jsp");
                chartSection.setTitle("Comparison Plots");
                chartSection.setFrame(WebPartView.FrameType.PORTAL);
                result.addView(chartSection);

                JspView<?> chromatogramSection = new JspView<>("/org/labkey/targetedms/view/passport/chromatograms.jsp");
                chromatogramSection.setTitle("Chromatograms");
                chromatogramSection.setFrame(WebPartView.FrameType.PORTAL);
                result.addView(chromatogramSection);
            }

            return result;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (_protein != null)
            {
                root.addChild(_protein.getName(), new ActionURL(TargetedMSController.ShowProteinAction.class, getContainer()).addParameter("id", _protein.getPepGroupId()));
            }
            if (_title != null)
            {
                root.addChild(_title);
            }
        }
    }

    // returns null if no protein found
    @Nullable
    private IProtein getProtein(Integer proteinId) throws ParserConfigurationException, SAXException, IOException
    {
        SQLFragment targetedMSProteinQuery = new SQLFragment();
        targetedMSProteinQuery.append("SELECT ps.seqid as seqid, pg.accession, ps.bestgenename, ps.description, ps.protsequence, ps.length, " +
                "pg.id as pgid, pg.species, pg.preferredname, pg.runid, pg.label, " +
                "r.dataid, r.filename, r.created, r.modified, r.formatversion " +
                "FROM targetedms.peptidegroup pg INNER JOIN targetedms.runs r on r.id = pg.runid LEFT OUTER JOIN prot.sequences ps ON ps.seqid = pg.sequenceid " +
                "WHERE r.container = ? AND pg.Id = ?");
        targetedMSProteinQuery.add(getContainer().getId());
        targetedMSProteinQuery.add(proteinId);

        getSqlDialect().limitRows(targetedMSProteinQuery, 1);
        DbSchema schema = TargetedMSManager.getSchema();

        Map<String, Object> map = new SqlSelector(schema, targetedMSProteinQuery).getMap();

        IProtein p = new IProtein();
        if (map == null)
            return null;
        p.setLabel((String) map.get("label"));
        p.setGene((String) map.get("bestgenename"));
        p.setSpecies((String) map.get("species"));
        p.setPreferredname((String) map.get("preferredname"));
        p.setPepGroupId((Long) map.get("pgid"));
        Number seqId = (Number)map.get("seqid");
        if (seqId != null)
        {
            p.setSequenceId(seqId.longValue());
        }
        p.setDescription((String) map.get("description"));
        p.setSequence((String) map.get("protsequence"));

        IFile f = new IFile();
        f.setFileName((String) map.get("filename"));
        f.setSoftwareVersion((String) map.get("softwareversion"));
        f.setCreatedDate((Date) map.get("created"));
        f.setModifiedDate((Date) map.get("modified"));
        f.setRunId((Long) map.get("runid"));

        p.setFile(f);
        p.setAccession((String) map.get("accession"));
        populateProteinKeywords(p);
        p.setFeatures(ProteinService.get().getProteinFeatures(p.getAccession()));
        populatePeptides(p);

        return p;
    }

    private void populateProteinKeywords(IProtein p)
    {
        if (p.getSequenceId() != null)
        {
            p.setKeywords(TargetedMSManager.getKeywords(p.getSequenceId()));
        }
    }

    private void populatePeptides(IProtein p)
    {
        if (p == null)
            return;

        Map<Long, IPeptide> peptideMap = new HashMap<>();

        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), "targetedms");
        TableInfo tinfo = schema.getTable("Passport_TotalPrecursorArea");

        SimpleFilter sf = new SimpleFilter(FieldKey.fromParts("PepGroupId"), p.getPepGroupId());

        new TableSelector(tinfo, sf, new Sort("AcquiredTime")).forEachResults(pep -> {
            long peptideId = pep.getLong("peptideid");
            IPeptide peptide = peptideMap.get(peptideId);
            if (peptide == null)
            {
                peptide = new IPeptide();
                peptide.setSequence(pep.getString("peptidesequence"));
                peptide.setStartIndex(pep.getInt("startindex"));
                peptide.setEndIndex(pep.getInt("endindex"));
                peptide.setPanoramaPeptideId(peptideId);
                peptide.setProteinId(p.getPepGroupId());
                peptideMap.put(peptide.getPanoramaPeptideId(), peptide);
            }
            long totalArea = pep.getLong("totalarea");
            String replicateName = pep.getString("replicate");
            if ("BeforeIncubation".equals(replicateName))
            {
                peptide.setBeforeTotalArea(totalArea);
                peptide.setPrecursorbeforeid(pep.getLong("panoramaprecursorid"));
                peptide.setBeforeSumArea(pep.getInt("sumarea"));
            }
            else if ("AfterIncubation".equals(replicateName))
            {
                peptide.setAfterTotalArea(totalArea);
                peptide.setPrecursorafterid(pep.getLong("panoramaprecursorid"));
                peptide.setAfterSumArea(pep.getInt("sumarea"));
            }

            String grouping = pep.getString("grouping");
            String timepoint = pep.getString("timepoint");
            if (grouping == null)
            {
                int groupingIndex = 1;
                for (IPeptide.ReplicateInfo replicateInfo : peptide.getReplicateInfo())
                {
                    if (Objects.equals(replicateInfo.getTimepoint(), timepoint))
                    {
                        groupingIndex++;
                    }
                }
                grouping = Integer.toString(groupingIndex);
            }
            peptide.addReplicateInfo(replicateName, timepoint, grouping, totalArea, pep.getLong("panoramaprecursorid"), pep.getDate("acquiredTime"));
        });

        List<IPeptide> peptides = new ArrayList<>();
        for (IPeptide peptide : peptideMap.values())
        {
            // Normalize AfterIncubation TotalArea to global standards from Panorama
            double afterRatioToGlobalStandards = peptide.getAfterTotalArea() / peptide.getAfterSumArea();
            double beforeRatioToGlobalStandards = peptide.getBeforeTotalArea() / peptide.getBeforeSumArea();
            double afterToBeforeRatio = afterRatioToGlobalStandards / beforeRatioToGlobalStandards;
            double normalizedAfterTotalArea = peptide.getBeforeTotalArea() * afterToBeforeRatio;
            peptide.setBeforeIntensity(peptide.getBeforeTotalArea());
            peptide.setAfterIntensity(normalizedAfterTotalArea);
            peptides.add(peptide);
        }
        p.setPep(peptides);
    }
}