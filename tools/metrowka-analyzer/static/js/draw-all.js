  function drawAll(data, selected, type) {
      switch(type) {
        case 'gauge': drawAllGauge(data); break;
        case 'interval': drawAllInterval(data); break;
        case 'rate': drawAllRate(data); break;
      }
  }

  function drawAllInterval(data) {
    $("#chartDistAll").empty();
    $("#sliderDistAll").empty();
    $("#legendDistAll").empty();
    
    //prepare data
    var seriesDataMin = [];
    var seriesDataMax = [];
    var seriesData50pct = [];
    var seriesData90pct = [];
    var seriesData99pct = [];
    var seriesData999pct = [];
    var seriesData9999pct = [];
    
    $.each(data.data, function(i,obj) {
      seriesDataMin.push({
        x: obj.start,
        y: obj.min
      });
      seriesDataMax.push({
        x: obj.start,
        y: obj.max
      });
      seriesData50pct.push({
        x: obj.start,
        y: obj.pct50
      });
      seriesData90pct.push({
        x: obj.start,
        y: obj.pct90
      });
      seriesData99pct.push({
        x: obj.start,
        y: obj.pct99
      });
      seriesData999pct.push({
        x: obj.start,
        y: obj.pct999
      });
      seriesData9999pct.push({
        x: obj.start,
        y: obj.pct9999
      });
    });
    
    var palette = new Rickshaw.Color.Palette( { scheme: 'spectrum2000' } );
    
    var seriesData = [
                      {
                        color: palette.color(),
                        data: seriesDataMax,
                        name: "max"
                      },
                      {
                        color: palette.color(),
                        data: seriesData9999pct,
                        name: "99.99 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData999pct,
                        name: "99.9 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData99pct,
                        name: "99 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData90pct,
                        name: "90 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData50pct,
                        name: "50 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesDataMin,
                        name: "min"
                      }
                      ];
    
    var graph = new Rickshaw.Graph({
      element: document.getElementById("chartDistAll"),
      width: 960,
      height: 500,
      xScale: d3.scale.linear(),
      yScale: d3.scale.linear(),
      renderer: 'line',
      interpolation: 'linear',
      series: seriesData
    });
    graph.render();
    
    var activeDataItem = {};

    var hover = new Rickshaw.Graph.HoverDetail({
      graph: graph,
      xFormatter: function(x) { return "Time: " + d3.time.format("%X")(new Date(x)); },
      yFormatter: getDuration
    });
    
    new Rickshaw.Graph.ClickDetail({
      graph: graph,
      clickHandler: function(value){
        $("#slider").slider("value", value.dataIndex);
      }
    });
    
      var legend = new Rickshaw.Graph.Legend( {
        graph: graph,
        element: document.getElementById('legendDistAll')
      } );

      var shelving = new Rickshaw.Graph.Behavior.Series.Toggle( {
        graph: graph,
        legend: legend
    } );
  
    var highlight = new Rickshaw.Graph.Behavior.Series.Highlight( {
        graph: graph,
        legend: legend
    } );
      
     var xAxis = new Rickshaw.Graph.Axis.X({
      graph: graph,
      tickFormat: function(x) { return d3.time.format("%X")(new Date(x)); }
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph: graph,
      tickFormat: getDuration
    });
    yAxis.render();
    
    var slider = new Rickshaw.Graph.RangeSlider({
      graph: graph,
      element: document.getElementById('sliderDistAll')
    });
    
    $("#minDistAll").text(getDuration(data.min));
    $("#maxDistAll").text(getDuration(data.max));
    $("#avgDistAll").text(getDuration(data.avg));
    $("#stdDevDistAll").text(getDuration(data.stdDeviation));
    $("#50pctDistAll").text(getDuration(data.pct50));
    $("#90pctDistAll").text(getDuration(data.pct90));
    $("#99pctDistAll").text(getDuration(data.pct99));
    $("#999pctDistAll").text(getDuration(data.pct999));
    $("#9999pctDistAll").text(getDuration(data.pct9999));
    $("#99999pctDistAll").text(getDuration(data.pct99999));
    $("#999999pctDistAll").text(getDuration(data.pct999999));
    $("#9999999pctDistAll").text(getDuration(data.pct9999999));
  }

  function drawAllGauge(data) {
    $("#chartDistAll").empty();
    $("#sliderDistAll").empty();
    $("#legendDistAll").empty();
    
    //prepare data
    var seriesDataMin = [];
    var seriesDataMax = [];
    var seriesData50pct = [];
    var seriesData90pct = [];
    var seriesData99pct = [];
    var seriesData999pct = [];
    var seriesData9999pct = [];
    
    $.each(data.data, function(i,obj) {
      seriesDataMin.push({
        x: obj.start,
        y: obj.min
      });
      seriesDataMax.push({
        x: obj.start,
        y: obj.max
      });
      seriesData50pct.push({
        x: obj.start,
        y: obj.pct50
      });
      seriesData90pct.push({
        x: obj.start,
        y: obj.pct90
      });
      seriesData99pct.push({
        x: obj.start,
        y: obj.pct99
      });
      seriesData999pct.push({
        x: obj.start,
        y: obj.pct999
      });
      seriesData9999pct.push({
        x: obj.start,
        y: obj.pct9999
      });
    });
    
    var palette = new Rickshaw.Color.Palette( { scheme: 'spectrum2000' } );
    
    var seriesData = [
                      {
                        color: palette.color(),
                        data: seriesDataMax,
                        name: "max"
                      },
                      {
                        color: palette.color(),
                        data: seriesData9999pct,
                        name: "99.99 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData999pct,
                        name: "99.9 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData99pct,
                        name: "99 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData90pct,
                        name: "90 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData50pct,
                        name: "50 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesDataMin,
                        name: "min"
                      }
                      ];
    
    var graph = new Rickshaw.Graph({
      element: document.getElementById("chartDistAll"),
      width: 960,
      height: 500,
      xScale: d3.scale.linear(),
      yScale: d3.scale.linear(),
      renderer: 'line',
      interpolation: 'linear',
      series: seriesData
    });
    graph.render();
    
    var activeDataItem = {};

    var hover = new Rickshaw.Graph.HoverDetail({
      graph: graph,
      xFormatter: function(x) { return "Time: " + d3.time.format("%X")(new Date(x)); }
    });
    
    new Rickshaw.Graph.ClickDetail({
      graph: graph,
      clickHandler: function(value){
        $("#slider").slider("value", value.dataIndex);
      }
    });
    
      var legend = new Rickshaw.Graph.Legend( {
        graph: graph,
        element: document.getElementById('legendDistAll')
      } );

      var shelving = new Rickshaw.Graph.Behavior.Series.Toggle( {
        graph: graph,
        legend: legend
    } );
  
    var highlight = new Rickshaw.Graph.Behavior.Series.Highlight( {
        graph: graph,
        legend: legend
    } );
      
     var xAxis = new Rickshaw.Graph.Axis.X({
      graph: graph,
      tickFormat: function(x) { return d3.time.format("%X")(new Date(x)); }
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph: graph
    });
    yAxis.render();
    
    var slider = new Rickshaw.Graph.RangeSlider({
      graph: graph,
      element: document.getElementById('sliderDistAll')
    });
    
    $("#minDistAll").text(data.min);
    $("#maxDistAll").text(data.max);
    $("#avgDistAll").text(data.avg);
    $("#stdDevDistAll").text(data.stdDeviation);
    $("#50pctDistAll").text(data.pct50);
    $("#90pctDistAll").text(data.pct90);
    $("#99pctDistAll").text(data.pct99);
    $("#999pctDistAll").text(data.pct999);
    $("#9999pctDistAll").text(data.pct9999);
    $("#99999pctDistAll").text(data.pct99999);
    $("#999999pctDistAll").text(data.pct999999);
    $("#9999999pctDistAll").text(data.pct9999999);
  }

  function drawAllRate(data) {
    $("#chartDistAll").empty();
    $("#sliderDistAll").empty();
    $("#legendDistAll").empty();

    var normalizeRate = function(x) {
      return  x / (Math.pow(2, 48) / Math.pow(10, 9));
    };
    
    var round2 = function(x) {
      return Math.round(x * 100) / 100;
    }
    
    $.each(data.data, function(i,obj) {
      obj.min = normalizeRate(obj.min);
      obj.max = normalizeRate(obj.max);
      obj.pct50 = normalizeRate(obj.pct50);
      obj.pct90 = normalizeRate(obj.pct90);
      obj.pct99 = normalizeRate(obj.pct99);
      obj.pct999 = normalizeRate(obj.pct999);
      obj.pct9999 = normalizeRate(obj.pct9999);
    });
    
    //prepare data
    var seriesDataMin = [];
    var seriesDataMax = [];
    var seriesData50pct = [];
    var seriesData90pct = [];
    var seriesData99pct = [];
    var seriesData999pct = [];
    var seriesData9999pct = [];
    
    $.each(data.data, function(i,obj) {
      seriesDataMin.push({
        x: obj.start,
        y: obj.min
      });
      seriesDataMax.push({
        x: obj.start,
        y: obj.max
      });
      seriesData50pct.push({
        x: obj.start,
        y: obj.pct50
      });
      seriesData90pct.push({
        x: obj.start,
        y: obj.pct90
      });
      seriesData99pct.push({
        x: obj.start,
        y: obj.pct99
      });
      seriesData999pct.push({
        x: obj.start,
        y: obj.pct999
      });
      seriesData9999pct.push({
        x: obj.start,
        y: obj.pct9999
      });
    });
    
    var palette = new Rickshaw.Color.Palette( { scheme: 'spectrum2000' } );
    
    var seriesData = [
                      {
                        color: palette.color(),
                        data: seriesDataMax,
                        name: "max"
                      },
                      {
                        color: palette.color(),
                        data: seriesData9999pct,
                        name: "99.99 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData999pct,
                        name: "99.9 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData99pct,
                        name: "99 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData90pct,
                        name: "90 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesData50pct,
                        name: "50 %ile"
                      },
                      {
                        color: palette.color(),
                        data: seriesDataMin,
                        name: "min"
                      }
                      ];
    
    var graph = new Rickshaw.Graph({
      element: document.getElementById("chartDistAll"),
      width: 960,
      height: 500,
      xScale: d3.scale.linear(),
      yScale: d3.scale.linear(),
      renderer: 'line',
      interpolation: 'linear',
      series: seriesData
    });
    graph.render();
    
    var activeDataItem = {};

    var hover = new Rickshaw.Graph.HoverDetail({
      graph: graph,
      xFormatter: function(x) { return "Time: " + d3.time.format("%X")(new Date(x)); }
    });
    
    new Rickshaw.Graph.ClickDetail({
      graph: graph,
      clickHandler: function(value){
        $("#slider").slider("value", value.dataIndex);
      }
    });
    
      var legend = new Rickshaw.Graph.Legend( {
        graph: graph,
        element: document.getElementById('legendDistAll')
      } );

      var shelving = new Rickshaw.Graph.Behavior.Series.Toggle( {
        graph: graph,
        legend: legend
    } );
  
    var highlight = new Rickshaw.Graph.Behavior.Series.Highlight( {
        graph: graph,
        legend: legend
    } );
      
     var xAxis = new Rickshaw.Graph.Axis.X({
      graph: graph,
      tickFormat: function(x) { return d3.time.format("%X")(new Date(x)); }
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph: graph
    });
    yAxis.render();
    
    var slider = new Rickshaw.Graph.RangeSlider({
      graph: graph,
      element: document.getElementById('sliderDistAll')
    });
    
    $("#minDistAll").text(round2(normalizeRate(data.min)) + "/s");
    $("#maxDistAll").text(round2(normalizeRate(data.max)) + "/s");
    $("#avgDistAll").text(round2(normalizeRate(data.avg)) + "/s");
    $("#stdDevDistAll").text(round2(normalizeRate(data.stdDeviation)) + "/s");
    $("#50pctDistAll").text(round2(normalizeRate(data.pct50)) + "/s");
    $("#90pctDistAll").text(round2(normalizeRate(data.pct90)) + "/s");
    $("#99pctDistAll").text(round2(normalizeRate(data.pct99)) + "/s");
    $("#999pctDistAll").text(round2(normalizeRate(data.pct999)) + "/s");
    $("#9999pctDistAll").text(round2(normalizeRate(data.pct9999)) + "/s");
    $("#99999pctDistAll").text(round2(normalizeRate(data.pct99999)) + "/s");
    $("#999999pctDistAll").text(round2(normalizeRate(data.pct999999)) + "/s");
    $("#9999999pctDistAll").text(round2(normalizeRate(data.pct9999999)) + "/s");
  }
  