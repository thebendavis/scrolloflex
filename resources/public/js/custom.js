$(document).ready(function(){
    $("#allcontacts").tablesorter({
        widgets: ["filter"],
        widgetOptions : {
            filter_external : '.search',
            filter_defaultFilter: { 0 : '~{query}' },
            filter_columnFilters: false,
            filter_saveFilters : false
        }
    });
});
