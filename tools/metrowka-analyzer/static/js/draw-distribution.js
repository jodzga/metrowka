  function drawGaugeDistribution(data) {
      $("#chart0").empty();
      $("#slider0").empty();
      $("#timeline0").empty();
      
      $.each(data.data, function(i,obj) {
        var span = obj.hi - obj.lo + 1;
        obj.x = obj.lo + span / 2;
        obj.y = obj.count / span;
      });
      
      var graph = new Rickshaw.Graph({
        element: document.getElementById("chart0"),
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
              name: 'Time'
            }
        ] 
      });
      graph.render();

      var hover = new Rickshaw.Graph.HoverDetail({
        graph: graph,
        xFormatter: function(x) { return "Value: " + x; },
        yFormatter: getDuration
      });
      
      var xAxis = new Rickshaw.Graph.Axis.X({
        graph: graph
      });
      xAxis.render();

      var yAxis = new Rickshaw.Graph.Axis.Y({
        graph: graph,
        tickFormat: getDuration
      });
      yAxis.render();
      
      var annotator = new Rickshaw.Graph.Annotate({
        graph: graph,
        element: document.getElementById('timeline0')
      });
      
      annotator.add(data.avg, "Average: " + data.avg);
      annotator.update();
      
      var slider = new Rickshaw.Graph.RangeSlider({
        graph: graph,
        element: document.getElementById('slider0')
      });
      
      $("#min0").text(data.min);
      $("#max0").text(data.max);
      $("#avg0").text(data.avg);
      $("#stdDev0").text(data.stdDeviation);
      $("#title0").text("Amount of time per value (histogram-like)");
  }

  function drawIntervalDistribution(data) {
      $("#chart0").empty();
      $("#slider0").empty();
      $("#timeline0").empty();

      var minSpan = Number.MAX_VALUE;
      $.each(data.data, function(i,obj) {
        var span = obj.hi - obj.lo + 1;
        if (span < minSpan) {
          minSpan = span;
        }
        var factor = span / minSpan;
        obj.x = obj.lo + span / 2;
        obj.y = obj.count / factor;
      });
      
      var graph = new Rickshaw.Graph({
        element: document.getElementById("chart0"),
        width: 960,
        height: 500,
        stroke: true,
        strokeWidth: 0.5,
        renderer: 'bar',
        xScale: d3.scale.linear(),
        yScale: d3.scale.linear(),
        series:[
            { color: 'steelblue',
              data: data.data,
              name: 'Count'
            }
        ] 
      });
      graph.render();

      var hover = new Rickshaw.Graph.HoverDetail({
        graph: graph,
        xFormatter: function(x) { return "Latency: " + getDuration(x); },
        yFormatter: function(y) { return y; }
      });
      
      var xAxis = new Rickshaw.Graph.Axis.X({
        graph: graph,
        tickFormat: getDuration
      });
      xAxis.render();

      var yAxis = new Rickshaw.Graph.Axis.Y({
        graph: graph
      });
      yAxis.render();
      
      var annotator = new Rickshaw.Graph.Annotate({
        graph: graph,
        element: document.getElementById('timeline0')
      });
      
      annotator.add(data.avg, "Average: " + getDuration(data.avg));
      annotator.update();
      
      var slider = new Rickshaw.Graph.RangeSlider({
        graph: graph,
        element: document.getElementById('slider0')
      });
      
      $("#min0").text(getDuration(data.min));
      $("#max0").text(getDuration(data.max));
      $("#avg0").text(getDuration(data.avg));
      $("#stdDev0").text(getDuration(data.stdDeviation));
      $("#title0").text("Count per latency (bar-like)");
  }

  function drawRateDistribution(data) {
    $("#chart0").empty();
    $("#slider0").empty();
    $("#timeline0").empty();

    $.each(data.data, function(i,obj) {
      var span = obj.hi - obj.lo + 1;
      obj.x = obj.lo + span / 2;
      obj.y = obj.count;
    });
    
    var normalizeRate = function(x) {
      return  x / (Math.pow(2, 48) / Math.pow(10, 9));
    };
    
    var round2 = function(x) {
      return Math.round(x * 100) / 100;
    }
    
    var graph = new Rickshaw.Graph({
      element: document.getElementById("chart0"),
      width: 960,
      height: 500,
      stroke: true,
      strokeWidth: 0.5,
      renderer: 'bar',
      interpolation: 'linear',
      xScale: d3.scale.linear(),
      yScale: d3.scale.linear(),
      series:[
          { color: 'steelblue',
            data: data.data,
            name: 'Time'
          }
      ] 
    });
    graph.render();

    var hover = new Rickshaw.Graph.HoverDetail({
      graph: graph,
      xFormatter: function(y) { return "Rate: " + round2(normalizeRate(y)) + "/s"; },
      yFormatter: getDuration
    });
    
    var xAxis = new Rickshaw.Graph.Axis.X({
      graph: graph,
      tickFormat: function(y) { return round2(normalizeRate(y)) + "/s"; }
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
      graph: graph,
      tickFormat: getDuration
    });
    yAxis.render();
    
    var annotator = new Rickshaw.Graph.Annotate({
      graph: graph,
      element: document.getElementById('timeline0')
    });
    
    annotator.add(data.avg, "Average: " + round2(normalizeRate(data.avg)) + "/s");
    annotator.update();
    
    var slider = new Rickshaw.Graph.RangeSlider({
      graph: graph,
      element: document.getElementById('slider0')
    });
    
    $("#min0").text(round2(normalizeRate(data.min)) + "/s");
    $("#max0").text(round2(normalizeRate(data.max)) + "/s");
    $("#avg0").text(round2(normalizeRate(data.avg)) + "/s");
    $("#stdDev0").text(round2(normalizeRate(data.stdDeviation)) + "/s");
    $("#title0").text("Amount of time per rate (bar-like)");
  }
  
  function drawDistribution(name, start, type) {
  $.getJSON(
      "/data/histogramData?name=" + name + "&startTimestamp=" + start
          + "&type=distribution").done(function(data) {
    switch (type) {
    case 'gauge':
      drawGaugeDistribution(data);
      break;
    case 'interval':
      drawIntervalDistribution(data);
      break;
    case 'rate':
      drawRateDistribution(data);
      break;
    }
  });
}
