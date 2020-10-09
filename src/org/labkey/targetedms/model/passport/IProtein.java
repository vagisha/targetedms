package org.labkey.targetedms.model.passport;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class IProtein
{
    public long getPepGroupId()
    {
        return pepGroupId;
    }

    public void setPepGroupId(long pepGroupId)
    {
        this.pepGroupId = pepGroupId;
    }

    private long pepGroupId;
    private long sequenceId;
    private Date modified;
    private String accession;
    private String preferredname;
    private String gene;
    private String species;
    private String label;
    private String description;
    private String sequence;
    List<IPeptide> pep;
    List<IKeyword> keywords;
    List<IFeature> features;
    List<IProject> projects;
    int length;
    IFile file;

    public List<IFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<IFeature> features)
    {
        features.sort(Comparator.comparingInt(IFeature::getStartIndex));
        this.features = features;
    }

    public List<IProject> getProjects()
    {
        return projects;
    }

    public void setProjects(List<IProject> projects)
    {
        this.projects = projects;
    }

    public List<IKeyword> getKeywords()
    {
        return keywords;
    }

    public void setKeywords(List<IKeyword> keywords)
    {
        this.keywords = keywords;
    }


    public List<IPeptide> getPep()
    {
        return pep;
    }

    public void setPep(List<IPeptide> pep)
    {
        this.pep = pep;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public String getSequence()
    {
        return sequence;
    }

    public void setSequence(String sequence)
    {
        this.sequence = sequence;
    }


    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public long getSequenceId()
    {
        return sequenceId;
    }

    public void setSequenceId(long sequenceId)
    {
        this.sequenceId = sequenceId;
    }


    public Date getModified()
    {
        return modified;
    }

    public void setModified(Date modified)
    {
        this.modified = modified;
    }

    public String getAccession()
    {
        return accession;
    }

    public String getName()
    {
        String name = getPreferredname();
        if (getDescription() != null)
        {
            String[] nameSplit = getDescription().split("\\(");
            if (nameSplit.length == 1)
                nameSplit = getDescription().split("OS");
            name = nameSplit[0];
        }
        return name;
    }

    public void setAccession(String accession)
    {
        this.accession = accession;
    }

    public String getPreferredname()
    {
        return preferredname;
    }

    public void setPreferredname(String preferredname)
    {
        this.preferredname = preferredname;
    }

    public String getGene()
    {
        return gene;
    }

    public void setGene(String gene)
    {
        this.gene = gene;
    }

    public String getSpecies()
    {
        return species;
    }

    public void setSpecies(String species)
    {
        this.species = species;
    }

    public IFile getFile()
    {
        return file;
    }

    public void setFile(IFile file)
    {
        this.file = file;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public JSONObject getJSON()
    {
        JSONObject protJSON = new JSONObject();
        protJSON.put("id", getPepGroupId());
        protJSON.put("datecreated", getFile().getCreatedDate());
        protJSON.put("modifiedcreated", getFile().getModifiedDate());
        protJSON.put("panoramarunid", getFile().getRunId());
        protJSON.put("panoramaproteinid", getPepGroupId());
        protJSON.put("accession", getAccession());
        protJSON.put("prefferredname", getName());
        protJSON.put("sequence", getSequence());
        JSONArray pepJSON = new JSONArray();
        List<IPeptide> peps = getPep();
        for (IPeptide pep : peps)
        {
            JSONObject p = new JSONObject();
            p.put("intensity", pep.getIntensity());
            p.put("beforeintensity", pep.getBeforeIntensity());
            p.put("normalizedafterintensity", pep.getAfterIntensity());
            p.put("startindex", pep.getStartIndex());
            p.put("endindex", pep.getEndIndex());
            p.put("sequence", pep.getSequence());
            p.put("panoramapeptideid", pep.getPanoramaPeptideId());
            p.put("panoramaprecursorbeforeid", pep.getPrecursorbeforeid());
            p.put("panoramaprecursorafterid", pep.getPrecursorafterid());
            p.put("panoramaprecursorid", pep.getPrecursorId());
            pepJSON.put(p);
        }
        JSONArray featJSON = new JSONArray();
        if (features != null)
        {
            for (IFeature feature : features)
            {
                JSONObject f = new JSONObject();
                f.put("startindex", feature.getStartIndex());
                f.put("endindex", feature.getEndIndex());
                f.put("type", feature.getType());
                f.put("description", feature.getDescription());
                f.put("original", feature.getOriginal());
                f.put("variation", feature.getVariation());
                featJSON.put(f);
            }
        }
        JSONArray projectsJSON = new JSONArray();
        if (projects != null)
        {
            for (IProject p : projects)
            {
                JSONObject pObj = new JSONObject();
                pObj.put("runId", p.getRunId());
                pObj.put("pepGroupId", p.getPeptideGroupId());
                pObj.put("fileName", p.getFileName());
                pObj.put("container", p.getContainer().getId());
                List<IPeptide> peptides = p.getPeptides();
                Collections.sort(peptides, new Comparator<IPeptide>()
                {
                    @Override
                    public int compare(IPeptide o1, IPeptide o2)
                    {
                        return o1.getStartIndex() - o2.getStartIndex();
                    }
                });
                JSONArray peptidesJSON = new JSONArray();
                for (IPeptide pep : peptides)
                {
                    JSONObject pepObj = new JSONObject();
                    pepObj.put("startindex", pep.getStartIndex());
                    pepObj.put("endindex", pep.getEndIndex());
                    pepObj.put("sequence", pep.getSequence());
                    peptidesJSON.put(pepObj);
                }
                pObj.put("peptides", peptidesJSON);
                projectsJSON.put(pObj);
            }
        }

        protJSON.put("projects", projectsJSON);
        protJSON.put("peptides", pepJSON);
        protJSON.put("features", featJSON);
        return protJSON;
    }

    public List<HtmlString> getProtSeqHTML()
    {
        String[] str = getSequence().split("");
        HtmlString[] htmlStrings = new HtmlString[str.length];
        for (int i = 0; i < str.length; i++)
        {
            htmlStrings[i] = HtmlString.of(str[i]);
        }
        List<HtmlString> groups = new ArrayList<>();
        if (features != null && features.get(features.size() - 1).getEndIndex() < str.length)
        {
            for (int i = 0; i < features.size(); i++)
            {
                IFeature f = features.get(i);
                int index = f.getStartIndex() - 1;
                HtmlString aa = htmlStrings[index];
                htmlStrings[index] = HtmlStringBuilder.of(HtmlString.unsafe("<span class=\"feature-aa feature-")).
                        append(f.type.replaceAll(" ", "")).
                        append(HtmlString.unsafe("\" index=\"" + i + "\">")).
                        append(aa).
                        append(HtmlString.unsafe("</span>")).getHtmlString();
            }
        }
        HtmlStringBuilder currentStr = HtmlStringBuilder.of();
        int counter = 1;
        for (int i = 0; i < str.length; i++)
        {
            currentStr.append(htmlStrings[i]);
            if (counter == 10 || i == str.length - 1)
            {
                groups.add(currentStr.getHtmlString());
                currentStr = HtmlStringBuilder.of();
                counter = 0;
            }
            counter++;
        }
        return groups;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (!IProtein.class.isAssignableFrom(o.getClass()))
        {
            return false;
        }
        final IProtein other = (IProtein) o;
        return this.accession.equals(other.getAccession());
    }

    public int hashCode()
    {
        return accession.hashCode();
    }

}
