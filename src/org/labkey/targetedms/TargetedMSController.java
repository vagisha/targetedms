/*
 * Copyright (c) 2012 LabKey Corporation
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

package org.labkey.targetedms;


import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.labkey.api.ProteinService;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.QueryViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.GridView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.PageConfig;
import org.labkey.targetedms.chart.ChromatogramChartMakerFactory;
import org.labkey.targetedms.chart.PrecursorPeakAreaChartMaker;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideChromInfo;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.PeptideChromatogramsTableInfo;
import org.labkey.targetedms.query.PeptideGroupManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorChromatogramsTableInfo;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.TargetedMSTable;
import org.labkey.targetedms.query.TransitionManager;
import org.labkey.targetedms.view.ChromatogramsDataRegion;
import org.labkey.targetedms.view.DocumentPrecursorsView;
import org.labkey.targetedms.view.DocumentTransitionsView;
import org.labkey.targetedms.view.ModifiedPeptideHtmlMaker;
import org.labkey.targetedms.view.PeptidePrecursorChromatogramsView;
import org.labkey.targetedms.view.PeptideSpectrumView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TargetedMSController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(TargetedMSController.class);

    public TargetedMSController()
    {
        setActionResolver(_actionResolver);
    }

    public static ActionURL getShowListURL(Container c)
    {
        return new ActionURL(ShowListAction.class, c);
    }

    public static ActionURL getShowRunURL(Container c)
    {
        return new ActionURL(ShowPrecursorListAction.class, c);
    }

    public static ActionURL getShowRunURL(Container c, int runId)
    {
        ActionURL url = getShowRunURL(c);
        url.addParameter("id", String.valueOf(runId));
        return url;
    }

    // ------------------------------------------------------------------------
    // Begin action
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/targetedms/view/hello.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a list of uploaded documents
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowListAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            QueryView gridView = ExperimentService.get().createExperimentRunWebPart(getViewContext(), TargetedMSModule.EXP_RUN_TYPE);
            gridView.setTitle(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME);
            // gridView.setTitleHref(PageFlowUtil.urlProvider(MS2Urls.class).getShowListUrl(getContainer()));
            gridView.setTitleHref(new ActionURL(TargetedMSController.ShowListAction.class, getContainer()));

            return new VBox(gridView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Skyline Documents");
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // Chromatogram actions
    // ------------------------------------------------------------------------

    @RequiresPermissionClass(ReadPermission.class)
    public class TransitionChromatogramChartAction extends ExportAction<ChromatogramForm>
    {
        @Override
        public void export(ChromatogramForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            TransitionChromInfo tci = TransitionManager.getTransitionChromInfo(getContainer(), form.getId());
            if (tci == null)
            {
                throw new NotFoundException("No such TransitionChromInfo found in this folder: " + form.getId());
            }
            PrecursorChromInfo pci = PrecursorManager.getPrecursorChromInfo(getContainer(), tci.getPrecursorChromInfoId());
            if (pci == null)
            {
                throw new NotFoundException("No such PrecursorChromInfo found in this folder: " + tci.getPrecursorChromInfoId());
            }

            JFreeChart chart = ChromatogramChartMakerFactory.createTransitionChromChart(tci, pci);

            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, form.getChartWidth(), form.getChartHeight());
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class PrecursorChromatogramChartAction extends ExportAction<ChromatogramForm>
    {
        @Override
        public void export(ChromatogramForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            PrecursorChromInfo pChromInfo = PrecursorManager.getPrecursorChromInfo(getContainer(), form.getId());
            if (pChromInfo == null)
            {
                throw new NotFoundException("No PrecursorChromInfo found in this folder for precursorChromInfoId: " + form.getId());
            }

            JFreeChart chart = ChromatogramChartMakerFactory.createPrecursorChromChart(pChromInfo, form.isSyncY(), form.isSyncX());
            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, form.getChartWidth(), form.getChartHeight());
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class PeptideChromatogramChartAction extends ExportAction<ChromatogramForm>
    {
        @Override
        public void export(ChromatogramForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            PeptideChromInfo pChromInfo = PeptideManager.getPeptideChromInfo(getContainer(), form.getId());
            if (pChromInfo == null)
            {
                throw new NotFoundException("No PeptideChromInfo found in this folder for peptideChromInfoId: " + form.getId());
            }

            JFreeChart chart = ChromatogramChartMakerFactory.createPeptideChromChart(pChromInfo, form.isSyncY(), form.isSyncX());
            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, form.getChartWidth(), form.getChartHeight());
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class PrecursorAllChromatogramsChartAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private int _peptideId; // save for use in appendNavTrail

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors) throws Exception
        {
            int precursorId = form.getId();
            Precursor precursor = PrecursorManager.getPrecursor(getContainer(), precursorId);
            if (precursor == null)
            {
                throw new NotFoundException("No such Precursor found in this folder: " + precursorId);
            }

            _run = TargetedMSManager.getRunForPrecursor(precursorId);
            _peptideId = precursor.getPeptideId();

            Peptide peptide = PeptideManager.get(precursor.getPeptideId());

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            PeptideSettings.IsotopeLabel label = IsotopeLabelManager.getIsotopeLabel(precursor.getIsotopeLabelId());

            PrecursorChromatogramsViewBean bean = new PrecursorChromatogramsViewBean(
                    new ActionURL(PrecursorAllChromatogramsChartAction.class, getContainer()).getLocalURIString()
            );

            bean.setForm(form);
            bean.setPrecursor(precursor);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setIsotopeLabel(label);
            bean.setRun(_run);

            JspView<List<PrecursorChromatogramsViewBean>> precursorInfo = new JspView("/org/labkey/targetedms/view/precursorChromatogramsView.jsp", bean);
            precursorInfo.setFrame(WebPartView.FrameType.PORTAL);
            precursorInfo.setTitle("Precursor");

            PrecursorChromatogramsTableInfo tableInfo = new PrecursorChromatogramsTableInfo(getContainer());
            tableInfo.setPrecursorId(precursorId);
            tableInfo.addPrecursorFilter();

            ChromatogramsDataRegion dRegion = new ChromatogramsDataRegion(getViewContext(), tableInfo,
                                                                   ChromatogramsDataRegion.PRECURSOR_CHROM_DATA_REGION);
            GridView gridView = new GridView(dRegion, errors);

            VBox vbox = new VBox();
            vbox.addView(precursorInfo);
            vbox.addView(gridView);
            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));

                root.addChild(_run.getFileName(), getShowRunURL(getContainer(), _run.getId()));

                ActionURL precChromUrl = new ActionURL(PeptideAllChromatogramsChartAction.class, getContainer());
                precChromUrl.addParameter("id", String.valueOf(_peptideId));
                root.addChild("Peptide Chromatograms", precChromUrl);

                root.addChild("Precursor Chromatograms");
            }
            return root;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class PeptideAllChromatogramsChartAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors) throws Exception
        {
            int peptideId = form.getId();
            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if (peptide == null)
            {
                throw new NotFoundException("No such Peptide found in this folder: " + peptideId);
            }

            _run = TargetedMSManager.getRunForPeptide(peptideId);

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            PeptideChromatogramsViewBean bean = new PeptideChromatogramsViewBean(
                    new ActionURL(PeptideAllChromatogramsChartAction.class, getContainer()).getLocalURIString()
            );

            bean.setForm(form);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setRun(_run);
            bean.setLabels(IsotopeLabelManager.getIsotopeLabels(_run.getId()));
            bean.setPrecursorList(PrecursorManager.getPrecursorsForPeptide(peptide.getId()));

            JspView<List<PrecursorChromatogramsViewBean>> peptideInfo = new JspView("/org/labkey/targetedms/view/peptideSummaryView.jsp", bean);
            peptideInfo.setFrame(WebPartView.FrameType.PORTAL);
            peptideInfo.setTitle("Peptide");

            PeptideChromatogramsTableInfo tableInfo = new PeptideChromatogramsTableInfo(getContainer());
            tableInfo.setPeptideId(peptideId);
            tableInfo.addPeptideFilter();

            ChromatogramsDataRegion dRegion = new ChromatogramsDataRegion(getViewContext(), tableInfo,
                                                              ChromatogramsDataRegion.PEPTIDE_CHROM_DATA_REGION);
            GridView gridView = new GridView(dRegion, errors);

            VBox vbox = new VBox();
            vbox.addView(peptideInfo);
            vbox.addView(gridView);
            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getFileName(), getShowRunURL(getContainer(), _run.getId()));
                root.addChild("Peptide Chromatograms");
            }
            return root;
        }
    }

    public static class PrecursorChromatogramsViewBean extends PeptideChromatogramsViewBean
    {
        private Precursor _precursor;
        private PeptideSettings.IsotopeLabel _isotopeLabel;

        public PrecursorChromatogramsViewBean(String resultsUri)
        {
            super(resultsUri);
        }

        public Precursor getPrecursor()
        {
            return _precursor;
        }

        public void setPrecursor(Precursor precursor)
        {
            _precursor = precursor;
        }

        public String getModifiedPeptideHtml()
        {
           return ModifiedPeptideHtmlMaker.getHtml(getPrecursor().getId());
        }

        public PeptideSettings.IsotopeLabel getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(PeptideSettings.IsotopeLabel isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }
    }

    public static class PeptideChromatogramsViewBean
    {
        private ChromatogramForm _form;
        private Peptide _peptide;
        private PeptideGroup _peptideGroup;
        private List<Precursor> _precursorList;
        private int _lightIsotopeLableId;
        private List<PeptideSettings.IsotopeLabel> labels;
        private TargetedMSRun _run;
        protected String _resultsUri;

        public PeptideChromatogramsViewBean(String resultsUri)
        {
            _resultsUri = resultsUri;
        }
        public String getResultsUri()
        {
            return _resultsUri;
        }

        public ChromatogramForm getForm()
        {
            return _form;
        }

        public void setForm(ChromatogramForm form)
        {
            _form = form;
        }

        public Peptide getPeptide()
        {
            return _peptide;
        }

        public void setPeptide(Peptide peptide)
        {
            _peptide = peptide;
        }

        public TargetedMSRun getRun()
        {
            return _run;
        }

        public void setRun(TargetedMSRun run)
        {
            _run = run;
        }

        public PeptideGroup getPeptideGroup()
        {
            return _peptideGroup;
        }

        public void setPeptideGroup(PeptideGroup peptideGroup)
        {
            _peptideGroup = peptideGroup;
        }

        public List<Precursor> getPrecursorList()
        {
            return _precursorList;
        }

        public void setPrecursorList(List<Precursor> precursorList)
        {
            _precursorList = precursorList;
        }

        public int getLightIsotopeLableId()
        {
            return _lightIsotopeLableId;
        }

        public void setLightIsotopeLableId(int lightIsotopeLableId)
        {
            _lightIsotopeLableId = lightIsotopeLableId;
        }

        public List<PeptideSettings.IsotopeLabel> getLabels()
        {
            return labels;
        }

        public void setLabels(List<PeptideSettings.IsotopeLabel> labels)
        {
            this.labels = labels;
        }
    }

    public static class ChromatogramForm
    {
        private int _id;
        private int _chartWidth = 400;
        private int _chartHeight = 400;
        private boolean _syncY = false;
        private boolean _syncX = false;

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }

        public int getChartWidth()
        {
            return _chartWidth;
        }

        public void setChartWidth(int chartWidth)
        {
            _chartWidth = chartWidth;
        }

        public int getChartHeight()
        {
            return _chartHeight;
        }

        public void setChartHeight(int chartHeight)
        {
            _chartHeight = chartHeight;
        }

        public boolean isSyncY()
        {
            return _syncY;
        }

        public void setSyncY(boolean syncY)
        {
            _syncY = syncY;
        }

        public boolean isSyncX()
        {
            return _syncX;
        }

        public void setSyncX(boolean syncX)
        {
            _syncX = syncX;
        }
    }


    // ------------------------------------------------------------------------
    // Action to display peptide details page
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private String _sequence;

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors) throws Exception
        {
            int peptideId = form.getId();  // peptide Id

            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if(peptide == null)
            {
                throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", peptideId));
            }
            _sequence = peptide.getSequence();

            VBox vbox = new VBox();

            _run = TargetedMSManager.getRunForPeptide(peptideId);

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            List<Precursor> precursorList = PrecursorManager.getPrecursorsForPeptide(peptide.getId());

            List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(_run.getId());

            PeptideChromatogramsViewBean bean = new PeptideChromatogramsViewBean(
                new ActionURL(ShowPeptideAction.class, getContainer()).getLocalURIString());
            bean.setForm(form);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setPrecursorList(precursorList);
            bean.setLightIsotopeLableId(labels.get(0).getId());
            bean.setLabels(labels);
            bean.setRun(_run);

            // summary for this peptide
            JspView<List<PeptideChromatogramsViewBean>> peptideInfo = new JspView("/org/labkey/targetedms/view/peptideSummaryView.jsp", bean);
            peptideInfo.setFrame(WebPartView.FrameType.PORTAL);
            peptideInfo.setTitle("Peptide Summary");
            vbox.addView(peptideInfo);

            // precursor and transition chromatograms. One row per replicate
            JspView<List<PeptideChromatogramsViewBean>> chartForm = new JspView("/org/labkey/targetedms/view/chromatogramsForm.jsp", bean);
            PeptidePrecursorChromatogramsView chromView = new PeptidePrecursorChromatogramsView(peptide, getContainer(),
                                                                                                getUser(), form, errors);
            chromView.enableExpandCollapse(PeptidePrecursorChromatogramsView.TITLE, false);
            vbox.addView(chartForm);
            vbox.addView(chromView);


            // library spectrum, if there is one
            PeptideSpectrumView spectrumView = new PeptideSpectrumView(peptide, errors);
            if(spectrumView.hasSpectrum())
            {
                spectrumView.enableExpandCollapse("PeptideSpectrumView", false);
                vbox.addView(spectrumView);
            }

            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getFileName(), getShowRunURL(getContainer(), _run.getId()));
                root.addChild(_sequence);
            }
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a library spectrum
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowSpectrumAction extends SimpleViewAction<ShowSpectrumForm>
    {
        @Override
        public ModelAndView getView(ShowSpectrumForm form, BindException errors) throws Exception
        {
            int peptideId = form.getId();  // peptide Id

            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if(peptide == null)
            {
                throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", peptideId));
            }

            PeptideSpectrumView view = new PeptideSpectrumView(peptide, errors);
            view.enableExpandCollapse("PeptideSpectrumView", false);
            VBox vbox = new VBox(view);
            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return null;  //TODO: link back to peptides details page
        }
    }

    public static class ShowSpectrumForm
    {
        private int _id;

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a peak areas for peptides of a protein
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptidePeakAreasAction extends ExportAction<ShowPeakAreaForm>
    {
        @Override
        public void export(ShowPeakAreaForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            int peptideGroupId = form.getId();  // peptide group Id

            PeptideGroup peptideGrp = PeptideGroupManager.getPeptideGroup(getContainer(), peptideGroupId);
            if(peptideGrp == null)
            {
                throw new NotFoundException(String.format("No peptide group found in this folder for peptideId: %d", peptideGroupId));
            }

            JFreeChart chart = PrecursorPeakAreaChartMaker.make(peptideGrp);
            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, form.getChartWidth(), form.getChartHeight());
        }
    }

    public static class ShowPeakAreaForm
    {
        private int _id;
        private int _chartWidth = 600;
        private int _chartHeight = 400;

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }

        public int getChartWidth()
        {
            return _chartWidth;
        }

        public void setChartWidth(int chartWidth)
        {
            _chartWidth = chartWidth;
        }

        public int getChartHeight()
        {
            return _chartHeight;
        }

        public void setChartHeight(int chartHeight)
        {
            _chartHeight = chartHeight;
        }
    }

    // ------------------------------------------------------------------------
    // Document upload action
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(InsertPermission.class)
    public class SkylineDocUploadAction extends RedirectAction<PipelinePathForm>
    {
        public ActionURL getSuccessURL(PipelinePathForm form)
        {
            return TargetedMSController.getShowListURL(getContainer());
        }

        public void validateCommand(PipelinePathForm form, Errors errors)
        {
        }

        public boolean doAction(PipelinePathForm form, BindException errors) throws Exception
        {
            for (File file : form.getValidatedFiles(getContainer()))
            {
                if (!file.isFile())
                {
                    throw new NotFoundException("Expected a file but found a directory: " + file.getName());
                }

                ViewBackgroundInfo info = getViewBackgroundInfo();
                try
                {
                    TargetedMSManager.addRunToQueue(info, file, form.getPipeRoot(getContainer()));
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
                catch (SQLException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a document's transition or precursor list
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public abstract class ShowRunDetailsAction <VIEWTYPE extends NestableQueryView> extends QueryViewAction<RunDetailsForm, VIEWTYPE>
    {
        protected TargetedMSRun _run;  // save for use in appendNavTrail

        public ShowRunDetailsAction()
        {
            super(RunDetailsForm.class);
        }

        public ModelAndView getHtmlView(final RunDetailsForm form, BindException errors) throws Exception
        {
            //this action requires that a specific experiment run has been specified
            if(!form.hasRunId())
                throw new RedirectException(new ActionURL(TargetedMSController.BeginAction.class, getViewContext().getContainer()));

            //ensure that the experiment run is valid and exists within the current container
            _run = validateRun(form.getId());

            VBox vBox = new VBox();

            RunDetailsBean bean = new RunDetailsBean();
            bean.setForm(form);
            bean.setRun(_run);

            JspView<RunDetailsBean> runSummaryView = new JspView<RunDetailsBean>("/org/labkey/targetedms/view/runSummaryView.jsp", bean);
            runSummaryView.setFrame(WebPartView.FrameType.PORTAL);
            runSummaryView.setTitle("Document Summary");

            vBox.addView(runSummaryView);

            VIEWTYPE view = createInitializedQueryView(form, errors, false, getDataRegionName());
            vBox.addView(view);

            NavTree menu = getViewSwitcherMenu();
            view.setNavMenu(menu);
            return vBox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getBaseName());
            }
            return root;
        }

        public abstract String getDataRegionName();

        public abstract NavTree getViewSwitcherMenu();

    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ShowTransitionListAction extends ShowRunDetailsAction<DocumentTransitionsView>
    {
        @Override
        protected DocumentTransitionsView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            DocumentTransitionsView view = new DocumentTransitionsView(getViewContext(),
                                                                       new TargetedMSSchema(getUser(), getContainer()),
                                                                       form.getId(),
                                                                       forExport);
            view.setShowExportButtons(true);
            return view;
        }

        @Override
        public String getDataRegionName()
        {
            return DocumentTransitionsView.DATAREGION_NAME;
        }

        @Override
        public NavTree getViewSwitcherMenu()
        {
            NavTree menu = new NavTree();
            ActionURL url = new ActionURL(ShowPrecursorListAction.class, getContainer());
            url.addParameter("id", _run.getId());

            menu.addChild(DocumentPrecursorsView.TITLE, url);
            return menu;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPrecursorListAction extends ShowRunDetailsAction<DocumentPrecursorsView>
    {
        @Override
        protected DocumentPrecursorsView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            DocumentPrecursorsView view = new DocumentPrecursorsView(getViewContext(),
                                                                   new TargetedMSSchema(getUser(), getContainer()),
                                                                   form.getId(),
                                                                   forExport);
            view.setShowExportButtons(true);
            return view;
        }

        @Override
        public String getDataRegionName()
        {
            return DocumentPrecursorsView.DATAREGION_NAME;
        }

        @Override
        public NavTree getViewSwitcherMenu()
        {
            NavTree menu = new NavTree();
            ActionURL url = new ActionURL(ShowTransitionListAction.class, getContainer());
            url.addParameter("id", _run.getId());

            menu.addChild(DocumentTransitionsView.TITLE, url);
            return menu;
        }
    }

    public static class RunDetailsForm extends QueryViewAction.QueryExportForm
    {
        private int _id = 0;
        private String _view;

        public void setId(int id)
        {
            _id = id;
        }

        public int getId()
        {
            return _id;
        }

        public boolean hasRunId() {
            return _id > 0;
        }

        public String getView()
        {
            return _view;
        }

        public void setView(String view)
        {
            _view = view;
        }
    }

    public static class RunDetailsBean
    {
        private RunDetailsForm _form;
        private TargetedMSRun _run;

        public RunDetailsForm getForm()
        {
            return _form;
        }

        public void setForm(RunDetailsForm form)
        {
            _form = form;
        }

        public TargetedMSRun getRun()
        {
            return _run;
        }

        public void setRun(TargetedMSRun run)
        {
            _run = run;
        }
    }

    @NotNull
    private TargetedMSRun validateRun(int runId)
    {
        Container c = getContainer();
        TargetedMSRun run = TargetedMSManager.getRun(runId);

        if (null == run)
            throw new NotFoundException("Run " + runId + " not found");
        if (run.isDeleted())
            throw new NotFoundException("Run has been deleted.");
        if (run.getStatusId() == SkylineDocImporter.STATUS_RUNNING)
            throw new NotFoundException("Run is still loading.  Current status: " + run.getStatus());
        if (run.getStatusId() == SkylineDocImporter.STATUS_FAILED)
            throw new NotFoundException("Run failed loading.  Status: " + run.getStatus());

        Container container = run.getContainer();

        if (null == container || !container.equals(c))
        {
            ActionURL url = getViewContext().getActionURL().clone();
            url.setContainer(run.getContainer());
            throw new RedirectException(url);
        }

        return run;
    }

    // ------------------------------------------------------------------------
    // Action to show a protein detail page
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinAction extends SimpleViewAction<ProteinDetailForm>
    {
        @Override
        public ModelAndView getView(final ProteinDetailForm form, BindException errors) throws Exception
        {
            PeptideGroup group = PeptideGroupManager.getPeptideGroup(getContainer(), form.getId());
            if (group == null)
            {
                throw new NotFoundException("Could not find peptide group #" + form.getId());
            }

            // Peptide group details
            DataRegion groupDetails = new DataRegion();
            TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
            TableInfo tableInfo = schema.getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
            groupDetails.setColumns(tableInfo.getColumns("Label", "Description", "Decoy", "Note"));
            groupDetails.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY);
            DetailsView groupDetailsView = new DetailsView(groupDetails, form.getId());
            groupDetailsView.setTitle("Peptide Group");

            VBox result = new VBox(groupDetailsView);


            // Protein sequence coverage
            if (group.getSequenceId() != null)
            {
                int seqId = group.getSequenceId().intValue();
                List<String> peptideSequences = new ArrayList<String>();
                for (Peptide peptide : PeptideManager.getPeptidesForGroup(group.getId()))
                {
                    peptideSequences.add(peptide.getSequence());
                }

                ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
                WebPartView sequenceView = proteinService.getProteinCoverageView(seqId, peptideSequences.toArray(new String[peptideSequences.size()]), 100);

                sequenceView.setTitle("Sequence Coverage");
                sequenceView.enableExpandCollapse("SequenceCoverage", false);
                result.addView(sequenceView);

                result.addView(proteinService.getAnnotationsView(seqId));
            }

            // List of peptides
            QuerySettings settings = new QuerySettings(getViewContext(), "Peptides", "Peptide");
            settings.setAllowChooseQuery(false);
            QueryView peptidesView = new QueryView(schema, settings, errors)
            {
                @Override
                protected TableInfo createTable()
                {
                    TargetedMSTable result = (TargetedMSTable) super.createTable();
                    result.addCondition(new SimpleFilter("PeptideGroupId", form.getId()));
                    List<FieldKey> visibleColumns = new ArrayList<FieldKey>();
                    visibleColumns.add(FieldKey.fromParts("Sequence"));
                    visibleColumns.add(FieldKey.fromParts("CalcNeutralMass"));
                    visibleColumns.add(FieldKey.fromParts("NumMissedCleavages"));
                    visibleColumns.add(FieldKey.fromParts("Rank"));
                    visibleColumns.add(FieldKey.fromParts("AvgMeasuredRetentionTime"));
                    visibleColumns.add(FieldKey.fromParts("PredictedRetentionTime"));
                    result.setDefaultVisibleColumns(visibleColumns);
                    return result;
                }
            };
            peptidesView.setTitle("Peptides");
            peptidesView.enableExpandCollapse("TargetedMSPeptides", false);
            peptidesView.setUseQueryViewActionExportURLs(true);
            result.addView(peptidesView);


            // Peptide peak areas
            ActionURL peakAreaUrl = new ActionURL(ShowPeptidePeakAreasAction.class, getContainer());
            peakAreaUrl.addParameter("id", form.getId());
            List<SampleFile> sampleFiles = ReplicateManager.getSampleFilesForRun(group.getRunId());
            //peakAreaUrl.addParameter("sampleFileId", sampleFiles.get(0).getId());
            JspView<String> peakAreaView = new JspView<String>("/org/labkey/targetedms/view/peptidePeakAreaView.jsp",
                                                               peakAreaUrl.getLocalURIString());
            peakAreaView.setTitle("Peak Areas");

            result.addView(peakAreaView);

            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    public static class ProteinDetailForm
    {
        private int _id;

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a protein detail page
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinAJAXAction extends SimpleViewAction<ProteinDetailForm>
    {
        @Override
        public ModelAndView getView(ProteinDetailForm form, BindException errors) throws Exception
        {
            PeptideGroup group = PeptideGroupManager.getPeptideGroup(getContainer(), form.getId());
            if (group == null)
            {
                throw new NotFoundException("Could not find peptide group #" + form.getId());
            }

            if (group.getSequenceId()!= null)
            {
                int seqId = group.getSequenceId().intValue();
                List<String> peptideSequences = new ArrayList<String>();
                for (Peptide peptide : PeptideManager.getPeptidesForGroup(group.getId()))
                {
                    peptideSequences.add(peptide.getSequence());
                }
                ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
                ActionURL searchURL = PageFlowUtil.urlProvider(MS2Urls.class).getProteinSearchUrl(getContainer());
                searchURL.addParameter("seqId", group.getSequenceId().intValue());
                searchURL.addParameter("identifier", group.getLabel());
                getViewContext().getResponse().getWriter().write("<a href=\"" + searchURL + "\">Search for other references to this protein</a><br/>");
                WebPartView sequenceView = proteinService.getProteinCoverageView(seqId, peptideSequences.toArray(new String[peptideSequences.size()]), 40);
                sequenceView.render(getViewContext().getRequest(), getViewContext().getResponse());
            }

            getPageConfig().setTemplate(PageConfig.Template.None);
            return null;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }
}