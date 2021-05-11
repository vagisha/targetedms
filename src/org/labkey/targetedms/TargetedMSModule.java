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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.PropertySchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.DirectoryPattern;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.module.SpringModule;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.permissions.ApplicationAdminPermission;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.usageMetrics.UsageMetricsService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.JspView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.targetedms.chart.ComparisonCategory;
import org.labkey.targetedms.chart.ReplicateLabelMinimizer;
import org.labkey.targetedms.datasource.MsDataSourceUtil;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.skyaudit.SkylineAuditLogParser;
import org.labkey.targetedms.passport.PassportController;
import org.labkey.targetedms.pipeline.TargetedMSPipelineProvider;
import org.labkey.targetedms.query.SkylineListSchema;
import org.labkey.targetedms.search.ModificationSearchWebPart;
import org.labkey.targetedms.search.ProteinSearchWebPart;
import org.labkey.targetedms.view.CalibrationCurveView;
import org.labkey.targetedms.view.LibraryQueryViewWebPart;
import org.labkey.targetedms.view.PeptideGroupViewWebPart;
import org.labkey.targetedms.view.QCSummaryWebPart;
import org.labkey.targetedms.view.TargetedMSRunsWebPartView;
import org.labkey.targetedms.view.TransitionPeptideSearchViewProvider;
import org.labkey.targetedms.view.TransitionProteinSearchViewProvider;
import org.labkey.targetedms.view.passport.ProteinListView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.api.targetedms.TargetedMSService.FOLDER_TYPE_PROP_NAME;
import static org.labkey.api.targetedms.TargetedMSService.MODULE_NAME;

public class TargetedMSModule extends SpringModule implements ProteomicsModule
{
    // Protocol prefix for importing .sky documents from Skyline
    public static final String IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX = "TargetedMS.ImportSky";
    // Protocol prefix for importing .zip archives from Skyline
    public static final String IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX = "TargetedMS.ImportSkyZip";

    public static final ExperimentRunType EXP_RUN_TYPE = new TargetedMSExperimentRunType();
    public static final String TARGETED_MS_SETUP = "Targeted MS Setup";
    public static final String TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD = "Chromatogram Library Download";
    public static final String TARGETED_MS_PRECURSOR_VIEW = "Targeted MS Precursor View";
    public static final String TARGETED_MS_PEPTIDE_VIEW = "Targeted MS Peptide View";
    public static final String TARGETED_MS_MOLECULE_PRECURSOR_VIEW = "Targeted MS Molecule Precursor View";
    public static final String TARGETED_MS_MOLECULE_VIEW = "Targeted MS Molecule View";
    public static final String TARGETED_MS_PEPTIDE_GROUP_VIEW = "Targeted MS Protein View";
    public static final String TARGETED_MS_RUNS_WEBPART_NAME = "Targeted MS Runs";
    public static final String TARGETED_MS_PROTEIN_SEARCH = "Targeted MS Protein Search";
    public static final String TARGETED_MS_PEPTIDE_SEARCH = "Targeted MS Peptide Search";
    public static final String TARGETED_MS_QC_SUMMARY = "Targeted MS QC Summary";
    public static final String TARGETED_MS_QC_PLOTS = "Targeted MS QC Plots";
    public static final String MASS_SPEC_SEARCH_WEBPART = "Mass Spec Search (Tabbed)";
    public static final String TARGETED_MS_PARETO_PLOT = "Targeted MS Pareto Plot";
    public static final String TARGETED_MS_CALIBRATION_CURVE = "Targeted MS Calibration Curve";

    public static final String PEPTIDE_TAB_NAME = "Peptides";
    public static final String PROTEIN_TAB_NAME = "Proteins";
    public static final String MOLECULE_TAB_NAME = "Molecules";

    public static final String[] EXPERIMENT_FOLDER_WEB_PARTS = new String[] {MASS_SPEC_SEARCH_WEBPART,
                                                                           TARGETED_MS_RUNS_WEBPART_NAME};

    public static final String[] LIBRARY_FOLDER_WEB_PARTS = new String[] {TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD,
                                                                          MASS_SPEC_SEARCH_WEBPART,
                                                                          TARGETED_MS_RUNS_WEBPART_NAME};

    public static final String[] PEPTIDE_TAB_WEB_PARTS = new String[] {
            TARGETED_MS_PEPTIDE_VIEW,
            TARGETED_MS_PRECURSOR_VIEW
    };

    public static final String[] MOLECULE_TAB_WEB_PARTS = new String[] {
            TARGETED_MS_MOLECULE_VIEW,
            TARGETED_MS_MOLECULE_PRECURSOR_VIEW
    };

    public static final String[] PROTEIN_TAB_WEB_PARTS = new String[] {
            TARGETED_MS_PEPTIDE_GROUP_VIEW
    };

    public static final String[] QC_FOLDER_WEB_PARTS = new String[] {TARGETED_MS_QC_SUMMARY, TARGETED_MS_QC_PLOTS};

    public static ModuleProperty FOLDER_TYPE_PROPERTY;
    public static ModuleProperty SKIP_CHROMATOGRAM_IMPORT_PROPERTY;
    public static ModuleProperty PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY;
    public static ModuleProperty SKYLINE_AUDIT_LEVEL_PROPERTY;


    public static final String AUTO_QC_PING_TIMEOUT = "TargetedMS AutoQCPing Timeout";

    public static final String SKIP_CHROMATOGRAM_IMPORT = "Skip chromatogram import into database";
    public static final String PREFER_SKYD_FILE_CHROMATOGRAMS = "Prefer loading chromatograms from SKYD file when possible";
    public static final String SKYLINE_AUDIT_LEVEL = "Audit log integrity level for the uploaded Skyline documents";

    public TargetedMSModule()
    {
        FOLDER_TYPE_PROPERTY = new ModuleProperty(this, FOLDER_TYPE_PROP_NAME);
        // Set up the TargetedMS Folder Type property
        FOLDER_TYPE_PROPERTY.setDefaultValue(TargetedMSService.FolderType.Undefined.toString());
        FOLDER_TYPE_PROPERTY.setCanSetPerContainer(true);
        FOLDER_TYPE_PROPERTY.setShowDescriptionInline(true);
        addModuleProperty(FOLDER_TYPE_PROPERTY);

        List<ModuleProperty.Option> options = List.of(
            new ModuleProperty.Option("Enabled", Boolean.TRUE.toString()),
            new ModuleProperty.Option("Disabled", Boolean.FALSE.toString())
        );
        // Set up the properties for controlling how chromatograms are managed in DB vs files
        SKIP_CHROMATOGRAM_IMPORT_PROPERTY = new ModuleProperty(this, SKIP_CHROMATOGRAM_IMPORT);
        SKIP_CHROMATOGRAM_IMPORT_PROPERTY.setInputType(ModuleProperty.InputType.combo);
        SKIP_CHROMATOGRAM_IMPORT_PROPERTY.setOptions(options);
        SKIP_CHROMATOGRAM_IMPORT_PROPERTY.setDefaultValue(Boolean.toString(false));
        SKIP_CHROMATOGRAM_IMPORT_PROPERTY.setCanSetPerContainer(true);
        SKIP_CHROMATOGRAM_IMPORT_PROPERTY.setDescription("Skyline stores chromatograms in SKYD files. Panorama can import them into its database, or leave them to be loaded from the file on demand");
        SKIP_CHROMATOGRAM_IMPORT_PROPERTY.setShowDescriptionInline(true);
        addModuleProperty(SKIP_CHROMATOGRAM_IMPORT_PROPERTY);

        //------------------------
        List<ModuleProperty.Option> auditOptions = List.of(
            new ModuleProperty.Option("0 - No Verification", "0"),
            new ModuleProperty.Option("1 - Hash Verification", "1"),
            new ModuleProperty.Option("2 - RSA Verification", "2")
        );
        // Set up the properties for controlling how chromatograms are managed in DB vs files
        SKYLINE_AUDIT_LEVEL_PROPERTY = new ModuleProperty(this, SKYLINE_AUDIT_LEVEL);
        SKYLINE_AUDIT_LEVEL_PROPERTY.setInputType(ModuleProperty.InputType.combo);
        SKYLINE_AUDIT_LEVEL_PROPERTY.setOptions(auditOptions);
        SKYLINE_AUDIT_LEVEL_PROPERTY.setDefaultValue("0");
        SKYLINE_AUDIT_LEVEL_PROPERTY.setCanSetPerContainer(true);
        SKYLINE_AUDIT_LEVEL_PROPERTY.setDescription("Defines requirements for the integrity of the audit log uploaded together with a Skyline document. <br>\n"+
                "0 means that no audit log is required. If the log file is present in the uploaded file it will be parsed and loaded as is.<br>\n " +
                "1 means that audit log is required and its integrity will be verified using MD5 hash-based algorythm. If log integrity verification fails the document upload will be cancelled.<br> \n" +
                "2 means that audit log is required and its integrity will be verified using RSA-encryption algorythm. If log integrity verification fails the document upload will be cancelled.");
        SKYLINE_AUDIT_LEVEL_PROPERTY.setShowDescriptionInline(true);
        addModuleProperty(SKYLINE_AUDIT_LEVEL_PROPERTY);
        //------------------------rr

        PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY = new ModuleProperty(this, PREFER_SKYD_FILE_CHROMATOGRAMS);
        PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.setInputType(ModuleProperty.InputType.combo);
        PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.setOptions(options);
        PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.setDefaultValue(Boolean.toString(false));
        PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.setCanSetPerContainer(true);
        PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.setDescription("Skyline stores chromatograms in SKYD files. Panorama can load them directly from the file, when preset, even if they have been previously imported into the database.");
        PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY.setShowDescriptionInline(true);
        addModuleProperty(PREFER_SKYD_FILE_CHROMATOGRAMS_PROPERTY);

        // setup the QC Summary webpart AutoQCPing timeout
        ModuleProperty autoQCPingProp = new ModuleProperty(this, AUTO_QC_PING_TIMEOUT);
        autoQCPingProp.setDescription("The number of minutes before the most recent AutoQCPing indicator is considered stale.");
        autoQCPingProp.setDefaultValue("15");
        autoQCPingProp.setShowDescriptionInline(true);
        autoQCPingProp.setCanSetPerContainer(true);
        addModuleProperty(autoQCPingProp);
    }

    @Override
    public String getName()
    {
        return MODULE_NAME;
    }

    @Override
    public Double getSchemaVersion()
    {
        return 21.005;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return List.of(
            new BaseWebPartFactory(TARGETED_MS_SETUP)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    JspView<Void> view = new JspView<>("/org/labkey/targetedms/view/folderSetup.jsp");
                    view.setTitle(TargetedMSController.CONFIGURE_TARGETED_MS_FOLDER);
                    return view;
                }

                @Override
                public String getDisplayName(Container container, String location)
                {
                    return "Panorama Setup";
                }
            },

            new BaseWebPartFactory(TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    JspView<Void> view = new JspView<>("/org/labkey/targetedms/view/chromatogramLibraryDownload.jsp");
                    view.setTitle(TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD);
                    return view;
                }
            },

            new BaseWebPartFactory(TARGETED_MS_PRECURSOR_VIEW)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                   return new LibraryQueryViewWebPart(portalCtx, TargetedMSSchema.TABLE_LIBRARY_PRECURSOR, "Precursors", "LibraryPrecursors");
                }
            },

            new BaseWebPartFactory(TARGETED_MS_MOLECULE_PRECURSOR_VIEW)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                   return new LibraryQueryViewWebPart(portalCtx, TargetedMSSchema.TABLE_LIBRARY_MOLECULE_PRECURSOR, "Precursors", "LibraryPrecursors");
                }
            },

            new BaseWebPartFactory(TARGETED_MS_PEPTIDE_VIEW)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new LibraryQueryViewWebPart(portalCtx, TargetedMSSchema.TABLE_PEPTIDE, "Peptides", "LibraryPeptides");
                }
            },

            new BaseWebPartFactory(TARGETED_MS_MOLECULE_VIEW)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new LibraryQueryViewWebPart(portalCtx, TargetedMSSchema.TABLE_MOLECULE, "Molecules", "LibraryMolecules");
                }
            },

            new BaseWebPartFactory(TARGETED_MS_PEPTIDE_GROUP_VIEW)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new PeptideGroupViewWebPart(portalCtx);
                }
            },

            new BaseWebPartFactory(TARGETED_MS_RUNS_WEBPART_NAME)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new TargetedMSRunsWebPartView(portalCtx);
                }
            },

            new BaseWebPartFactory(TARGETED_MS_PROTEIN_SEARCH)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new ProteinSearchWebPart(new ProteinService.ProteinSearchForm()
                    {
                        @Override
                        public int[] getSeqId()
                        {
                            return new int[0];
                        }

                        @Override
                        public boolean isExactMatch()
                        {
                            return true;
                        }
                    });
                }
            },

            new BaseWebPartFactory(TARGETED_MS_PEPTIDE_SEARCH)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    JspView<Void> view = new JspView<>("/org/labkey/targetedms/search/peptideSearch.jsp");
                    view.setTitle("Peptide Search");
                    return view;
                }
            },

            new BaseWebPartFactory(ModificationSearchWebPart.NAME)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    TargetedMSController.ModificationSearchForm form = TargetedMSController.ModificationSearchForm.createDefault();
                    // When rendering a webpart with LABKEY.WebPart in a wiki page, config.partConfig can be used to set the
                    // configuration parameters for the webpart. Read the parameters from the ViewContext and initialize the
                    // form accordingly.
                    // For example, partConfig: {hideIncludeSubfolder: true, includeSubfolders: true} should hide the
                    // "includeSubfolders" checkbox and set the value of "includeSubfolders" to true.
                    String inclSubFolders = (String)portalCtx.get("includeSubfolders");
                    if(!StringUtils.isBlank(inclSubFolders))
                    {
                        form.setIncludeSubfolders(Boolean.valueOf(inclSubFolders));
                    }
                    String hideIncludeSubfolder = (String)portalCtx.get("hideIncludeSubfolder");
                    if(!StringUtils.isBlank(hideIncludeSubfolder))
                    {
                        form.setHideIncludeSubfolders(Boolean.valueOf(hideIncludeSubfolder));
                    }
                    return new ModificationSearchWebPart(form);
                }
            },

            new BaseWebPartFactory(TARGETED_MS_QC_PLOTS)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    JspView<Void> result = new JspView<>("/org/labkey/targetedms/view/qcTrendPlotReport.jsp");
                    result.addClientDependency(ClientDependency.fromPath("Ext4"));
                    result.setTitle("QC Plots");
                    result.setFrame(WebPartView.FrameType.PORTAL);
                    return result;
                }
            },

            new BaseWebPartFactory(TARGETED_MS_QC_SUMMARY)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    return new QCSummaryWebPart(portalCtx, 3);
                }
            },

            new BaseWebPartFactory(TARGETED_MS_PARETO_PLOT)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    JspView<Void> result = new JspView<>("/org/labkey/targetedms/view/paretoPlot.jsp");
                    result.addClientDependency(ClientDependency.fromPath("Ext4"));
                    result.setTitle("Pareto Plots");
                    result.setFrame(WebPartView.FrameType.PORTAL);
                    return result;
                }
            },

            new BaseWebPartFactory("Passport")
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    QueryView v =  ProteinListView.createView(portalCtx);
                    v.setTitle("Passport");
                    v.setFrame(WebPartView.FrameType.PORTAL);
                    return v;
                }
            },

            new BaseWebPartFactory(TARGETED_MS_CALIBRATION_CURVE)
            {
                @Override
                public WebPartView<?> getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    String calCurveIdString = webPart.getPropertyMap().get("calibrationCurveId");
                    long calCurveId = 0;
                    if (calCurveIdString != null)
                    {
                        try
                        {
                            calCurveId = Long.parseLong(calCurveIdString);
                        }
                        catch (NumberFormatException ignored) {}
                    }
                    return new CalibrationCurveView(portalCtx.getUser(), portalCtx.getContainer(), calCurveId);
                }

                @Override
                public boolean isAvailable(Container c, String scope, String location)
                {
                    return false;
                }
            }
        );
    }

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(TargetedMSManager.get().getSchemaName());
    }

    @Override
    protected void init()
    {
        addController("targetedms", TargetedMSController.class);
        addController("passport", PassportController.class);

        TargetedMSSchema.register(this);
        SkylineListSchema.register(this);

        UsageMetricsService svc = UsageMetricsService.get();
        if (null != svc)
        {
            svc.registerUsageMetrics(MODULE_NAME, () ->
            {
                Map<String, Object> metric = new HashMap<>();
                DbSchema schema = TargetedMSManager.getSchema();
                metric.put("runCount", new SqlSelector(schema, "SELECT COUNT(*) FROM TargetedMS.Runs WHERE Deleted = ?", Boolean.FALSE).getObject(Long.class));
                metric.put("guideSetCount", new SqlSelector(schema, "SELECT COUNT(*) FROM TargetedMS.GuideSet").getObject(Long.class));
                metric.put("guideSetContainerCount", new SqlSelector(schema, "SELECT COUNT(DISTINCT Container) FROM TargetedMS.GuideSet").getObject(Long.class));

                SQLFragment folderTypeSQL = new SQLFragment("SELECT p.value, COUNT(*) AS FolderCount FROM ");
                folderTypeSQL.append(PropertySchema.getInstance().getTableInfoProperties(), "p");
                folderTypeSQL.append(" INNER JOIN ");
                folderTypeSQL.append(PropertySchema.getInstance().getTableInfoPropertySets(), "ps");
                folderTypeSQL.append(" ON p.\"set\" = ps.\"set\" WHERE ps.category = 'moduleProperties.TargetedMS' ");
                folderTypeSQL.append(" AND p.name = ? GROUP BY value");
                folderTypeSQL.add(FOLDER_TYPE_PROP_NAME);

                Map<String, Long> folderCounts = new HashMap<>();
                new SqlSelector(PropertySchema.getInstance().getSchema(), folderTypeSQL).forEach(rs ->
                        folderCounts.put(rs.getString("value"), rs.getLong("FolderCount")));

                metric.put("folderCounts", folderCounts);

                return metric;
            });
        }
        TargetedMSService.setInstance(new TargetedMSServiceImpl());
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new TargetedMSPipelineProvider(this));

        ExperimentService.get().registerExperimentDataHandler(new TargetedMSDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new SkylineBinaryDataHandler());

        ExperimentService.get().registerExperimentRunTypeSource(container -> {
            if (container == null || container.getActiveModules().contains(TargetedMSModule.this))
            {
                return Collections.singleton(EXP_RUN_TYPE);
            }
            return Collections.emptySet();
        });

        //register the Targeted MS folder type
        FolderTypeManager.get().registerFolderType(this, new TargetedMSFolderType(this));

        ProteinService proteinService = ProteinService.get();
        proteinService.registerProteinSearchView(new TransitionProteinSearchViewProvider());
        proteinService.registerPeptideSearchView(new TransitionPeptideSearchViewProvider());
        proteinService.registerProteinSearchFormView(new ProteinSearchWebPart.ProteinSearchFormViewProvider());

        AuditLogService.get().registerAuditType(new TargetedMsRepresentativeStateAuditProvider());

        TargetedMSListener listener = new TargetedMSListener();
        ContainerManager.addContainerListener(listener);

		ActionURL chromatogramURL = new ActionURL(TargetedMSController.ChromatogramCrawlerAction.class, ContainerManager.getRoot());
        AdminConsole.addLink(AdminConsole.SettingsLinkType.Configuration, "Targeted MS Chromatogram Crawler", chromatogramURL, ApplicationAdminPermission.class);

        FileContentService fcs = FileContentService.get();
        if(null != fcs)
        {
            DirectoryPattern extWatersRaw = new DirectoryPattern(this);
            extWatersRaw.setExt(".*\\.raw$");
            extWatersRaw.setFileExt("^_FUNC.*\\.DAT$");

            DirectoryPattern extAgilentRaw = new DirectoryPattern(this);
            extAgilentRaw.setExt(".*\\.d$");

            DirectoryPattern extAgilentRawSubDir = new DirectoryPattern(this);
            extAgilentRawSubDir.setExt("^AcqData$");
            extAgilentRaw.setSubDirectory(extAgilentRawSubDir);

            DirectoryPattern extBrukerRaw1 = new DirectoryPattern(this);
            extBrukerRaw1.setExt(".*\\.d$");
            extBrukerRaw1.setFileExt("^analysis.baf$");

            DirectoryPattern extBrukerRaw2 = new DirectoryPattern(this);
            extBrukerRaw2.setExt(".*\\.d$");
            extBrukerRaw2.setFileExt("^analysis.tdf$");

            fcs.addZiploaderPattern(extWatersRaw);
            fcs.addZiploaderPattern(extAgilentRaw);
            fcs.addZiploaderPattern(extBrukerRaw1);
            fcs.addZiploaderPattern(extBrukerRaw2);
        }
    }

    @NotNull
    @Override
    public Set<Class> getIntegrationTests()
    {
        return Set.of(
            MsDataSourceUtil.TestCase.class,
            SkylineAuditLogManager.TestCase.class
        );
    }

    @NotNull
    @Override
    public Set<Class> getUnitTests()
    {
        return Set.of(
            ComparisonCategory.TestCase.class,
            ReplicateLabelMinimizer.TestCase.class,
            SampleFile.TestCase.class,
            SkylineAuditLogParser.TestCase.class,
            TargetedMSController.TestCase.class
        );
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new TargetedMSUpgradeCode();
    }

    public static TargetedMSService.FolderType getFolderType(@NotNull Container container)
    {
        TargetedMSModule targetedMSModule = null;
        for (Module m : container.getActiveModules())
        {
            if (m instanceof TargetedMSModule)
            {
                targetedMSModule = (TargetedMSModule) m;
                break;
            }
        }
        if (targetedMSModule != null)
        {
            ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(FOLDER_TYPE_PROP_NAME);
            return TargetedMSService.FolderType.valueOf(moduleProperty.getValueContainerSpecific(container));
        }

        return null;
    }
}
