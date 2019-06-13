/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.targetedms.chart;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.targetedms.parser.PeptideSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* User: vsharma
* Date: 7/23/2014
* Time: 3:18 PM
*/
public interface ComparisonCategory
{
    public String getCategoryLabel();

    public String getDisplayLabel();

    public String getSortingLabel();

    public class ReplicateCategory implements ComparisonCategory
    {
        private final String _label;
        private final String _sortingLabel;

        public ReplicateCategory(String label)
        {
            _label = label;
            _sortingLabel = _label;
        }

        public ReplicateCategory(String label, String sortingLabel)
        {
            _label = label;
            _sortingLabel = sortingLabel;
        }

        @Override
        public String getCategoryLabel()
        {
            return _label;
        }

        @Override
        public String getDisplayLabel()
        {
            return getCategoryLabel();
        }

        @Override
        public String getSortingLabel()
        {
            return _sortingLabel;
        }
    }

    public class PeptideCategory implements ComparisonCategory
    {
        private String _modifiedSequence;
        private int _charge;
        private String _isotopeLabel;
        private String _annotationValue;
        private String _sequence;
        private String _seqPrefix;
        private boolean _useChargeInDisplayLabel = true;

        public PeptideCategory(String modifiedSequence, int charge, String isotopeLabel, String annotValue)
        {
            _modifiedSequence = modifiedSequence;
            _sequence = makeSequenceWithLowerCaseMods(modifiedSequence);
            _seqPrefix = _sequence;
            _charge = charge;
            _isotopeLabel = isotopeLabel;
            _annotationValue = annotValue;
        }

        private String makeSequenceWithLowerCaseMods(String modifiedSequence)
        {
            StringBuilder sb = new StringBuilder(modifiedSequence.length());
            int index = 0;
            while (true)
            {
                int modificationIndex = modifiedSequence.indexOf('[', index);
                if (modificationIndex < 0)
                {
                    sb.append(modifiedSequence.substring(index));
                    return sb.toString();
                }
                sb.append(modifiedSequence.substring(index, modificationIndex - 1));
                sb.append(Character.toLowerCase(modifiedSequence.charAt(modificationIndex - 1)));
                index = modifiedSequence.indexOf(']', modificationIndex + 1) + 1;
                if (index == 0)
                    return sb.toString();
            }
        }

        public String getModifiedSequence()
        {
            return _modifiedSequence;
        }

        public String getSequence()
        {
            return _sequence;
        }

        public int getCharge()
        {
            return _charge;
        }

        public String getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public String getCategoryLabel()
        {
            return makeLabel(false);
        }

        private String makeLabel(boolean makeSortingLabel)
        {
            StringBuilder label = new StringBuilder();

            if (!makeSortingLabel && hasAnnotationValue())
            {
                label.append(_annotationValue).append(", ");
            }
            label.append(_modifiedSequence);
            if (_charge > 0)
            {
                label.append(LabelFactory.getChargeLabel(_charge, false));
            }
            if (_isotopeLabel != null && !_isotopeLabel.equalsIgnoreCase(PeptideSettings.IsotopeLabel.LIGHT))
            {
                label.append(" (").append(_isotopeLabel).append(")");
            }

            if (makeSortingLabel && hasAnnotationValue())
            {
                label.append(", ").append(_annotationValue);
            }
            return label.toString();
        }

        public boolean hasAnnotationValue()
        {
            return !StringUtils.isBlank(_annotationValue);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PeptideCategory that = (PeptideCategory) o;

            if (_charge != that._charge) return false;
            if (_annotationValue != null ? !_annotationValue.equals(that._annotationValue) : that._annotationValue != null)
                return false;
            if (_isotopeLabel != null ? !_isotopeLabel.equals(that._isotopeLabel) : that._isotopeLabel != null)
                return false;
            if (!_modifiedSequence.equals(that._modifiedSequence)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _modifiedSequence.hashCode();
            result = 31 * result + _charge;
            result = 31 * result + (_isotopeLabel != null ? _isotopeLabel.hashCode() : 0);
            result = 31 * result + (_annotationValue != null ? _annotationValue.hashCode() : 0);
            return result;
        }

        public void setUseChargeInDisplayLabel(boolean useChargeInDisplayLabel)
        {
            _useChargeInDisplayLabel = useChargeInDisplayLabel;
        }

        public void setSeqPrefix(String seqPrefix)
        {
            _seqPrefix = seqPrefix;
        }

        @Override
        public String getDisplayLabel()
        {
            StringBuilder label = new StringBuilder();

            if (hasAnnotationValue())
            {
                label.append(_annotationValue).append(", ");
            }
            label.append(_seqPrefix);
            if (_useChargeInDisplayLabel)
            {
                label.append(LabelFactory.getChargeLabel(_charge, false));
            }
            return label.toString();
        }

        @Override
        public String getSortingLabel()
        {
            return makeLabel(true);
        }

        public static void trimPeptideCategoryLabels(Set<ComparisonCategory.PeptideCategory> peptideCategories)
        {
            Map<String, Set<Integer>> peptideChargeMap = getPeptideChargeMap(peptideCategories);
            for(ComparisonCategory.PeptideCategory pepCategory: peptideCategories)
            {
                if(peptideChargeMap != null && peptideChargeMap.get(getPeptideChargeMapKey(pepCategory)).size() == 1)
                    pepCategory.setUseChargeInDisplayLabel(false);
            }

            makeUniquePrefixes(new ArrayList<>(peptideCategories), 3);
        }

        private static Map<String, Set<Integer>> getPeptideChargeMap(Set<ComparisonCategory.PeptideCategory> peptideCategories)
        {
            Map<String, Set<Integer>> pepChargeMap = new HashMap<>();

            for(ComparisonCategory.PeptideCategory pepCategory: peptideCategories)
            {
                String peptideChargeMapKey = getPeptideChargeMapKey(pepCategory);
                if(peptideChargeMapKey == null)
                    continue;
                Set<Integer> pepChargeStates = pepChargeMap.get(peptideChargeMapKey);
                if(pepChargeStates == null)
                {
                    pepChargeStates = new HashSet<>();
                    pepChargeMap.put(peptideChargeMapKey, pepChargeStates);
                }
                pepChargeStates.add(pepCategory.getCharge());
            }

            return pepChargeMap;
        }

        private static String getPeptideChargeMapKey(ComparisonCategory.PeptideCategory pepCategory)
        {
            if(pepCategory != null)
            {
                return pepCategory.getModifiedSequence() + "_" + pepCategory.getIsotopeLabel();
            }
            return null;
        }

        private static void makeUniquePrefixes(List<PeptideCategory> peptideCategories, int prefixLen)
        {
            if(peptideCategories == null || peptideCategories.size() == 0)
                return;

            if(peptideCategories.size() == 1)
            {
                ComparisonCategory.PeptideCategory category = peptideCategories.get(0);
                String sequence = category.getSequence();
                prefixLen = Math.max(3, prefixLen - 1);
                category.setSeqPrefix(sequence.substring(0, Math.min(prefixLen, sequence.length())));
                return;
            }

            Set<String> uniqSequences = new HashSet<>();
            for(ComparisonCategory.PeptideCategory category: peptideCategories)
            {
                uniqSequences.add(category.getSequence());
            }
            if(uniqSequences.size() == 1)
            {
                prefixLen = Math.max(3, prefixLen - 1);
                prefixLen = Math.min(prefixLen, peptideCategories.get(0).getSequence().length());

                // If all the given categories have the same sequence, set the
                for(ComparisonCategory.PeptideCategory category: peptideCategories)
                {
                    String sequence = category.getSequence();
                    category.setSeqPrefix(sequence.substring(0, prefixLen));
                }
                return;
            }

            Map<String, List<ComparisonCategory.PeptideCategory>> prefixCategoryMap = new HashMap<>();
            for(ComparisonCategory.PeptideCategory category: peptideCategories)
            {
                String sequence = category.getSequence();
                String prefix = category.getSequence().substring(0, Math.min(sequence.length(), prefixLen));
                List<ComparisonCategory.PeptideCategory> categoriesForPrefix = prefixCategoryMap.get(prefix);
                if(categoriesForPrefix == null)
                {
                    categoriesForPrefix = new ArrayList<>();
                    prefixCategoryMap.put(prefix, categoriesForPrefix);

                }
                categoriesForPrefix.add(category);
            }

            for(String prefix: prefixCategoryMap.keySet())
            {
                List<ComparisonCategory.PeptideCategory> categoriesForPrefix = prefixCategoryMap.get(prefix);
                makeUniquePrefixes(categoriesForPrefix, prefixLen + 1);
            }
        }
    }

    public class MoleculeCategory implements ComparisonCategory
    {
        private String _customIonName;
        private int _charge;
        private String _annotationValue;
        private boolean _useChargeInDisplayLabel = true;

        public MoleculeCategory(String customIonName, int charge, String annotValue)
        {
            _customIonName = customIonName;
            _charge = charge;
            _annotationValue = annotValue;
        }

        public String getCustomIonName()
        {
            return _customIonName;
        }

        public int getCharge()
        {
            return _charge;
        }

        public String getAnnotationValue()
        {
            return _annotationValue;
        }

        public void setUseChargeInDisplayLabel(boolean useChargeInDisplayLabel)
        {
            _useChargeInDisplayLabel = useChargeInDisplayLabel;
        }

        public boolean isUseChargeInDisplayLabel()
        {
            return _useChargeInDisplayLabel;
        }

        public String getCategoryLabel()
        {
            return makeLabel(false);
        }

        private String makeLabel(boolean makeSortingLabel)
        {
            StringBuilder label = new StringBuilder();

            if (!makeSortingLabel && hasAnnotationValue())
            {
                label.append(getAnnotationValue()).append(", ");
            }

            label.append(getCustomIonName());

            if (getCharge() > 0)
            {
                label.append(LabelFactory.getChargeLabel(getCharge(), false));
            }

            if (makeSortingLabel && hasAnnotationValue())
            {
                label.append(", ").append(getAnnotationValue());
            }
            return label.toString();
        }

        public boolean hasAnnotationValue()
        {
            return !StringUtils.isBlank(getAnnotationValue());
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MoleculeCategory that = (MoleculeCategory) o;

            if (_charge != that._charge) return false;
            if (_annotationValue != null ? !_annotationValue.equals(that._annotationValue) : that._annotationValue != null)
                return false;
            if (!_customIonName.equals(that._customIonName)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _customIonName.hashCode();
            result = 31 * result + _charge;
            result = 31 * result + (_annotationValue != null ? _annotationValue.hashCode() : 0);
            return result;
        }

        @Override
        public String getDisplayLabel()
        {
            StringBuilder label = new StringBuilder();

            if (hasAnnotationValue())
            {
                label.append(getAnnotationValue()).append(", ");
            }
            label.append(getCustomIonName());
            if (isUseChargeInDisplayLabel())
            {
                label.append(LabelFactory.getChargeLabel(getCharge(), false));
            }
            return label.toString();
        }

        @Override
        public String getSortingLabel()
        {
            return makeLabel(true);
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testTrimPeptideCategoryLabels()
        {
            ComparisonCategory.PeptideCategory category1 = new ComparisonCategory.PeptideCategory("A", 2, "light", null);
            ComparisonCategory.PeptideCategory category2 = new ComparisonCategory.PeptideCategory("AB", 2, "light", null);
            ComparisonCategory.PeptideCategory category3 = new ComparisonCategory.PeptideCategory("ABCXYZ", 2, "light", null);
            ComparisonCategory.PeptideCategory category4 = new ComparisonCategory.PeptideCategory("ABCXYZ", 3, "light", null);
            ComparisonCategory.PeptideCategory category5 = new ComparisonCategory.PeptideCategory("ABCXYZ", 2, "heavy", null);
            ComparisonCategory.PeptideCategory category6 = new ComparisonCategory.PeptideCategory("ABCXYZ", 3, "heavy", null);
            ComparisonCategory.PeptideCategory category7 = new ComparisonCategory.PeptideCategory("ABDAAA", 2, "light", null);
            ComparisonCategory.PeptideCategory category8 = new ComparisonCategory.PeptideCategory("ABDEEEE", 2, "light", null);
            ComparisonCategory.PeptideCategory category9 = new ComparisonCategory.PeptideCategory("ABDFAAA", 2, "light", null);
            ComparisonCategory.PeptideCategory category10 = new ComparisonCategory.PeptideCategory("UVWXYZ", 2, "light", null);
            ComparisonCategory.PeptideCategory category11 = new ComparisonCategory.PeptideCategory("S[+122.0]DKPDM[+16.0]AEIEKFDK", 2, "light", null);
            ComparisonCategory.PeptideCategory category12 = new ComparisonCategory.PeptideCategory("S[+122.0]DKPDMAEIEKFDK", 2, "light", null);


            Set<PeptideCategory> peptideCategoryList = new HashSet<>(1);
            peptideCategoryList.add(category1);
            peptideCategoryList.add(category2);
            peptideCategoryList.add(category3);
            peptideCategoryList.add(category4);
            peptideCategoryList.add(category5);
            peptideCategoryList.add(category6);
            peptideCategoryList.add(category7);
            peptideCategoryList.add(category8);
            peptideCategoryList.add(category9);
            peptideCategoryList.add(category10);
            peptideCategoryList.add(category11);
            peptideCategoryList.add(category12);

            PeptideCategory.trimPeptideCategoryLabels(peptideCategoryList);
            assertTrue(category1.getCategoryLabel().equals("A++"));
            assertTrue(category1.getDisplayLabel().equals("A"));
            assertTrue(category2.getCategoryLabel().equals("AB++"));
            assertTrue(category2.getDisplayLabel().equals("AB"));
            assertTrue(category3.getCategoryLabel().equals("ABCXYZ++"));
            assertTrue(category3.getDisplayLabel().equals("ABC++"));
            assertTrue(category4.getCategoryLabel().equals("ABCXYZ+++"));
            assertTrue(category4.getDisplayLabel().equals("ABC+++"));
            assertTrue(category5.getCategoryLabel().equals("ABCXYZ++ (heavy)"));
            assertTrue(category5.getDisplayLabel().equals("ABC++"));
            assertTrue(category6.getCategoryLabel().equals("ABCXYZ+++ (heavy)"));
            assertTrue(category6.getDisplayLabel().equals("ABC+++"));
            assertTrue(category7.getCategoryLabel().equals("ABDAAA++"));
            assertTrue(category7.getDisplayLabel().equals("ABDA"));
            assertTrue(category8.getCategoryLabel().equals("ABDEEEE++"));
            assertTrue(category8.getDisplayLabel().equals("ABDE"));
            assertTrue(category9.getCategoryLabel().equals("ABDFAAA++"));
            assertTrue(category9.getDisplayLabel().equals("ABDF"));
            assertTrue(category10.getCategoryLabel().equals("UVWXYZ++"));
            assertTrue(category10.getDisplayLabel().equals("UVW"));
            assertTrue(category11.getCategoryLabel().equals("S[+122.0]DKPDM[+16.0]AEIEKFDK++"));
            assertTrue(category11.getDisplayLabel().equals("sDKPDm"));
            assertTrue(category12.getCategoryLabel().equals("S[+122.0]DKPDMAEIEKFDK++"));
            assertTrue(category12.getDisplayLabel().equals("sDKPDM"));
        }
    }
}
