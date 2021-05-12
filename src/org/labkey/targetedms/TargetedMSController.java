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

package org.labkey.targetedms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keypoint.PngEncoder;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.action.ApiJsonWriter;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ApiUsageException;
import org.labkey.api.action.CustomApiForm;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.HasViewContext;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.QueryViewAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.analytics.AnalyticsService;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.files.view.FilesWebPart;
import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.model.ViewCategory;
import org.labkey.api.reports.model.ViewCategoryManager;
import org.labkey.api.reports.report.RedirectReport;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.security.ActionNames;
import org.labkey.api.security.RequiresLogin;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ApplicationAdminPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.TargetedMSUrls;
import org.labkey.api.targetedms.model.SampleFileInfo;
import org.labkey.api.util.Button;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.util.DOM;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.util.URLHelper;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.GridView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.PopupMenu;
import org.labkey.api.view.Portal;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.api.view.template.PageConfig;
import org.labkey.api.visualization.VisualizationService;
import org.labkey.targetedms.chart.ChromatogramChartMakerFactory;
import org.labkey.targetedms.chart.ComparisonChartMaker;
import org.labkey.targetedms.chromlib.ChromatogramLibraryUtils;
import org.labkey.targetedms.clustergrammer.ClustergrammerClient;
import org.labkey.targetedms.clustergrammer.ClustergrammerHeatMap;
import org.labkey.targetedms.conflict.ConflictPeptide;
import org.labkey.targetedms.conflict.ConflictPrecursor;
import org.labkey.targetedms.conflict.ConflictProtein;
import org.labkey.targetedms.conflict.ConflictTransition;
import org.labkey.targetedms.model.GuideSet;
import org.labkey.targetedms.model.GuideSetKey;
import org.labkey.targetedms.model.GuideSetStats;
import org.labkey.targetedms.model.QCMetricConfiguration;
import org.labkey.targetedms.model.QCPlotFragment;
import org.labkey.targetedms.model.RawMetricDataSet;
import org.labkey.targetedms.model.passport.IKeyword;
import org.labkey.targetedms.outliers.OutlierGenerator;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.GeneralMoleculeChromInfo;
import org.labkey.targetedms.parser.Molecule;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.QuantificationSettings;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.SampleFileChromInfo;
import org.labkey.targetedms.parser.SkylineBinaryParser;
import org.labkey.targetedms.parser.SkylineDocumentParser;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.parser.list.ListDefinition;
import org.labkey.targetedms.parser.skyaudit.AuditLogEntry;
import org.labkey.targetedms.parser.speclib.SpeclibReaderException;
import org.labkey.targetedms.pipeline.ChromatogramCrawlerJob;
import org.labkey.targetedms.query.ChromatogramDisplayColumnFactory;
import org.labkey.targetedms.query.ConflictResultsManager;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.LibraryManager;
import org.labkey.targetedms.query.ModificationManager;
import org.labkey.targetedms.query.ModifiedSequenceDisplayColumn;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.MoleculePrecursorManager;
import org.labkey.targetedms.query.PeptideChromatogramsTableInfo;
import org.labkey.targetedms.query.PeptideGroupManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorChromatogramsTableInfo;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.SkylineListManager;
import org.labkey.targetedms.query.SkylineListSchema;
import org.labkey.targetedms.query.TargetedMSTable;
import org.labkey.targetedms.query.TransitionManager;
import org.labkey.targetedms.search.ModificationSearchWebPart;
import org.labkey.targetedms.view.CalibrationCurveChart;
import org.labkey.targetedms.view.CalibrationCurveView;
import org.labkey.targetedms.view.CalibrationCurvesView;
import org.labkey.targetedms.view.ChromatogramsDataRegion;
import org.labkey.targetedms.view.DocumentPrecursorsView;
import org.labkey.targetedms.view.DocumentTransitionsView;
import org.labkey.targetedms.view.DocumentView;
import org.labkey.targetedms.view.GroupComparisonView;
import org.labkey.targetedms.view.InstrumentSummaryWebPart;
import org.labkey.targetedms.view.ModifiedPeptideHtmlMaker;
import org.labkey.targetedms.view.MoleculePrecursorChromatogramsView;
import org.labkey.targetedms.view.PeptidePrecursorChromatogramsView;
import org.labkey.targetedms.view.PeptidePrecursorsView;
import org.labkey.targetedms.view.PeptideTransitionsView;
import org.labkey.targetedms.view.QCSummaryWebPart;
import org.labkey.targetedms.view.SmallMoleculePrecursorsView;
import org.labkey.targetedms.view.SmallMoleculeTransitionsView;
import org.labkey.targetedms.view.TargetedMsRunListView;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatch;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;
import org.labkey.targetedms.view.spectrum.PeptideSpectrumView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.labkey.api.targetedms.TargetedMSService.FOLDER_TYPE_PROP_NAME;
import static org.labkey.api.targetedms.TargetedMSService.FolderType;
import static org.labkey.api.targetedms.TargetedMSService.MODULE_NAME;
import static org.labkey.api.targetedms.TargetedMSService.RAW_FILES_DIR;
import static org.labkey.api.targetedms.TargetedMSService.RAW_FILES_TAB;
import static org.labkey.api.util.DOM.Attribute.height;
import static org.labkey.api.util.DOM.Attribute.method;
import static org.labkey.api.util.DOM.Attribute.src;
import static org.labkey.api.util.DOM.Attribute.width;
import static org.labkey.api.util.DOM.DIV;
import static org.labkey.api.util.DOM.SPAN;
import static org.labkey.api.util.DOM.TD;
import static org.labkey.api.util.DOM.TR;
import static org.labkey.api.util.DOM.UL;
import static org.labkey.api.util.DOM.X.FORM;
import static org.labkey.api.util.DOM.at;
import static org.labkey.api.util.DOM.cl;
import static org.labkey.targetedms.TargetedMSModule.EXPERIMENT_FOLDER_WEB_PARTS;
import static org.labkey.targetedms.TargetedMSModule.LIBRARY_FOLDER_WEB_PARTS;
import static org.labkey.targetedms.TargetedMSModule.PEPTIDE_TAB_WEB_PARTS;
import static org.labkey.targetedms.TargetedMSModule.PROTEIN_TAB_NAME;
import static org.labkey.targetedms.TargetedMSModule.PROTEIN_TAB_WEB_PARTS;
import static org.labkey.targetedms.TargetedMSModule.QC_FOLDER_WEB_PARTS;

public class TargetedMSController extends SpringActionController
{
    private static final Logger LOG = LogManager.getLogger(TargetedMSController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(TargetedMSController.class);
    public static final String CONFIGURE_TARGETED_MS_FOLDER = "Configure Panorama Folder";

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

    public static ActionURL getShowRunURL(Container c, long runId)
    {
        ActionURL url = getShowRunURL(c);
        url.addParameter("id", runId);
        return url;
    }

    public static ActionURL getShowCalibrationCurvesURL(Container c, long runId)
    {
        ActionURL url = new ActionURL(ShowCalibrationCurvesAction.class, c);
        url.addParameter("id", runId);
        return url;
    }

    // ------------------------------------------------------------------------
    // Action to setup a new folder
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class FolderSetupAction extends FormHandlerAction<FolderSetupForm>
    {
        public static final String DATA_PIPELINE_TAB = "Data Pipeline";
        public static final String RUNS_TAB = "Runs";
        public static final String ANNOTATIONS_TAB = "Annotations";
        public static final String GUIDE_SETS_TAB = "Guide Sets";
        public static final String PARETO_PLOT_TAB = "Pareto Plot";

        public static final String DATA_PIPELINE_WEBPART = "Data Pipeline";

        @Override
        public void validateCommand(FolderSetupForm target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(FolderSetupForm folderSetupForm, BindException errors)
        {
            Container c = getContainer();
            TargetedMSModule targetedMSModule = null;
            for (Module m : c.getActiveModules())
            {
                if (m instanceof TargetedMSModule)
                {
                    targetedMSModule = (TargetedMSModule) m;
                }
            }
            if (targetedMSModule == null)
            {
                return true; // no TargetedMS module found - do nothing
            }
            ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(FOLDER_TYPE_PROP_NAME);
            switch (FolderType.valueOf(moduleProperty.getValueContainerSpecific(c)))
            {
                case Experiment:
                case ExperimentMAM:
                case Library:
                case LibraryProtein:
                case QC:
                    return true;  // Module type already set to LibraryProtein
                case Undefined:
                    // continue with the remainder of the function
                    break;
            }

            if (FolderType.Experiment.toString().equals(folderSetupForm.getFolderType()) ||
                    FolderType.ExperimentMAM.toString().equals(folderSetupForm.getFolderType()))
            {
                moduleProperty.saveValue(getUser(), c, folderSetupForm.getFolderType());

                // setup the EXPERIMENTAL_DATA default webparts
                addDashboardTab(DefaultFolderType.DEFAULT_DASHBOARD, c, EXPERIMENT_FOLDER_WEB_PARTS);
            }
            else if (FolderType.Library.toString().equals(folderSetupForm.getFolderType()))
            {
                // setup the CHROMATOGRAM_LIBRARY default webparts
                if (folderSetupForm.isPrecursorNormalized())
                {
                    moduleProperty.saveValue(getUser(), c, FolderType.LibraryProtein.toString());
                }
                else
                {
                    moduleProperty.saveValue(getUser(), c, FolderType.Library.toString());
                }


                addDashboardTab(DefaultFolderType.DEFAULT_DASHBOARD, c, LIBRARY_FOLDER_WEB_PARTS);
                // Add the appropriate web parts to the page
                if(folderSetupForm.isPrecursorNormalized())
                {
                    addDashboardTab(PROTEIN_TAB_NAME, c, PROTEIN_TAB_WEB_PARTS);
                    addDashboardTab(TargetedMSModule.PEPTIDE_TAB_NAME, c, PEPTIDE_TAB_WEB_PARTS);
                }
            }
            else if (FolderType.QC.toString().equals(folderSetupForm.getFolderType()))
            {
                moduleProperty.saveValue(getUser(), c, FolderType.QC.toString());
                addDashboardTab(DefaultFolderType.DEFAULT_DASHBOARD, c, QC_FOLDER_WEB_PARTS);

                ArrayList<Portal.WebPart> runsTab = new ArrayList<>();
                runsTab.add(Portal.getPortalPart(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME).createWebPart());
                Portal.saveParts(c, RUNS_TAB, runsTab);
                Portal.addProperty(c, RUNS_TAB, Portal.PROP_CUSTOMTAB);

                ArrayList<Portal.WebPart> annotationsTab = new ArrayList<>();
                Portal.WebPart annotationsPart = Portal.getPortalPart("Query").createWebPart();
                annotationsPart.setProperty(QueryParam.schemaName.toString(), "targetedms");
                annotationsPart.setProperty(QueryParam.queryName.toString(), "qcannotation");
                annotationsTab.add(annotationsPart);
                Portal.WebPart annotationTypesPart = Portal.getPortalPart("Query").createWebPart();
                annotationTypesPart.setProperty(QueryParam.schemaName.toString(), "targetedms");
                annotationTypesPart.setProperty(QueryParam.queryName.toString(), "qcannotationtype");
                annotationsTab.add(annotationTypesPart);
                Portal.WebPart replicateAnnotationPart = Portal.getPortalPart("Query").createWebPart();
                replicateAnnotationPart.setProperty(QueryParam.schemaName.toString(), "targetedms");
                replicateAnnotationPart.setProperty(QueryParam.queryName.toString(), "replicateannotation");
                annotationsTab.add(replicateAnnotationPart);
                Portal.saveParts(c, ANNOTATIONS_TAB, annotationsTab);
                Portal.addProperty(c, ANNOTATIONS_TAB, Portal.PROP_CUSTOMTAB);

                ArrayList<Portal.WebPart> guideSetsTab = new ArrayList<>();
                Portal.WebPart guideSetsPart = Portal.getPortalPart("Query").createWebPart();
                guideSetsPart.setProperty(QueryParam.schemaName.toString(), "targetedms");
                guideSetsPart.setProperty(QueryParam.queryName.toString(), "guideset");
                guideSetsTab.add(guideSetsPart);
                Portal.saveParts(c, GUIDE_SETS_TAB, guideSetsTab);
                Portal.addProperty(c, GUIDE_SETS_TAB, Portal.PROP_CUSTOMTAB);

                ArrayList<Portal.WebPart> paretoPlotTab = new ArrayList<>();
                Portal.WebPart paretoPlotPart = Portal.getPortalPart(TargetedMSModule.TARGETED_MS_PARETO_PLOT).createWebPart();
                paretoPlotTab.add(paretoPlotPart);
                Portal.saveParts(c, PARETO_PLOT_TAB, paretoPlotTab);
                Portal.addProperty(c, PARETO_PLOT_TAB, Portal.PROP_CUSTOMTAB);
            }

            // Add additional portal pages (tabs) and webparts
            addDataPipelineTab(c);
            addRawFilesPipelineTab(c);

            // Inform listeners so that any additinal folder configuration can be done.
            TargetedMSService.get().getTargetedMSFolderTypeListeners().forEach(listener -> listener.folderCreated(c, getUser()));

            return true;
        }

        private void addDataPipelineTab(Container c)
        {
            List<Portal.WebPart> tab = new ArrayList<>();
            Portal.WebPart webPart = Portal.getPortalPart(DATA_PIPELINE_WEBPART).createWebPart();
            tab.add(webPart);
            Portal.saveParts(c, DATA_PIPELINE_TAB, tab);
            Portal.addProperty(c, DATA_PIPELINE_TAB, Portal.PROP_CUSTOMTAB);
        }

        @Override
        public URLHelper getSuccessURL(FolderSetupForm folderSetupForm)
        {
            return getContainer().getStartURL(getUser());
        }

    }

    public static void addDashboardTab(String tab, Container c, String[] includeWebParts)
    {
        ArrayList<Portal.WebPart> newWebParts = new ArrayList<>();
        for(String name: includeWebParts)
        {
            Portal.WebPart webPart = Portal.getPortalPart(name).createWebPart();
            newWebParts.add(webPart);
        }

        Portal.saveParts(c, tab, newWebParts);
        if (DefaultFolderType.DEFAULT_DASHBOARD.equals(tab))
        {
            // Save webparts to both pages, otherwise the TARGETED_MS_SETUP webpart gets copied over from
            // portal.default to DefaultDashboard
            Portal.saveParts(c, Portal.DEFAULT_PORTAL_PAGE_ID, newWebParts); // this will remove the TARGETED_MS_SETUP
        }
        else
        {
            Portal.addProperty(c, tab, Portal.PROP_CUSTOMTAB);
        }
    }

    private static class ChromatogramCrawlerForm
    {

    }

    @RequiresPermission(ApplicationAdminPermission.class)
    public class ChromatogramCrawlerAction extends FormViewAction<ChromatogramCrawlerForm>
    {
        @Override
        public void validateCommand(ChromatogramCrawlerForm target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(ChromatogramCrawlerForm form, boolean reshow, BindException errors) throws Exception
        {
            return new HtmlView("Chromatogram Crawler", DIV("Crawl all containers under the parent " + getContainer().getPath(),
                    FORM(at(method, "POST"),
                            new Button.ButtonBuilder("Start Crawl").submit(true).build())));
        }

        @Override
        public boolean handlePost(ChromatogramCrawlerForm form, BindException errors) throws Exception
        {
            PipelineJob job = new ChromatogramCrawlerJob(getViewBackgroundInfo(), PipelineService.get().getPipelineRootSetting(ContainerManager.getRoot()));
            PipelineService.get().queueJob(job);
            return true;
        }

        @Override
        public URLHelper getSuccessURL(ChromatogramCrawlerForm form)
        {
            return urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            urlProvider(AdminUrls.class).addAdminNavTrail(root, "Chromatogram Crawler", getClass(), getContainer());
        }
    }

    // ------------------------------------------------------------------------
    // Action to create a Raw Data tab
    // ------------------------------------------------------------------------
    @RequiresPermission(AdminPermission.class)
    public class AddRawDataTabAction extends FormHandlerAction
    {
        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(Object o, BindException errors)
        {
            Container c = getContainer(); ;
            if(!c.hasActiveModuleByName(MODULE_NAME))
            {
                return true; // no TargetedMS module found - do nothing
            }
            addRawFilesPipelineTab(c);

            return true;
        }

        @Override
        public URLHelper getSuccessURL(Object o)
        {
            return urlProvider(ProjectUrls.class).getBeginURL(getContainer(), RAW_FILES_TAB);
        }
    }

    public static void addRawFilesPipelineTab(Container c)
    {
        FileContentService service = FileContentService.get();
        if (null != service)
        {
            List<Portal.WebPart> tab = new ArrayList<>();
            Portal.WebPart webPart = Portal.getPortalPart(FilesWebPart.PART_NAME).createWebPart();
            configureRawDataTab(webPart, c, service);
            tab.add(webPart);
            Portal.saveParts(c, RAW_FILES_TAB, tab);
            Portal.addProperty(c, RAW_FILES_TAB, Portal.PROP_CUSTOMTAB);
        }
    }

    public static void configureRawDataTab(Portal.WebPart webPart, Container c, FileContentService service)
    {
        if (null != service)
        {
            Path fileRoot = service.getFileRootPath(c, FileContentService.ContentType.files);
            if (fileRoot != null)
            {
                Path rawFileDir = fileRoot.resolve(RAW_FILES_DIR);
                if (!Files.exists(rawFileDir))
                {
                    try
                    {
                        Files.createDirectories(rawFileDir);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }

            String fileRootString = FileContentService.FILES_LINK + "/" + RAW_FILES_DIR + "/";
            webPart.setProperty(FilesWebPart.FILE_ROOT_PROPERTY_NAME, fileRootString);
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a list of uploaded documents
    // ------------------------------------------------------------------------
    @RequiresPermission(AdminPermission.class)
    public class SetupAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            JspView view = new JspView("/org/labkey/targetedms/view/folderSetup.jsp");
            view.setFrame(WebPartView.FrameType.NONE);

            getPageConfig().setNavTrail(ContainerManager.getCreateContainerWizardSteps(getContainer(), getContainer().getParent()));
            getPageConfig().setTemplate(PageConfig.Template.Wizard);
            getPageConfig().setTitle(CONFIGURE_TARGETED_MS_FOLDER);

            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    // ------------------------------------------------------------------------
    // Action to show QC reports
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class LeveyJenningsAction extends SimpleViewAction<URLParameterBean>
    {
        @Override
        public ModelAndView getView(URLParameterBean urlParameterBean, BindException errors)
        {
            return new JspView("/org/labkey/targetedms/view/qcTrendPlotReport.jsp");
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("QC Reports");
        }
    }

    public static class URLParameterBean
    {
        private String metric;
        private String startDate;
        private String endDate;
        private List<String> _plotTypes;
        private Boolean _largePlot;

        public String getMetric()
        {
            return metric;
        }

        public void setMetric(String metric)
        {
            this.metric = metric;
        }

        public String getStartDate()
        {
            return startDate;
        }

        public void setStartDate(String startDate)
        {
            this.startDate = startDate;
        }

        public String getEndDate()
        {
            return endDate;
        }

        public void setEndDate(String endDate)
        {
            this.endDate = endDate;
        }

        public void setPlotTypes(List<String> plotTypes)
        {
            _plotTypes = plotTypes;
        }

        public List<String> getPlotTypes()
        {
            return _plotTypes;
        }

        public Boolean getLargePlot()
        {
            return _largePlot;
        }

        public void setLargePlot(Boolean largePlot)
        {
            _largePlot = largePlot;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class LeveyJenningsPlotOptionsAction extends MutatingApiAction<LeveyJenningsPlotOptions>
    {
        private static final String CATEGORY = "TargetedMSLeveyJenningsPlotOptions";

        @Override
        public Object execute(LeveyJenningsPlotOptions form, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            // only stash and retrieve plot option properties for logged in users
            if (!getUser().isGuest())
            {
                PropertyManager.PropertyMap properties = PropertyManager.getWritableProperties(getUser(), getContainer(), CATEGORY, true);

                Map<String, String> valuesToPersist = form.getAsMapOfStrings();
                if (!valuesToPersist.isEmpty())
                {
                    // note: start, end date and selectedAnnotations handled separately since they can be null and we want to persist that
                    valuesToPersist.put("startDate", form.getStartDate());
                    valuesToPersist.put("endDate", form.getEndDate());
                    valuesToPersist.put("selectedAnnotations", form.getSelectedAnnotationsString());

                    properties.putAll(valuesToPersist);
                    properties.save();
                }
                else
                {
                    if(properties.containsKey("selectedAnnotations") && (ReplicateManager.getReplicateAnnotationNameValues(getContainer()).size() == 0))
                    {
                        // If there are no replicate annotations in this folder anymore, remove any saved annotation filters
                        // Issue 35726: No way to clear previously saved replicate annotation values in QC plots if folder no longer contains annotations
                        properties.remove("selectedAnnotations");
                        properties.save();
                    }
                }

                response.put("properties", properties);
            }

            return response;
        }
    }

    private static class LeveyJenningsPlotOptions
    {
        private String _metric;
        private String _yAxisScale;
        private Boolean _groupedX;
        private Boolean _singlePlot;
        private Boolean _showExcluded;
        private Integer _dateRangeOffset;
        private String _startDate;
        private String _endDate;
        private List<String> _plotTypes;
        private Boolean _largePlot;
        public List<String> _selectedAnnotations;

        public Map<String, String> getAsMapOfStrings()
        {
            Map<String, String> valueMap = new HashMap<>();
            if (_metric != null)
                valueMap.put("metric", _metric);
            if (_yAxisScale != null)
                valueMap.put("yAxisScale", _yAxisScale);
            if (_groupedX != null)
                valueMap.put("groupedX", Boolean.toString(_groupedX));
            if (_singlePlot != null)
                valueMap.put("singlePlot", Boolean.toString(_singlePlot));
            if (_showExcluded != null)
                valueMap.put("showExcluded", Boolean.toString(_showExcluded));
            if (_dateRangeOffset != null)
                valueMap.put("dateRangeOffset", Integer.toString(_dateRangeOffset));
            if (_plotTypes != null && !_plotTypes.isEmpty())
                valueMap.put("plotTypes", StringUtils.join(_plotTypes, ","));
            if (_largePlot != null)
                valueMap.put("largePlot", Boolean.toString(_largePlot));
            if(_selectedAnnotations != null)
                valueMap.put("selectedAnnotations", getSelectedAnnotationsString());
            // note: start and end date handled separately since they can be null and we want to persist that
            return valueMap;
        }

        public void setMetric(String metric)
        {
            _metric = metric;
        }

        public void setyAxisScale(String yAxisScale)
        {
            _yAxisScale = yAxisScale;
        }

        public void setGroupedX(Boolean groupedX)
        {
            _groupedX = groupedX;
        }

        public void setSinglePlot(Boolean singlePlot)
        {
            _singlePlot = singlePlot;
        }

        public void setShowExcluded(Boolean showExcluded)
        {
            _showExcluded = showExcluded;
        }

        public void setDateRangeOffset(Integer dateRangeOffset)
        {
            _dateRangeOffset = dateRangeOffset;
        }

        public void setStartDate(String startDate)
        {
            _startDate = startDate;
        }

        public String getStartDate()
        {
            return _startDate;
        }

        public void setEndDate(String endDate)
        {
            _endDate = endDate;
        }

        public String getEndDate()
        {
            return _endDate;
        }

        public void setPlotTypes(List<String> plotTypes)
        {
            _plotTypes = plotTypes;
        }

        public List<String> getPlotTypes()
        {
            return _plotTypes;
        }

        public void setLargePlot(Boolean largePlot)
        {
            _largePlot = largePlot;
        }

        public List<String> getSelectedAnnotations()
        {
            return _selectedAnnotations;
        }

        public void setSelectedAnnotations(List<String> annotations)
        {
            _selectedAnnotations = annotations;
        }

        public String getSelectedAnnotationsString()
        {
            return StringUtils.join(_selectedAnnotations, ",");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class GetQCSummaryAction extends ReadOnlyApiAction<QCSummaryForm>
    {
        @Override
        public Object execute(QCSummaryForm form, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            List<Map<String, Object>> containers = new ArrayList<>();

            // include the QC Summary properties for the current container
            containers.add(getContainerQCSummaryProperties(getContainer(), getContainer(), false));

            // include the QC Summary properties for the direct subfolders, of type QC, that the user has read permission
            if (form.isIncludeSubfolders())
            {
                for (Container container : getContainer().getChildren())
                {
                    Container bestContainer = container;
                    Container mostRecentPingContainer = TargetedMSManager.getMostRecentPingChild(getUser(), container);
                    // Find the most recent AutoQC ping for the subfolder or its children
                    TargetedMSService.FolderType folderType = TargetedMSManager.getFolderType(mostRecentPingContainer);
                    // Make sure it's a QC folder
                    if (mostRecentPingContainer.hasPermission(getUser(), ReadPermission.class) && folderType == TargetedMSService.FolderType.QC)
                    {
                         bestContainer = mostRecentPingContainer;
                    }

                    folderType = TargetedMSManager.getFolderType(bestContainer);
                    if (bestContainer.hasPermission(getUser(), ReadPermission.class) && folderType == TargetedMSService.FolderType.QC)
                    {
                        containers.add(getContainerQCSummaryProperties(bestContainer, container, true));
                    }
                }
            }

            response.put("containers", containers);
            return response;
        }
    }

    private Map<String, Object> getContainerQCSummaryProperties(Container container, Container instrumentContainer, boolean isSubfolder)
    {
        Map<String, Object> properties = new HashMap<>();
        SQLFragment sql;

        properties.put("id", container.getId());
        properties.put("name", instrumentContainer.equals(container) ? container.getName() : (instrumentContainer.getName() + " - " + container.getName()));
        properties.put("path", container.getPath());
        properties.put("subfolder", isSubfolder);

        // # Skyline documents, count of rows in targetedms.Runs
        // and date of last import, max(created) from targetedms.Runs
        sql = new SQLFragment("SELECT COUNT(Id) As docCount, MAX(Created) AS lastImportDate FROM ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r").append(" WHERE Container = ? AND StatusId = ?");
        sql.add(container.getId()).add(SkylineDocImporter.STATUS_SUCCESS);
        Map<String, Object> valueMap = new SqlSelector(TargetedMSSchema.getSchema(), sql).getMap();
        properties.put("docCount", valueMap.get("docCount"));
        properties.put("lastImportDate", valueMap.get("lastImportDate"));

        // # sample files, count of rows in targetedms.SampleFile
        sql = new SQLFragment("SELECT COUNT(s.Id) FROM ").append(TargetedMSManager.getTableInfoSampleFile(), "s");
        sql.append(" JOIN ").append(TargetedMSManager.getTableInfoReplicate(), "re").append(" ON s.ReplicateId = re.Id");
        sql.append(" JOIN ").append(TargetedMSManager.getTableInfoRuns(), "r").append(" ON re.RunId = r.Id");
        sql.append(" WHERE r.Container = ?").add(container.getId());
        properties.put("fileCount", new SqlSelector(TargetedMSSchema.getSchema(), sql).getObject(Integer.class));

        // # precursors tracked, count of distinct precursors. Include peptides and small molecules
        sql = new SQLFragment("SELECT DISTINCT COALESCE(p.ModifiedSequence, ");
        sql.append(" concat(mp.CustomIonName, mp.IonFormula,mp.massMonoisotopic, mp.massAverage, gp.mz)) AS SeriesLabel, gp.Charge");
        sql.append(" FROM ").append(TargetedMSManager.getTableInfoGeneralPrecursor(), "gp");
        sql.append(" JOIN ").append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm").append(" ON gp.GeneralMoleculeId = gm.Id");
        sql.append(" JOIN ").append(TargetedMSManager.getTableInfoPeptideGroup(), "pg").append(" ON gm.PeptideGroupId = pg.Id");
        sql.append(" JOIN ").append(TargetedMSManager.getTableInfoRuns(), "r").append(" ON pg.RunId = r.Id");
        sql.append(" LEFT JOIN ").append(TargetedMSManager.getTableInfoPrecursor(), "p").append(" ON p.Id = gp.Id");
        sql.append(" LEFT JOIN ").append(TargetedMSManager.getTableInfoMoleculePrecursor(), "mp").append(" ON mp.Id = gp.Id");
        sql.append(" WHERE r.Container = ?").add(container.getId());
        properties.put("precursorCount", new SqlSelector(TargetedMSSchema.getSchema(), sql).getRowCount());

        // AutoQCPing information
        Map<String, Object> autoQCPingMap = TargetedMSManager.get().getAutoQCPingMap(container);
        if (autoQCPingMap != null)
        {
            // check if the last modified date is recent (i.e. within the last 15 min)
            long timeoutMinutesAgo = System.currentTimeMillis() - (TargetedMSManager.get().getAutoQCPingTimeout(container) * 60000);
            Timestamp lastModified = (Timestamp)autoQCPingMap.get("Modified");
            autoQCPingMap.put("isRecent", lastModified.getTime() >= timeoutMinutesAgo);
        }
        properties.put("autoQCPing", autoQCPingMap);
        properties.put("metricCount", TargetedMSManager.get().getEnabledQCMetricConfigurations(instrumentContainer, getUser()).size());

        return properties;
    }

    @RequiresPermission(ReadPermission.class)
    public class GetQCMetricConfigurationsAction extends ReadOnlyApiAction
    {
        @Override
        public Object execute(Object form, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            List<QCMetricConfiguration> enabledQCMetricConfigurations = TargetedMSManager.get().getEnabledQCMetricConfigurations(getContainer(), getUser());

            List<JSONObject> result = new ArrayList<>();
            for (QCMetricConfiguration configuration : enabledQCMetricConfigurations)
            {
                   result.add(configuration.toJSON());
            }
            response.put("configurations", result);
            return response;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class GetContainerReplicateAnnotationsAction extends ReadOnlyApiAction
    {
        @Override
        public Object execute(Object form, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            // Returned annotations are already sorted by name, value
            List<ReplicateAnnotation> replicateAnnotations = ReplicateManager.getReplicateAnnotationNameValues(getContainer());

            /*
              JSON example -
              replicateAnnotations:
              [
                {"name": "annotation1", "values": ["value1", "value2"]},
                {"name": "annotation2": "values": ["val1", "val2", "val3"]},
               ...
              ]
             */
            JSONArray annotations = new JSONArray();

            JSONObject annotation = null;
            JSONArray values = null;

            for(ReplicateAnnotation ra: replicateAnnotations)
            {
                if(annotation == null || !ra.getName().equals(annotation.get("name")))
                {
                    annotation = new JSONObject();
                    annotation.put("name", ra.getName());
                    values = new JSONArray();
                    annotation.put("values", values);

                    annotations.put(annotation);
                }

                values.put(ra.getValue());
            }

            response.put("replicateAnnotations", annotations);
            return response;
        }
    }

    private Container getCandidateContainer(Container container, User user)
    {
        Container sharedContainer = ContainerManager.getSharedContainer();
        Container rootContainer = ContainerManager.getRoot();

        boolean isParentValid = false;
        if(container.equals(sharedContainer)){
            return rootContainer;
        }

        while (!isParentValid)
        {
            container = container.getParent();
            if(container.equals(rootContainer)){
                return sharedContainer;
            }

            FolderType folderType = TargetedMSManager.getFolderType(container);
            isParentValid = container.hasPermission(user, ReadPermission.class) && folderType == FolderType.QC;
        }
        return container;
    }

    private static class QCSummaryForm
    {
        boolean includeSubfolders;

        public boolean isIncludeSubfolders()
        {
            return includeSubfolders;
        }

        public void setIncludeSubfolders(boolean includeSubfolders)
        {
            this.includeSubfolders = includeSubfolders;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class QCSummaryHistoryAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new QCSummaryWebPart(getViewContext(), null);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("QC Summary History");
        }
    }

    public static class QCMetricOutliersForm
    {
        private Integer _sampleLimit;

        public Integer getSampleLimit()
        {
            return _sampleLimit;
        }

        public void setSampleLimit(Integer sampleLimit)
        {
            _sampleLimit = sampleLimit;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class GetQCMetricOutliersAction extends ReadOnlyApiAction<QCMetricOutliersForm>
    {

        @Override
        public Object execute(QCMetricOutliersForm form, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            List<QCMetricConfiguration> enabledQCMetricConfigurations = TargetedMSManager.get().getEnabledQCMetricConfigurations(getContainer(), getUser());

            if(enabledQCMetricConfigurations.isEmpty())
            {
                response.put("outliers", "no enabled qc configurations");
                return response;
            }

            List<GuideSet> guideSets = TargetedMSManager.getGuideSets(getContainer(), getUser());
            Map<Integer, QCMetricConfiguration> metricMap = enabledQCMetricConfigurations.stream().collect(Collectors.toMap(QCMetricConfiguration::getId, Function.identity()));

            List<RawMetricDataSet> rawMetricDataSets = OutlierGenerator.get().getRawMetricDataSets(getContainer(), getUser(), enabledQCMetricConfigurations, null, null, Collections.emptyList(), true);

            Map<GuideSetKey, GuideSetStats> stats = OutlierGenerator.get().getAllProcessedMetricGuideSets(rawMetricDataSets, guideSets.stream().collect(Collectors.toMap(GuideSet::getRowId, Function.identity())));

            List<SampleFileInfo> sampleFiles = OutlierGenerator.get().getSampleFiles(rawMetricDataSets, stats, metricMap, getContainer(), form.getSampleLimit());

            response.put("sampleFiles", sampleFiles.stream().map(SampleFileInfo::toJSON).collect(Collectors.toList()));
            response.put("guideSets", guideSets.stream().map(x -> x.toJSON(rawMetricDataSets, metricMap, stats)).collect(Collectors.toList()));

            return response;
        }
    }

    public static class QCPlotsDataForm
    {
        private int _metricId;
        private boolean _includeLJ;
        private boolean _includeMR;
        private boolean _includeMeanCusum;
        private boolean _includeVariableCusum;
        private Date _startDate;
        private Date _endDate;
        private List<OutlierGenerator.AnnotationGroup> _selectedAnnotations;
        private boolean _showExcluded;
        private boolean _showReferenceGS;

        public int getMetricId()
        {
            return _metricId;
        }

        public void setMetricId(int metricId)
        {
            this._metricId = metricId;
        }

        public boolean isIncludeLJ()
        {
            return _includeLJ;
        }

        public void setIncludeLJ(boolean includeLJ)
        {
            this._includeLJ = includeLJ;
        }

        public boolean isIncludeMR()
        {
            return _includeMR;
        }

        public void setIncludeMR(boolean includeMR)
        {
            this._includeMR = includeMR;
        }

        public boolean isIncludeMeanCusum()
        {
            return _includeMeanCusum;
        }

        public void setIncludeMeanCusum(boolean includeMeanCusum)
        {
            this._includeMeanCusum = includeMeanCusum;
        }

        public boolean isIncludeVariableCusum()
        {
            return _includeVariableCusum;
        }

        public void setIncludeVariableCusum(boolean includeVariableCusum)
        {
            this._includeVariableCusum = includeVariableCusum;
        }

        public Date getStartDate()
        {
            return _startDate;
        }

        public void setStartDate(java.sql.Date startDate)
        {
            _startDate = startDate;
        }

        public Date getEndDate()
        {
            return _endDate;
        }

        public void setEndDate(java.sql.Date endDate)
        {
            _endDate = endDate;
        }

        public List<OutlierGenerator.AnnotationGroup> getSelectedAnnotations()
        {
            return _selectedAnnotations;
        }

        public void setSelectedAnnotations(List<OutlierGenerator.AnnotationGroup> selectedAnnotations)
        {
            _selectedAnnotations = selectedAnnotations;
        }

        public boolean isShowExcluded()
        {
            return _showExcluded;
        }

        public void setShowExcluded(boolean showExcluded)
        {
            _showExcluded = showExcluded;
        }

        public boolean isShowReferenceGS()
        {
            return _showReferenceGS;
        }

        public void setShowReferenceGS(boolean showReferenceGS)
        {
            _showReferenceGS = showReferenceGS;
        }
    }

    /**
     * Action to get quality control plots data
     * The returned data {@link QCPlotFragment} is separated by the peptide
     * */
    @RequiresPermission(ReadPermission.class)
    @Marshal(Marshaller.Jackson)
    public class GetQCPlotsDataAction extends ReadOnlyApiAction<QCPlotsDataForm>
    {

        @Override
        protected ObjectMapper createRequestObjectMapper()
        {
            SimpleDateFormat df = new SimpleDateFormat("y-M-d");
            ObjectMapper mapper = new ObjectMapper();
            mapper.setDateFormat(df);
            return mapper;
        }

        @Override
        public Object execute(QCPlotsDataForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            OutlierGenerator generator = OutlierGenerator.get();

            int passedMetricId = form.getMetricId();
            boolean filterQCPoints = false;

            List<GuideSet> guideSets = TargetedMSManager.getGuideSets(getContainer(), getUser());
            GuideSet closestOutOfRangeGuideSet;
            var rangeStartDate = form.getStartDate();
            if (form.isShowReferenceGS() && !guideSets.isEmpty())
            {
                closestOutOfRangeGuideSet = getClosestPastGuideSet(guideSets, form.getStartDate());
                if (null != closestOutOfRangeGuideSet)
                {
                    rangeStartDate = closestOutOfRangeGuideSet.getTrainingStart();
                    filterQCPoints = true;
                }
            }

            final Date qcStartDate = rangeStartDate;
            List<QCMetricConfiguration> qcMetricConfigurations = TargetedMSManager
                    .getEnabledQCMetricConfigurations(getContainer(), getUser())
                    .stream()
                    .filter(qcMetricConfiguration -> qcMetricConfiguration.getId() == passedMetricId)
                    .collect(Collectors.toList());

            // get start date and end date for this qc folder
            Map<String,Object> qcFolderDateRange = TargetedMSManager.getQCFolderDateRange(getContainer());
            Date qcFolderStartDate = (Date) qcFolderDateRange.get("startDate");
            Date qcFolderEndDate = (Date) qcFolderDateRange.get("endDate");

            // always query for the full range
            List<RawMetricDataSet> rawMetricDataSets = generator.getRawMetricDataSets(getContainer(), getUser(), qcMetricConfigurations, qcFolderStartDate, qcFolderEndDate, form.getSelectedAnnotations(), form.isShowExcluded());
            Map<GuideSetKey, GuideSetStats> stats = generator.getAllProcessedMetricGuideSets(rawMetricDataSets, guideSets.stream().collect(Collectors.toMap(GuideSet::getRowId, Function.identity())));
            boolean zoomedRange = qcFolderStartDate != null &&
                    qcFolderEndDate != null &&
                    (DateUtil.getDateOnly(qcFolderStartDate).compareTo(rangeStartDate) != 0 ||
                    DateUtil.getDateOnly(qcFolderEndDate).compareTo(form.getEndDate()) != 0);
            Map<GuideSetKey, GuideSetStats> targetedStats;

            if (zoomedRange)
            {
                // filter the stats for targeted range
                Predicate<RawMetricDataSet> withInDateRange = rawMetricDataSet -> rawMetricDataSet.getAcquiredTime() != null &&
                        (qcStartDate != null &&
                        (rawMetricDataSet.getAcquiredTime().after(qcStartDate)
                                || DateUtil.getDateOnly(rawMetricDataSet.getAcquiredTime()).compareTo(qcStartDate) == 0)) &&
                        (form.getEndDate() != null &&
                        (rawMetricDataSet.getAcquiredTime().before(form.getEndDate())
                                || DateUtil.getDateOnly(rawMetricDataSet.getAcquiredTime()).compareTo(form.getEndDate()) == 0));
                rawMetricDataSets = rawMetricDataSets
                        .stream()
                        .filter(withInDateRange)
                        .collect(Collectors.toList());
                targetedStats = generator.getAllProcessedMetricGuideSets(rawMetricDataSets, guideSets.stream().collect(Collectors.toMap(GuideSet::getRowId, Function.identity())));
                // attach mean and sd stats from full stats
                targetedStats.forEach((guideSetKey, guideSetStats) -> {
                    GuideSetStats correctGuideSetStats = stats.get(guideSetKey);
                    guideSetStats.setAverage(correctGuideSetStats.getAverage());
                    guideSetStats.setStandardDeviation(correctGuideSetStats.getStandardDeviation());
                    guideSetStats.setMovingRangeAverage(correctGuideSetStats.getMovingRangeAverage());
                    guideSetStats.setMovingRangeStdDev(correctGuideSetStats.getMovingRangeStdDev());
                });
            }
            else
            {
                targetedStats = stats;
            }

            Map<Integer, QCMetricConfiguration> metricMap = qcMetricConfigurations.stream().collect(Collectors.toMap(QCMetricConfiguration::getId, Function.identity()));
            List<SampleFileInfo> sampleFiles = OutlierGenerator.get().getSampleFiles(rawMetricDataSets, targetedStats, metricMap, getContainer(), null);
            List<QCPlotFragment> qcPlotFragments = OutlierGenerator.get().getQCPlotFragment(rawMetricDataSets, targetedStats);

            response.put("sampleFiles", sampleFiles.stream().map(SampleFileInfo::toQCPlotJSON).collect(Collectors.toList()));
            response.put("plotDataRows", qcPlotFragments
                    .stream()
                    .map(qcPlotFragment -> qcPlotFragment.toJSON(form.isIncludeLJ(), form.isIncludeMR(), form.isIncludeMeanCusum(), form.isIncludeVariableCusum()))
                    .collect(Collectors.toList()));
            response.put("metricProps", metricMap.get(passedMetricId).toJSON());
            response.put("filterQCPoints", filterQCPoints);

            return response;
        }

        private GuideSet getClosestPastGuideSet(List<GuideSet> guideSets, Date startDate)
        {
            GuideSet gs = null;
            for (GuideSet guideSet : guideSets)
            {
                // guideset end date should be before or equal start date
                if (!guideSet.isDefault() &&
                        (guideSet.getTrainingEnd().before(startDate) || DateUtil.getDateOnly(guideSet.getTrainingEnd()).compareTo(startDate) == 0))
                {
                    if (null == gs)
                    {
                        gs = guideSet;
                    }
                    else if (guideSet.getTrainingEnd().after(gs.getTrainingEnd()))
                    {
                        gs = guideSet;
                    }
                }
            }

            return gs;
        }
    }
    // ------------------------------------------------------------------------
    // Action to show a list of chromatogram library archived revisions
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ArchivedRevisionsAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            JspView view = new JspView("/org/labkey/targetedms/view/archivedRevisionsDownload.jsp");
            view.setFrame(WebPartView.FrameType.PORTAL);
            view.setTitle("Archived Revisions");

            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Download Chromatogram Library");
        }
    }


    // ------------------------------------------------------------------------
    // Action to show a list of uploaded documents
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    @ActionNames("showList, begin")
    public class ShowListAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            TargetedMsRunListView runListView = TargetedMsRunListView.createView(getViewContext());
            VBox vbox = new VBox();
            FolderType folderType = TargetedMSManager.getFolderType(getContainer());
            if(folderType == TargetedMSService.FolderType.Library || folderType == TargetedMSService.FolderType.LibraryProtein)
            {
                vbox.addView(new JspView("/org/labkey/targetedms/view/conflictSummary.jsp"));
            }
            vbox.addView(runListView);
            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Skyline Documents");
        }
    }

    // ------------------------------------------------------------------------
    // Chromatogram actions
    // ------------------------------------------------------------------------

    @RequiresPermission(ReadPermission.class)
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

            JFreeChart chart;
            if (PrecursorManager.getPrecursor(getContainer(), pci.getPrecursorId(), getUser()) != null)
            {
                chart = new ChromatogramChartMakerFactory().createTransitionChromChart(tci, pci, getUser(), getContainer());
            }
            else if (MoleculePrecursorManager.getPrecursor(getContainer(), pci.getPrecursorId(), getUser()) != null)
            {
                chart = new ChromatogramChartMakerFactory().createMoleculeTransitionChromChart(tci, pci, getUser(), getContainer());
            }
            else
            {
                throw new NotFoundException("No Precursor or MoleculePrecursor found in this folder for id: " + pci.getPrecursorId());
            }

            writeChart(form, response, chart);
        }
    }

    @RequiresPermission(ReadPermission.class)
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

            ChromatogramChartMakerFactory factory = new ChromatogramChartMakerFactory();
            factory.setSyncIntensity(form.isSyncY());
            factory.setSyncRt(form.isSyncX());
            factory.setSplitGraph(form.isSplitGraph());
            factory.setShowOptimizationPeaks(form.isShowOptimizationPeaks());
            factory.setLegend(form.isLegend());

            JFreeChart chart;
            if (PrecursorManager.getPrecursor(getContainer(), pChromInfo.getPrecursorId(), getUser()) != null)
            {
                chart = factory.createPrecursorChromChart(pChromInfo, getUser(), getContainer());
            }
            else if (MoleculePrecursorManager.getPrecursor(getContainer(), pChromInfo.getPrecursorId(), getUser()) != null)
            {
                chart = factory.createMoleculePrecursorChromChart(pChromInfo, getUser(), getContainer());
            }
            else
            {
                throw new NotFoundException("No Precursor or MoleculePrecursor found in this folder for id: " + pChromInfo.getPrecursorId());
            }

            writeChart(form, response, chart);
        }
    }

    private void writeChart(AbstractChartForm form, HttpServletResponse response, JFreeChart chart)
            throws Exception
    {
        TextTitle title = chart.getTitle();
        String filename = title == null ? "PanoramaPlot" : FileUtil.makeLegalName(title.getText().replace('"', '_'));

        if ("json".equals(form.getFormat()))
        {
            Map<String, Object> jsonPayload = new HashMap<>();

            String xLabel = null;
            if (chart.getPlot() instanceof XYPlot)
            {
                List<Map<String, Object>> series = new ArrayList<>();
                XYPlot plot = chart.getXYPlot();
                XYDataset dataset = plot.getDataset();
                for (int i = 0; i < dataset.getSeriesCount(); i++)
                {
                    Map<String, Object> info = new HashMap<>();
                    info.put("label", dataset.getSeriesKey(i));
                    ChartColor color = (ChartColor) plot.getRenderer().getSeriesPaint(i);
                    info.put("color", String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
                    series.add(info);
                }
                jsonPayload.put("series", series);

                if (plot.getDomainAxisCount() > 0)
                {
                    xLabel = plot.getDomainAxis().getLabel();
                    plot.getDomainAxis().setLabel(null);
                }
            }
            if (chart.getPlot() instanceof CategoryPlot)
            {
                CategoryPlot plot = chart.getCategoryPlot();
                if (plot.getDomainAxisCount() > 0)
                {
                    xLabel = plot.getDomainAxis().getLabel();
                    plot.getDomainAxis().setLabel(null);
                }
            }
            if (xLabel != null)
            {
                jsonPayload.put("xLabel", xLabel);
            }
            jsonPayload.put("title", title == null ? null : title.getText());
            // Null out the title so that it's not part of the SVG and let the client render it as text via the DOM
            chart.setTitle((String)null);
            jsonPayload.put("svg", renderSVG(form, chart));
            new ApiSimpleResponse(jsonPayload).render(new ApiJsonWriter(response));
        }
        else if ("svg".equalsIgnoreCase(form.getFormat()))
        {
            response.setContentType("image/svg+xml");
            response.getWriter().write(renderSVG(form, chart));
        }
        else if ("pdf".equalsIgnoreCase(form.getFormat()))
        {
            VisualizationService.get().renderSvgAsPdf(renderSVG(form, chart), filename + ".pdf", getViewContext().getResponse());
        }
        else
        {
            response.setContentType("image/png");

            if ("pngDownload".equalsIgnoreCase(form.getFormat()))
            {
                response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + ".png" + "\"");
            }

            if (!form.hasDpi())
            {
                ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, form.getChartWidth(), form.getChartHeight(), false, 5);
            }
            else
            {
                int dpi = form.getDpi();
                double scaleFactor = (double) dpi / (double) AbstractChartForm.SCREEN_RES;
                int w = form.getChartWidth();
                int h = form.getChartHeight();
                int desiredWidth = (int) (w * scaleFactor);
                int desiredHeight = (int) (h * scaleFactor);

                BufferedImage image = chart.createBufferedImage(desiredWidth, desiredHeight, w, h, null);
                PngEncoder encoder = new PngEncoder(image);
                encoder.setDpi(dpi, dpi);
                encoder.setCompressionLevel(5);
                byte[] bytes = encoder.pngEncode();
                response.getOutputStream().write(bytes);
            }
        }
    }

    @NotNull
    private String renderSVG(AbstractChartForm form, JFreeChart chart) throws SVGGraphics2DIOException
    {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator from Batik
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        Dimension dim = new Dimension(form.getChartWidth(), form.getChartHeight());
        svgGenerator.setSVGCanvasSize(dim);
        Rectangle r = new Rectangle(dim);

        chart.draw(svgGenerator, r);

        // Render SVG to a string
        StringWriter out = new StringWriter();
        svgGenerator.stream(out, true); // Use CSS style attributes
        return out.toString();
    }

    @RequiresPermission(ReadPermission.class)
    @ActionNames("generalMoleculeChromatogramChart, peptideChromatogramChart, moleculeChromatogramChart")
    public class GeneralMoleculeChromatogramChartAction extends ExportAction<ChromatogramForm>
    {
        @Override
        public void export(ChromatogramForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            GeneralMoleculeChromInfo gmChromInfo = PeptideManager.getGeneralMoleculeChromInfo(getContainer(), form.getId());
            if (gmChromInfo == null)
            {
                throw new NotFoundException("No GeneralMoleculeChromInfo found in this folder for id: " + form.getId());
            }

            ChromatogramChartMakerFactory factory = new ChromatogramChartMakerFactory();
            factory.setSyncIntensity(form.isSyncY());
            factory.setSyncRt(form.isSyncX());
            factory.setSplitGraph(form.isSplitGraph());

            JFreeChart chart;
            if (PeptideManager.getPeptide(getContainer(), gmChromInfo.getGeneralMoleculeId()) != null)
            {
                chart = factory.createPeptideChromChart(gmChromInfo, getUser(), getContainer());
            }
            else if (MoleculeManager.getMolecule(getContainer(), gmChromInfo.getGeneralMoleculeId()) != null)
            {
                chart = factory.createMoleculeChromChart(gmChromInfo, getUser(), getContainer());
            }
            else
            {
                throw new NotFoundException("No Peptide or Molecule found in this folder for id: " + gmChromInfo.getGeneralMoleculeId());
            }

            writeChart(form, response, chart);
        }
    }


    @RequiresPermission(ReadPermission.class)
    public class PrecursorAllChromatogramsChartAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private long _peptideId; // save for use in appendNavTrail
        private Precursor _precursor;

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors)
        {
            long precursorId = form.getId();
            _precursor = PrecursorManager.getPrecursor(getContainer(), precursorId, getUser());
            if (_precursor == null)
            {
                throw new NotFoundException("No such Precursor found in this folder: " + precursorId);
            }

            _run = TargetedMSManager.getRunForPrecursor(precursorId);
            _peptideId = _precursor.getGeneralMoleculeId();

            Peptide peptide = PeptideManager.getPeptide(getContainer(), _precursor.getGeneralMoleculeId());

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            PeptideSettings.IsotopeLabel label = IsotopeLabelManager.getIsotopeLabel(_precursor.getIsotopeLabelId());

            PrecursorChromatogramsViewBean bean = new PrecursorChromatogramsViewBean(
                    new ActionURL(PrecursorAllChromatogramsChartAction.class, getContainer()).getLocalURIString()
            );

            int maxTransitions = TargetedMSManager.getMaxTransitionCountForPrecursor(precursorId);
            form.setDefaultChartHeight(ChromatogramDisplayColumnFactory.calculateChartHeight(maxTransitions));

            bean.setForm(form);
            bean.setPrecursor(_precursor);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setIsotopeLabel(label);
            bean.setRun(_run);
            bean.setTargetedMSSchema(new TargetedMSSchema(getUser(), getContainer()));

            JspView<PrecursorChromatogramsViewBean> precursorInfo = new JspView<>("/org/labkey/targetedms/view/precursorChromatogramsView.jsp", bean);
            precursorInfo.setFrame(WebPartView.FrameType.PORTAL);
            precursorInfo.setTitle("Precursor Summary");

            PrecursorChromatogramsTableInfo tableInfo = new PrecursorChromatogramsTableInfo(new TargetedMSSchema(getUser(), getContainer()), form.getChartWidth(), form.getChartHeight());
            tableInfo.setPrecursorId(precursorId);
            tableInfo.addPrecursorFilter();

            ChromatogramsDataRegion dRegion = new ChromatogramsDataRegion(getViewContext(), tableInfo,
                    ChromatogramsDataRegion.PRECURSOR_CHROM_DATA_REGION);
            GridView gridView = new GridView(dRegion, errors);
            gridView.setFrame(WebPartView.FrameType.PORTAL);
            gridView.setTitle("Chromatograms");

            VBox vbox = new VBox();
            vbox.addView(precursorInfo);
            vbox.addView(gridView);

            // Summary charts for the precursor
            SummaryChartBean summaryChartBean = new SummaryChartBean(_run);
            summaryChartBean.setPeptideId(_precursor.getGeneralMoleculeId());
            summaryChartBean.setPrecursorId(_precursor.getId());
            summaryChartBean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(_run.getId()));
            summaryChartBean.setReplicateAnnotationValueList(ReplicateManager.getUniqueSortedAnnotationNameValue(_run.getId()));

            JspView<SummaryChartBean> summaryChartView = new JspView<>("/org/labkey/targetedms/view/summaryChartsView.jsp",
                    summaryChartBean);
            summaryChartView.setTitle("Summary Charts");
            summaryChartView.enableExpandCollapse("SummaryChartsView", false);

            vbox.addView(summaryChartView);

            // library spectrum
            addSpectrumViews(_run, vbox, _precursor, errors);
            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));

                root.addChild(_run.getDescription(), getShowRunURL(getContainer(), _run.getId()));

                ActionURL pepDetailsUrl = new ActionURL(ShowPeptideAction.class, getContainer());
                pepDetailsUrl.addParameter("id", String.valueOf(_peptideId));
                root.addChild("Peptide Details", pepDetailsUrl);

                root.addChild("Precursor Chromatograms: " + _precursor.getModifiedSequence());
            }
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class MoleculePrecursorAllChromatogramsChartAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private long _moleculeId; // save for use in appendNavTrail
        private MoleculePrecursor _precursor;

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors)
        {
            long precursorId = form.getId();
            _precursor = MoleculePrecursorManager.getPrecursor(getContainer(), precursorId, getUser());
            if (_precursor == null)
            {
                throw new NotFoundException("No such MoleculePrecursor found in this folder: " + precursorId);
            }

            _run = TargetedMSManager.getRunForPrecursor(precursorId);
            _moleculeId = _precursor.getGeneralMoleculeId();

            Molecule molecule = MoleculeManager.getMolecule(getContainer(), _precursor.getGeneralMoleculeId());
            PeptideGroup pepGroup = PeptideGroupManager.get(molecule.getPeptideGroupId());

            MoleculePrecursorChromatogramsViewBean bean = new MoleculePrecursorChromatogramsViewBean(
                    new ActionURL(MoleculePrecursorAllChromatogramsChartAction.class, getContainer()).getLocalURIString()
            );
            bean.setForm(form);
            bean.setPrecursor(_precursor);
            bean.setMolecule(molecule);
            bean.setPeptideGroup(pepGroup);
            bean.setRun(_run);

            JspView<MoleculePrecursorChromatogramsViewBean> precursorInfo = new JspView<>("/org/labkey/targetedms/view/moleculePrecursorChromatogramsView.jsp", bean);
            precursorInfo.setFrame(WebPartView.FrameType.PORTAL);
            precursorInfo.setTitle("Molecule Precursor Summary");

            PrecursorChromatogramsTableInfo tableInfo = new PrecursorChromatogramsTableInfo(new TargetedMSSchema(getUser(), getContainer()));
            tableInfo.setPrecursorId(precursorId);
            tableInfo.addPrecursorFilter();

            ChromatogramsDataRegion dRegion = new ChromatogramsDataRegion(getViewContext(), tableInfo,
                    ChromatogramsDataRegion.PRECURSOR_CHROM_DATA_REGION);
            GridView gridView = new GridView(dRegion, errors);
            gridView.setFrame(WebPartView.FrameType.PORTAL);
            gridView.setTitle("Chromatograms");

            // Summary charts for the molecule precursor
            SummaryChartBean summaryChartBean = new SummaryChartBean(_run);
            summaryChartBean.setMoleculeId(_precursor.getGeneralMoleculeId());
            summaryChartBean.setMoleculePrecursorId(_precursor.getId());
            summaryChartBean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(_run.getId()));
            summaryChartBean.setReplicateAnnotationValueList(ReplicateManager.getUniqueSortedAnnotationNameValue(_run.getId()));

            JspView<SummaryChartBean> summaryChartView = new JspView<>("/org/labkey/targetedms/view/summaryChartsView.jsp", summaryChartBean);
            summaryChartView.setTitle("Summary Charts");
            summaryChartView.enableExpandCollapse("SummaryChartsView", false);

            VBox vbox = new VBox();
            vbox.addView(precursorInfo);
            vbox.addView(gridView);
            vbox.addView(summaryChartView);
            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));

                root.addChild(_run.getDescription(), getShowRunURL(getContainer(), _run.getId()));

                ActionURL molDetailsUrl = new ActionURL(ShowMoleculeAction.class, getContainer());
                molDetailsUrl.addParameter("id", String.valueOf(_moleculeId));
                root.addChild("Molecule Details", molDetailsUrl);

                root.addChild("Molecule Precursor Chromatograms: " + _precursor.toString());
            }
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class PeptideAllChromatogramsChartAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors)
        {
            long peptideId = form.getId();
            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if (peptide == null)
            {
                throw new NotFoundException("No such Peptide found in this folder: " + peptideId);
            }

            _run = TargetedMSManager.getRunForGeneralMolecule(peptideId);

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            PeptideChromatogramsViewBean bean = new PeptideChromatogramsViewBean(
                    new ActionURL(PeptideAllChromatogramsChartAction.class, getContainer()).getLocalURIString()
            );

            bean.setForm(form);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setRun(_run);
            bean.setLabels(IsotopeLabelManager.getIsotopeLabels(_run.getId()));
            bean.setPrecursorList(PrecursorManager.getPrecursorsForPeptide(peptide.getId(), new TargetedMSSchema(getUser(), getContainer())));

            JspView<PeptideChromatogramsViewBean> peptideInfo = new JspView<>("/org/labkey/targetedms/view/peptideSummaryView.jsp", bean);
            peptideInfo.setFrame(WebPartView.FrameType.PORTAL);
            peptideInfo.setTitle("Peptide");

            PeptideChromatogramsTableInfo tableInfo = new PeptideChromatogramsTableInfo(new TargetedMSSchema(getUser(), getContainer()));
            tableInfo.setPeptideId(peptideId);
            tableInfo.addPeptideFilter();

            ChromatogramsDataRegion dRegion = new ChromatogramsDataRegion(getViewContext(), tableInfo,
                    ChromatogramsDataRegion.PEPTIDE_CHROM_DATA_REGION);
            GridView gridView = new GridView(dRegion, errors);
            gridView.setFrame(WebPartView.FrameType.PORTAL);
            gridView.setTitle("Chromatograms");

            VBox vbox = new VBox();
            vbox.addView(peptideInfo);
            vbox.addView(gridView);
            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getDescription(), getShowRunURL(getContainer(), _run.getId()));
                root.addChild("Peptide Chromatograms");
            }
        }
    }

    public static class MoleculePrecursorChromatogramsViewBean extends MoleculeChromatogramsViewBean
    {
        private MoleculePrecursor _precursor;

        public MoleculePrecursorChromatogramsViewBean(String resultsUri)
        {
            super(resultsUri);
        }

        public String getPrecursorLabel()
        {
            return _precursor.toString();
        }

        public MoleculePrecursor getPrecursor()
        {
            return _precursor;
        }

        public void setPrecursor(MoleculePrecursor precursor)
        {
            _precursor = precursor;
        }
    }

    public static class PrecursorChromatogramsViewBean extends PeptideChromatogramsViewBean
    {
        private Precursor _precursor;
        private PeptideSettings.IsotopeLabel _isotopeLabel;
        private TargetedMSSchema _targetedMSSchema;

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

        public HtmlString getModifiedPeptideHtml()
        {
            return new ModifiedPeptideHtmlMaker().getPrecursorHtml(getPrecursor(), getRun().getId(), _targetedMSSchema);
        }

        public PeptideSettings.IsotopeLabel getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(PeptideSettings.IsotopeLabel isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }

        public void setTargetedMSSchema(TargetedMSSchema s)
        {
            _targetedMSSchema = s;
        }
    }

    public static class GeneralMoleculeChromatogramsViewBean
    {
        private ChromatogramForm _form;
        private PeptideGroup _peptideGroup;
        private TargetedMSRun _run;
        protected String _resultsUri;

        private List<String> _replicateAnnotationNameList;
        private List<ReplicateAnnotation> _replicateAnnotationValueList;
        private List<Replicate> _replicatesFilter;
        private boolean _canBeSplitView;
        private boolean _showOptPeaksOption;


        public ChromatogramForm getForm()
        {
            return _form;
        }

        public void setForm(ChromatogramForm form)
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

        public PeptideGroup getPeptideGroup()
        {
            return _peptideGroup;
        }

        public void setPeptideGroup(PeptideGroup peptideGroup)
        {
            _peptideGroup = peptideGroup;
        }

        public String getResultsUri()
        {
            return _resultsUri;
        }

        public void setResultsUri(String resultsUri)
        {
            _resultsUri = resultsUri;
        }

        public boolean canBeSplitView()
        {
            return _canBeSplitView;
        }

        public void setCanBeSplitView(boolean canBeSplitView)
        {
            _canBeSplitView = canBeSplitView;
        }

        public boolean isShowOptPeaksOption()
        {
            return _showOptPeaksOption;
        }

        public void setShowOptPeaksOption(boolean showOptPeaksOption)
        {
            _showOptPeaksOption = showOptPeaksOption;
        }

        public List<Replicate> getReplicatesFilter()
        {
            return _replicatesFilter != null ? _replicatesFilter : Collections.emptyList();
        }
        public void setReplicatesFilter(List<Replicate> replciates)
        {
            _replicatesFilter = replciates;
        }

        public List<String> getReplicateAnnotationNameList()
        {
            return _replicateAnnotationNameList != null ? _replicateAnnotationNameList : Collections.emptyList();
        }

        public void setReplicateAnnotationNameList(List<String> replicateAnnotationNameList)
        {
            _replicateAnnotationNameList = replicateAnnotationNameList;
        }

        public List<ReplicateAnnotation> getReplicateAnnotationValueList()
        {
            return _replicateAnnotationValueList != null ? _replicateAnnotationValueList : Collections.emptyList();
        }

        public void setReplicateAnnotationValueList(List<ReplicateAnnotation> replicateAnnotationValueList)
        {
            _replicateAnnotationValueList = replicateAnnotationValueList;
        }
    }

    public static class MoleculeChromatogramsViewBean extends GeneralMoleculeChromatogramsViewBean
    {
        private Molecule _molecule;
        private List<MoleculePrecursor> _precursorList;

        public MoleculeChromatogramsViewBean(String resultsUri)
        {
            _resultsUri = resultsUri;
        }

        public Molecule getMolecule()
        {
            return _molecule;
        }

        public void setMolecule(Molecule molecule)
        {
            _molecule = molecule;
        }

        public List<MoleculePrecursor> getPrecursorList()
        {
            return _precursorList;
        }

        public void setPrecursorList(List<MoleculePrecursor> precursorList)
        {
            _precursorList = precursorList;
        }
    }

    public static class PeptideChromatogramsViewBean extends GeneralMoleculeChromatogramsViewBean
    {
        private Peptide _peptide;
        private List<Precursor> _precursorList;
        private List<PeptideSettings.IsotopeLabel> labels;

        public PeptideChromatogramsViewBean(String resultsUri)
        {
            _resultsUri = resultsUri;
        }

        public Peptide getPeptide()
        {
            return _peptide;
        }

        public void setPeptide(Peptide peptide)
        {
            _peptide = peptide;
        }

        public List<Precursor> getPrecursorList()
        {
            return _precursorList;
        }

        public void setPrecursorList(List<Precursor> precursorList)
        {
            _precursorList = precursorList;
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

    public static class ChromatogramForm extends AbstractChartForm
    {
        private long _id;
        private boolean _syncY = false;
        private boolean _syncX = false;
        private boolean _splitGraph = false;
        private boolean _showOptimizationPeaks = false;
        private String _annotationsFilter;
        private String _replicatesFilter;
        private boolean _update;

        public ChromatogramForm()
        {
            setDefaultChartWidth(400);
        }

        public long getId()
        {
            return _id;
        }

        public void setId(long id)
        {
            _id = id;
        }


        public String getReplicatesFilter()
        {
            return _replicatesFilter;
        }

        public void setReplicatesFilter(String replicatesFilter)
        {
            _replicatesFilter = replicatesFilter;
        }

        public List<Integer> getReplicatesFilterList()
        {
            List<Integer> replicatesList = new ArrayList<>();
            if(_replicatesFilter == null)
            {
                return null;
            }
            else {

                String filterReps[] = _replicatesFilter.split(",");
                for(String rep: filterReps)
                {
                    try
                    {
                        replicatesList.add(Integer.parseInt(rep));

                    }
                    catch (NumberFormatException e)
                    {
                        LOG.debug("Error parsing replicate Id: "+rep,e);
                    }
                }

                return replicatesList;
            }
        }

        public String getAnnotationsFilter()
        {
            return _annotationsFilter;
        }

        public List<ReplicateAnnotation> getAnnotationFilter()
        {
            List<ReplicateAnnotation> replicateList = new ArrayList<>();
            if(_annotationsFilter == null)
            {
                return null;
            }
            else {

                String filterReps[] = _annotationsFilter.split(",");
                for(int repCounter = 0; repCounter < filterReps.length; repCounter++)
                {
                    String[] filterNameValue = filterReps[repCounter].split(" : ");
                    if (filterNameValue.length >= 2)
                    {
                        ReplicateAnnotation rep = new ReplicateAnnotation();
                        rep.setName(filterNameValue[0]);
                        rep.setValue(filterNameValue[1]);
                        replicateList.add(rep);
                    }
                }

                return replicateList;
            }
        }

        public void setAnnotationsFilter(String annotationsFilter)
        {
            _annotationsFilter = annotationsFilter;
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

        public boolean isSplitGraph()
        {
            return _splitGraph;
        }

        public void setSplitGraph(boolean splitGraph)
        {
            _splitGraph = splitGraph;
        }

        public boolean isShowOptimizationPeaks()
        {
            return _showOptimizationPeaks;
        }

        public void setShowOptimizationPeaks(boolean showOptimizationPeaks)
        {
            _showOptimizationPeaks = showOptimizationPeaks;
        }

        public boolean isUpdate()
        {
            return _update;
        }

        public void setUpdate(boolean update)
        {
            _update = update;
        }
    }


    // ------------------------------------------------------------------------
    // Action to display peptide details page
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ShowPeptideAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private String _sequence;

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors)
        {
            long peptideId = form.getId();  // peptide Id

            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if(peptide == null)
            {
                throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", peptideId));
            }
            _sequence = peptide.getSequence();

            VBox vbox = new VBox();

            _run = TargetedMSManager.getRunForGeneralMolecule(peptideId);

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            int maxTransitions = TargetedMSManager.getMaxTransitionCount(peptideId);
            form.setDefaultChartHeight(ChromatogramDisplayColumnFactory.calculateChartHeight(maxTransitions));

            List<Precursor> precursorList = PrecursorManager.getPrecursorsForPeptide(peptide.getId(), new TargetedMSSchema(getUser(), getContainer()));

            List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(_run.getId());

            PeptideChromatogramsViewBean bean = new PeptideChromatogramsViewBean(
                    new ActionURL(ShowPeptideAction.class, getContainer()).getLocalURIString());
            bean.setForm(form);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setPrecursorList(precursorList);
            bean.setLabels(labels);
            bean.setRun(_run);

            // Summary for this peptide
            JspView<PeptideChromatogramsViewBean> peptideInfo = new JspView<>("/org/labkey/targetedms/view/peptideSummaryView.jsp", bean);
            peptideInfo.setFrame(WebPartView.FrameType.PORTAL);
            peptideInfo.setTitle("Peptide Summary");
            vbox.addView(peptideInfo);

            // Precursor and transition chromatograms. One row per replicate
            VBox chromatogramsBox = new VBox();
            boolean canBeSplitView = PrecursorManager.canBeSplitView(form.getId());
            bean.setCanBeSplitView(canBeSplitView);
            if(canBeSplitView && !form.isUpdate())
            {
                form.setSplitGraph(true);
            }
            boolean showOptPeaksOption = PrecursorManager.hasOptimizationPeaks(form.getId());
            bean.setShowOptPeaksOption(showOptPeaksOption);

            PeptidePrecursorChromatogramsView chromView = new PeptidePrecursorChromatogramsView(peptide, new TargetedMSSchema(getUser(), getContainer()),form, errors);
            JspView<PeptideChromatogramsViewBean> chartForm = new JspView<>("/org/labkey/targetedms/view/chromatogramsForm.jsp", bean);

            chromatogramsBox.setTitle("Chromatograms");
            chromatogramsBox.enableExpandCollapse("Chromatograms", false);
            chromatogramsBox.addView(chartForm);
            chromatogramsBox.addView(chromView);
            chromatogramsBox.setShowTitle(true);
            chromatogramsBox.setFrame(WebPartView.FrameType.PORTAL);
            vbox.addView(chromatogramsBox);

            // Summary charts for the peptide
            SummaryChartBean summaryChartBean = new SummaryChartBean(_run);
            summaryChartBean.setPeptideId(peptideId);
            summaryChartBean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(_run.getId()));
            summaryChartBean.setReplicateAnnotationValueList(ReplicateManager.getUniqueSortedAnnotationNameValue(_run.getId()));

            JspView<SummaryChartBean> summaryChartView = new JspView<>("/org/labkey/targetedms/view/summaryChartsView.jsp",
                    summaryChartBean);
            summaryChartView.setTitle("Summary Charts");
            summaryChartView.enableExpandCollapse("SummaryChartsView", false);

            vbox.addView(summaryChartView);
            addSpectrumViews(_run, vbox, peptide, errors);
            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getDescription(), getShowRunURL(getContainer(), _run.getId()));
                root.addChild(_sequence);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Action to display small molecule details page
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ShowMoleculeAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private String _customIonName;

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors)
        {
            long moleculeId = form.getId();

            Molecule molecule = MoleculeManager.getMolecule(getContainer(), moleculeId);
            if (molecule == null)
            {
                throw new NotFoundException(String.format("No small molecule found in this folder for moleculeId: %d", moleculeId));
            }
            _customIonName = molecule.getCustomIonName();

            VBox vbox = new VBox();

            _run = TargetedMSManager.getRunForGeneralMolecule(moleculeId);

            PeptideGroup pepGroup = PeptideGroupManager.get(molecule.getPeptideGroupId());

            List<MoleculePrecursor> precursorList = MoleculePrecursorManager.getPrecursorsForMolecule(molecule.getId(), new TargetedMSSchema(getUser(), getContainer()));

            MoleculeChromatogramsViewBean bean = new MoleculeChromatogramsViewBean(
                    new ActionURL(ShowMoleculeAction.class, getContainer()).getLocalURIString());
            bean.setForm(form);
            bean.setMolecule(molecule);
            bean.setPeptideGroup(pepGroup);
            bean.setPrecursorList(precursorList);
            bean.setRun(_run);

            // Summary for this molecule
            JspView<MoleculeChromatogramsViewBean> moleculeInfo = new JspView<>("/org/labkey/targetedms/view/moleculeSummaryView.jsp", bean);
            moleculeInfo.setFrame(WebPartView.FrameType.PORTAL);
            moleculeInfo.setTitle("Small Molecule Summary");
            vbox.addView(moleculeInfo);

            // Molecule precursor and transition chromatograms. One row per replicate
            VBox chromatogramsBox = new VBox();

            boolean canBeSplitView = PrecursorManager.canBeSplitView(form.getId());
            bean.setCanBeSplitView(canBeSplitView);
            if(canBeSplitView && !form.isUpdate())
            {
                form.setSplitGraph(true);
            }

            int maxTransitions = TargetedMSManager.getMaxTransitionCount(moleculeId);
            if (maxTransitions > 10)
            {
                form.setDefaultChartHeight(300 + maxTransitions * 10);
            }

            MoleculePrecursorChromatogramsView chromView = new MoleculePrecursorChromatogramsView(molecule, new TargetedMSSchema(getUser(), getContainer()), form, errors);
            JspView<MoleculeChromatogramsViewBean> chartForm = new JspView<>("/org/labkey/targetedms/view/chromatogramsForm.jsp", bean);

            chromatogramsBox.setTitle("Chromatograms");
            chromatogramsBox.enableExpandCollapse("Chromatograms", false);
            chromatogramsBox.addView(chartForm);
            chromatogramsBox.addView(chromView);
            chromatogramsBox.setShowTitle(true);
            chromatogramsBox.setFrame(WebPartView.FrameType.PORTAL);
            vbox.addView(chromatogramsBox);

            // Summary charts for the molecule
            SummaryChartBean summaryChartBean = new SummaryChartBean(_run);
            summaryChartBean.setMoleculeId(moleculeId);
            summaryChartBean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(_run.getId()));
            summaryChartBean.setReplicateAnnotationValueList(ReplicateManager.getUniqueSortedAnnotationNameValue(_run.getId()));
            JspView<SummaryChartBean> summaryChartView = new JspView<>("/org/labkey/targetedms/view/summaryChartsView.jsp", summaryChartBean);
            summaryChartView.setTitle("Summary Charts");
            summaryChartView.enableExpandCollapse("SummaryChartsView", false);
            vbox.addView(summaryChartView);

            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getDescription(), getShowRunURL(getContainer(), _run.getId()));
                root.addChild(_customIonName);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a library spectrum
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ShowSpectrumAction extends SimpleViewAction<ShowSpectrumForm>
    {
        @Override
        public ModelAndView getView(ShowSpectrumForm form, BindException errors)
        {
            long peptideId = form.getId();  // peptide Id

            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if(peptide == null)
            {
                throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", peptideId));
            }

            TargetedMSRun run = TargetedMSManager.getRunForGeneralMolecule(peptideId);

            VBox vbox = new VBox();
            addSpectrumViews(run, vbox, peptide, errors);
            return vbox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            //TODO: link back to peptides details page
        }
    }

    private void addSpectrumViews(TargetedMSRun run, VBox vbox, Precursor precursor, BindException errors)
    {
        PipeRoot root = PipelineService.get().getPipelineRootSetting(getContainer());
        if (null != root)
        {
            List<SpeclibReaderException> specLibErrors = new ArrayList<>();
            List<LibrarySpectrumMatch> matches = LibrarySpectrumMatchGetter.getMatches(precursor, root.getContainer(), specLibErrors);
            addSpectrumViews(run, vbox,
                    matches,
                    LibrarySpectrumMatchGetter.getUnsupportedLibraries(run),
                    specLibErrors);
        }
        else
        {
            errors.reject (ERROR_MSG, "Pipeline root not found.");
        }
    }

    private void addSpectrumViews(TargetedMSRun run, VBox vbox, Peptide peptide, BindException errors)
    {
        PipeRoot root = PipelineService.get().getPipelineRootSetting(getContainer());
        if (null != root)
        {
            LocalDirectory localDirectory = LocalDirectory.create(root, MODULE_NAME);
            try
            {
                List<SpeclibReaderException> specLibErrors = new ArrayList<>();
                List<LibrarySpectrumMatch> matches = LibrarySpectrumMatchGetter.getMatches(peptide, getUser(), getContainer(), root.getContainer(), specLibErrors);
                addSpectrumViews(run, vbox,
                        matches,
                        LibrarySpectrumMatchGetter.getUnsupportedLibraries(run),
                        specLibErrors);
            }
            finally
            {
                localDirectory.cleanUpLocalDirectory();
            }
        }
        else
        {
            errors.reject (ERROR_MSG, "Pipeline root not found.");
        }
    }

    private void addSpectrumViews(TargetedMSRun run, VBox vbox, List<LibrarySpectrumMatch> libSpectraMatchList, List<String> unsupportedLibraries,
                                  List<SpeclibReaderException> specLibErrors)
    {
        PeptideSettings.ModificationSettings modSettings = ModificationManager.getSettings(run.getRunId());
        int idx = 0;
        for(LibrarySpectrumMatch libSpecMatch: libSpectraMatchList)
        {
            libSpecMatch.setLorikeetId(idx);
            if(modSettings != null)
            {
                libSpecMatch.setMaxNeutralLosses(modSettings.getMaxNeutralLosses());
            }
            PeptideSpectrumView spectrumView = new PeptideSpectrumView(libSpecMatch);
            spectrumView.enableExpandCollapse("PeptideSpectrumView_" + idx, false);
            vbox.addView(spectrumView);

            idx++;
        }
        if(unsupportedLibraries.size() > 0)
        {
            HtmlView view = new HtmlView(DIV("Annotated spectra cannot be displayed from the following unsupported "
                            + (unsupportedLibraries.size() == 1 ? "library" : "libraries") + ": ",
                    (unsupportedLibraries.size() == 1 ?
                            SPAN(cl("labkey-error"), unsupportedLibraries.get(0))
                            : DIV(cl("labkey-error"), UL(unsupportedLibraries.stream().map(DOM::LI)))
                    )
            ));
            view.setTitle("Unsupported Spectrum " + (unsupportedLibraries.size() == 1 ? "Library" : "Libraries"));
            vbox.addView(view);
        }
        if(specLibErrors.size() > 0)
        {
            HtmlView view = new HtmlView(DOM.LK.ERRORS(specLibErrors.stream().map(e -> new LabKeyError(e.getMessage())).collect(Collectors.toList())));
            view.setTitle("Spectrum Library Errors");
            vbox.addView(view);
            specLibErrors.forEach(e -> LOG.info(e.getMessage(), e));
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class LibrarySpectrumDataAction extends ReadOnlyApiAction<SpectrumDataForm>
    {
        @Override
        public Object execute(SpectrumDataForm form, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            Precursor precursor = PrecursorManager.getPrecursor(getContainer(), form.getPrecursorId(), getUser());
            if (precursor == null)
            {
                return response;
            }
            Peptide peptide = PeptideManager.getPeptide(getContainer(), precursor.getGeneralMoleculeId());
            if (peptide == null)
            {
                response.put("error", "Could not find peptide/molecule for id " + precursor.getGeneralMoleculeId());
                return response;
            }
            TargetedMSRun run = TargetedMSManager.getRunForGeneralMolecule(peptide.getId());
            if (run == null)
            {
                response.put("error", "Could not find run for id " + precursor.getGeneralMoleculeId());
                return response;
            }

            List<PeptideSettings.SpectrumLibrary> libraries = LibraryManager.getLibraries(run.getId());
            PeptideSettings.SpectrumLibrary library = null;
            for (PeptideSettings.SpectrumLibrary lib : libraries)
            {
                if (lib.getName().equals(form.getLibraryName()))
                {
                    library = lib;
                    break;
                }
            }

            if (library == null)
            {
                response.put("error", "Could not find the library :" + form.getLibraryName());
                return response;
            }

            Path libFilePath = LibraryManager.getLibraryFilePath(run.getId(), library);
            PipeRoot root = PipelineService.get().getPipelineRootSetting(getContainer());
            if (null != root)
            {
                LibrarySpectrumMatch spectrumMatch;
                try
                {
                    spectrumMatch = LibrarySpectrumMatchGetter.getSpectrumMatch(run, peptide, precursor, library,
                            libFilePath, root.getContainer(), form.getRedundantRefSpectrumId(), form.getSourceFile());
                }
                catch (SpeclibReaderException e)
                {
                    LOG.info(e.getMessage(), e);
                    response.put("error", e.getMessage());
                    return response;
                }

                if (spectrumMatch == null)
                {
                    response.put("error", "Could not find spectrum in library " + form.getLibraryName());
                    return response;
                }
                Map<String, Object> spectrumDetails = new HashMap<>(4);
                spectrumDetails.put("sequence", spectrumMatch.getPeptide());
                spectrumDetails.put("staticMods", spectrumMatch.getStructuralModifications());
                spectrumDetails.put("variableMods", spectrumMatch.getVariableModifications());
                spectrumDetails.put("maxNeutralLossCount", spectrumMatch.getMaxNeutralLosses());
                spectrumDetails.put("charge", spectrumMatch.getCharge());
                spectrumDetails.put("fileName", spectrumMatch.getSpectrum().getSourceFileName());
                spectrumDetails.put("retentionTime", spectrumMatch.getSpectrum().getRetentionTimeF2());
                spectrumDetails.put("peaks", spectrumMatch.getPeaks());
                spectrumDetails.put("redundantSpectra", spectrumMatch.getRedundantSpectraList()); // Only spectrum read from the non-redundant library will have a list of redundant spectra.

                response.put("spectrum", spectrumDetails);
            }
            else
            {
                response.put("error", "Pipeline root not found.");
            }

            return response;
        }
    }

    private static class SpectrumDataForm
    {
        private String _libraryName;
        private int _redundantRefSpectrumId;
        private long _precursorId;
        private String _sourceFile;

        public String getLibraryName()
        {
            return _libraryName;
        }

        public void setLibraryName(String libraryName)
        {
            _libraryName = libraryName;
        }

        public int getRedundantRefSpectrumId()
        {
            return _redundantRefSpectrumId;
        }

        public void setRedundantRefSpectrumId(int redundantRefSpectrumId)
        {
            _redundantRefSpectrumId = redundantRefSpectrumId;
        }

        public long getPrecursorId()
        {
            return _precursorId;
        }

        public void setPrecursorId(long precursorId)
        {
            _precursorId = precursorId;
        }

        public String getSourceFile()
        {
            return _sourceFile;
        }

        public void setSourceFile(String sourceFile)
        {
            _sourceFile = sourceFile;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class AutoQCPingAction extends MutatingApiAction<Object>
    {
        @Override
        public Object execute(Object o, BindException errors)
        {
            // Get current record, if present
            Map<String, Object> currentRow = TargetedMSManager.get().getAutoQCPingMap(getContainer());
            if (currentRow == null)
            {
                // Add a new record for this container
                currentRow = Table.insert(getUser(), TargetedMSManager.getTableInfoAutoQCPing(), Collections.singletonMap("Container", getContainer()));
            }
            else
            {
                // Update the current one with the new timestamp
                currentRow = Table.update(getUser(), TargetedMSManager.getTableInfoAutoQCPing(), currentRow, getContainer());
            }

            // Just return the full record back to the caller
            return new ApiSimpleResponse(currentRow);
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

    private abstract class ShowSummaryChartAction<FORM> extends ExportAction<FORM>
    {
        PeptideGroup _peptideGrp = null;
        Peptide _peptide = null;
        Precursor _precursor = null;
        Molecule _molecule = null;
        MoleculePrecursor _moleculePrecursor = null;

        public void validatePeptideGroup(SummaryChartForm form)
        {
            if (form.getPeptideGroupId() != 0)
            {
                _peptideGrp = PeptideGroupManager.getPeptideGroup(getContainer(), form.getPeptideGroupId());
                if (_peptideGrp == null)
                {
                    throw new NotFoundException(String.format("No peptide group found in this folder for peptideGroupId: %d", form.getPeptideGroupId()));
                }
            }
        }

        public void validatePeptide(SummaryChartForm form)
        {
            if (form.getPeptideId() != 0)
            {
                _peptide = PeptideManager.getPeptide(getContainer(), form.getPeptideId());
                if (_peptide == null)
                {
                    throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", form.getPeptideId()));
                }

                if (_peptideGrp == null)
                {
                    _peptideGrp = PeptideGroupManager.getPeptideGroup(getContainer(), _peptide.getPeptideGroupId());
                }

                if(form.getPrecursorId() != 0)
                {
                    _precursor = PrecursorManager.getPrecursor(getContainer(), form.getPrecursorId(), getUser());
                    if (_precursor == null)
                    {
                        throw new NotFoundException(String.format("No precursor found in this folder for precursorId: %d", form.getPrecursorId()));
                    }
                }
            }
        }

        public void validateMolecule(SummaryChartForm form)
        {
            if (form.getMoleculeId() != 0)
            {
                _molecule = MoleculeManager.getMolecule(getContainer(), form.getMoleculeId());
                if (_molecule == null)
                {
                    throw new NotFoundException(String.format("No small molecule found in this folder for moleculeId: %d", form.getMoleculeId()));
                }

                if (_peptideGrp == null)
                {
                    _peptideGrp = PeptideGroupManager.getPeptideGroup(getContainer(), _molecule.getPeptideGroupId());
                }

                if(form.getMoleculePrecursorId() != 0)
                {
                    _moleculePrecursor = MoleculePrecursorManager.getPrecursor(getContainer(), form.getMoleculePrecursorId(), getUser());
                    if (_moleculePrecursor == null)
                    {
                        throw new NotFoundException(String.format("No molecule precursor found in this folder for precursorId: %d", form.getMoleculePrecursorId()));
                    }
                }
            }
        }

        public JFreeChart createEmptyChart()
        {
            JFreeChart chart = ChartFactory.createBarChart("", "", "", null, PlotOrientation.VERTICAL, false, false, false);
            chart.setTitle(new TextTitle("No chromatogram data found.", new java.awt.Font("SansSerif", Font.PLAIN, 12)));
            return chart;
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a peak areas chart
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ShowPeakAreasAction extends ShowSummaryChartAction<SummaryChartForm>
    {
        @Override
        public void validate(SummaryChartForm form, BindException errors)
        {
            validatePeptideGroup(form);
            validatePeptide(form);
            validateMolecule(form);
        }

        @Override
        public void export(SummaryChartForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            JFreeChart chart;
            if (form.isAsProteomics())
            {
                chart = new ComparisonChartMaker().makePeakAreasChart(
                        form.getReplicateId(),
                        _peptideGrp,
                        _peptide,
                        _precursor,
                        form.getGroupByReplicateAnnotName(),
                        form.getFilterByReplicateAnnotName(),
                        form.isCvValues(),
                        form.isLogValues(), getUser(), getContainer()
                );
            }
            else
            {
                chart = new ComparisonChartMaker().makePeakAreasChart(
                        form.getReplicateId(),
                        _peptideGrp,
                        _molecule,
                        _moleculePrecursor,
                        form.getGroupByReplicateAnnotName(),
                        form.getFilterByReplicateAnnotName(),
                        form.isCvValues(),
                        form.isLogValues(), getUser(), getContainer()
                );
            }

            if (null == chart)
            {
                chart = createEmptyChart();
                form.setChartHeight(20);
                form.setChartWidth(300);
            }

            writeChart(form, response, chart);
        }
    }

    // ------------------------------------------------------------------------
    // Action to display retention times chart.
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ShowRetentionTimesChartAction extends ShowSummaryChartAction<SummaryChartForm>
    {
        @Override
        public void validate(SummaryChartForm form, BindException errors)
        {
            validatePeptideGroup(form);
            validatePeptide(form);
            validateMolecule(form);
        }

        @Override
        public void export(SummaryChartForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            if (form.getValue() == null)
                form.setValue("All");

            JFreeChart chart;
            if (form.isAsProteomics())
            {
                chart = new ComparisonChartMaker().makeRetentionTimesChart(
                        form.getReplicateId(),
                        _peptideGrp,
                        _peptide,
                        _precursor,
                        form.getGroupByReplicateAnnotName(),
                        form.getFilterByReplicateAnnotName(),
                        form.getValue(), form.isCvValues(),
                        getUser(), getContainer()
                );
            }
            else
            {
                chart = new ComparisonChartMaker().makeRetentionTimesChart(
                        form.getReplicateId(),
                        _peptideGrp,
                        _molecule,
                        _moleculePrecursor,
                        form.getGroupByReplicateAnnotName(),
                        form.getFilterByReplicateAnnotName(),
                        form.getValue(), form.isCvValues(),
                        getUser(), getContainer()
                );
            }

            if (null == chart)
            {
                chart = createEmptyChart();
                form.setChartHeight(20);
                form.setChartWidth(300);
            }

            writeChart(form, response, chart);
        }
    }


    public abstract static class AbstractChartForm
    {
        public static final int SCREEN_RES = 72;

        private int _defaultChartWidth = 600;
        private int _defaultChartHeight = 400;

        private Integer _chartWidth = null;
        private Integer _chartHeight = null;
        private int _dpi = 144;

        private String _format = "png";
        private boolean _legend = true;

        public int getChartWidth()
        {
            return _chartWidth == null ? _defaultChartWidth : _chartWidth.intValue();
        }

        public void setDefaultChartWidth(int defaultChartWidth)
        {
            _defaultChartWidth = defaultChartWidth;
        }

        public void setDefaultChartHeight(int defaultChartHeight)
        {
            _defaultChartHeight = defaultChartHeight;
        }

        public void setChartWidth(int chartWidth)
        {
            if (chartWidth > 0)
            {
                _chartWidth = chartWidth;
            }
        }

        public int getChartHeight()
        {
            return _chartHeight == null ? _defaultChartHeight : _chartHeight.intValue();
        }

        public void setChartHeight(int chartHeight)
        {
            if (chartHeight > 0)
            {
                _chartHeight = chartHeight;
            }
        }

        public int getDpi()
        {
            return _dpi;
        }

        public void setDpi(int dpi)
        {
            _dpi = dpi;
        }

        public boolean hasDpi()
        {
            return _dpi > SCREEN_RES;
        }

        public String getFormat()
        {
            return _format;
        }

        public void setFormat(String format)
        {
            _format = format;
        }

        public boolean isLegend()
        {
            return _legend;
        }

        public void setLegend(boolean legend)
        {
            _legend = legend;
        }
    }

    public static class SummaryChartForm extends AbstractChartForm
    {
        private boolean _asProteomics;
        private long _peptideGroupId;
        private long _replicateId = 0; // A value of 0 means all replicates should be included in the plot.
        private String _groupByReplicateAnnotName;
        private ReplicateAnnotation _annotationFilter;
        private String _filterByReplicateAnnotName;
        private boolean _cvValues;
        private boolean _logValues;
        private String _value;

        // fields for proteomics
        private long _peptideId = 0;
        private long _precursorId = 0;

        // fields for small molecule
        private long _moleculeId = 0;
        private long _moleculePrecursorId = 0;

        public boolean isAsProteomics()
        {
            return _asProteomics;
        }

        public void setAsProteomics(boolean asProteomics)
        {
            _asProteomics = asProteomics;
        }

        public String getValue()
        {
            return _value;
        }

        public void setValue(String value)
        {
            _value = value;
        }

        public long getPeptideGroupId()
        {
            return _peptideGroupId;
        }

        public void setPeptideGroupId(long peptideGroupId)
        {
            _peptideGroupId = peptideGroupId;
        }

        public long getReplicateId()
        {
            return _replicateId;
        }

        public void setReplicateId(long replicateId)
        {
            _replicateId = replicateId;
        }

        public String getGroupByReplicateAnnotName()
        {
            return _groupByReplicateAnnotName;
        }

        public void setGroupByReplicateAnnotName(String groupByReplicateAnnotName)
        {
            _groupByReplicateAnnotName = groupByReplicateAnnotName;
        }

        public String getFilterByReplicateAnnotName()
        {
            return _filterByReplicateAnnotName;
        }

        public void setFilterByReplicateAnnotName(String filterByReplicateAnnotName)
        {
            _filterByReplicateAnnotName = filterByReplicateAnnotName;
        }

        public long getPeptideId()
        {
            return _peptideId;
        }

        public void setPeptideId(long peptideId)
        {
            _peptideId = peptideId;
        }

        public long getPrecursorId()
        {
            return _precursorId;
        }

        public void setPrecursorId(long precursorId)
        {
            _precursorId = precursorId;
        }

        public long getMoleculeId()
        {
            return _moleculeId;
        }

        public void setMoleculeId(long moleculeId)
        {
            _moleculeId = moleculeId;
        }

        public long getMoleculePrecursorId()
        {
            return _moleculePrecursorId;
        }

        public void setMoleculePrecursorId(long moleculePrecursorId)
        {
            _moleculePrecursorId = moleculePrecursorId;
        }

        public boolean isCvValues()
        {
            return _cvValues;
        }

        public void setCvValues(boolean cvValues)
        {
            _cvValues = cvValues;
        }

        public boolean isLogValues()
        {
            return _logValues;
        }

        public void setLogValues(boolean logValues)
        {
            _logValues = logValues;
        }

        public ReplicateAnnotation getAnnotationFilter()
        {
            return _annotationFilter;
        }

        public void setAnnotationFilter(ReplicateAnnotation annotationFilter)
        {
            _annotationFilter = annotationFilter;
        }
    }

    public static class SkylinePipelinePathForm extends PipelinePathForm
    {
        private String[] _proteinRepresentative = new String[0];
        private String[] _peptideRepresentative = new String[0];

        public String[] getProteinRepresentative()
        {
            return _proteinRepresentative;
        }

        public void setProteinRepresentative(String[] representative)
        {
            _proteinRepresentative = representative;
        }

        public String[] getPeptideRepresentative()
        {
            return _peptideRepresentative;
        }

        public void setPeptideRepresentative(String[] peptideRepresentative)
        {
            _peptideRepresentative = peptideRepresentative;
        }

        @Override
        public List<File> getValidatedFiles(Container c)
        {
            List<File> files = super.getValidatedFiles(c);
            List<File> resolvedFiles = new ArrayList<>(files.size());
            for(File file: files)
            {
                resolvedFiles.add(FileUtil.resolveFile(file));  // Strips out ".." and "." from the path
            }
            return resolvedFiles;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class SkylineDocUploadOptionsAction extends FormViewAction<SkylinePipelinePathForm>
    {
        @Override
        public void validateCommand(SkylinePipelinePathForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(SkylinePipelinePathForm form, boolean reshow, BindException errors)
        {
            form.getValidatedFiles(getContainer());
            return new JspView<>("/org/labkey/targetedms/view/confirmImport.jsp", form, errors);
        }

        @Override
        public boolean handlePost(SkylinePipelinePathForm form, BindException errors)
        {
            return false;
        }

        @Override
        public URLHelper getSuccessURL(SkylinePipelinePathForm form)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Confirm TargetedMS Data Import");
        }
    }

    // ------------------------------------------------------------------------
    // Document upload action
    // ------------------------------------------------------------------------
    @RequiresPermission(InsertPermission.class)
    public class SkylineDocUploadAction extends FormHandlerAction<SkylinePipelinePathForm>
    {
        @Override
        public void validateCommand(SkylinePipelinePathForm form, Errors errors)
        {
        }

        @Override
        public boolean handlePost(SkylinePipelinePathForm form, BindException errors) throws Exception
        {
            for (Path path : form.getValidatedPaths(getContainer(), false))
            {
                if (Files.isDirectory(path))
                {
                    throw new NotFoundException("Expected a file but found a directory: " + FileUtil.getFileName(path));
                }

                ViewBackgroundInfo info = getViewBackgroundInfo();
                try
                {
                    TargetedMSManager.addRunToQueue(info, path);
                }
                catch (PipelineValidationException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }

        @Override
        public ActionURL getSuccessURL(SkylinePipelinePathForm form)
        {
            return urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class SkylineDocUploadApiAction extends MutatingApiAction<PipelinePathForm>
    {
        @Override
        public ApiResponse execute(PipelinePathForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            List<Map<String, Object>> jobDetailsList = new ArrayList<>();

            for (Path path : form.getValidatedPaths(getContainer(), false))
            {
                if (Files.isDirectory(path))
                {
                    throw new NotFoundException("Expected a file but found a directory: " + FileUtil.getFileName(path));
                }

                ViewBackgroundInfo info = getViewBackgroundInfo();
                try
                {
                    Integer jobId = TargetedMSManager.addRunToQueue(info, path);
                    Map<String, Object> detailsMap = new HashMap<>(4);
                    detailsMap.put("Path", form.getPath());
                    detailsMap.put("File", FileUtil.getFileName(path));
                    detailsMap.put("RowId", jobId);
                    jobDetailsList.add(detailsMap);
                }
                catch (PipelineValidationException e)
                {
                    throw new ApiUsageException(e);
                }
            }
            response.put("UploadedJobDetails", jobDetailsList);
            return response;
        }
    }

    @RequiresLogin
    public class GetMaxSupportedVersionsAction extends ReadOnlyApiAction
    {
        @Override
        public ApiResponse execute(Object object, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            response.put("SKY_version", SkylineDocumentParser.MAX_SUPPORTED_VERSION);
            response.put("SKYD_version", SkylineBinaryParser.FORMAT_VERSION_CACHE.ordinal());
            return response;
        }
    }

    private JspView getSummaryView(RunDetailsForm form, TargetedMSRun run)
    {
        Set<Integer> ids = Collections.singleton(ExperimentService.get().getExpRun(run.getExperimentRunLSID()).getRowId());

        RunDetailsBean bean = new RunDetailsBean();
        bean.setForm(form);
        bean.setRun(run);
        bean.setVersionCount(TargetedMSManager.getLinkedVersions(getUser(), getContainer(), ids, ids).size());

        JspView<RunDetailsBean> runSummaryView = new JspView<>("/org/labkey/targetedms/view/runSummaryView.jsp", bean);
        runSummaryView.setFrame(WebPartView.FrameType.PORTAL);
        runSummaryView.setTitle("Document Summary");

        return runSummaryView;
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowVersionsAction extends SimpleViewAction<RunDetailsForm>
    {
        protected TargetedMSRun _run;  // save for use in appendNavTrail

        @Override
        public ModelAndView getView(RunDetailsForm form, BindException errors)
        {
            //this action requires that a specific experiment run has been specified
            if(!form.hasRunId())
                throw new RedirectException(new ActionURL(ShowListAction.class, getContainer()));

            //ensure that the experiment run is valid and exists within the current container
            _run = validateRun(form.getId());

            VBox vBox = new VBox();
            vBox.addView(getSummaryView(form, _run));

            ExpRun expRun = ExperimentService.get().getExpRun(_run.getExperimentRunLSID());
            if (expRun != null)
            {
                JspView<ExpRun> runMethodChain = new JspView<>("/org/labkey/targetedms/view/runMethodChain.jsp", expRun);
                runMethodChain.setFrame(WebPartView.FrameType.PORTAL);
                runMethodChain.setTitle("Document Versions");
                vBox.addView(runMethodChain);
            }
            return vBox;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getDescription(), getShowRunURL(getContainer(), _run.getId()));
            }
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowPKAction extends SimpleViewAction<PKForm>
    {
        private TargetedMSRun _run;  // save for use in appendNavTrail
        private GeneralMolecule _molecule;

        @Override
        public void validate(PKForm form, BindException errors)
        {
            if (form.getRunId() == null || form.getGeneralMoleculeId() == null)
                throw new NotFoundException("Missing one of the required parameters, RunId or GeneralMoleculeId.");

            _run = validateRun(form.getRunId());

            _molecule = PeptideManager.getPeptide(getContainer(), form.getGeneralMoleculeId());
            if (_molecule == null)
                _molecule = MoleculeManager.getMolecule(getContainer(), form.getGeneralMoleculeId());
            if (_molecule == null)
                throw new NotFoundException("Could not find Molecule " + form.getGeneralMoleculeId());
        }

        @Override
        public ModelAndView getView(PKForm form, BindException errors)
        {
            List<String> subgroupNames = TargetedMSManager.get().getReplicateSubgroupNames(getUser(), getContainer(), _molecule);
            form.setSampleGroupNames(subgroupNames);
            return new JspView<>("/org/labkey/targetedms/view/pharmacokinetics.jsp", form);
        }
        
        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                ActionURL showCalibrationCurvesURL = getShowCalibrationCurvesURL(getContainer(), _run.getId());
                root.addChild(_run.getDescription(), showCalibrationCurvesURL);
            }
            root.addChild("Pharmacokinetics");
        }
    }

    @RequiresPermission(ReadPermission.class)
    @Marshal(Marshaller.Jackson)
    public class PharmacokineticsOptionsAction extends MutatingApiAction<PKOptions>
    {
        private static final String CATEGORY = "TargetedMSPharmacokineticsOptions";
        private static final char SEPARATOR = '|';

        @Override
        public Object execute(PKOptions form, BindException errors)
        {
            PropertyManager.PropertyMap properties = PropertyManager.getWritableProperties(getContainer(), getPropMapName(form), true);

            // only stash option properties for users with editor role (i.e. UpdatePermissions)
            if (form.getSubgroups() != null && getContainer().hasPermission(getUser(), UpdatePermission.class))
            {
                for (Map.Entry<String, PKSubgroupOptions> subgroupEntry : form.getSubgroups().entrySet())
                {
                    properties.put(subgroupEntry.getKey() + SEPARATOR + "nonIVC0", subgroupEntry.getValue().getNonIVC0());
                    properties.put(subgroupEntry.getKey() + SEPARATOR + "c0", subgroupEntry.getValue().getC0AsString());
                    properties.put(subgroupEntry.getKey() + SEPARATOR + "terminal", subgroupEntry.getValue().getTerminalAsString());
                }
                properties.save();
            }

            ApiSimpleResponse response = new ApiSimpleResponse();
            Map<String, PKSubgroupOptions> subgroupOptions = new HashMap<>();
            for (Map.Entry<String, String> prop : properties.entrySet())
            {
                // expect the prop key to be subgroup|name, dismiss the prop if it doesn't fit that shape
                String[] keys = StringUtils.split(prop.getKey(), SEPARATOR);
                if (keys.length == 2)
                {
                    String subgroup = keys[0];
                    String propName = keys[1];
                    if (!subgroupOptions.containsKey(subgroup))
                        subgroupOptions.put(subgroup, new PKSubgroupOptions());
                    subgroupOptions.get(subgroup).setProp(propName, prop.getValue());
                }
            }
            response.put("subgroups", subgroupOptions);
            return response;
        }

        private String getPropMapName(PKOptions form)
        {
            return CATEGORY + (form.getMoleculeId() != null ? form.getMoleculeId() : "null");
        }
    }

    private static class PKOptions
    {
        private Integer _moleculeId;
        private Map<String, PKSubgroupOptions> _subgroups;

        public void setMoleculeId(Integer moleculeId)
        {
            _moleculeId = moleculeId;
        }

        public Integer getMoleculeId()
        {
            return _moleculeId;
        }

        public Map<String, PKSubgroupOptions> getSubgroups()
        {
            return _subgroups;
        }

        public void setSubgroups(Map<String, PKSubgroupOptions> subgroups)
        {
            _subgroups = subgroups;
        }
    }

    private static class PKSubgroupOptions
    {
        private String _nonIVC0;
        private List<String> _c0;
        private List<String> _terminal;

        public void setNonIVC0(String nonIVC0)
        {
            _nonIVC0 = nonIVC0;
        }

        public String getNonIVC0()
        {
            return _nonIVC0;
        }

        public void setC0(List<String> c0)
        {
            _c0 = c0;
        }

        public List<String> getC0()
        {
            return _c0;
        }

        public String getC0AsString()
        {
            return _c0 != null ? StringUtils.join(_c0.toArray(), ',') : null;
        }

        public void setTerminal(List<String> terminal)
        {
            _terminal = terminal;
        }

        public List<String> getTerminal()
        {
            return _terminal;
        }

        public String getTerminalAsString()
        {
            return _terminal != null ? StringUtils.join(_terminal.toArray(), ',') : null;
        }

        public void setProp(String name, String value)
        {
            if ("nonIVC0".equalsIgnoreCase(name))
                setNonIVC0(value);
            else if ("c0".equalsIgnoreCase(name))
                setC0(Arrays.asList(StringUtils.split(value, ',')));
            else if ("terminal".equalsIgnoreCase(name))
                setTerminal(Arrays.asList(StringUtils.split(value, ',')));
        }
    }

    public abstract class AbstractShowRunDetailsAction<FormType extends RunDetailsForm, ViewType extends QueryView>
            extends QueryViewAction<FormType, ViewType>
    {
        protected TargetedMSRun _run;  // save for use in appendNavTrail

        protected AbstractShowRunDetailsAction(Class<FormType> formClass)
        {
            super(formClass);
        }

        @Override
        public void validate(RunDetailsForm form, BindException errors)
        {
            //this action requires that a specific experiment run has been specified
            if(!form.hasRunId())
                throw new RedirectException(new ActionURL(ShowListAction.class, getContainer()));

            //ensure that the experiment run is valid and exists within the current container
            _run = validateRun(form.getId());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addNavTrail(root, _run);
        }

        public void addNavTrail(NavTree root, TargetedMSRun run)
        {
            root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
            if (run != null)
            {
                root.addChild(run.getDescription(), getShowRunURL(getContainer(), run.getId()));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a document's transition or precursor list, with both proteomics and small molecule views
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public abstract class ShowRunSplitDetailsAction<VIEWTYPE extends DocumentView> extends AbstractShowRunDetailsAction<RunDetailsForm, VIEWTYPE>
    {
        protected String _dataRegion;

        public ShowRunSplitDetailsAction()
        {
            super(RunDetailsForm.class);
        }

        @Override
        public ModelAndView getHtmlView(final RunDetailsForm form, BindException errors) throws Exception
        {
            VBox vBox = new VBox();
            vBox.addView(getSummaryView(form, _run));

            VIEWTYPE view;

            // for proteomics version of the Precursor List query view
            if(_run.getPeptideCount() > 0)
            {
                view = createInitializedQueryView(form, errors, false, getDataRegionNamePeptide());
                _dataRegion = view.getDataRegionName();
                vBox.addView(view);
            }

            // for small molecule version of the Precursor List query view
            if(_run.getSmallMoleculeCount() > 0)
            {
                view = createInitializedQueryView(form, errors, false, getDataRegionNameSmallMolecule());
                _dataRegion = view.getDataRegionName();
                vBox.addView(view);
            }

            return vBox;
        }

        public abstract String getDataRegionNamePeptide();
        public abstract String getDataRegionNameSmallMolecule();
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowTransitionListAction extends ShowRunSplitDetailsAction<DocumentTransitionsView>
    {
        @Override
        protected DocumentTransitionsView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            DocumentTransitionsView view;

            if(dataRegion.equals(PeptideTransitionsView.DATAREGION_NAME))
            {
                view = new PeptideTransitionsView(getViewContext(),
                        new TargetedMSSchema(getUser(), getContainer()), TargetedMSSchema.TABLE_TRANSITION,
                        form.getId(), forExport);
            }
            else
            {
                view = new SmallMoleculeTransitionsView(getViewContext(),
                        new TargetedMSSchema(getUser(), getContainer()), TargetedMSSchema.TABLE_MOLECULE_TRANSITION,
                        form.getId(), forExport);
            }

            view.setShowExportButtons(true);
            return view;
        }

        @Override
        public String getDataRegionNamePeptide()
        {
            return PeptideTransitionsView.DATAREGION_NAME;
        }

        @Override
        public String getDataRegionNameSmallMolecule()
        {
            return SmallMoleculeTransitionsView.DATAREGION_NAME;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowPrecursorListAction extends ShowRunSplitDetailsAction<DocumentPrecursorsView>
    {
        // Invoked via reflection
        @SuppressWarnings("UnusedDeclaration")
        public ShowPrecursorListAction()
        {
        }

        public ShowPrecursorListAction(ViewContext ctx)
        {
            setViewContext(ctx);
        }

        @Override
        protected DocumentPrecursorsView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            DocumentPrecursorsView view;
            if(PeptidePrecursorsView.DATAREGION_NAME.equals(dataRegion))
            {
                FolderType folderType = TargetedMSManager.getFolderType(getContainer());
                String queryName;
                if (folderType == FolderType.LibraryProtein || folderType == FolderType.Library)
                {
                    queryName = TargetedMSSchema.TABLE_LIBRARY_DOC_PRECURSOR;
                }
                else
                {
                    queryName = TargetedMSSchema.TABLE_EXPERIMENT_PRECURSOR;
                }

                view = new PeptidePrecursorsView(getViewContext(),
                        new TargetedMSSchema(getUser(), getContainer()),
                        queryName,
                        form.getId(),
                        forExport)
                {
                };
            }
            else
            {
                view = new SmallMoleculePrecursorsView(getViewContext(),
                        new TargetedMSSchema(getUser(), getContainer()),
                        TargetedMSSchema.TABLE_MOLECULE_PRECURSOR,
                        form.getId(),
                        forExport);
            }

            view.setShowExportButtons(true);
            view.setShowDetailsColumn(false);
            view.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTH);

            return view;
        }

        @Override
        public String getDataRegionNamePeptide()
        {
            return PeptidePrecursorsView.DATAREGION_NAME;
        }

        @Override
        public String getDataRegionNameSmallMolecule()
        {
            return SmallMoleculePrecursorsView.DATAREGION_NAME;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowGroupComparisonAction extends ShowRunSplitDetailsAction<GroupComparisonView>
    {
        public ShowGroupComparisonAction()
        {
            setCommandClass(GroupComparisonView.Form.class);
        }

        @Override
        public String getDataRegionNamePeptide()
        {
            return GroupComparisonView.DATAREGION_NAME;
        }

        @Override
        public String getDataRegionNameSmallMolecule()
        {
            return GroupComparisonView.DATAREGION_NAME_SM_MOL;
        }

        @Override
        protected GroupComparisonView createQueryView(
                RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            return new GroupComparisonView(getViewContext(),
                    new TargetedMSSchema(getUser(), getContainer()),
                    (GroupComparisonView.Form) form,
                    forExport, dataRegion);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowCalibrationCurvesAction extends ShowRunSplitDetailsAction<CalibrationCurvesView>
    {
        @Override
        public String getDataRegionNamePeptide()
        {
            return CalibrationCurvesView.DATAREGION_NAME;
        }

        @Override
        public String getDataRegionNameSmallMolecule()
        {
            return CalibrationCurvesView.DATAREGION_NAME_SM_MOL;
        }

        @Override
        protected CalibrationCurvesView createQueryView(
                RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            return new CalibrationCurvesView(getViewContext(),
                    new TargetedMSSchema(getUser(), getContainer()),
                    form,
                    forExport, dataRegion);
        }
    }

    public static class InstrumentForm extends QueryViewAction.QueryExportForm
    {
        private String _serialNumber;

        public String getSerialNumber()
        {
            return _serialNumber;
        }

        public void setSerialNumber(String serialNumber)
        {
            _serialNumber = serialNumber;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowInstrumentAction extends QueryViewAction<InstrumentForm, QueryView>
    {
        public ShowInstrumentAction()
        {
            super(InstrumentForm.class);
        }

        private static final String FOLDER_SUMMARY = "FolderSummary";

        private InstrumentForm _form;

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Instrument " + (_form == null ? "" : _form.getSerialNumber()));
        }

        @Override
        protected QueryView createQueryView(InstrumentForm form, BindException errors, boolean forExport, @Nullable String dataRegion) throws Exception
        {
            if (TargetedMSSchema.TABLE_SAMPLE_FILE.equalsIgnoreCase(dataRegion))
            {
                QuerySettings settings = new QuerySettings(getViewContext(), TargetedMSSchema.TABLE_SAMPLE_FILE, TargetedMSSchema.TABLE_SAMPLE_FILE);
                Sort sort = new Sort();
                sort.appendSortColumn(FieldKey.fromParts("AcquiredTime"), Sort.SortDirection.DESC, false);
                settings.setBaseSort(sort);
                settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("InstrumentSerialNumber"), form.getSerialNumber()));
                settings.setContainerFilterName(ContainerFilter.Type.AllFolders.name());
                TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
                return schema.createView(getViewContext(), settings, errors);
            }
            if (FOLDER_SUMMARY.equalsIgnoreCase(dataRegion))
            {
                QuerySettings settings = new QuerySettings(getViewContext(), FOLDER_SUMMARY, "InstrumentSummaryByFolder");
                settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("InstrumentSerialNumber"), form.getSerialNumber()));
                settings.setContainerFilterName(ContainerFilter.Type.AllFolders.name());
                TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
                return schema.createView(getViewContext(), settings, errors);
            }
            throw new NotFoundException("Unknown dataRegion: " + dataRegion);
        }

        @Override
        public ModelAndView getView(InstrumentForm form, BindException errors) throws Exception
        {
            if (form.getSerialNumber() == null)
            {
                throw new NotFoundException("No instrument serial number specified");
            }
            _form = form;

            QueryView folderSummaryView = createQueryView(form, errors, false, FOLDER_SUMMARY);
            folderSummaryView.setTitle("Data in this Server");
            folderSummaryView.setFrame(WebPartView.FrameType.PORTAL);

            QueryView sampleFileView = createQueryView(form, errors, false, TargetedMSSchema.TABLE_SAMPLE_FILE);
            sampleFileView.setTitle("Data from this Instrument");
            sampleFileView.setFrame(WebPartView.FrameType.PORTAL);

            return new VBox(folderSummaryView, sampleFileView);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowReplicatesAction extends ShowRunSingleDetailsAction<RunDetailsForm>
    {
        private static final String REPLICATE_DATA_REGION_NAME = "Replicate";
        private static final String INSTRUMENT_SUMMARY_DATA_REGION_NAME = "InstrumentSummary";

        public ShowReplicatesAction()
        {
            super(RunDetailsForm.class, "Replicate List", "Replicate");
        }

        @Override
        protected QueryView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            if (dataRegion.equalsIgnoreCase(INSTRUMENT_SUMMARY_DATA_REGION_NAME))
            {
                return new InstrumentSummaryWebPart(getViewContext());
            }
            else
            {
                QuerySettings settings = new QuerySettings(getViewContext(), REPLICATE_DATA_REGION_NAME, TargetedMSSchema.SAMPLE_FILE_RUN_PREFIX + _run.getRunId());
                settings.setBaseSort(new Sort(FieldKey.fromParts("ReplicateId", "Name")));
                TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
                return schema.createView(getViewContext(), settings, errors);
            }
        }

        @Override
        protected ModelAndView getHtmlView(RunDetailsForm form, BindException errors) throws Exception
        {
            VBox vBox = (VBox) super.getHtmlView(form, errors);
            vBox.addView(createQueryView(form, errors, true, INSTRUMENT_SUMMARY_DATA_REGION_NAME), 1);
            return vBox;
        }
    }

    public abstract class ShowRunSingleDetailsAction<FormType extends RunDetailsForm> extends AbstractShowRunDetailsAction<FormType, QueryView>
    {
        protected String _title;
        protected final String _dataRegionName;

        public ShowRunSingleDetailsAction(Class<FormType> formClass, String title, String dataRegionName)
        {
            super(formClass);
            _title = title;
            _dataRegionName = dataRegionName;
        }

        @Override
        protected ModelAndView getHtmlView(FormType form, BindException errors) throws Exception
        {
            WebPartView replicatesView = createInitializedQueryView(form, errors, false, _dataRegionName);
            replicatesView.setFrame(WebPartView.FrameType.PORTAL);
            replicatesView.setTitle(_title);

            VBox vBox = new VBox();
            vBox.addView(getSummaryView(form, _run));
            vBox.addView(replicatesView);
            return vBox;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowListsAction extends ShowRunSingleDetailsAction<RunDetailsForm>
    {
        public ShowListsAction()
        {
            super(RunDetailsForm.class, "Skyline Lists", "List");
        }

        @Override
        protected QueryView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            QuerySettings settings = new QuerySettings(getViewContext(), _dataRegionName, TargetedMSSchema.TABLE_LIST_DEFINITION);
            settings.setBaseSort(new Sort(FieldKey.fromParts("Name")));
            TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
            QueryView view = schema.createView(getViewContext(), settings, errors);
            view.setShowDetailsColumn(false);
            return view;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowPTMReportAction extends ShowRunSingleDetailsAction<RunDetailsForm>
    {
        public ShowPTMReportAction()
        {
            super(RunDetailsForm.class, "PTM Report", "ptmReport");
        }

        @Override
        protected QueryView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            QuerySettings settings = new QuerySettings(getViewContext(), _dataRegionName, "PTMPercents")
            {
                @Override
                protected QueryDefinition createQueryDef(UserSchema schema)
                {
                    QueryDefinition queryDef = super.createQueryDef(schema);

                    String queryName = "PTMPercents" + form.getId();
                    QueryDefinition tempDef = QueryService.get().createQueryDef(getUser(), getContainer(), schema.getSchemaPath(), queryName);
                    tempDef.setIsHidden(true);
                    tempDef.setIsTemporary(true);
                    tempDef.setSql(queryDef.getSql() + " IN (SELECT sf.SampleName FROM targetedms.SampleFile sf WHERE sf.ReplicateId.RunId = " + form.getId() + ")");
                    tempDef.setMetadataXml(queryDef.getMetadataXml());

                    return tempDef;
                }
            };
            // Issue 40731- prevent expensive cross-folder queries when the results will always be scoped to the current
            // run anyway
            settings.setContainerFilterName(null);
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("PeptideGroupId", "RunId"), form.getId()));
            settings.setBaseSort(new Sort("Sequence"));
            TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
            return schema.createView(getViewContext(), settings, errors);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowPeptideMapAction extends ShowRunSingleDetailsAction<RunDetailsForm>
    {
        public ShowPeptideMapAction()
        {
            super(RunDetailsForm.class, "Peptide Map", "PeptideIds");
        }

        @Override
        protected QueryView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            QuerySettings settings = new QuerySettings(getViewContext(), _dataRegionName, "PeptideIds");
            // Issue 40731- prevent expensive cross-folder queries when the results will always be scoped to the current
            // run anyway
            settings.setContainerFilterName(null);
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("PeptideGroupId", "RunId"), form.getId()));
            TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
            return schema.createView(getViewContext(), settings, errors);
        }
    }

    public static class ListDetailsForm extends RunDetailsForm
    {
        private int _listId;

        public int getListId()
        {
            return _listId;
        }

        public void setListId(int listId)
        {
            _listId = listId;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowListContentAction extends ShowRunSingleDetailsAction<ListDetailsForm>
    {
        private ListDefinition _listDef;

        public ShowListContentAction()
        {
            super(ListDetailsForm.class,"Skyline List", "ListContents");
        }

        @Override
        public void validate(ListDetailsForm form, BindException errors)
        {
            _listDef = SkylineListManager.getListDefinition(getContainer(), form.getListId());
            if (_listDef == null)
            {
                throw new NotFoundException("No list found with ID " + form.getListId());
            }
            form.setId(_listDef.getRunId());
            _title = "Skyline List: " + _listDef.getName();
            super.validate(form, errors);
        }

        @Override
        protected QueryView createQueryView(ListDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            QuerySettings settings = new QuerySettings(getViewContext(), _dataRegionName, _listDef.getUserSchemaTableName());
            SkylineListSchema schema = new SkylineListSchema(getUser(), getContainer());
            QueryView view = schema.createView(getViewContext(), settings, errors);
            view.setShowDetailsColumn(false);
            return view;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowSkylineAuditLogAction extends ShowRunSingleDetailsAction<RunDetailsForm>
    {
        private static final String LOG_QUERY_NAME = "AuditLogTraverse";

        public ShowSkylineAuditLogAction()
        {
            super(RunDetailsForm.class,"Skyline Audit Log", "SkylineAuditLog");
        }

        @Override
        protected QueryView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion)
        {
            QuerySettings settings = new QuerySettings(getViewContext(), _dataRegionName, LOG_QUERY_NAME);
            TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("RunId"), form.getId()));
            return schema.createView(getViewContext(), settings, errors);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowSkylineAuditLogExtraInfoAJAXAction extends SimpleViewAction<SkylineAuditLogExtraInfoForm>
    {
        @Override
        public ModelAndView getView(SkylineAuditLogExtraInfoForm form, BindException errors) throws Exception
        {
            SkylineAuditLogExtraInfoBean bean;
            String errMessage = null;
            AuditLogEntry ent = AuditLogEntry.retrieve(form.getEntryId(), getViewContext());
            if(ent == null)
                errMessage = String.format("Entry with id %d does not exist", form.getEntryId());
            if(errMessage != null)
                bean = new SkylineAuditLogExtraInfoBean(null, errMessage);
            else
                bean = new SkylineAuditLogExtraInfoBean(ent, null);


            WebPartView extraInfoView = new JspView<>("/org/labkey/targetedms/view/skylineAuditLogExtraInfoView.jsp", bean);
            getPageConfig().setTemplate(PageConfig.Template.None);
            return extraInfoView;

        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    public static class SkylineAuditLogExtraInfoBean
    {
        private AuditLogEntry _entry;
        private String _error;

        public SkylineAuditLogExtraInfoBean(AuditLogEntry pEntry, String pError){
            _entry = pEntry;
            _error = pError;
        }
        public AuditLogEntry getEntry()
        {
            return _entry;
        }
    }

    public static class SkylineAuditLogExtraInfoForm
    {
        private long _runId;
        private int _entryId;


        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public int getEntryId()
        {
            return _entryId;
        }

        public void setEntryId(int entryId)
        {
            _entryId = entryId;
        }
    }

    public static class RunDetailsForm extends QueryViewAction.QueryExportForm
    {
        private long _id = 0;
        private String _fileName;
        private String _view;
        private boolean _attemptedLookupByName;

        public String getFileName()
        {
            return _fileName;
        }

        public void setFileName(String fileName)
        {
            _fileName = fileName;
        }

        public void setId(long id)
        {
            _id = id;
        }

        public long getId()
        {
            if (_id == 0 && _fileName != null && !_attemptedLookupByName)
            {
                // Support an alternative to the RowId-style parameter to look up by document name. See issue 40377
                _attemptedLookupByName = true;
                TargetedMSRun run = TargetedMSManager.getRunByFileName(_fileName, getContainer());
                if (run != null)
                {
                    _id = run.getId();
                }
            }
            return _id;
        }

        public boolean hasRunId() {
            return getId() > 0;
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
        private int _versionCount;
        private int _calibrationCurveCount;

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

        public int getVersionCount()
        {
            return _versionCount;
        }

        public void setVersionCount(int versions)
        {
            _versionCount = versions;
        }
    }

    @NotNull
    private TargetedMSRun validateRun(long runId)
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

    public static class IdForm
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

    @RequiresPermission(ReadPermission.class)
    public class ShowSampleFileAction extends SimpleViewAction<IdForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private SampleFile _sampleFile;

        @Override
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            _sampleFile = TargetedMSManager.getSampleFile(form.getId(), getContainer());
            if (_sampleFile == null)
            {
                throw new NotFoundException("Could not find SampleFile with ID " + form.getId());
            }
            Replicate replicate = TargetedMSManager.getReplicate(_sampleFile.getReplicateId(), getContainer());
            if (replicate == null)
            {
                throw new NotFoundException("Could not find Replicate with ID " + _sampleFile.getReplicateId());
            }
            _run = TargetedMSManager.getRun(replicate.getRunId());

            VBox result = new VBox();

            // Summary for this sample file
            DOM.Renderable renderable = DOM.TABLE(cl("lk-fields-table"),
                    TR(TD(cl("labkey-form-label"), "Sample File Name"),
                            TD(_sampleFile.getSampleName())),
                    TR(TD(cl("labkey-form-label"), "File Path"),
                            TD(_sampleFile.getFilePath())),
                    TR(TD(cl("labkey-form-label"), "Acquired Time"),
                            TD(DateUtil.formatDateTime(getContainer(), _sampleFile.getAcquiredTime()))),
                    TR(TD(cl("labkey-form-label"), "Modified Time"),
                            TD(DateUtil.formatDateTime(getContainer(), _sampleFile.getModifiedTime()))),
                    TR(TD(cl("labkey-form-label"), "Skyline ID"),
                            TD(_sampleFile.getSkylineId())),
                    TR(TD(cl("labkey-form-label"), "Instrument Serial Number"),
                            TD(_sampleFile.getInstrumentSerialNumber())),
                    TR(TD(cl("labkey-form-label"), "Replicate Name"),
                            TD(replicate.getName()))
            );

            HtmlView summaryView = new HtmlView(renderable);
            summaryView.setFrame(WebPartView.FrameType.PORTAL);
            summaryView.setTitle("Sample File Summary");
            result.addView(summaryView);

            List<SampleFileChromInfo> sampleFileChromInfos = TargetedMSManager.getSampleFileChromInfos(_sampleFile);

            for (SampleFileChromInfo sampleFileChromInfo : sampleFileChromInfos)
            {
                ActionURL chromURL = new ActionURL(SampleFileChromatogramChartAction.class, getContainer());
                chromURL.addParameter("id", sampleFileChromInfo.getId());
                SampleFileChromInfoForm imgForm = new SampleFileChromInfoForm();
                HtmlView chromView = new HtmlView(DOM.IMG(at(src, chromURL.toString(), height, imgForm.getChartHeight(), width, imgForm.getChartWidth())));
                chromView.setFrame(WebPartView.FrameType.PORTAL);
                chromView.setTitle(sampleFileChromInfo.getTextId());
                result.addView(chromView);
            }

            return result;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
            root.addChild(_run.getDescription(), getShowRunURL(getContainer(), _run.getId()));
            root.addChild(_sampleFile.getSampleName());
        }
    }

    public static class SampleFileChromInfoForm extends AbstractChartForm
    {
        private int _id;

        public SampleFileChromInfoForm()
        {
            setChartWidth(1200);
            setChartHeight(300);
        }

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class SampleFileChromatogramChartAction extends ExportAction<SampleFileChromInfoForm>
    {
        @Override
        public void export(SampleFileChromInfoForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            SampleFileChromInfo chromInfo = TargetedMSManager.getSampleFileChromInfo(form.getId(), getContainer());
            if (chromInfo == null)
            {
                throw new NotFoundException("No SampleFileChromInfo found in this folder for id: " + form.getId());
            }

            ChromatogramChartMakerFactory factory = new ChromatogramChartMakerFactory();
            JFreeChart chart = factory.createSampleFileChromChart(chromInfo, getUser(), getContainer());

            writeChart(form, response, chart);
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a protein detail page
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ShowProteinAction extends SimpleViewAction<ProteinDetailForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private String _proteinLabel;

        @Override
        public ModelAndView getView(final ProteinDetailForm form, BindException errors)
        {
            PeptideGroup group = PeptideGroupManager.getPeptideGroup(getContainer(), form.getId());
            if (group == null)
            {
                throw new NotFoundException("Could not find protein #" + form.getId());
            }

            _run = TargetedMSManager.getRun(group.getRunId());
            _proteinLabel = group.getLabel();

            Integer moleculeCount = TargetedMSManager.getPeptideGroupMoleculeCount(_run, group.getId());
            // Peptide group details
            VBox result = new VBox();

            TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
            Integer peptideCount = addProteinSummaryViews(result, group, _run, getUser(), getContainer());

            // List of peptides
            if (peptideCount != null && peptideCount > 0)
            {
                List<FieldKey> baseVisibleColumns = new ArrayList<>();
                baseVisibleColumns.add(FieldKey.fromParts(ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
                baseVisibleColumns.add(FieldKey.fromParts("CalcNeutralMass"));
                baseVisibleColumns.add(FieldKey.fromParts("NumMissedCleavages"));
                result.addView(getGeneralMoleculeQueryView(form, schema, errors, "Peptides", "Peptide", baseVisibleColumns));
            }

            // List of small molecules
            if (moleculeCount != null && moleculeCount > 0)
            {
                List<FieldKey> baseVisibleColumns = new ArrayList<>();
                baseVisibleColumns.add(FieldKey.fromParts("Molecule"));
                baseVisibleColumns.add(FieldKey.fromParts("CustomIonName"));
                baseVisibleColumns.add(FieldKey.fromParts("IonFormula"));
                baseVisibleColumns.add(FieldKey.fromParts("MassAverage"));
                baseVisibleColumns.add(FieldKey.fromParts("MassMonoisotopic"));
                result.addView(getGeneralMoleculeQueryView(form, schema, errors, "Small Molecules", "Molecule", baseVisibleColumns));
            }

            SummaryChartBean summaryChartBean = new SummaryChartBean(_run);
            summaryChartBean.setPeptideGroupId(form.getId());
            summaryChartBean.setReplicateList(ReplicateManager.getReplicatesForRun(group.getRunId()));
            summaryChartBean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(group.getRunId()));
            summaryChartBean.setReplicateAnnotationValueList(ReplicateManager.getUniqueSortedAnnotationNameValue(group.getRunId()));
            int count = 0;
            // Peptide summary charts
            if (peptideCount != null && peptideCount > 0)
            {
                count += peptideCount.intValue();
                summaryChartBean.setPeptideList(new ArrayList<>(PeptideManager.getPeptidesForGroup(group.getId(), new TargetedMSSchema(getUser(), getContainer()))));
            }
            // Molecule summary charts
            else if (moleculeCount != null && moleculeCount > 0)
            {
                count += moleculeCount.intValue();
                summaryChartBean.setMoleculeList(new ArrayList<>(MoleculeManager.getMoleculesForGroup(group.getId())));
            }
            summaryChartBean.setInitialWidth(Math.max(600, 100 + count * 30));
            JspView<SummaryChartBean> summaryChartView = new JspView<>("/org/labkey/targetedms/view/summaryChartsView.jsp", summaryChartBean);
            summaryChartView.setTitle("Summary Charts");
            summaryChartView.enableExpandCollapse("SummaryChartsView", false);
            result.addView(summaryChartView);

            return result;
        }

        private QueryView getGeneralMoleculeQueryView(ProteinDetailForm form, TargetedMSSchema schema, BindException errors,
                                                      String title, String queryName, List<FieldKey> baseVisibleColumns)
        {
            String dataRegionName = title.replaceAll(" ", "");
            QuerySettings settings = new QuerySettings(getViewContext(), dataRegionName, queryName);
            settings.getBaseFilter().addAllClauses(new SimpleFilter(FieldKey.fromParts("PeptideGroupId"), form.getId()));
            QueryView view = new QueryView(schema, settings, errors);

            if (null == view.getCustomView())
            {
                baseVisibleColumns.add(FieldKey.fromParts("AvgMeasuredRetentionTime"));
                baseVisibleColumns.add(FieldKey.fromParts("PredictedRetentionTime"));
                baseVisibleColumns.add(FieldKey.fromParts("RtCalculatorScore"));
                settings.setFieldKeys(baseVisibleColumns);
            }
            view.setTitle(title);
            view.enableExpandCollapse("TargetedMS" + dataRegionName, false);
            view.setUseQueryViewActionExportURLs(true);
            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            new ShowPrecursorListAction(getViewContext()).addNavTrail(root, _run);
            if (_proteinLabel != null)
            {
                root.addChild(_proteinLabel);
            }
        }
    }

    public static Integer addProteinSummaryViews(VBox box, PeptideGroup group, TargetedMSRun run, User user, Container container)
    {
        Integer peptideCount = TargetedMSManager.getPeptideGroupPeptideCount(run, group.getId());
        boolean proteomics = peptideCount != null && peptideCount.intValue() > 0;

        JspView<PeptideGroup> detailsView = new JspView<>("/org/labkey/targetedms/view/moleculeListView.jsp", group);
        detailsView.setFrame(WebPartView.FrameType.PORTAL);
        detailsView.setTitle(proteomics ? "Protein" : "Molecule List");

        box.addView(detailsView);

        if (group.getSequenceId() != null)
        {
            int seqId = group.getSequenceId().intValue();
            List<String> peptideSequences = new ArrayList<>();
            for (Peptide peptide : PeptideManager.getPeptidesForGroup(group.getId(), new TargetedMSSchema(user, container)))
            {
                peptideSequences.add(peptide.getSequence());
            }

            ProteinService proteinService = ProteinService.get();
            WebPartView<?> sequenceView = proteinService.getProteinCoverageView(seqId, peptideSequences.toArray(new String[0]), 100, true, group.getAccession());

            sequenceView.setTitle("Sequence Coverage");
            sequenceView.enableExpandCollapse("SequenceCoverage", false);
            box.addView(sequenceView);

            List<IKeyword> keywords = TargetedMSManager.getKeywords(seqId);

            Map<String, Collection<HtmlString>> extraAnnotations = new LinkedHashMap<>();

            for (IKeyword keyword : keywords)
            {
                HtmlString link = new Link.LinkBuilder(keyword.label).href("https://www.uniprot.org/keywords/" + keyword.id).target("_blank").clearClasses().getHtmlString();
                if (IKeyword.BIOLOGICAL_PROCESS_CATEGORY.equals(keyword.categoryId))
                    extraAnnotations.computeIfAbsent("Biological Processes", k -> new ArrayList<>()).add(link);
                if (IKeyword.MOLECULAR_FUNCTION_CATEGORY.equals(keyword.categoryId))
                    extraAnnotations.computeIfAbsent("Molecular Functions", k -> new ArrayList<>()).add(link);
            }

            box.addView(proteinService.getAnnotationsView(seqId, extraAnnotations));
        }

        return peptideCount;
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

    public static class SummaryChartBean
    {
        private boolean _showControls = true;
        private int _initialWidth = 600;
        private int _initialHeight = 400;

        private long _peptideGroupId;
        private List<Replicate> _replicateList;
        private List<String> _replicateAnnotationNameList;
        private List<ReplicateAnnotation> _replicateAnnotationValueList;

        // fields for proteomics
        private long _peptideId;
        private long _precursorId;
        private List<Peptide> _peptideList;

        // fields for small molecule
        private long _moleculeId;
        private long _moleculePrecursorId;
        private List<Molecule> _moleculeList;

        private final TargetedMSRun _run;

        private SummaryChartBean(TargetedMSRun run)
        {
            _run = run;
        }

        public boolean isShowControls()
        {
            return _showControls;
        }

        public void setShowControls(boolean showControls)
        {
            _showControls = showControls;
        }

        public int getInitialWidth()
        {
            return _initialWidth;
        }

        public void setInitialWidth(int initialWidth)
        {
            _initialWidth = initialWidth;
        }

        public int getInitialHeight()
        {
            return _initialHeight;
        }

        public void setInitialHeight(int initialHeight)
        {
            _initialHeight = initialHeight;
        }

        public long getPeptideGroupId()
        {
            return _peptideGroupId;
        }

        public void setPeptideGroupId(long peptideGroupId)
        {
            _peptideGroupId = peptideGroupId;
        }

        public long getPeptideId()
        {
            return _peptideId;
        }

        public void setPeptideId(long peptideId)
        {
            _peptideId = peptideId;
        }

        public long getPrecursorId()
        {
            return _precursorId;
        }

        public void setPrecursorId(long precursorId)
        {
            _precursorId = precursorId;
        }

        public long getMoleculeId()
        {
            return _moleculeId;
        }

        public void setMoleculeId(long moleculeId)
        {
            _moleculeId = moleculeId;
        }

        public long getMoleculePrecursorId()
        {
            return _moleculePrecursorId;
        }

        public void setMoleculePrecursorId(long moleculePrecursorId)
        {
            _moleculePrecursorId = moleculePrecursorId;
        }

        public List<Replicate> getReplicateList()
        {
            return _replicateList != null ? _replicateList : Collections.emptyList();
        }

        public void setReplicateList(List<Replicate> replicateList)
        {
            _replicateList = replicateList;
        }

        public List<String> getReplicateAnnotationNameList()
        {
            return _replicateAnnotationNameList != null ? _replicateAnnotationNameList : Collections.emptyList();
        }

        public void setReplicateAnnotationNameList(List<String> replicateAnnotationNameList)
        {
            _replicateAnnotationNameList = replicateAnnotationNameList;
        }

        public List<ReplicateAnnotation> getReplicateAnnotationValueList()
        {
            return _replicateAnnotationValueList != null ? _replicateAnnotationValueList : Collections.emptyList();
        }

        public void setReplicateAnnotationValueList(List<ReplicateAnnotation> replicateAnnotationValueList)
        {
            _replicateAnnotationValueList = replicateAnnotationValueList;
        }

        public List<Peptide> getPeptideList()
        {
            return _peptideList != null ? _peptideList : Collections.emptyList();
        }

        public void setPeptideList(List<Peptide> peptideList)
        {
            _peptideList = peptideList;
        }

        public List<Molecule> getMoleculeList()
        {
            return _moleculeList != null ? _moleculeList : Collections.emptyList();
        }

        public void setMoleculeList(List<Molecule> moleculeList)
        {
            _moleculeList = moleculeList;
        }

        public TargetedMSRun getRun()
        {
            return _run;
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a protein detail page
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
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

            WebPartView<?> view;
            if (group.getSequenceId() != null)
            {
                int seqId = group.getSequenceId().intValue();
                List<String> peptideSequences = new ArrayList<>();
                for (Peptide peptide : PeptideManager.getPeptidesForGroup(group.getId(), new TargetedMSSchema(getUser(), getContainer())))
                {
                    peptideSequences.add(peptide.getSequence());
                }
                ProteinService proteinService = ProteinService.get();
                ActionURL searchURL = urlProvider(MS2Urls.class).getProteinSearchUrl(getContainer());
                searchURL.addParameter("seqId", group.getSequenceId().intValue());
                searchURL.addParameter("identifier", group.getLabel());
                getViewContext().getResponse().getWriter().write("<a href=\"" + searchURL + "\">Search for other references to this protein</a><br/>");
                view = proteinService.getProteinCoverageView(seqId, peptideSequences.toArray(new String[peptideSequences.size()]), 40, true, null);
            }
            else
            {
                view = new HtmlView(HtmlString.of("No sequence details available: " + group.getLabel()));
            }
            view.render(getViewContext().getRequest(), getViewContext().getResponse());

            getPageConfig().setTemplate(PageConfig.Template.None);
            return null;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    // ------------------------------------------------------------------------
    // Action to show representative data conflicts, if any, in a container
    // ------------------------------------------------------------------------
    @RequiresPermission(InsertPermission.class)
    public class ShowProteinConflictUiAction extends SimpleViewAction<ConflictUIForm>
    {
        @Override
        public ModelAndView getView(ConflictUIForm form, BindException errors)
        {
            List<ConflictProtein> conflictProteinList = ConflictResultsManager.getConflictedProteins(getContainer());
            if(conflictProteinList.size() == 0)
            {
                errors.reject(ERROR_MSG, "Library folder "+getContainer().getPath()+" does not contain any conflicting proteins.");
                return new SimpleErrorView(errors, true);
            }

            // If the list contains the same conflicted proteins from multiple files return the ones from the
            // oldest run first.  Or, use the runId from the form if we are given one.
            int conflictRunId = form.getConflictedRunId();
            boolean useMin = false;
            if(conflictRunId == 0)
            {
                conflictRunId = Integer.MAX_VALUE;
                useMin = true;
            }
            String conflictRunFileName = null;
            Map<String, Integer> conflictRunFiles = new HashMap<>();
            for(ConflictProtein cProtein: conflictProteinList)
            {
                if(useMin && (cProtein.getNewProteinRunId() < conflictRunId))
                {
                    conflictRunId = cProtein.getNewProteinRunId();
                    conflictRunFileName = cProtein.getNewRunFile();
                }
                else if(!useMin && (conflictRunId == cProtein.getNewProteinRunId()))
                {
                    conflictRunFileName = cProtein.getNewRunFile();
                }
                conflictRunFiles.put(cProtein.getNewRunFile(), cProtein.getNewProteinRunId());
            }

            //ensure that the run is valid and exists within the current container
            validateRun(conflictRunId);

            if(conflictRunFileName == null)
            {
                throw new NotFoundException("Run with ID "+conflictRunId+" does not have any protein conflicts.");
            }

            List<ConflictProtein> singleRunConflictProteins = new ArrayList<>();
            for(ConflictProtein cProtein: conflictProteinList)
            {
                if(cProtein.getNewProteinRunId() != conflictRunId)
                    continue;
                singleRunConflictProteins.add(cProtein);
            }

            ProteinConflictBean bean = new ProteinConflictBean();
            bean.setCurrentConflictRunFile(conflictRunFileName);
            bean.setConflictProteinList(singleRunConflictProteins);
            if(conflictRunFiles.size() > 1)
            {
                bean.setAllConflictRunFiles(conflictRunFiles);
            }

            JspView<ProteinConflictBean> conflictInfo = new JspView<>("/org/labkey/targetedms/view/proteinConflictResolutionView.jsp", bean);
            conflictInfo.setFrame(WebPartView.FrameType.PORTAL);
            conflictInfo.setTitle("Library Protein Conflicts");

            return conflictInfo;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Protein Conflicts");
        }
    }

    public static class ConflictUIForm
    {
        private int _conflictedRunId;

        public int getConflictedRunId()
        {
            return _conflictedRunId;
        }

        public void setConflictedRunId(int conflictedRunId)
        {
            _conflictedRunId = conflictedRunId;
        }
    }

    public static class ProteinConflictBean
    {
        private List<ConflictProtein> _conflictProteinList;
        private Map<String, Integer> _allConflictRunFiles;
        private String _conflictRunFileName;

        public List<ConflictProtein> getConflictProteinList()
        {
            return _conflictProteinList;
        }

        public void setConflictProteinList(List<ConflictProtein> conflictProteinList)
        {
            _conflictProteinList = conflictProteinList;
        }

        public Map<String, Integer> getAllConflictRunFiles()
        {
            return _allConflictRunFiles;
        }

        public void setAllConflictRunFiles(Map<String, Integer> allConflictRunFiles)
        {
            _allConflictRunFiles = allConflictRunFiles;
        }

        public void setCurrentConflictRunFile(String conflictRunFileName)
        {
            _conflictRunFileName = conflictRunFileName;
        }

        public String getConflictRunFileName()
        {
            return _conflictRunFileName;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class ProteinConflictPeptidesAjaxAction extends ReadOnlyApiAction<ProteinPeptidesForm>
    {
        @Override
        public ApiResponse execute(ProteinPeptidesForm proteinPeptidesForm, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            int newProteinId = proteinPeptidesForm.getNewProteinId();
            if(PeptideGroupManager.getPeptideGroup(getContainer(), newProteinId) == null)
            {
                throw new NotFoundException("PeptideGroup with ID "+newProteinId+" was not found in the container.");
            }
            int oldProteinId = proteinPeptidesForm.getOldProteinId();
            if(PeptideGroupManager.getPeptideGroup(getContainer(), oldProteinId) == null)
            {
                throw new NotFoundException("PeptideGroup with ID "+oldProteinId+" was not found in the container.");
            }

            List<ConflictPeptide> conflictPeptides = ConflictResultsManager.getConflictPeptidesForProteins(newProteinId, oldProteinId, getUser(), getContainer());
            // Sort them by ascending peptide ranks in the new protein
            conflictPeptides.sort(Comparator.comparingInt(ConflictPeptide::getNewPeptideRank));
            List<Map<String, Object>> conflictPeptidesMap = new ArrayList<>();
            for(ConflictPeptide peptide: conflictPeptides)
            {
                Map<String, Object> map = new HashMap<>();
                // PrecursorHtmlMaker.getHtml(peptide.getNewPeptide(), peptide.getNewPeptidePrecursor(), )
                String newPepSequence = peptide.getNewPeptide() != null ? peptide.getNewPeptide().getPeptideModifiedSequence() : "-";

                map.put("newPeptide", newPepSequence);
                String newPepRank = peptide.getNewPeptide() != null ? String.valueOf(peptide.getNewPeptideRank()) : "-";
                map.put("newPeptideRank", newPepRank);
                String oldPepSequence = peptide.getOldPeptide() != null ? peptide.getOldPeptide().getPeptideModifiedSequence() : "-";

                map.put("oldPeptide", oldPepSequence);
                String oldPepRank = peptide.getOldPeptide() != null ? String.valueOf(peptide.getOldPeptideRank()) : "-";
                map.put("oldPeptideRank",oldPepRank);
                conflictPeptidesMap.add(map);
            }

            response.put("conflictPeptides", conflictPeptidesMap);
            return response;
        }
    }

    public static class ProteinPeptidesForm
    {
        private int _newProteinId;
        private int _oldProteinId;

        public int getNewProteinId()
        {
            return _newProteinId;
        }

        public void setNewProteinId(int newProteinId)
        {
            _newProteinId = newProteinId;
        }

        public int getOldProteinId()
        {
            return _oldProteinId;
        }

        public void setOldProteinId(int oldProteinId)
        {
            _oldProteinId = oldProteinId;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class ShowPrecursorConflictUiAction extends SimpleViewAction<ConflictUIForm>
    {
        @Override
        public ModelAndView getView(ConflictUIForm form, BindException errors)
        {
            List<ConflictPrecursor> conflictPrecursorList = ConflictResultsManager.getConflictedPrecursors(getContainer());
            if(conflictPrecursorList.size() == 0)
            {
                errors.reject(ERROR_MSG, "Library folder "+getContainer().getPath()+" does not contain any conflicting data.");
                return new SimpleErrorView(errors, true);
            }

            // If the list contains the same conflicted precursors from multiple files return the ones from the
            // oldest run first.  Or, use the runId from the form if we are given one.
            int conflictRunId = form.getConflictedRunId();
            boolean useMin = false;
            if(conflictRunId == 0)
            {
                conflictRunId = Integer.MAX_VALUE;
                useMin = true;
            }

            String conflictRunFileName = null;
            Map<String, Integer> conflictRunFiles = new HashMap<>();
            for(ConflictPrecursor cPrecursor: conflictPrecursorList)
            {
                if(useMin && cPrecursor.getNewPrecursorRunId() < conflictRunId)
                {
                    conflictRunId = cPrecursor.getNewPrecursorRunId();
                    conflictRunFileName = cPrecursor.getNewRunFile();
                }
                else if(!useMin && cPrecursor.getNewPrecursorRunId() == conflictRunId)
                {
                    conflictRunFileName = cPrecursor.getNewRunFile();
                }
                conflictRunFiles.put(cPrecursor.getNewRunFile(), cPrecursor.getNewPrecursorRunId());
            }

            //ensure that the run is valid and exists within the current container
            validateRun(conflictRunId);

            if(conflictRunFileName == null)
            {
                throw new NotFoundException("Run with ID "+conflictRunId+" does not have any peptide conflicts.");
            }

            List<ConflictPrecursor> singleRunConflictPrecursors = new ArrayList<>();
            for(ConflictPrecursor cPrecursor: conflictPrecursorList)
            {
                if(cPrecursor.getNewPrecursorRunId() != conflictRunId)
                    continue;
                singleRunConflictPrecursors.add(cPrecursor);
            }

            PrecursorConflictBean bean = new PrecursorConflictBean();
            bean.setConflictRunFileName(conflictRunFileName);
            bean.setConflictPrecursorList(singleRunConflictPrecursors);
            if(conflictRunFiles.size() > 1)
            {
                bean.setAllConflictRunFiles(conflictRunFiles);
            }

            JspView<PrecursorConflictBean> conflictInfo = new JspView<>("/org/labkey/targetedms/view/precursorConflictResolutionView.jsp", bean);
            conflictInfo.setFrame(WebPartView.FrameType.PORTAL);
            conflictInfo.setTitle("Library Conflicts");

            return conflictInfo;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Precursor Conflicts");
        }
    }

    public static class PrecursorConflictBean
    {
        private List<ConflictPrecursor> _conflictPrecursorList;
        private Map<String, Integer> _allConflictRunFiles;
        private String _conflictRunFileName;

        public List<ConflictPrecursor> getConflictPrecursorList()
        {
            return _conflictPrecursorList;
        }

        public void setConflictPrecursorList(List<ConflictPrecursor> conflictPrecursorList)
        {
            _conflictPrecursorList = conflictPrecursorList;
        }

        public Map<String, Integer> getAllConflictRunFiles()
        {
            return _allConflictRunFiles;
        }

        public void setAllConflictRunFiles(Map<String, Integer> allConflictRunFiles)
        {
            _allConflictRunFiles = allConflictRunFiles;
        }

        public String getConflictRunFileName()
        {
            return _conflictRunFileName;
        }

        public void setConflictRunFileName(String conflictRunFileName)
        {
            _conflictRunFileName = conflictRunFileName;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class PrecursorConflictTransitionsAjaxAction extends ReadOnlyApiAction<ConflictPrecursorsForm>
    {
        @Override
        public ApiResponse execute(ConflictPrecursorsForm conflictPrecursorsForm, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            int newPrecursorId = conflictPrecursorsForm.getNewPrecursorId();
            if(PrecursorManager.getPrecursor(getContainer(), newPrecursorId, getUser()) == null &&
               MoleculePrecursorManager.getPrecursor(getContainer(), newPrecursorId, getUser()) == null)
            {
                throw new NotFoundException("Precursor with ID "+newPrecursorId+" was not found in the container.");
            }
            int oldPrecursorId = conflictPrecursorsForm.getOldPrecursorId();
            if(PrecursorManager.getPrecursor(getContainer(), oldPrecursorId, getUser()) == null &&
                MoleculePrecursorManager.getPrecursor(getContainer(), oldPrecursorId, getUser()) == null)
            {
                throw new NotFoundException("Precursor with ID "+oldPrecursorId+" was not found in the container.");
            }

            List<ConflictTransition> conflictTransitions = ConflictResultsManager.getConflictTransitionsForPrecursors(newPrecursorId, oldPrecursorId, getUser(), getContainer());
            // Sort them by ascending transitions ranks in the new precursor
            conflictTransitions.sort(Comparator.comparingInt(ConflictTransition::getNewTransitionRank));
            List<Map<String, Object>> conflictTransitionsMap = new ArrayList<>();
            for(ConflictTransition transition: conflictTransitions)
            {
                Map<String, Object> map = new HashMap<>();
                String newTransitionLabel = transition.getNewTransition() != null ? transition.getNewTransition().toString() : "-";
                map.put("newTransition", newTransitionLabel);
                String newTransRank = transition.getNewTransition() != null ? String.valueOf(transition.getNewTransitionRank()) : "-";
                map.put("newTransitionRank", newTransRank);
                String oldTransLabel = transition.getOldTransition() != null ? transition.getOldTransition().toString() : "-";
                map.put("oldTransition", oldTransLabel);
                String oldPepRank = transition.getOldTransition() != null ? String.valueOf(transition.getOldTransitionRank()) : "-";
                map.put("oldTransitionRank",oldPepRank);
                conflictTransitionsMap.add(map);
            }

            response.put("conflictTransitions", conflictTransitionsMap);
            return response;
        }
    }

    public static class ConflictPrecursorsForm
    {
        private int _newPrecursorId;
        private int _oldPrecursorId;

        public int getNewPrecursorId()
        {
            return _newPrecursorId;
        }

        public void setNewPrecursorId(int newPrecursorId)
        {
            _newPrecursorId = newPrecursorId;
        }

        public int getOldPrecursorId()
        {
            return _oldPrecursorId;
        }

        public void setOldPrecursorId(int oldPrecursorId)
        {
            _oldPrecursorId = oldPrecursorId;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class ResolveConflictAction extends FormHandlerAction<ResolveConflictForm>
    {
        @Override
        public void validateCommand(ResolveConflictForm target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(ResolveConflictForm resolveConflictForm, BindException errors) throws Exception
        {
            if (resolveConflictForm.getConflictLevel() == null)
            {
                errors.reject(ERROR_MSG, "Missing 'conflictLevel' parameter.");
                return false;
            }

            boolean resolveProtein = resolveConflictForm.getConflictLevel().equalsIgnoreCase("protein");
            boolean resolvePrecursor = resolveConflictForm.getConflictLevel().equalsIgnoreCase("peptide");

            if (!resolveProtein && !resolvePrecursor)
            {
                errors.reject(ERROR_MSG, resolveConflictForm.getConflictLevel() + " is an invalid value for 'conflictLevel' parameter."+
                        " Valid values are 'peptide' or 'protein'.");

                return false;
            }

            List<Long> selectedIds = resolveConflictForm.getSelectedIds();
            List<Long> deselectIds = resolveConflictForm.getDeselectedIds();

            if (selectedIds == null || selectedIds.isEmpty())
            {
                errors.reject(ERROR_MSG, "No IDs were found to be marked as representative.");
                return false;
            }

            if (deselectIds == null || deselectIds.isEmpty())
            {
                errors.reject(ERROR_MSG, "No IDs were found to be marked as deprecated.");
                return false;
            }

            // ensure that the peptide-group or precursor Ids belong to a run in the container
            if (resolveProtein)
            {
                if (!PeptideGroupManager.ensureContainerMembership(selectedIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the selected peptideGroupIds were not found in the container.");
                }
                if (!PeptideGroupManager.ensureContainerMembership(deselectIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the deselected peptideGroupIds were not found in the container.");
                }
            }

            if (resolvePrecursor)
            {
                if (!PrecursorManager.ensureContainerMembership(selectedIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the selected precursorIds were not found in the container.");
                }
                if (!PrecursorManager.ensureContainerMembership(deselectIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the deselected precursorIds were not found in the container.");
                }
            }

            try (DbScope.Transaction transaction = TargetedMSManager.getSchema().getScope().ensureTransaction())
            {
                if (resolveProtein)
                {
                    // Set RepresentativeDataState to Representative.
                    PeptideGroupManager.updateRepresentativeStatus(selectedIds, RepresentativeDataState.Representative);

                    // Set to either NotRepresentative or Representative_Deprecated.
                    // If the original status was Representative it will be updated to Representative_Deprecated.
                    // If the original status was Conflicted it will be update to NotRepresentative.
                    PeptideGroupManager.updateStatusToDeprecatedOrNotRepresentative(deselectIds);

                    // If there are runs in the container that no longer have any representative data mark
                    // them as being not representative.
                    TargetedMSManager.markRunsNotRepresentative(getContainer(), TargetedMSRun.RepresentativeDataState.Representative_Protein);
                }
                else
                {
                    // Set RepresentativeDataState to Representative.
                    PrecursorManager.updateRepresentativeStatus(selectedIds, RepresentativeDataState.Representative);

                    // Set to either NotRepresentative or Representative_Deprecated.
                    // If the original status was Representative it will be updated to Representative_Deprecated.
                    // If the original status was Conflicted it will be update to NotRepresentative.
                    PrecursorManager.updateStatusToDeprecatedOrNotRepresentative(deselectIds);

                    // If there are runs in the container that no longer have any representative data mark
                    // them as being not representative.
                    TargetedMSManager.markRunsNotRepresentative(getContainer(), TargetedMSRun.RepresentativeDataState.Representative_Peptide);
                }

                // Increment the chromatogram library revision number for this container.
                PipeRoot root = PipelineService.get().getPipelineRootSetting(getContainer());
                if (null != root)
                {
                    LocalDirectory localDirectory = LocalDirectory.create(root, MODULE_NAME);
                    try
                    {
                        ChromatogramLibraryUtils.incrementLibraryRevision(getContainer(), getUser(), localDirectory);
                    }
                    finally
                    {
                        localDirectory.cleanUpLocalDirectory();
                    }
                }
                else
                {
                    throw new ValidationException("Pipeline root not found.");      // TODO: set errors?
                }

                // Add event to audit log.
                TargetedMsRepresentativeStateAuditProvider.addAuditEntry(getContainer(), getUser(), "Conflict resolved.");
                transaction.commit();
            }
            return true;
        }

        @Override
        public URLHelper getSuccessURL(ResolveConflictForm resolveConflictForm)
        {
            return getContainer().getStartURL(getUser());
        }
    }

    public static class ResolveConflictForm
    {
        public String _conflictLevel; // Either 'peptide' or 'protein'
        public String _selectedInputValues;
        private List<Long> _selectedIds;
        private List<Long> _deselectedIds;

        public String getConflictLevel()
        {
            return _conflictLevel;
        }

        public void setConflictLevel(String conflictLevel)
        {
            _conflictLevel = conflictLevel;
        }

        public String getSelectedInputValues()
        {
            return _selectedInputValues;
        }

        public void setSelectedInputValues(String selectedInputValues)
        {
            _selectedInputValues = selectedInputValues;
            if(!StringUtils.isBlank(selectedInputValues))
            {
                String[] vals = selectedInputValues.split(",");
                _selectedIds = new ArrayList<>(vals.length);
                _deselectedIds = new ArrayList<>(vals.length);

                for(String value: vals)
                {
                    int idx = value.indexOf('_');
                    if(idx != -1)
                    {
                        long selected = Long.parseLong(value.substring(0, idx));
                        long deselected = Long.parseLong(value.substring(idx+1));
                        _selectedIds.add(selected);
                        _deselectedIds.add(deselected);
                    }
                }
            }
        }

        public List<Long> getSelectedIds()
        {
            return _selectedIds;
        }

        public List<Long> getDeselectedIds()
        {
            return _deselectedIds;
        }
    }

    private static String getDocumentSize(TargetedMSRun run)
    {
        if (run.getDocumentSize() != null)
        {
            return FileUtils.byteCountToDisplaySize(run.getDocumentSize());
        }
        else
        {
            Path skyDocFile = SkylineFileUtils.getSkylineFile(run.getExperimentRunLSID(), run.getContainer());
            if (skyDocFile != null && !Files.isDirectory(skyDocFile))
            {
                try
                {
                    return FileUtils.byteCountToDisplaySize(Files.size(skyDocFile));
                }
                catch (IOException ignored) {}
            }
        }
        return "Unknown size";
    }

    public static ClientDependency getDownloadMenuClientDependency()
    {
        return ClientDependency.fromPath("/TargetedMS/js/clipboardHelper.js");
    }

    /**
     * Has a dependency on JavaScript utility to help with copying to clipboard, which is up to the caller to add to
     *  the page via a call to getDownloadMenuClientDependency() */
    public static PopupMenu createDownloadMenu(@NotNull TargetedMSRun run)
    {
        String onClickScript = null;
        if (!StringUtils.isBlank(AnalyticsService.getTrackingScript()))
        {
            // http://www.blastam.com/blog/how-to-track-downloads-in-google-analytics
            // Tell the browser to wait 400ms before going to the download.  This is to ensure
            // that the GA tracking request goes through. Some browsers will interrupt the tracking
            // request if the download opens on the same page.
            String timeout = "setTimeout(function(){location.href=that.href;},400);return false;";

            onClickScript = "if(_gaq) {that=this; _gaq.push(['_trackEvent', 'SkyDocDownload', '" + run.getContainer().getPath() + "', '" + run.getFileName() + "']); " + timeout + "}";
        }

        String documentSize = getDocumentSize(run);

        NavTree navTree = new NavTree(documentSize, null, null, "fa fa-download");
        navTree.setTip("Download");

        final ActionURL fullDownloadUrl = new ActionURL(TargetedMSController.DownloadDocumentAction.class, run.getContainer()).replaceParameter("id", Long.toString(run.getId()));

        ActionURL pointerDownloadUrl = fullDownloadUrl.clone().replaceParameter("view", "skyp");

        NavTree pointerDownloadNavTree = new NavTree("SkyP file (link only, 1KB)", pointerDownloadUrl);
        pointerDownloadNavTree.setScript(onClickScript);
        navTree.addChild(pointerDownloadNavTree);

        navTree.addSeparator();

        NavTree fullDownloadNavTree = new NavTree("Full Skyline file", fullDownloadUrl);
        fullDownloadNavTree.setScript(onClickScript);
        navTree.addChild(fullDownloadNavTree);

        ActionURL runURL = new ActionURL(ShowPrecursorListAction.class, run.getContainer()).addParameter("fileName", run.getFileName());
        if (AppProps.getInstance().getUseContainerRelativeURL())
        {
            NavTree relativeURLNavTree = new NavTree("Copy relative URL to clipboard", runURL.toContainerRelativeURL());
            relativeURLNavTree.setScript("copyStringToClipboard(" + PageFlowUtil.jsString(runURL.toContainerRelativeURL()) + ");return false;");
            relativeURLNavTree.setTip("Relative URLs are good for using in wiki pages in this folder, especially for folders that may later move or be copied");
            navTree.addChild(relativeURLNavTree);
        }

        NavTree fullURL = new NavTree("Copy full URL to clipboard", runURL.getURIString());
        fullURL.setScript("copyStringToClipboard(" + PageFlowUtil.jsString(runURL.getURIString()) + ");return false;");
        fullURL.setTip("Full URLs are good for using in emails and publications");
        navTree.addChild(fullURL);

        PopupMenu menu = new PopupMenu(navTree);
        menu.setButtonStyle(org.labkey.api.view.PopupMenu.ButtonStyle.IMAGE_AND_TEXT);
        return menu;
    }

    // ------------------------------------------------------------------------
    // Action to download a Skyline zip file.
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class DownloadDocumentAction extends SimpleViewAction<RunDetailsForm>
    {
        @Override
        public ModelAndView getView(RunDetailsForm form, BindException errors) throws Exception
        {
            if (form.getId() < 0)
            {
                throw new NotFoundException("No run ID specified.");
            }
            TargetedMSRun run = validateRun(form.getId());
            ExpData data = ExperimentService.get().getExpData(run.getDataId());
            if(data == null)
            {
                throw new NotFoundException("No input data found for targetedms run " + run.getId());
            }
            Path file = data.getFilePath();
            if (file == null)
            {
                throw new NotFoundException("Data file for run " + run.getFileName() + " was not found.");
            }
            if(!Files.exists(file))
            {
                errors.addError(new LabKeyError("File: " + file + " does not exist. It may have been deleted on the server."));
                return new SimpleErrorView(errors, true);
            }

            String fileName = FileUtil.getFileName(file);
            String baseFileName = SkylineFileUtils.getBaseName(fileName);

            if ("skyp".equalsIgnoreCase(form.getView()))
            {
                String url = data.getWebDavURL(ExpData.PathType.full);
                if (url != null)
                {
                    StringBuilder content = new StringBuilder();
                    content.append(url);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(content.toString().getBytes(StringUtilsLabKey.DEFAULT_CHARSET));
                    PageFlowUtil.streamFile(getViewContext().getResponse(), Collections.emptyMap(), baseFileName + ".skyp", inputStream, true);
                }
                else
                {
                    errors.addError(new LabKeyError("File: " + file + " cannot be located for download. It may have been deleted on the server."));
                    return new SimpleErrorView(errors, true);
                }
            }
            else
            {
                // Download the full file
                try (InputStream inputStream = Files.newInputStream(file))
                {
                    // Canonicalize file name to use newer preferred convention of .sky.zip to make it easier to recognize on the client side
                    String preferredFileName = fileName.toLowerCase().endsWith(".zip") ? (baseFileName + ".sky.zip") : fileName;
                    PageFlowUtil.streamFile(getViewContext().getResponse(), Collections.emptyMap(), preferredFileName, inputStream, true);
                }
            }
            return null;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Unable to find Skyline file");
        }
    }

    // ------------------------------------------------------------------------
    // Actions to export chromatogram libraries
    // ------------------------------------------------------------------------
    public static class DownloadForm
    {
        int revision;

        public int getRevision()
        {
            return revision;
        }

        public void setRevision(int revision)
        {
            this.revision = revision;
        }
    }
    @RequiresPermission(ReadPermission.class)
    public class DownloadChromLibraryAction extends SimpleViewAction<DownloadForm>
    {
        @Override
        public ModelAndView getView(DownloadForm form, BindException errors) throws Exception
        {
            // Check if the folder has any representative data
            List<Long> representativeRunIds = TargetedMSManager.getCurrentRepresentativeRunIds(getContainer());
            if(representativeRunIds.size() == 0)
            {
                //errors.reject(ERROR_MSG, "Folder "+getContainer().getPath()+" does not contain any representative data.");
                //return new SimpleErrorView(errors, true);
                throw new NotFoundException("Folder "+getContainer().getPath()+" does not contain a chromatogram library.");
            }

            // Get the latest library revision.
            int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(getContainer(), getUser());
            int libraryRevision = ( form.getRevision() != 0) ? form.getRevision() : currentRevision;

            Container container = getContainer();
            Path chromLibFile = ChromatogramLibraryUtils.getChromLibFile(container, libraryRevision);

            // If the library is not found (i.e. was deleted),
            if(!Files.exists(chromLibFile))
            {
                // create a new library file if the version numbers match
                if(libraryRevision == currentRevision)
                {
                    PipeRoot root = PipelineService.get().getPipelineRootSetting(getContainer());
                    if (null != root)
                    {
                        LocalDirectory localDirectory = LocalDirectory.create(root, MODULE_NAME);
                        try
                        {
                            ChromatogramLibraryUtils.writeLibrary(container, getUser(), localDirectory, libraryRevision);
                        }
                        finally
                        {
                            localDirectory.cleanUpLocalDirectory();
                        }
                    }
                    else
                    {
                        throw new ValidationException("Pipeline root not found.");      // TODO: set errors?
                    }
                }
                else
                {
                    throw new NotFoundException("Unable to find archived library for revision " + libraryRevision);
                }
            }


            // construct new filename
            String fileName = ChromatogramLibraryUtils.getDownloadFileName(container, libraryRevision);
            try (InputStream inputStream = Files.newInputStream(chromLibFile))
            {
                PageFlowUtil.streamFile(getViewContext().getResponse(), Collections.emptyMap(), fileName, inputStream, true);
            }

            return null;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class IsLibraryCurrentAction extends ReadOnlyApiAction<LibraryDetailsForm>
    {
        @Override
        public ApiResponse execute(LibraryDetailsForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            Container container = getContainer();

            // Required parameters in the request.
            if(form.getPanoramaServer() == null)
            {
                throw new ApiUsageException("Missing required parameter 'panoramaServer'");
            }
            if(form.getContainer() == null)
            {
                throw new ApiUsageException("Missing required parameter 'container'");
            }
            if(form.getSchemaVersion() == null)
            {
                throw new ApiUsageException("Missing required parameter 'schemaVersion'");
            }
            if(form.getLibraryRevision() == null)
            {
                throw new ApiUsageException("Missing required parameter 'libraryRevision'");
            }


            // Check panorama server.
            URLHelper requestParamServerUrl = new URLHelper(form.getPanoramaServer());
            URLHelper requestServerUrl = new URLHelper(getViewContext().getActionURL().getBaseServerURI());
            if(!URLHelper.queryEqual(requestParamServerUrl, requestServerUrl))
            {
                response.put("errorMessage", "Incorrect Panorama server: "+form.getPanoramaServer());
                return response;
            }

            // Check container path.
            if(!container.getPath().equals(form.getContainer()))
            {
                response.put("errorMessage", "Mismatch in container path. Expected "+container.getPath()+", found "+form.getContainer());
                return response;
            }

            // Check the schema version and library revision.
            if(!ChromatogramLibraryUtils.isRevisionCurrent(getContainer(), getUser(), form.getSchemaVersion(), form.getLibraryRevision()))
            {
                response.put("isUptoDate", Boolean.FALSE);
                return response;
            }

            response.put("isUptoDate", Boolean.TRUE);
            return response;
        }
    }

    public static class LibraryDetailsForm
    {
        private String _panoramaServer;
        private String _container;
        private String _schemaVersion;
        private Integer _libraryRevision;

        public String getPanoramaServer()
        {
            return _panoramaServer;
        }

        public void setPanoramaServer(String panoramaServer)
        {
            _panoramaServer = panoramaServer;
        }

        public String getContainer()
        {
            return _container;
        }

        public void setContainer(String container)
        {
            _container = container;
        }

        public String getSchemaVersion()
        {
            return _schemaVersion;
        }

        public void setSchemaVersion(String schemaVersion)
        {
            _schemaVersion = schemaVersion;
        }

        public Integer getLibraryRevision()
        {
            return _libraryRevision;
        }

        public void setLibraryRevision(Integer libraryRevision)
        {
            _libraryRevision = libraryRevision;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ModificationSearchAction extends QueryViewAction<ModificationSearchForm, QueryView>
    {
        public ModificationSearchAction()
        {
            super(ModificationSearchForm.class);
        }

        @Override
        protected QueryView createQueryView(ModificationSearchForm form, BindException errors, boolean forExport, String dataRegion)
        {
            return createModificationSearchView(form, errors);
        }

        @Override
        protected ModelAndView getHtmlView(final ModificationSearchForm form, BindException errors)
        {
            VBox result = new VBox(new ModificationSearchWebPart(form));

            if (form.isNtermSearch() || form.isCtermSearch() || form.getModificationSearchStr() != null)
                result.addView(createModificationSearchView(form, errors));

            return result;
        }

        private QueryView createModificationSearchView(final ModificationSearchForm form, BindException errors)
        {
            if (! getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.class)))
                return null;

            ViewContext viewContext = getViewContext();
            QuerySettings settings = new QuerySettings(viewContext, "TargetedMSMatches", "Precursor");
//            QuerySettings settings = new QuerySettings(viewContext, "TargetedMSMatches");

            if (form.isIncludeSubfolders())
                settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

            QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
            {
                @Override
                protected TableInfo createTable()
                {
                    TargetedMSTable precursorTable = (TargetedMSTable) super.createTable();
                    TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
                    FilteredTable<TargetedMSSchema> result = new FilteredTable<>(precursorTable, schema);
                    result.wrapAllColumns(true);

                    DetailsURL detailsURLs = new DetailsURL(new ActionURL(TargetedMSController.ShowPeptideAction.class, getContainer()), Collections.singletonMap("id", "PeptideId"));
                    detailsURLs.setContainerContext(new ContainerContext.FieldKeyContext(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "Folder")));
                    result.setDetailsURL(detailsURLs);

                    if (form.isNtermSearch())
                    {
                        result.addCondition(new SQLFragment("ModifiedSequence LIKE '_" + form.getDeltaMassSearchStr(true) + "%' ESCAPE '!'"));
                    }
                    else if (form.isCtermSearch())
                    {
                        result.addCondition(new SQLFragment("ModifiedSequence LIKE '%" + form.getDeltaMassSearchStr(true) + "' ESCAPE '!'"));
                    }
                    else
                    {
                        String modStr = form.getModificationSearchStr();
                        result.addCondition(new SimpleFilter(FieldKey.fromParts("ModifiedSequence"), modStr, modStr != null ? CompareType.CONTAINS_ONE_OF : CompareType.ISBLANK));
                    }

                    List<FieldKey> visibleColumns = new ArrayList<>();

                    visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Label"));
                    visibleColumns.add(FieldKey.fromParts("PeptideId", ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
                    visibleColumns.add(FieldKey.fromParts(ModifiedSequenceDisplayColumn.PRECURSOR_COLUMN_NAME));
                    visibleColumns.add(FieldKey.fromParts("NeutralMass"));
                    visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "File"));
                    if (form.isIncludeSubfolders())
                    {
                        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "Folder", "Path"));
                    }
                    result.setDefaultVisibleColumns(visibleColumns);
                    result.setName("Precursor");

                    List<TableCustomizer> customizers = TargetedMSService.get().getModificationSearchResultCustomizers();
                    for(TableCustomizer customizer : customizers)
                    {
                        customizer.customize(result);
                    }

                    return result;
                }
            };
            result.setTitle("Targeted MS Peptides");
            result.setUseQueryViewActionExportURLs(true);
            return result;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Modification Search Results");
        }
    }

    public static class ModificationSearchForm extends QueryViewAction.QueryExportForm implements HasViewContext
    {
        private ViewContext _context;
        private String _searchType;
        private String _modificationNameType;
        private Boolean _structural;
        private Boolean _isotopeLabel;
        private String _customName;
        private String _unimodName;
        private String _aminoAcids;
        private char[] _aminoAcidArr;
        private Double _deltaMass;
        private boolean _includeSubfolders;
        private String _modSearchPairsStr;
        private boolean _hideIncludeSubfolders; // Hide or show the includeSubfolders checkbox

        public static ModificationSearchForm createDefault()
        {
            ModificationSearchForm result = new ModificationSearchForm();
            result.setSearchType("deltaMass");
            return result;
        }

        public String getModificationSearchStr()
        {
            String modStr = null;
            String delim = "";

            if (_modSearchPairsStr != null)
            {
                // Issue 17596: allow for a set of AA / DeltaMass pairs
                modStr = "";
                for (Pair<String, Double> entry : getModSearchPairs())
                {
                    for (char aa : splitAminoAcidString(entry.getKey()))
                    {
                        modStr += delim + aa + getDeltaMassSearchStr(entry.getValue(), false);
                        delim = ";";
                    }
                }
            }
            else if (_aminoAcidArr != null && _aminoAcidArr.length > 0)
            {
                modStr = "";
                for (char aa : _aminoAcidArr)
                {
                    modStr += delim + aa + getDeltaMassSearchStr(false);
                    delim = ";";
                }
            }

            return modStr;
        }

        public String getDeltaMassSearchStr(boolean withEscapeChar)
        {
            return getDeltaMassSearchStr(_deltaMass, withEscapeChar);
        }

        public String getDeltaMassSearchStr(Double deltaMass, boolean withEscapeChar)
        {
            // use ! as the escape character in the SQL LIKE clause with brackets (i.e. ModifiedSequence LIKE '%![+8!]' ESCAPE '!' )
            DecimalFormat df = new DecimalFormat("0.0");
            return (withEscapeChar ? "!" : "") + "[" + (deltaMass != null && deltaMass > 0 ? "+" : "") + (deltaMass == null ? "" : df.format(deltaMass)) + (withEscapeChar ? "!" : "") + "]";
        }

        public char[] splitAminoAcidString(String aminoAcids)
        {
            return aminoAcids.replaceAll("[^A-Za-z]","").toUpperCase().toCharArray();
        }

        public boolean isCtermSearch()
        {
            return _aminoAcids != null && _aminoAcids.equals("]");
        }

        public boolean isNtermSearch()
        {
            return _aminoAcids != null && _aminoAcids.equals("[");
        }

        @Override
        public void setViewContext(ViewContext context)
        {
            _context = context;
        }

        @Override
        public ViewContext getViewContext()
        {
            return _context;
        }

        public Double getDeltaMass()
        {
            return _deltaMass;
        }

        public void setDeltaMass(Double deltaMass)
        {
            _deltaMass = deltaMass;
        }

        public String getAminoAcids()
        {
            return _aminoAcids;
        }

        public void setAminoAcids(String aminoAcids)
        {
            _aminoAcids = aminoAcids;

            if (_aminoAcids != null)
                _aminoAcidArr = splitAminoAcidString(_aminoAcids);
        }

        public char[] getAminoAcidArr()
        {
            return _aminoAcidArr;
        }

        public String getSearchType()
        {
            return _searchType;
        }

        public void setSearchType(String searchType)
        {
            _searchType = searchType;
        }

        public String getModificationNameType()
        {
            return _modificationNameType;
        }

        public void setModificationNameType(String modificationNameType)
        {
            _modificationNameType = modificationNameType;
        }

        public Boolean isStructural()
        {
            return _structural;
        }

        public void setStructural(Boolean structural)
        {
            _structural = structural;
        }

        public Boolean isIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(Boolean isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }

        public String getCustomName()
        {
            return _customName;
        }

        public void setCustomName(String customName)
        {
            _customName = customName;
        }

        public String getUnimodName()
        {
            return _unimodName;
        }

        public void setUnimodName(String unimodName)
        {
            _unimodName = unimodName;
        }

        public boolean isIncludeSubfolders()
        {
            return _includeSubfolders;
        }

        public void setIncludeSubfolders(boolean includeSubfolders)
        {
            _includeSubfolders = includeSubfolders;
        }

        public List<Pair<String, Double>> getModSearchPairs()
        {
            List<Pair<String, Double>> pairs = new ArrayList<>();
            if (_modSearchPairsStr != null)
            {
                String[] pairStrs = _modSearchPairsStr.split(";");
                for (String pairStr : pairStrs)
                {
                    String[] pair = pairStr.split(",");
                    if (pair.length == 2)
                    {
                        try {
                            pairs.add(new Pair<>(pair[0], Double.parseDouble(pair[1])));
                        }
                        catch (NumberFormatException e)
                        {
                            // skip any pairs that don't conform to the expected format
                        }
                    }
                }
            }
            return pairs;
        }

        public String getModSearchPairsStr()
        {
            return _modSearchPairsStr;
        }

        public void setModSearchPairsStr(String modSearchPairsStr)
        {
            _modSearchPairsStr = modSearchPairsStr;
        }

        public boolean isHideIncludeSubfolders()
        {
            return _hideIncludeSubfolders;
        }

        public void setHideIncludeSubfolders(boolean hideIncludeSubfolders)
        {
            _hideIncludeSubfolders = hideIncludeSubfolders;
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testModificationSearch()
        {
            // test amino acid parsing and modificaation search string generation
            ModificationSearchForm form = ModificationSearchForm.createDefault();

            form.setDeltaMass(10.0);
            form.setAminoAcids("R");
            assertEquals("Unexpected number of parsed amino acids", 1, form.getAminoAcidArr().length);
            assertEquals('R', form.getAminoAcidArr()[0]);
            assertEquals("Unexpected modification search string", "R[+10.0]", form.getModificationSearchStr());

            form.setDeltaMass(8.0);
            form.setAminoAcids("RK");
            assertEquals("Unexpected number of parsed amino acids", 2, form.getAminoAcidArr().length);
            assertEquals('R', form.getAminoAcidArr()[0]);
            assertEquals('K', form.getAminoAcidArr()[1]);
            assertEquals("Unexpected modification search string", "R[+8.0];K[+8.0]", form.getModificationSearchStr());

            form.setDeltaMass(8.01);
            form.setAminoAcids("R K N");
            assertEquals("Unexpected number of parsed amino acids", 3, form.getAminoAcidArr().length);
            assertEquals('R', form.getAminoAcidArr()[0]);
            assertEquals('K', form.getAminoAcidArr()[1]);
            assertEquals('N', form.getAminoAcidArr()[2]);
            assertEquals("Unexpected modification search string", "R[+8.0];K[+8.0];N[+8.0]", form.getModificationSearchStr());

            form.setDeltaMass(-144.11);
            form.setAminoAcids("R,K;N S|T");
            assertEquals("Unexpected number of parsed amino acids", 5, form.getAminoAcidArr().length);
            assertEquals('R', form.getAminoAcidArr()[0]);
            assertEquals('K', form.getAminoAcidArr()[1]);
            assertEquals('N', form.getAminoAcidArr()[2]);
            assertEquals('S', form.getAminoAcidArr()[3]);
            assertEquals('T', form.getAminoAcidArr()[4]);
            assertEquals("Unexpected modification search string", "R[-144.1];K[-144.1];N[-144.1];S[-144.1];T[-144.1]", form.getModificationSearchStr());

            form.setAminoAcids("[");
            assertTrue(form.isNtermSearch());
            assertFalse(form.isCtermSearch());
            form.setAminoAcids("]");
            assertTrue(form.isCtermSearch());
            assertFalse(form.isNtermSearch());

            form.setModSearchPairsStr("GT,6;VG,5");
            assertEquals("Unexpected modification search string", "G[+6.0];T[+6.0];V[+5.0];G[+5.0]", form.getModificationSearchStr());

            form.setDeltaMass(10.0);
            assertEquals("Unexpected delta mass search string", "[+10.0]", form.getDeltaMassSearchStr(false));
            assertEquals("Unexpected delta mass search string", "![+10.0!]", form.getDeltaMassSearchStr(true));
        }
    }

    public static class FolderSetupForm
    {
        private String _folderType;
        private boolean _precursorNormalized;

        public String getFolderType()
        {
            return _folderType;
        }

        public void setFolderType(String folderType)
        {
            _folderType = folderType;
        }

        public boolean isPrecursorNormalized()
        {
            return _precursorNormalized;
        }

        public void setPrecursorNormalized(boolean precursorNormalized)
        {
            _precursorNormalized = precursorNormalized;
        }
    }

    // ------------------------------------------------------------------------
    // Actions to render a graph of library statistics
    // - viewable from the chromatogramLibraryDownload.jsp webpart
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class GraphLibraryStatisticsAction extends ExportAction
    {
        @Override
        public void export(Object o, HttpServletResponse response, BindException errors) throws Exception
        {
            int width;
            int height = 250;

            DefaultCategoryDataset dataset = getNumProteinsNumPeptidesByDate();
            width = dataset.getColumnCount() * 50 + 50;

            JFreeChart chart = ChartFactory.createBarChart(
                    null,                     // chart title
                    null,                     // domain axis label
                    "# added",                // range axis label
                    dataset,                  // data
                    PlotOrientation.VERTICAL, // orientation
                    true,                     // include legend
                    false,                     // tooltips?
                    false                     // URLs?
            );
            // Don't show fractional values on the y-axis
            chart.getCategoryPlot().getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            chart.setBackgroundPaint(new Color(1,1,1,1));

            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
        }

        // ------------------------------------------------------------------------
        // Helper method to return representative proteins and peptides grouped by date
        // - returns a dataset for use with JFreeChart
        // ------------------------------------------------------------------------
        private DefaultCategoryDataset getNumProteinsNumPeptidesByDate() {
            final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            // determine the folder type
            final FolderType folderType = TargetedMSManager.getFolderType(getContainer());

            SQLFragment sqlFragment = new SQLFragment();
            sqlFragment.append("SELECT COALESCE(x.RunDate,y.RunDate) AS RunDate, ProteinCount, PeptideCount, MoleculeCount FROM ");
            sqlFragment.append("(SELECT pepCount.RunDate, COUNT(DISTINCT pepCount.PeptideId) AS PeptideCount, COUNT(DISTINCT pepCount.MoleculeId) AS MoleculeCount ");
            sqlFragment.append("FROM   ( SELECT ");
            sqlFragment.append("r.Created as RunDate, ");
            sqlFragment.append("p.Id AS PeptideId, ");
            sqlFragment.append("m.Id AS MoleculeId ");
            sqlFragment.append("FROM ");
            sqlFragment.append("\ttargetedms.Runs AS r INNER JOIN \n");
            sqlFragment.append("\ttargetedms.PeptideGroup AS pg ON r.Id = pg.RunId INNER JOIN\n");
            sqlFragment.append("\ttargetedms.GeneralMolecule AS gm ON gm.PeptideGroupId = pg.Id INNER JOIN\n");
            sqlFragment.append("\ttargetedms.GeneralPrecursor AS gp ON gp.GeneralMoleculeId = gm.Id LEFT OUTER JOIN\n");
            sqlFragment.append("\ttargetedms.peptide AS p ON p.Id = gm.Id LEFT OUTER JOIN\n");
            sqlFragment.append("\ttargetedms.molecule AS m ON m.Id = gm.Id\n");
            sqlFragment.append("WHERE r.Deleted = ? AND r.Container = ? ");
            // Only proteins (PeptideGroup) are marked as representative in "LibraryProtein" folder types. Get the Ids
            // of all the peptides of representative proteins.
            if(folderType == FolderType.LibraryProtein)
                sqlFragment.append("AND pg.RepresentativeDataState = ? ");
                // Precursors are marked a representative in "LibraryPeptide" folder type.  Get the peptide Ids
                // of all the representative precursors.
            else
                sqlFragment.append("AND gp.RepresentativeDataState = ? ");
            sqlFragment.append(") AS pepCount ");
            sqlFragment.append("GROUP BY pepCount.RunDate) AS x FULL OUTER JOIN ");
            sqlFragment.append("(SELECT protCount.RunDate, COUNT(DISTINCT protCount.Id) AS ProteinCount ");
            sqlFragment.append("FROM   ( SELECT ");
            sqlFragment.append("r.Created as RunDate, ");
            sqlFragment.append("pg.Id ");
            sqlFragment.append("FROM ");
            sqlFragment.append("targetedms.Runs AS r, ");
            sqlFragment.append("targetedms.PeptideGroup AS pg ");
            sqlFragment.append("WHERE ");
            sqlFragment.append("pg.RunId = r.Id AND pg.RepresentativeDataState = ?  AND r.Deleted = ? AND r.Container = ? ");
            sqlFragment.append(") AS protCount ");
            sqlFragment.append("GROUP BY protCount.RunDate) AS y ");
            sqlFragment.append("ON x.RunDate = y.RunDate ORDER BY COALESCE(x.RunDate,y.RunDate); ");

            sqlFragment.add(false);
            sqlFragment.add(getContainer().getId());
            sqlFragment.add(RepresentativeDataState.Representative.ordinal());
            sqlFragment.add(RepresentativeDataState.Representative.ordinal());
            sqlFragment.add(false);
            sqlFragment.add(getContainer().getId());

            // grab data from database
            SqlSelector sqlSelector = new SqlSelector(TargetedMSSchema.getSchema(), sqlFragment);

            // build HashMap of values for binning purposes
            final LinkedHashMap<Date, Integer> protMap = new LinkedHashMap<>();
            final LinkedHashMap<Date, Integer> pepMap = new LinkedHashMap<>();
            final LinkedHashMap<Date, Integer> moleculeMap = new LinkedHashMap<>();

            // add data to maps - binning by the date specified in simpleDateFormat
            sqlSelector.forEach(rs -> {
                Date runDate = rs.getDate("runDate");
                int protCount = protMap.getOrDefault(runDate, 0);
                protMap.put(runDate, protCount + rs.getInt("ProteinCount"));
                int pepCount = pepMap.getOrDefault(runDate, 0);
                pepMap.put(runDate, pepCount + rs.getInt("PeptideCount"));
                int moleculeCount = moleculeMap.getOrDefault(runDate, 0);
                moleculeMap.put(runDate, moleculeCount + rs.getInt("MoleculeCount"));
            });

            LinkedHashMap<Date, Integer> binnedProtMap = binDateHashMap(protMap, 0);
            LinkedHashMap<Date, Integer> binnedPepMap = binDateHashMap(pepMap, 0);
            LinkedHashMap<Date, Integer> binnedMoleculeMap = binDateHashMap(moleculeMap, 0);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d");

            if (protMap.size() > 10) // if more than 2 weeks, bin by week
            {
                binnedProtMap = binDateHashMap(protMap, Calendar.DAY_OF_WEEK);
                binnedPepMap = binDateHashMap(pepMap, Calendar.DAY_OF_WEEK);
                binnedMoleculeMap = binDateHashMap(moleculeMap, Calendar.DAY_OF_WEEK);
            }
            if (binnedProtMap.size() > 10)
            {
                binnedProtMap = binDateHashMap(protMap, Calendar.DAY_OF_MONTH);
                binnedPepMap = binDateHashMap(pepMap, Calendar.DAY_OF_MONTH);
                binnedMoleculeMap = binDateHashMap(moleculeMap, Calendar.DAY_OF_MONTH);
                simpleDateFormat = new SimpleDateFormat("MMM yy");
            }
            // put all data from maps into dataset
            for (Map.Entry<Date, Integer> entry : binnedProtMap.entrySet())
            {
                Date key = entry.getKey();
                if (folderType == FolderType.LibraryProtein)
                    dataset.addValue(entry.getValue(), "Proteins", simpleDateFormat.format(key));
                dataset.addValue(binnedPepMap.get(key), "Peptides", simpleDateFormat.format(key));
                dataset.addValue(binnedMoleculeMap.get(key), "Molecules", simpleDateFormat.format(key));
            }

            return dataset;
        }
    }

    // binDateHashMap - function to bin an existing hashmap of <date, count> into different date increments
    // useful values for mode include
    //   0 - do not perform any additional binning
    //   Calendar.DAY_OF_WEEK - bin by week
    //   Calendar.DAY_OF_MONTH - bin by month
    public static LinkedHashMap<Date, Integer> binDateHashMap(LinkedHashMap<Date, Integer> hashMap, int mode )
    {
        LinkedHashMap<Date, Integer> newMap = new LinkedHashMap<>();

        // put all data from maps into dataset
        for (Map.Entry<Date, Integer> entry : hashMap.entrySet())
        {
            Date keyDate = entry.getKey();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(keyDate);
            calendar.clear(Calendar.HOUR);
            calendar.clear(Calendar.MINUTE);
            calendar.clear(Calendar.MILLISECOND);
            if ( mode != 0) // bin by week or month, passed as an argument
                calendar.set(mode, 1);
            Date newDate = calendar.getTime();

            int count = newMap.getOrDefault(keyDate, 0);
            newMap.put(newDate, count + hashMap.get(keyDate));
        }

        return newMap;
    }

    public static ChromLibAnalyteCounts getChromLibAnalyteCounts(User user, Container container, int libRevision)
    {
        int currentLibRevision = ChromatogramLibraryUtils.getCurrentRevision(container, user);
        if(libRevision == currentLibRevision)
        {
            return getCurrentChromLibAnalyteCounts(user, container);
        }
        else
        {
            ChromLibAnalyteCounts counts = null;
            // Read the counts from the .clib file on the filesystem
            try
            {
                counts = ChromatogramLibraryUtils.getLibraryAnalyteCounts(container, libRevision);
            }
            catch (IOException | SQLException e)
            {
                LOG.error("Error reading from chromatogram library file " + ChromatogramLibraryUtils.getDownloadFileName(container, libRevision)
                        + " in container " + container.getPath(), e);
            }
            return counts == null ? ChromLibAnalyteCounts.NOT_EXISTS : counts;
        }
    }

    public static ChromLibAnalyteCounts getCurrentChromLibAnalyteCounts(User user, Container container)
    {
        ChromLibAnalyteCounts clibAnalyteCounts = new ChromLibAnalyteCounts();
        clibAnalyteCounts.setPeptideGroupCount((int) getNumRepresentativeProteins(user, container));
        clibAnalyteCounts.setPeptideCount((int)getNumRepresentativePeptides(container));
        clibAnalyteCounts.setTransitionCount((int)getNumRankedTransitions(container));
        clibAnalyteCounts.setMoleculeCount((int)getNumRepresentativeMolecules(container));
        return clibAnalyteCounts;
    }

    public static class ChromLibAnalyteCounts
    {
        private int _peptideGroupCount;
        private int _peptideCount;
        private int _transitionCount;
        private int _moleculeCount;

        public static final ChromLibAnalyteCounts NOT_EXISTS = new ChromLibAnalyteCounts(-1, -1, -1, -1);

        public ChromLibAnalyteCounts() {}

        private ChromLibAnalyteCounts(int peptideGroupCount, int peptideCount, int moleculeCount, int transitionCount)
        {
            _peptideGroupCount = peptideGroupCount;
            _peptideCount = peptideCount;
            _moleculeCount = moleculeCount;
            _transitionCount = transitionCount;
        }

        public int getPeptideGroupCount()
        {
            return _peptideGroupCount;
        }

        public void setPeptideGroupCount(int peptideGroupCount)
        {
            _peptideGroupCount = peptideGroupCount;
        }

        public int getPeptideCount()
        {
            return _peptideCount;
        }

        public void setPeptideCount(int peptideCount)
        {
            _peptideCount = peptideCount;
        }

        public int getTransitionCount()
        {
            return _transitionCount;
        }

        public void setTransitionCount(int transitionCount)
        {
            _transitionCount = transitionCount;
        }

        public int getMoleculeCount()
        {
            return _moleculeCount;
        }

        public void setMoleculeCount(int moleculeCount)
        {
            _moleculeCount = moleculeCount;
        }

        public boolean exists()
        {
            return _peptideGroupCount != -1 && _peptideCount != -1 && _moleculeCount != -1 && _transitionCount != -1;
        }
    }

    public static long getNumRepresentativeProteins(User user, Container container) {
        long peptideGroupCount = 0;
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo peptideGroup = schema.getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
        if (peptideGroup != null)
        {
            SimpleFilter peptideGroupFilter = new SimpleFilter(FieldKey.fromParts("RepresentativeDataState"), RepresentativeDataState.Representative.ordinal(), CompareType.EQUAL);
            peptideGroupCount = new TableSelector(peptideGroup, peptideGroupFilter, null).getRowCount();
        }
        return peptideGroupCount;
    }

    public static long getNumRepresentativePeptides(Container container)
    {
        return getNumRepresentativeGeneralMolecules(container, TargetedMSManager.getTableInfoPeptide());
    }

    public static long getNumRepresentativeMolecules(Container container)
    {
        return getNumRepresentativeGeneralMolecules(container, TargetedMSManager.getTableInfoMolecule());
    }

    private static long getNumRepresentativeGeneralMolecules(Container container, TableInfo moleculeTypeTable)
    {
        SQLFragment sqlFragment = new SQLFragment();
        sqlFragment.append("SELECT DISTINCT(p.Id) FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralMolecule(), "p");
        sqlFragment.append(", ");
        sqlFragment.append(moleculeTypeTable, "mt");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pc");
        sqlFragment.append(" WHERE ");
        sqlFragment.append(" p.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND pc.GeneralMoleculeId = p.Id AND mt.Id = p.Id ");
        sqlFragment.append(" AND r.Deleted = ? AND r.Container = ? ");
        sqlFragment.append("AND pc.RepresentativeDataState = ? ");

        // add variables
        sqlFragment.add(false);
        sqlFragment.add(container.getId());
        sqlFragment.add(RepresentativeDataState.Representative.ordinal());

        // run the query on the database and count rows
        SqlSelector sqlSelector = new SqlSelector(TargetedMSSchema.getSchema(), sqlFragment);

        return sqlSelector.getRowCount();
    }

    public static long getNumRankedTransitions(Container container) {

        SQLFragment sqlFragment = new SQLFragment();
        sqlFragment.append("SELECT DISTINCT(tr.Id) FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralTransition(), "tr");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralMolecule(), "p");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "pc");
        sqlFragment.append(" WHERE ");
        sqlFragment.append("tr.generalPrecursorId = pc.Id AND pc.GeneralMoleculeId = p.Id AND p.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? ");
        sqlFragment.append("AND pc.RepresentativeDataState = ? ");

        sqlFragment.add(false);
        sqlFragment.add(container.getId());
        sqlFragment.add(RepresentativeDataState.Representative.ordinal());

        SqlSelector sqlSelector = new SqlSelector(TargetedMSSchema.getSchema(), sqlFragment);
        return sqlSelector.getRowCount();
    }

    /*
     * BEGIN RENAME CODE BLOCK
     */
    public static class RunForm extends ReturnUrlForm
    {
        public enum PARAMS
        {
            run, expanded, grouping
        }

        int run = 0;
        String columns;

        public void setRun(int run)
        {
            this.run = run;
        }

        public int getRun()
        {
            return run;
        }

        public String getColumns()
        {
            return columns;
        }

        public void setColumns(String columns)
        {
            this.columns = columns;
        }

        @Override
        public ActionURL getReturnActionURL()
        {
            ActionURL result;
            try
            {
                result = super.getReturnActionURL();
                if (result != null)
                {
                    return result;
                }
            }
            catch (Exception e)
            {
                // Bad URL -- fall through
            }

            // Bad or missing returnUrl -- go to showRun or showList
            Container c = HttpView.currentContext().getContainer();

            if (0 != run)
                return getShowRunURL(c, run);
            else
                return getShowListURL(c);
        }
    }

    public static ActionURL getRenameRunURL(Container c, TargetedMSRun run, ActionURL returnURL)
    {
        ActionURL url = new ActionURL(RenameRunAction.class, c);
        url.addParameter("run", run.getRunId() );
        url.addReturnURL(returnURL);
        return url;
    }

    public static class RenameForm extends RunForm
    {
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class RenameRunAction extends FormViewAction<RenameForm>
    {
        private TargetedMSRun _run;
        private URLHelper _returnURL;

        @Override
        public void validateCommand(RenameForm target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(RenameForm form, boolean reshow, BindException errors)
        {
            _run = validateRun(form.getRun());
            _returnURL = form.getReturnURLHelper(getShowRunURL(getContainer(), form.getRun()));

            String description = form.getDescription();
            if (description == null || description.length() == 0)
                description = _run.getDescription();

            RenameBean bean = new RenameBean();
            bean.run = _run;
            bean.description = description;
            bean.returnURL = _returnURL;

            getPageConfig().setFocusId("description");

            JspView<RenameBean> jview = new JspView<>("/org/labkey/targetedms/view/renameRun.jsp", bean);
            jview.setFrame(WebPartView.FrameType.NONE);
            return jview;
        }

        @Override
        public boolean handlePost(RenameForm form, BindException errors) throws BatchValidationException
        {
            _run = validateRun(form.getRun());
            TargetedMSManager.renameRun(form.getRun(), form.getDescription(), getUser());
            return true;
        }

        @Override
        public URLHelper getSuccessURL(RenameForm form)
        {
            return form.getReturnURLHelper();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addRunNavTrail(root, _run, _returnURL, "Rename Run", getPageConfig(), null);
        }
    }


    public class RenameBean
    {
        public TargetedMSRun run;
        public String description;
        public URLHelper returnURL;
    }

    private void addRunNavTrail(NavTree root, TargetedMSRun run, URLHelper runURL, String title, PageConfig page, String helpTopic)
    {
        addRootNavTrail(root, null, page, helpTopic);

        if (run != null)
        {
            if (null != runURL)
                root.addChild(run.getDescription(), runURL);
            else
                root.addChild(run.getDescription());
        }

        if (null != title)
            root.addChild(title);
    }

    private void addRootNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        page.setHelpTopic(new HelpTopic(null == helpTopic ? "targetedms" : helpTopic));
        root.addChild("TargetedMS Runs", getShowListURL(getContainer()));
        if (null != title)
            root.addChild(title);
    }

    /*
     * END RENAME CODE BLOCK
     */

    // ------------------------------------------------------------------------
    // BEGIN Method building (link versions) actions
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class GetLinkVersionsAction extends ReadOnlyApiAction<SelectedRowIdsForm>
    {
        @Override
        public Object execute(SelectedRowIdsForm form, BindException errors)
        {
            Collection<Integer> linkedRowIds = new ArrayList<>();

            //get selectedRowIds params
            List<Integer> selectedRowIds = form.getSelectedRowIds();
            if (form.isIncludeSelected())
                linkedRowIds.addAll(selectedRowIds);

            linkedRowIds = TargetedMSManager.getLinkedVersions(getUser(), getContainer(), selectedRowIds, linkedRowIds);

            //send selected rowIds and its links to the client
            ApiSimpleResponse response = new ApiSimpleResponse();
            response.put("linkedRowIds", linkedRowIds.toArray());
            return response;
        }
    }

    public static class SelectedRowIdsForm
    {
        List<Integer> selectedRowIds;
        boolean includeSelected;

        public List<Integer> getSelectedRowIds()
        {
            return selectedRowIds;
        }

        public void setSelectedRowIds(List<Integer> selectedRowIds)
        {
            this.selectedRowIds = selectedRowIds;
        }

        public boolean isIncludeSelected()
        {
            return includeSelected;
        }

        public void setIncludeSelected(boolean includeSelected)
        {
            this.includeSelected = includeSelected;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class RemoveLinkVersionAction extends MutatingApiAction<RowIdForm>
    {
        @Override
        public void validateForm(RowIdForm form, Errors errors)
        {
            if (form.getRowId() == null)
            {
                errors.reject(ERROR_MSG, "No run rowId provided.");
            }
            else
            {
                // verify that the run rowId is valid and matches an existing run
                // and if the run replaces any other runs, it should only replace one
                ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
                if (run == null)
                    errors.reject(ERROR_MSG, "No run found for id " + form.getRowId() + ".");
                else if (!run.getReplacesRuns().isEmpty() && run.getReplacesRuns().size() > 1)
                    errors.reject(ERROR_MSG, "Run " + form.getRowId() + " replaces more than one run.");
            }
        }

        @Override
        public Object execute(RowIdForm form, BindException errors) throws BatchValidationException
        {
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            ExpRun replaces = run.getReplacesRuns().isEmpty() ? null : run.getReplacesRuns().get(0);
            ExpRun replacedBy = run.getReplacedByRun();

            DbScope scope = ExperimentService.get().getSchema().getScope();
            try (DbScope.Transaction transaction = scope.ensureTransaction())
            {
                // if the run is in the middle of a chain, connect its child/parent
                // then remove any references from the child/parent
                if (replaces != null) {
                    replaces.setReplacedByRun(replacedBy);
                    replaces.save(getViewContext().getUser());
                }
                if (replacedBy != null)
                {
                    run.setReplacedByRun(null);
                    run.save(getViewContext().getUser());
                }

                transaction.commit();
            }

            return new ApiSimpleResponse("success", true);
        }
    }

    public static class RowIdForm
    {
        Integer _rowId;

        public void setRowId(Integer rowId)
        {
            _rowId = rowId;
        }

        public Integer getRowId()
        {
            return _rowId;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class SaveLinkVersionsAction extends MutatingApiAction<ChainedVersions>
    {
        @Override
        public void validateForm(ChainedVersions form, Errors errors)
        {
            // verify that the run and replacedByRun rowIds are valid and match an existing run
            for (Map.Entry<Integer, Integer> entry : form.getRuns().entrySet())
            {
                if (entry.getKey() == null)
                    errors.reject(ERROR_MSG, "No run found for id " + entry.getKey());
                ExpRun run = ExperimentService.get().getExpRun(entry.getKey());
                if (run == null || !run.getContainer().equals(getContainer()))
                    errors.reject(ERROR_MSG, "No run found for id " + entry.getKey());
                if (entry.getValue() == null)
                    errors.reject(ERROR_MSG, "No run found for id " + entry.getValue());
                ExpRun replacedByRun = ExperimentService.get().getExpRun(entry.getValue());
                if (replacedByRun == null || !replacedByRun.getContainer().equals(getContainer()))
                    errors.reject(ERROR_MSG, "No run found for id " + entry.getValue());
                if (entry.getKey().equals(entry.getValue()))
                    errors.reject(ERROR_MSG, "Run cannot be replaced by itself: id " + entry.getValue());
            }
        }

        @Override
        public Object execute(ChainedVersions form, BindException errors) throws BatchValidationException
        {
            DbScope scope = ExperimentService.get().getSchema().getScope();

            Set<Integer> ids = new HashSet<>(form.getRuns().keySet());
            ids.addAll(form.getRuns().values());

            Collection<Integer> allLinkedIds = TargetedMSManager.getLinkedVersions(getUser(), getContainer(), ids, ids);

            try (DbScope.Transaction transaction = scope.ensureTransaction())
            {
                // Clear out any existing links
                allLinkedIds.forEach((x) ->
                {
                    ExpRun run = ExperimentService.get().getExpRun(x);
                    if (run.getReplacedByRun() != null)
                    {
                        run.setReplacedByRun(null);
                        try
                        {
                            run.save(getUser());
                        }
                        catch (BatchValidationException e)
                        {
                            throw new UnexpectedException(e);
                        }
                    }
                });

                for (Map.Entry<Integer, Integer> entry : form._runs.entrySet())
                {
                    ExpRun run = ExperimentService.get().getExpRun(entry.getKey());
                    ExpRun replacedByRun = ExperimentService.get().getExpRun(entry.getValue());

                    if (run != null && replacedByRun != null)
                    {
                        run.setReplacedByRun(replacedByRun);
                        run.save(getUser());
                    }
                }

                transaction.commit();
            }

            return new ApiSimpleResponse("success", true);
        }

        private List<Integer> getLinkedListOfChainedDocuments(Set<Map.Entry<Integer, Integer>> entries)
        {
            int index = 0;
            List<Integer> chainedDocumentsList = new LinkedList<>();


            for (Map.Entry<Integer, Integer> entry : entries)
            {
                ExpRun run = ExperimentService.get().getExpRun(entry.getKey());
                ExpRun replacedRun = entry.getValue() != null ? ExperimentService.get().getExpRun(entry.getValue()) : null;

                if (chainedDocumentsList.contains(run.getRowId()) || chainedDocumentsList.contains(replacedRun.getRowId()))
                {
                    addToDocumentChainAtCorrectIndex(run, replacedRun, chainedDocumentsList);
                }
                else
                {
                    chainedDocumentsList.add(index++, run.getRowId());
                    chainedDocumentsList.add(index++, replacedRun.getRowId());
                }
            }
            return chainedDocumentsList;
        }

        private void addToDocumentChainAtCorrectIndex(ExpRun run, ExpRun replacedByRun, List<Integer> list)
        {
            for (int i = 0; i < list.size(); i++)
            {
                final Integer rowid = list.get(i);
                if (rowid == run.getRowId())
                {
                    list.add(i + 1, replacedByRun.getRowId());
                    break;
                }
                else if (rowid == replacedByRun.getRowId())
                {
                    list.add(i, run.getRowId());
                    break;
                }
            }
        }

    }

    public static class ChainedVersions implements CustomApiForm
    {
        private Map<Integer, Integer> _runs = new HashMap<>();

        @Override
        public void bindProperties(Map<String,Object> properties)
        {
            JSONObject json;
            if (properties instanceof JSONObject)
                json = (JSONObject)properties;
            else
                json = new JSONObject(properties);

            List<Map<String, Object>> list = json.getJSONArray("runs").toMapList();
            for (Map<String, Object> entry : list)
            {
                Integer rowId = (Integer) entry.get("RowId");
                Integer replacedByRunId = (Integer) entry.get("ReplacedByRun");
                _runs.put(rowId, replacedByRunId);
            }
        }

        public Map<Integer, Integer> getRuns()
        {
            return _runs;
        }
    }
    // ------------------------------------------------------------------------
    // END Method building (link versions) actions
    // ------------------------------------------------------------------------ 1

    @RequiresPermission(InsertPermission.class)
    public static class ClustergrammerHeatMapAction extends MutatingApiAction<ClustergrammerForm>
    {
        @Override
        public void validateForm(ClustergrammerForm form, Errors errors)
        {
            if (form.getTitle() == null)
                errors.reject(ERROR_MSG, "A Custergrammer report title is required.");

            if (form.getSelectedIds() == null || form.getSelectedIds().length <= 0)
                errors.reject(ERROR_MSG, "No files selected.");
        }

        @Override
        public ApiResponse execute(ClustergrammerForm form, BindException errors) throws Exception
        {
            Map<String, Map<String, Double>> results = TargetedMSManager.getClustergrammerQuery(getUser(), getContainer(), form.getSelectedIds());
            if (results.size() == 0)
            {
                errors.reject(ERROR_MSG, "No results for the selected file(s)");
                return null;
            }

            ClustergrammerHeatMap hm = new ClustergrammerHeatMap(results, form.getTitle());
            ClustergrammerClient client = new ClustergrammerClient();
            String hmLink = client.generateHeatMap(hm, errors);

            if (hmLink != null && !errors.hasErrors())
            {
                RedirectReport report = (RedirectReport) ReportService.get().createReportInstance(ReportService.LINK_REPORT_TYPE);

                ReportDescriptor rd = report.getDescriptor();

                try
                {
                    URLHelper url = new URLHelper(hmLink);
                    report.setUrl(url);
                }
                catch (URISyntaxException e)
                {
                    throw new IllegalArgumentException(e.getMessage());
                }

                rd.setContainer(getContainer().getId());
                rd.setOwner(getUser().getUserId());
                rd.setReportName(form.getTitle());
                rd.setReportDescription(form.getDescription());

                String[] categoryParts = new String[] {"Clustergrammer"};
                ViewCategory category = ViewCategoryManager.getInstance().ensureViewCategory(getContainer(), getUser(), categoryParts);
                rd.setCategoryId(category.getRowId());

                ReportService.get().saveReport(getViewContext(), rd.getReportName(), report);
            }

            ApiSimpleResponse response = new ApiSimpleResponse();
            response.put("heatMapURL", hmLink);
            return response;
        }
    }

    public static class ClustergrammerForm
    {
        private String _title;
        private String _description;
        private Long[] _selectedIds;

        public String getTitle()
        {
            return _title;
        }

        public void setTitle(String title)
        {
            _title = title;
        }

        public void setDescription(String description)
        {
            _description = description;
        }

        public String getDescription()
        {
            return _description;
        }

        public Long[] getSelectedIds()
        {
            return _selectedIds;
        }

        public void setSelectedIds(Long[] selectedIds)
        {
            _selectedIds = selectedIds;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowFiguresOfMeritAction extends SimpleViewAction<FomForm>
    {
        private TargetedMSRun _run;
        private GeneralMolecule _generalMolecule;

        @Override
        public void validate(FomForm form, BindException errors)
        {
            if (form.getRunId() == null || form.getGeneralMoleculeId() == null)
                throw new NotFoundException("Missing one of the required parameters, RunId or GeneralMoleculeId.");

            _run = TargetedMSManager.getRun(form.getRunId());
            if (_run == null || !_run.getContainer().equals(getContainer()))
                throw new NotFoundException("Could not find RunId " + form.getRunId());
        }

        @Override
        public ModelAndView getView(FomForm form, BindException errors)
        {
            UserSchema schema = QueryService.get().getUserSchema(getUser(), getViewContext().getContainer(), TargetedMSSchema.SCHEMA_NAME);
            TableInfo tableInfo = schema.getTable(TargetedMSSchema.TABLE_MOLECULE_INFO);
            if (tableInfo == null)
            {
                throw new NotFoundException("Query " + TargetedMSSchema.SCHEMA_NAME + "." + TargetedMSSchema.TABLE_MOLECULE_INFO + " not found.");
            }

            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("GeneralMoleculeId"), form.getGeneralMoleculeId(), CompareType.EQUAL);
            filter.addCondition(FieldKey.fromString("RunId"), form.getRunId(), CompareType.EQUAL);
            TableSelector ts = new TableSelector(tableInfo, filter, null);

            if (ts.getRowCount() < 1)
            {
                throw new NotFoundException("GeneralMoleculeId " + form.getGeneralMoleculeId() + " not found for RunId " + form.getRunId());
            }

            form = ts.getObject(FomForm.class);

            JspView<FomForm> figuresOfMeritView = new JspView<>("/org/labkey/targetedms/view/figuresOfMerit.jsp", form);
            figuresOfMeritView.setTitle("Figures of Merit");

            QuantificationSettings settings = new TableSelector(schema.getTable(TargetedMSSchema.TABLE_QUANTIIFICATION_SETTINGS), new SimpleFilter(FieldKey.fromParts("RunId"), form.getRunId()), null).getObject(QuantificationSettings.class);
            form.setQuantificationSettings(settings);

            if (form.getPeptideName() != null)
            {
                _generalMolecule = PeptideManager.getPeptide(getContainer(), form.getGeneralMoleculeId());
            }
            else
            {
                _generalMolecule = MoleculeManager.getMolecule(getContainer(), form.getGeneralMoleculeId());
            }

            return figuresOfMeritView;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getDescription(), getShowCalibrationCurvesURL(getContainer(), _run.getId()));

                if (_generalMolecule != null)
                {
                    root.addChild(_generalMolecule.getTextId());
                }

            }
        }
    }

    public static class PKForm
    {
        Integer _runId;
        Integer _generalMoleculeId;
        private List<String> _sampleGroupNames;

        public Integer getRunId()
        {
            return _runId;
        }

        public void setRunId(Integer runId)
        {
            _runId = runId;
        }

        public Integer getGeneralMoleculeId()
        {
            return _generalMoleculeId;
        }

        public void setGeneralMoleculeId(Integer generalMoleculeId)
        {
            _generalMoleculeId = generalMoleculeId;
        }

        public void setSampleGroupNames(List<String> sampleGroupNames)
        {
            _sampleGroupNames = sampleGroupNames;
        }

        public List<String> getSampleGroupNames()
        {
            return _sampleGroupNames;
        }
    }

    public static class FomForm
    {
        Integer _runId;
        Integer _generalMoleculeId;
        String _peptideName;
        String _moleculeName;
        String _fileName;
        String _sampleFiles;
        QuantificationSettings _settings;

        public Integer getRunId()
        {
            return _runId;
        }

        public void setRunId(Integer runId)
        {
            _runId = runId;
        }

        public Integer getGeneralMoleculeId()
        {
            return _generalMoleculeId;
        }

        public void setGeneralMoleculeId(Integer moleculeId)
        {
            _generalMoleculeId = moleculeId;
        }

        public String getPeptideName()
        {
            return _peptideName;
        }

        public void setPeptideName(String peptideName)
        {
            _peptideName = peptideName;
        }

        public String getMoleculeName()
        {
            return _moleculeName;
        }

        public void setMoleculeName(String moleculeName)
        {
            _moleculeName = moleculeName;
        }

        public String getFileName()
        {
            return _fileName;
        }

        public void setFileName(String fileName)
        {
            _fileName = fileName;
        }

        public String getSampleFiles()
        {
            return _sampleFiles;
        }

        public void setSampleFiles(String sampleFiles)
        {
            _sampleFiles = sampleFiles;
        }

        private void setQuantificationSettings(QuantificationSettings settings)
        {
            _settings = settings;
        }

        /** Defaults to 30 if nothing is set */
        public double getMaxLOQBias()
        {
            return _settings == null || _settings.getMaxLOQBias() == null ? 30.0 : _settings.getMaxLOQBias().doubleValue();
        }

        /** Defaults to null if nothing is set */
        public Double getMaxLOQCV()
        {
            return _settings == null ? null : _settings.getMaxLOQCV();
        }

        /** Defaults to "none" if nothing is set */
        public String getLODCalculation()
        {
            return _settings == null || _settings.getLODCalculation() == null ? "none" : _settings.getLODCalculation();
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowCalibrationCurveAction extends QueryViewAction<CalibrationCurveForm, QueryView>
    {
        protected TargetedMSRun _run;  // save for use in appendNavTrail
        private CalibrationCurveChart _chart;
        private boolean _asProteomics;

        public ShowCalibrationCurveAction()
        {
            super(CalibrationCurveForm.class);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getDescription(), getShowCalibrationCurvesURL(getContainer(), _run.getId()));
                if (_chart.getMolecule() != null)
                {
                    root.addChild(_chart.getMolecule().getTextId());
                }
            }
        }

        @Override
        public void validate(CalibrationCurveForm form, BindException errors)
        {
            //ensure that the experiment run is valid and exists within the current container
            _run = validateRun(form.getId());
        }

        @Override
        protected QueryView createQueryView(CalibrationCurveForm form, BindException errors, boolean forExport, @Nullable String dataRegion)
        {
            UserSchema schema = new TargetedMSSchema(getUser(), getContainer());
            String queryName = _asProteomics ? "CalibrationCurvePrecursors" : "CalibrationCurveMoleculePrecursors";
            QuerySettings settings = new QuerySettings(getViewContext(), "curveDetail", queryName);
            settings.getBaseFilter().addCondition(FieldKey.fromParts("CalibrationCurve"), form.getCalibrationCurveId());
            settings.setBaseSort(new Sort("SampleFileId/SampleName"));
            QueryView result = QueryView.create(getViewContext(), schema, settings, errors);
            result.setTitle("Quantitation Ratios");
            return result;
        }

        @Override
        public ModelAndView getView(CalibrationCurveForm calibrationCurveForm, BindException errors)
        {
            CalibrationCurveView curvePlotView = new CalibrationCurveView(getUser(), getContainer(), calibrationCurveForm.getCalibrationCurveId());
            _chart = curvePlotView.getChart();
            _asProteomics = _chart.getMolecule() != null && _chart.getMolecule() instanceof Peptide;

            // Summary charts for the precursor
            SummaryChartBean summaryChartBean = new SummaryChartBean(_run);
            summaryChartBean.setShowControls(false);
            summaryChartBean.setInitialHeight(300);
            summaryChartBean.setInitialWidth(1200);

            // Use different setter and details URL for Peptide vs Small Molecule
            ActionURL detailsUrl = null;
            if (_chart.getMolecule() != null)
            {
                if (_asProteomics)
                {
                    summaryChartBean.setPeptideId(_chart.getMolecule().getId());
                    detailsUrl = new ActionURL(ShowPeptideAction.class, getContainer());
                }
                else
                {
                    summaryChartBean.setMoleculeId(_chart.getMolecule().getId());
                    detailsUrl = new ActionURL(ShowMoleculeAction.class, getContainer());
                }
            }

            JspView<SummaryChartBean> summaryChartView = new JspView<>("/org/labkey/targetedms/view/summaryChartsView.jsp", summaryChartBean);
            summaryChartView.setTitle("Summary Charts");
            if (detailsUrl != null)
            {
                detailsUrl.addParameter("id", _chart.getMolecule().getId());
                summaryChartView.setTitleHref(detailsUrl);
            }

            return new VBox(curvePlotView, summaryChartView, createQueryView(calibrationCurveForm, errors, false, null));
        }
    }

    public static class CalibrationCurveForm extends RunDetailsForm
    {
        int calibrationCurveId;

        public int getCalibrationCurveId()
        {
            return calibrationCurveId;
        }

        public void setCalibrationCurveId(int calibrationCurveId)
        {
            this.calibrationCurveId = calibrationCurveId;
        }
    }

    public static class TargetedMSUrlsImpl implements TargetedMSUrls
    {
        public static TargetedMSUrlsImpl get()
        {
            return (TargetedMSUrlsImpl) urlProvider(TargetedMSUrls.class);
        }

        @Override
        public ActionURL getDownloadDocumentUrl(Container container, long runId)
        {
            ActionURL url = new ActionURL(TargetedMSController.DownloadDocumentAction.class, container);
            url.addParameter("id", runId);
            return url;
        }

        @Override
        public ActionURL getShowRunUrl(Container container, long runId)
        {
            ActionURL url = new ActionURL(TargetedMSController.ShowPrecursorListAction.class, container);
            url.addParameter("id", runId);
            return url;
        }

        @Override
        public ActionURL getShowProteinUrl(Container container, int proteinId)
        {
            ActionURL url = new ActionURL(TargetedMSController.ShowProteinAction.class, container);
            url.addParameter("id", proteinId);
            return url;
        }

        @Override
        public ActionURL getShowPeptideUrl(Container container, long peptideId)
        {
            ActionURL url = new ActionURL(TargetedMSController.ShowPeptideAction.class, container);
            url.addParameter("id", peptideId);
            return url;
        }
    }
}
