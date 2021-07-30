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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.targetedms.parser.PeptideSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
* User: vsharma
* Date: 7/23/2014
* Time: 3:18 PM
*/
public interface ComparisonCategory
{
    String getCategoryLabel();

    String getDisplayLabel();

    String getSortingLabel();

    class ReplicateCategory implements ComparisonCategory
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

    class PeptideCategory implements ComparisonCategory
    {
        private final String _modifiedSequence;
        private final int _charge;
        private final String _isotopeLabel;
        private final String _annotationValue;
        private final String _sequence;
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
                if (modificationIndex <= 0)
                {
                    sb.append(modifiedSequence.substring(index));
                    return sb.toString();
                }
                if (index != modificationIndex)
                {
                    sb.append(modifiedSequence, index, modificationIndex - 1);
                    sb.append(Character.toLowerCase(modifiedSequence.charAt(modificationIndex - 1)));
                }
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

        @Override
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
            if (!Objects.equals(_annotationValue, that._annotationValue))
                return false;
            if (!Objects.equals(_isotopeLabel, that._isotopeLabel))
                return false;
            return Objects.equals(_modifiedSequence, that._modifiedSequence);
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
                if(peptideChargeMap.get(getPeptideChargeMapKey(pepCategory)).size() == 1)
                    pepCategory.setUseChargeInDisplayLabel(false);
            }

            makeUniquePrefixes(new ArrayList<>(peptideCategories), 3);
        }

        @NotNull
        private static Map<String, Set<Integer>> getPeptideChargeMap(Set<ComparisonCategory.PeptideCategory> peptideCategories)
        {
            Map<String, Set<Integer>> pepChargeMap = new HashMap<>();

            for(ComparisonCategory.PeptideCategory pepCategory: peptideCategories)
            {
                String peptideChargeMapKey = getPeptideChargeMapKey(pepCategory);
                if(peptideChargeMapKey == null)
                    continue;
                Set<Integer> pepChargeStates = pepChargeMap.computeIfAbsent(peptideChargeMapKey, k -> new HashSet<>());
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
                List<ComparisonCategory.PeptideCategory> categoriesForPrefix = prefixCategoryMap.computeIfAbsent(prefix, k -> new ArrayList<>());
                categoriesForPrefix.add(category);
            }

            for(String prefix: prefixCategoryMap.keySet())
            {
                List<ComparisonCategory.PeptideCategory> categoriesForPrefix = prefixCategoryMap.get(prefix);
                makeUniquePrefixes(categoriesForPrefix, prefixLen + 1);
            }
        }
    }

    class MoleculeCategory implements ComparisonCategory
    {
        private final String _customIonName;
        private final int _charge;
        private final String _annotationValue;
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

        @Override
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
            if (!Objects.equals(_annotationValue, that._annotationValue))
                return false;
            return Objects.equals(_customIonName, that._customIonName);
        }

        @Override
        public int hashCode()
        {
            int result = _customIonName != null ? _customIonName.hashCode() : 0;
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

    class TestCase extends Assert
    {
        @Test
        public void testBadPeptideSequenceParse()
        {
            // Ensure that we pass through bogus peptide strings without exceptions
            ComparisonCategory.PeptideCategory badCategory1 = new ComparisonCategory.PeptideCategory("S[+122.0", 2, "light", null);
            ComparisonCategory.PeptideCategory badCategory2 = new ComparisonCategory.PeptideCategory("[+122.0]S", 2, "light", null);
            ComparisonCategory.PeptideCategory multiMod = new ComparisonCategory.PeptideCategory("S[+122.0][+16.0]", 2, "light", null);
            ComparisonCategory.PeptideCategory multiMod2 = new ComparisonCategory.PeptideCategory("SS[+122.0][+16.0]A[+4.0]B", 2, "light", null);

            assertEquals("s", badCategory1._sequence);
            assertEquals(badCategory2._modifiedSequence, badCategory2._sequence);
            assertEquals("s", multiMod._sequence);
            assertEquals("SsaB", multiMod2._sequence);
        }

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
            assertEquals("A++", category1.getCategoryLabel());
            assertEquals("A", category1.getDisplayLabel());
            assertEquals("AB++", category2.getCategoryLabel());
            assertEquals("AB", category2.getDisplayLabel());
            assertEquals("ABCXYZ++", category3.getCategoryLabel());
            assertEquals("ABC++", category3.getDisplayLabel());
            assertEquals("ABCXYZ+++", category4.getCategoryLabel());
            assertEquals("ABC+++", category4.getDisplayLabel());
            assertEquals("ABCXYZ++ (heavy)", category5.getCategoryLabel());
            assertEquals("ABC++", category5.getDisplayLabel());
            assertEquals("ABCXYZ+++ (heavy)", category6.getCategoryLabel());
            assertEquals("ABC+++", category6.getDisplayLabel());
            assertEquals("ABDAAA++", category7.getCategoryLabel());
            assertEquals("ABDA", category7.getDisplayLabel());
            assertEquals("ABDEEEE++", category8.getCategoryLabel());
            assertEquals("ABDE", category8.getDisplayLabel());
            assertEquals("ABDFAAA++", category9.getCategoryLabel());
            assertEquals("ABDF", category9.getDisplayLabel());
            assertEquals("UVWXYZ++", category10.getCategoryLabel());
            assertEquals("UVW", category10.getDisplayLabel());
            assertEquals("S[+122.0]DKPDM[+16.0]AEIEKFDK++", category11.getCategoryLabel());
            assertEquals("sDKPDm", category11.getDisplayLabel());
            assertEquals("S[+122.0]DKPDMAEIEKFDK++", category12.getCategoryLabel());
            assertEquals("sDKPDM", category12.getDisplayLabel());
        }
    }
}
