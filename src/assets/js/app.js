page = require('page');

const explore = function(ctx) {
  console.log("loading");
  const next   = document.getElementById('next');
  const prev   = document.getElementById('previous');    
  const iframe = document.getElementById('ring-iframe');

  const onClick = function(e) {
    e.preventDefault();

    console.log("sup");
    const target = e.target;
    iframe.src = target.href;
  }

  prev.onclick = onClick;
  next.onclick = onClick;
}

const loadRing = function(ctx, next) {
  console.log("loading");
  next();
}

const rings = function(ctx) {
  //
}

page('/rings', rings);

page('/rings/:id/*', loadRing, explore);

document.addEventListener("DOMContentLoaded", function() {
  console.log('hey');
  page({click: false});
});

