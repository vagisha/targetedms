function initAutoComplete(url, renderId, prefetch)
{
    var completionStore = createStore(url, prefetch);
    createInputElement(renderId, completionStore);
}

function createStore(url, prefetch)
{
    var completionStore;

    if(!prefetch) {
        completionStore = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.obj.nonword(['value']),
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            remote: {
                url: url,
                wildcard: '%QUERY',
                transform: function (response) {
                    // console.log(response.completions);
                    return response.completions;
                }
            }
        });
    }
    else
    {
        completionStore = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.obj.nonword(['name']),
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            prefetch: {
                url: url,
                cache: false,
                transform: function (response) {
                    // console.log(response.completions);
                    return response.completions;
                }
            }
        });
    }

    completionStore.initialize();
    return completionStore;
}

function createInputElement(renderId, store)
{
    // renderId = 'scrollable-dropdown-menu';
    $("#" + renderId + " .typeahead").tagsinput(
            {
                typeaheadjs: [
                    {
                        highlight: true,
                        minLength: 3,
                        hint: false
                    },
                    {
                        name: 'completionStore',
                        displayKey: 'name',
                        valueKey: 'value',
                        limit: Infinity,
                        source: store.ttAdapter()
                    }
                    ],
                freeInput: true

    });

    // https://stackoverflow.com/questions/37973713/bootstrap-tagsinput-form-submited-on-press-enter-key
    $("#" + renderId + " .typeahead").tagsinput({
        confirmKeys: [13]
    });

    $("#" + renderId + " input").on('keypress', function(e){
        if (e.keyCode == 13){
            // e.keyCode = 188;
            e.preventDefault();
        };
    });
}