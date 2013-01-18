package org.labkey.targetedms.chromlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:22 AM
 */
public class LibProtein implements ObjectWithId
{
    private int _id;
    private String _name;
    private String _description;
    private String _sequence;

    private List<LibPeptide> _peptides;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        this._id = id;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        this._name = name;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        this._sequence = sequence;
    }

    public void addPeptide(LibPeptide peptide)
    {
        if(_peptides == null)
        {
            _peptides = new ArrayList<LibPeptide>();
        }
        _peptides.add(peptide);
    }

    List<LibPeptide> getPeptides()
    {
        if(_peptides == null)
            return Collections.emptyList();
       else
            return Collections.unmodifiableList(_peptides);
    }
}
