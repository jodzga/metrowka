  
  function roundTo9s(x) {
    var xs = x.toString();
    var dotIdx = xs.indexOf(".");
    if (dotIdx == -1)
      return xs;
    for (i = dotIdx + 1; i < xs.length; i++) {
      if (xs.charAt(i) != '9') {
        return xs.substring(0, i + 1);
      }
    }
    return xs;
  }

  function drawIntervalPercentiles(data) {
      $("#chart1").empty();

      var graph = new Rickshaw.Graph({
        element: document.getElementById("chart1"),
        width: 960,
        height: 500,
        stroke: true,
        strokeWidth: 0.5,
        renderer: 'area',
        interpolation: 'linear',
        xScale: d3.scale.linear(),
        yScale: d3.scale.linear(),
        series:[
            { color: 'steelblue',
              data: data.data,
              name: 'Latency'
            }
        ] 
      });
      graph.render();

      var hover = new Rickshaw.Graph.HoverDetail({
        graph: graph,
        xFormatter: function(x) { return "Percentile: " + roundTo9s(data.data[x].pct); },
        yFormatter: getDuration
      });
      
      var xAxis = new Rickshaw.Graph.Axis.X({
        graph: graph,
        tickFormat: function(i) { return roundTo9s(data.data[i].pct); }
      });
      xAxis.render();

      var yAxis = new Rickshaw.Graph.Axis.Y({
        graph: graph,
        tickFormat: getDuration
      });
      yAxis.render();
      
      $("#title1").text("Latency percentiles");
  }

  function drawGaugePercentiles(data) {
    $("#chart1").empty();

    var graph = new Rickshaw.Graph({
      element: document.getElementById("chart1"),
      width: 960,
      height: 500,
      stroke: true,
      strokeWidth: 0.5,
      renderer: 'area',
      interpolation: 'linear',
      xScale: d3.scale.linear(),
      yScale: d3.scale.linear(),
      series:[
          { color: 'steelblue',
            data: data.data,
            name: 'Value'
          }
      ] 
    });
    graph.render();

    var hover = new Rickshaw.Graph.HoverDetail({
      graph: graph,
      xFormatter: function(x) { return "Percentile: " + roundTo9s(data.data[x].pct); }
    });
    
    var xAxis = new Rickshaw.Graph.Axis.X({
      graph: graph,
      tickFormat: function(i) { return roundTo9s(data.data[i].pct); }
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph: graph
    });
    yAxis.render();
    
    $("#title1").text("Value percentiles");
  }

  function drawRatePercentiles(data) {
    $("#chart1").empty();

    var normalizeRate = function(x) {
      return  x / (Math.pow(2, 48) / Math.pow(10, 9));
    };
    
    var round2 = function(x) {
      return Math.round(x * 100) / 100;
    }
    
    var graph = new Rickshaw.Graph({
      element: document.getElementById("chart1"),
      width: 960,
      height: 500,
      stroke: true,
      strokeWidth: 0.5,
      renderer: 'area',
      interpolation: 'linear',
      xScale: d3.scale.linear(),
      yScale: d3.scale.linear(),
      series:[
          { color: 'steelblue',
            data: data.data,
            name: 'Rate'
          }
      ] 
    });
    graph.render();

    var hover = new Rickshaw.Graph.HoverDetail({
      graph: graph,
      xFormatter: function(x) { return "Percentile: " + roundTo9s(data.data[x].pct); },
      yFormatter: function(y) { return round2(normalizeRate(y)) + "/s"; }
    });
    
    var xAxis = new Rickshaw.Graph.Axis.X({
      graph: graph,
      tickFormat: function(i) { return roundTo9s(data.data[i].pct); }
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph: graph,
      tickFormat: function(y) { return round2(normalizeRate(y)) + "/s"; }
    });
    yAxis.render();
    
    $("#title1").text("Rate percentiles");
  }  
  
  function drawPercentiles(name, start, type) {
  $.getJSON(
      "/data/histogramData?name=" + name + "&startTimestamp=" + start
          + "&type=percentiles").done(function(data) {
            
    $.each(data.data, function(i,obj) {
      obj.pct = obj.x;
      obj.x = i;
    });
    
    switch (type) {
    case 'gauge':
      drawGaugePercentiles(data);
      break;
    case 'interval':
      drawIntervalPercentiles(data);
      break;
    case 'rate':
      drawRatePercentiles(data);
      break;
    }
  });
}
