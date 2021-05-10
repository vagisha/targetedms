package org.labkey.targetedms.model.passport;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.protein.ProteinFeature;
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
    private Long sequenceId;
    private Date modified;
    private String accession;
    private String preferredname;
    private String gene;
    private String species;
    private String label;
    private String description;
    private String sequence;
    List<IPeptide> pep;
    List<IKeyword> keywords = Collections.emptyList();
    List<ProteinFeature> features = Collections.emptyList();
    IFile file;

    public List<ProteinFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<ProteinFeature> features)
    {
        this.features = features;
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

    public Long getSequenceId()
    {
        return sequenceId;
    }

    public void setSequenceId(Long sequenceId)
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
        return StringUtils.trim(name == null ? getLabel() : name);
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

    public JSONObject getJSON(boolean includeFeatures)
    {
        JSONObject protJSON = new JSONObject();
        protJSON.put("id", getPepGroupId());
        protJSON.put("datecreated", getFile().getCreatedDate());
        protJSON.put("modifiedcreated", getFile().getModifiedDate());
        protJSON.put("panoramarunid", getFile().getRunId());
        protJSON.put("panoramaproteinid", getPepGroupId());
        protJSON.put("accession", getAccession());
        protJSON.put("preferredname", getName());
        protJSON.put("sequence", getSequence());
        JSONArray pepJSON = new JSONArray();
        List<IPeptide> peps = getPep();
        for (IPeptide pep : peps)
        {
            pepJSON.put(pep.toJSON());
        }
        JSONArray featJSON = new JSONArray();
        if (includeFeatures && features != null)
        {
            for (ProteinFeature feature : features)
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

        protJSON.put("peptides", pepJSON);
        protJSON.put("features", featJSON);
        return protJSON;
    }

    public List<HtmlString> getProtSeqHTML()
    {
        if (getSequence() == null)
        {
            return Collections.emptyList();
        }
        String[] str = getSequence().split("");
        HtmlString[] htmlStrings = new HtmlString[str.length];
        for (int i = 0; i < str.length; i++)
        {
            htmlStrings[i] = HtmlString.of(str[i]);
        }
        List<HtmlString> groups = new ArrayList<>();
        if (features != null && !features.isEmpty() && features.get(features.size() - 1).getEndIndex() < str.length)
        {
            for (int i = 0; i < features.size(); i++)
            {
                ProteinFeature f = features.get(i);
                int index = f.getStartIndex() - 1;
                HtmlString aa = htmlStrings[index];
                htmlStrings[index] = HtmlStringBuilder.of(HtmlString.unsafe("<span class=\"feature-aa feature-")).
                        append(f.getType().replaceAll(" ", "")).
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
