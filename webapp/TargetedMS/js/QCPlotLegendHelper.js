/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define("LABKEY.targetedms.QCPlotLegendHelper", {

    peptidePrefixDictionary: {},
    customIonPrefixDictionary: {},
    ellipsis: '\u2026',

    moleculesStartWith: function (molecules, prefix) {
        return molecules.every(function (molecule) {
            return molecule.startsWith(prefix);
        })
    },

    getPrefix: function (name) {
        return name.substr(0, Math.min(name.length, this.minLegendLength));
    },

    getSuffix: function (name) {
        return (name.length > this.minLegendLength) ?
                name.substr(Math.max(this.minLegendLength, name.length - this.minLegendLength)) :
                '';
    },

    shorterOf: function (elided, original) {
        return (elided.length < original.length) ? elided : original;
    },

    addString: function (name, isSequence) {
        var dict = isSequence ? this.peptidePrefixDictionary : this.customIonPrefixDictionary;

        // Add to dictionary of sequences with this prefix.
        var prefix = this.getPrefix(name);
        var prefixMatches = dict[prefix];
        if (!prefixMatches) {
            dict[prefix] = prefixMatches = {};
        }

        // Add to dictionary of sequences with this prefix and suffix.
        var suffix = this.getSuffix(name);
        var prefixSuffixMatches = prefixMatches[suffix];
        if (!prefixSuffixMatches) {
            prefixMatches[suffix] = prefixSuffixMatches = [];
        }

        // Add to list of sequences that have the same prefix and suffix.
        if (prefixSuffixMatches.indexOf(name) === -1) {
            prefixSuffixMatches.push(name);
        }
    },

    stripModifications: function (seq) {
        var index = 0;
        var stripped = '';

        while (true) {
            var modificationIndex = seq.indexOf('[', index);
            if (modificationIndex < 0) {
                stripped += seq.substr(index);
                return stripped;
            }

            stripped += seq.substr(index, modificationIndex - 1 - index);
            stripped += seq.substr(modificationIndex - 1, 1).toLowerCase();
            index = seq.indexOf(']', modificationIndex + 1) + 1;
            if (index == 0)
                return stripped;
        }

    },

    // Create a prefix generator for the given list of sequences
    setupLegendPrefixes: function (items, minLength) {
        this.minLegendLength = minLength;

        // Get all molecules
        var molecules = [];
        for (var item in items) {
            if (items.hasOwnProperty(item) && Ext4.isDefined(items[item].fragment) && items[item].fragment !== '' &&
                    items[item].dataType != 'Peptide') {
                molecules.push(items[item].fragment);
            }
        }

        // Find longest common leading string in non-peptide names
        if (molecules.length > 1) {
            var commonLead = molecules.reduce(function (a, b) {
                return a.length <= b.length ? a : b;
            });
            while (commonLead.length > 0 && !this.moleculesStartWith(molecules, commonLead)) {
                commonLead = commonLead.substr(0, commonLead.length - 1);
            }

            this.commonStartLength = (commonLead.length > minLength) ? commonLead.length : 0; // Very short common leads ok

            // In case of ["foo bar C10", "foo bar C12"] we'd like to just drop "foo bar " and get ["C10", "C12"]
            if (this.commonStartLength > minLength && (commonLead.lastIndexOf(' ') >= (this.commonStartLength - minLength))) {
                this.commonStartLength = commonLead.lastIndexOf(' ') + 1;
            }
        }
        else {
            this.commonStartLength = 0;
        }

        for (item in items) {
            if (items.hasOwnProperty(item) && Ext4.isDefined(items[item]) && Ext4.isDefined(items[item].fragment)) {
                this.addString((items[item].dataType === 'Peptide' ? this.stripModifications(items[item].fragment)
                        : items[item].fragment.substr(this.commonStartLength)), items[item].dataType === 'Peptide');
            }
        }
    },

    // Return a unique prefix for the given identifier (which must have been included in the
    // list of identifiers given in setupLegendPrefixes).
    getUniquePrefix: function (identifier, isPeptide) {
        if (!Ext4.isDefined(identifier))
            return '';

        if (isPeptide) {
            identifier = this.stripModifications(identifier);
        }
        else {
            identifier = identifier.substr(this.commonStartLength);
        }

        // Get sequences that match this prefix, and ones that match both prefix and suffix.
        var prefix = this.getPrefix(identifier);
        var matchingPrefixes = isPeptide ? this.peptidePrefixDictionary[prefix] : this.customIonPrefixDictionary[prefix];
        var suffix = this.getSuffix(identifier);
        var matchingPrefixAndSuffix = matchingPrefixes[suffix];

        // If there is only one sequence with this prefix, return the prefix (unless the identifer is already short enough).
        if (Object.keys(matchingPrefixes).length === 1 && Object.keys(matchingPrefixAndSuffix).length === 1)
            return this.shorterOf((identifier.length > (this.minLegendLength * 2) + 1) ? (prefix + this.ellipsis) : identifier, identifier);

        // If there is only one sequence with this prefix/suffix, return the combo.
        if (matchingPrefixAndSuffix.length === 1)
            return this.shorterOf((identifier.length > (this.minLegendLength * 2) + 1) ? (prefix + this.ellipsis + suffix) : identifier, identifier);

        // If the matching sequences can be differentiated by length, use length specifier.
        var matchingLengthCount = 0;
        Ext4.each(matchingPrefixAndSuffix, function (prefSuf) {
            if (prefSuf.length === identifier.length) {
                matchingLengthCount++;
            }
        }, this);

        if (matchingLengthCount === 1)
            return this.shorterOf(prefix + "(" + (identifier.length - this.minLegendLength) + ")", identifier);

        // Use ellipses to indicate common parts of matching sequences.
        var matches = [];
        Ext4.each(matchingPrefixAndSuffix, function (prefSuf) {
            if (prefSuf !== identifier)
                matches.push(prefSuf);
        }, this);

        var lastDifference = this.minLegendLength;
        for (var i = this.minLegendLength; i < identifier.length; i++) {

            // Remove any matches that don't match the current character of this sequence.
            var matchCount = matches.length;
            for (var j = matchCount - 1; j >= 0; j--) {
                if (matches[j].length <= i || matches[j][i] != identifier[i]) {
                    matches.splice(j, 1);
                }
            }

            // If we found any non-matching sequences, add the non-matching character to
            // this prefix.
            if (matchCount > matches.length) {
                if (lastDifference < i)
                    prefix += ((i > lastDifference + 1) ? this.ellipsis : identifier[lastDifference]);
                lastDifference = i + 1;
                prefix += identifier[i];

                // If there are no remaining matches, we are done.
                if (matches.length === 0)
                    return this.shorterOf((i < identifier.length) ? prefix + this.ellipsis : prefix, identifier);
            }
        }

        // If we got here, then it means that there is something else which matches this identifier's suffix
        // and is longer.  Return either the prefix with the length specifier, or the entire identifier.
        return this.shorterOf(prefix + "(" + identifier.length + ")", identifier);
    },

    getLegendItemText: function(precursorInfo)
    {
        var prefix = this.getUniquePrefix(precursorInfo.fragment, (precursorInfo.dataType == 'Peptide'));
        return prefix + ", " + precursorInfo.mz;
    }
})
;