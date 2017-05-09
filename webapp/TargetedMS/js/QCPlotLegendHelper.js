Ext4.define("LABKEY.targetedms.QCPlotLegendHelper", {

    peptidePrefixDictionary: {},
    customIonPrefixDictionary: {},

    moleculesStartWith(molecules, prefix) {
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

        var prefix = this.getPrefix(name);
        var prefixMatches = dict[prefix];
        if (!prefixMatches) {
            dict[prefix] = prefixMatches = {};
        }

        var suffix = this.getSuffix(name);
        var prefixSuffixMatches = prefixMatches[suffix];
        if (!prefixSuffixMatches) {
            prefixMatches[suffix] = prefixSuffixMatches = [];
        }

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
            while (commonLead.length > 0 && !moleculesStartWith(molecules, commonLead)) {
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

    getUniquePrefix: function (identifier, isPeptide) {
        if (!Ext4.isDefined(identifier))
            return '';

        if (isPeptide) {
            identifier = this.stripModifications(identifier);
        }
        else {
            identifier = identifier.substr(this.commonStartLength);
        }

        var prefix = this.getPrefix(identifier);
        var matchingPrefixes = isPeptide ? this.peptidePrefixDictionary[prefix] : this.customIonPrefixDictionary[prefix];
        var suffix = this.getSuffix(identifier);
        var matchingPrefixAndSuffix = matchingPrefixes[suffix];

        if (Object.keys(matchingPrefixes).length === 1 && Object.keys(matchingPrefixAndSuffix).length === 1)
            return this.shorterOf((identifier.length > (this.minLegendLength * 2) + 1) ? (prefix + "...") : identifier, identifier);

        if (matchingPrefixAndSuffix.length === 1)
            return this.shorterOf((identifier.length > (this.minLegendLength * 2) + 1) ? (prefix + "..." + suffix) : identifier, identifier);

        var matchingLengthCount = 0;
        Ext4.each(matchingPrefixAndSuffix, function (prefSuf) {
            if (prefSuf.length === identifier.length) {
                matchingLengthCount++;
            }
        }, this);

        if (matchingLengthCount === 1)
            return this.shorterOf("{" + prefix + "}({" + (identifier.length - this.minLegendLength) + "})", identifier);

        var matches = [];
        Ext4.each(matchingPrefixAndSuffix, function (prefSuf) {
            if (prefSuf === identifier)
                matches.push(prefSuf);
        }, this);

        var lastDifference = this.minLegendLength;
        for (var i = this.minLegendLength; i < identifier.length; i++) {

            var matchCount = matches.length;
            for (var j = matchCount - 1; j >= 0; j--) {
                if (matches[j].length <= i || matches[j][i] != identifier[i]) {
                    matches.splice(j, 1);
                }
            }

            if (matchCount > matches.length) {
                if (lastDifference < i)
                    prefix += ((i > lastDifference + 1) ? "..." : identifier[lastDifference]);
                lastDifference = i + 1;
                prefix += identifier[i];

                if (matches.length === 0)
                    return this.shorterOf((i < identifier.length) ? prefix + "..." : prefix, identifier);
            }
        }

        return this.shorterOf("{" + prefix + "}({" + identifier.length + "})", identifier);
    }

})
;