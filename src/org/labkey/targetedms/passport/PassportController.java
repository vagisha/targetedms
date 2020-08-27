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
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.reader.Readers;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.model.passport.IFeature;
import org.labkey.targetedms.model.passport.IFile;
import org.labkey.targetedms.model.passport.IKeyword;
import org.labkey.targetedms.model.passport.IPeptide;
import org.labkey.targetedms.model.passport.IProtein;
import org.labkey.targetedms.view.passport.ProteinListView;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.targetedms.TargetedMSManager.getSqlDialect;

public class PassportController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(PassportController.class);
    private static Logger LOG = Logger.getLogger(PassportController.class);

    public PassportController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
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

    @RequiresPermission(ReadPermission.class)
    public class ProteinAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            ViewContext viewContext = getViewContext();
            String accession = viewContext.getRequest().getParameter("accession");
            IProtein protein = getProtein(accession);

            if (null != protein)
            {
                return new JspView<>("/org/labkey/targetedms/view/passport/ProteinWebPart.jsp", protein);
            }
            else
            {
                throw  new NotFoundException("Protein not found for accession: " + accession);
            }
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Passport Protein View");
        }
    }

    private void populateUniprotData(IProtein p)
    {
        String url = "https://www.uniprot.org/uniprot/?query=accession:" + p.getAccession() + "&format=xml";
        List<IFeature> features = new ArrayList<>();
        try
        {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            { // success
                BufferedReader in = Readers.getReader(con.getInputStream());
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine);
                }
                in.close();

                DocumentBuilderFactory dbf =
                        DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(response.toString()));

                Document doc = db.parse(is);
                doc.getFirstChild();
                Element entry = (Element) doc.getFirstChild().getFirstChild();
                NodeList featureElements = entry.getElementsByTagName("feature");
                for (int i = 0; i < featureElements.getLength(); i++)
                {
                    try
                    {
                        Element feature = (Element) featureElements.item(i);
                        IFeature f = new IFeature();
                        f.setType(feature.getAttribute("type"));
                        f.setDescription(feature.getAttribute("description"));
                        Element location = (Element) feature.getElementsByTagName("location").item(0);
                        if (f.isVariation())
                        {
                            if (location.getChildNodes().getLength() == 1)
                            {
                                int loc = Integer.parseInt(((Element) location.getElementsByTagName("position").item(0)).getAttribute("position"));
                                f.setStartIndex(loc);
                                f.setEndIndex(loc);
                                if (feature.getElementsByTagName("original").getLength() == 0 || feature.getElementsByTagName("variation").getLength() == 0)
                                {
                                    continue;
                                }
                                String original = feature.getElementsByTagName("original").item(0).getFirstChild().getNodeValue();
                                String variation = feature.getElementsByTagName("variation").item(0).getFirstChild().getNodeValue();
                                f.setOriginal(original);
                                f.setVariation(variation);
                            }
                            else if (location.getChildNodes().getLength() == 2)
                            {
                                f = getPosition(f, location);
                            }

                        }
                        else
                        {
                            f = getPosition(f, location);
                        }
                        features.add(f);
                    }
                    catch (Exception e)
                    {
                        // we don't really care at the moment but exception is likely if xml is formatted differently than expected or given in the spec which happens sometimes
                        continue;
                    }
                }
                p.setFeatures(features);
            }
            else
            {
                LOG.info("GET request did not work");
            }
        }
        catch (Exception e)
        {
            p.setFeatures(features);
            LOG.error("Error populating uniprot data ", e);
            throw new RuntimeException(e);
        }
    }

    private IFeature getPosition(IFeature f, Element location)
    {
        if (location.getElementsByTagName("begin").getLength() == 1)
        {
            int begin = Integer.parseInt(((Element) location.getElementsByTagName("begin").item(0)).getAttribute("position"));
            int end = Integer.parseInt(((Element) location.getElementsByTagName("end").item(0)).getAttribute("position"));
            f.setStartIndex(begin);
            f.setEndIndex(end);
        }
        else
        {
            int loc = Integer.parseInt(((Element) location.getElementsByTagName("position").item(0)).getAttribute("position"));
            f.setStartIndex(loc);
            f.setEndIndex(loc);
        }
        return f;
    }

    // returns null if no protein found
    @Nullable
    private IProtein getProtein(String accession)
    {
        SQLFragment targetedMSProteinQuery = new SQLFragment();
        targetedMSProteinQuery.append("SELECT ps.seqid as seqid, ps.bestgenename, ps.description, ps.protsequence, ps.length, " +
                "pg.id as pgid, pg.species, pg.preferredname, pg.runid, pg.label, " +
                "r.dataid, r.filename, r.created, r.modified, r.formatversion " +
                "FROM targetedms.peptidegroup pg, targetedms.runs r, prot.sequences ps " +
                "WHERE r.id = pg.runid AND r.container = ? AND ps.seqid = pg.sequenceid " +
                "AND pg.accession = ? ");
        getSqlDialect().limitRows(targetedMSProteinQuery, 1);
        targetedMSProteinQuery.add(getContainer().getId());
        targetedMSProteinQuery.add(accession);
        DbSchema schema = TargetedMSManager.getSchema();
        try
        {
            Map<String, Object> map = new SqlSelector(schema, targetedMSProteinQuery).getMap();

            IProtein p = new IProtein();
            if (map == null)
                return null;
            p.setLabel((String) map.get("label"));
            p.setGene((String) map.get("bestgenename"));
            p.setSpecies((String) map.get("species"));
            p.setPreferredname((String) map.get("preferredname"));
            p.setPepGroupId((Long) map.get("pgid"));
            p.setSequenceId((Integer) map.get("seqid"));
            p.setDescription((String) map.get("description"));
            p.setSequence((String) map.get("protsequence"));
            p.setLength((Integer) map.get("length"));

            IFile f = new IFile();
            f.setFileName((String) map.get("filename"));
            f.setSoftwareVersion((String) map.get("softwareversion"));
            f.setCreatedDate((Date) map.get("created"));
            f.setModifiedDate((Date) map.get("modified"));
            f.setRunId((Long) map.get("runid"));

            p.setFile(f);
            p.setAccession(accession);
            populateProteinKeywords(p);
            populateUniprotData(p);
            populatePeptides(p);
//            populateProjects(p); TODO: project stuff needs more work - will happen next update
            return p;
        }
        catch (Exception e)
        {
            LOG.error("Error populating keywords ", e);
            throw new RuntimeException(e);
        }

    }

    private void populateProteinKeywords(IProtein p)
    {
        String qs = "SELECT kw.keywordid, kw.keyword, kw.category, kc.label " +
                "FROM prot.sequences p, prot.annotations a, prot.identifiers pi, targetedms.keywords kw, targetedms.keywordcategories kc " +
                "WHERE p.seqid = ? AND a.seqid = p.seqid AND pi.identid = a.annotident AND kw.keywordid = pi.identifier AND kc.categoryid = kw.category";
        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), "targetedms");
        SQLFragment keywordQuery = new SQLFragment();
        keywordQuery.append(qs);
        keywordQuery.add(p.getSequenceId());

        SqlSelector sqlSelector = new SqlSelector(schema.getDbSchema(), keywordQuery);
        List<IKeyword> keywords = new ArrayList<>();
        try
        {
            sqlSelector.forEach(prot -> keywords.add(new IKeyword(prot.getString("keywordid"),
                    prot.getString("category"),
                    prot.getString("keyword"),
                    prot.getString("label"))));
        }
        catch (Exception e)
        {
            LOG.error("Error populating keywords ", e);
            p.setKeywords(keywords);
            throw new RuntimeException(e);
        }
        p.setKeywords(keywords);
    }

    private void populatePeptides(IProtein p)
    {
        if (p == null)
            return;

        Map<Long, IPeptide> peptideMap = new HashMap<>();

        UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), "targetedms");
        TableInfo tinfo = schema.getTable("Passport_TotalPrecursorArea");

        SimpleFilter sf = new SimpleFilter();
        sf.addCondition(new BaseColumnInfo("PepGroupId"), p.getPepGroupId());

        try
        {
            new TableSelector(tinfo, sf, null).forEachResults(pep -> {
                long peptideId = pep.getLong("peptideid");
                IPeptide peptide = peptideMap.get(peptideId);
                if (peptide == null)
                {
                    peptide = new IPeptide();
                    peptide.setSequence(pep.getString("peptidesequence"));
                    peptide.setStartIndex(pep.getInt("startindex"));
                    peptide.setEndIndex(pep.getInt("endindex"));
                    peptide.setPanoramaPeptideId(peptideId);
                    peptide.setProteinId(p.getSequenceId());
                }
                long totalArea = pep.getLong("totalarea");
                String replicateName = pep.getString("replicate");
                if ("BeforeIncubation".equals(replicateName))
                {
                    peptide.setBeforeTotalArea(totalArea);
                    peptide.setPrecursorbeforeid(pep.getLong("panoramaprecursorid"));
                    peptide.setBeforeSumArea(pep.getInt("sumarea"));
                }
                else if (replicateName.equals("AfterIncubation"))
                {
                    peptide.setAfterTotalArea(totalArea);
                    peptide.setPrecursorafterid(pep.getLong("panoramaprecursorid"));
                    peptide.setAfterSumArea(pep.getInt("sumarea"));
                }
                peptideMap.put(peptide.getPanoramaPeptideId(), peptide);
            });
        }
        catch (Exception e)
        {
            LOG.error("Error populating peptides ", e);
            p.setPep(Collections.emptyList());
            throw new RuntimeException(e);
        }
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