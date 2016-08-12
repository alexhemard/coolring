page = require('page');

const explore = function(ctx) {
  console.log("exploring");
}

const loadRing = function(ctx, next) {
  console.log("loading");
  next();
}

const rings = function(ctx) {
  //
}

page('/rings', rings);

page('/rings/:id/explore', loadRing, explore);

document.addEventListener("DOMContentLoaded", function() {
  console.log('loaded');
  page({click: false});
});

