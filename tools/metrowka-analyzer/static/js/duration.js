var getDuration = function(nanoIn) {
  var nano = Math.round(nanoIn);
  var dur = {};
  var units = [ {
    label : "ns",
    mod : 1000
  }, {
    label : "μs",
    mod : 1000
  }, {
    label : "ms",
    mod : 1000
  }, {
    label : "s",
    mod : 60
  }, {
    label : "m",
    mod : 60
  }, {
    label : "h",
    mod : 24
  }, {
    label : "d",
    mod : 31
  } ];
  // calculate the individual unit values...
  units.forEach(function(u) {
    nano = (nano - (dur[u.label] = (nano % u.mod))) / u.mod;
  });
  units.reverse();
  // convert object to a string representation...
  dur.toString = function() {
    var time = units.map(function(u) {
      if (dur[u.label] == 0) {
        return '';
      } else {
        return dur[u.label] + u.label;
      }
    }).join(' ').trim();
    if (time) {
      return time;
    } else {
      return '0ns';
    }
  };
  return dur;
};
