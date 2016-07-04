/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var getConfig, validate, isProviderRequired, draw, update;

(function() {

    var CHART_LOCATION = '/extensions/chart-templates/';

    /**
     * return the config to be populated in the chart configuration UI
     * @param schema
     */
    getConfig = function(schema) {
        var chartConf = require(CHART_LOCATION + '/scatter-chart/config.json').config;
        /*
         dynamic logic goes here
         */

         var columns = [];

         columns.push("None");
         for(var i=0; i < schema.length; i++) {
            columns.push(schema[i]["fieldName"]);
         }

        for(var i=0; i < chartConf.length; i++) {
            if (chartConf[i]["fieldName"] == "color"
                || chartConf[i]["fieldName"] == "size" ) {
                chartConf[i]["valueSet"] = columns;
            }
        }

        return chartConf;
    };

    /**
     * validate the user inout for the chart configuration
     * @param chartConfig
     */
    validate = function(chartConfig) {
        return true;
    };

    /**
     * TO be used when provider configuration steps need to be skipped
     */
    isProviderRequired = function() {

    }


    /**
     * return the gadget content
     * @param chartConfig
     * @param schema
     * @param data
     */
    draw = function(placeholder, chartConfig, _schema, data) {
        var schema = toVizGrammarSchema(_schema);
        var view = {
            id: "chart-0",
            schema: schema,
            chartConfig: buildChartConfig(chartConfig),
            data: function() {
                if(data) {
                    var result = [];
                    console.log(data);
                    data.forEach(function(item) {
                        var row = [];
                        schema[0].metadata.names.forEach(function(name) {
                            row.push(item[name]);
                        });
                        result.push(row);
                    });
                    console.log(result);
                    wso2gadgets.onDataReady(result);
                }
            }

        };

        try {
            wso2gadgets.init(placeholder, view);
            var view = wso2gadgets.load("chart-0");
        } catch (e) {
            console.error(e);
        }

    };

    /**
     *
     * @param data
     */
    update = function(data) {
        wso2gadgets.onDataReady(data,"append");
    }

    buildChartConfig = function (_chartConfig) {
        var conf = {};
        conf.x = _chartConfig.x;
        conf.charts = [];
        conf.charts[0] = {
            type : "scatter",
            y: _chartConfig.y
        };

        if (_chartConfig.color != "None") {
            conf.charts[0].color = _chartConfig.color;
        }

        if (_chartConfig.size != "None") {
            conf.charts[0].size = _chartConfig.size;
        }

        return conf;
    };


}());
