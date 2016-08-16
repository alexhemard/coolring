page = require('page');

const explore = function(ctx) {
  const next     = document.getElementById('next');
  const prev     = document.getElementById('previous');
  const iframe   = document.getElementById('ring-iframe');
  const ring     = ctx.ring;
  const sites    = ctx.ring.sites;
  const current  = sites.find((site) => iframe.getAttribute('data-site-id') == site.id);
  const nextSite = sites.find((site) => site.id === current.next_site);
  const prevSite = sites.find((site) => current.id === site.next_site);

  next.href = nextSite.url;
  next.setAttribute('data-site-id', nextSite.id);
  prev.href = prevSite.url;
  prev.setAttribute('data-site-id', prevSite.id);

  const onClick = function(e) {
    e.stopPropagation();
    e.preventDefault();

    const target  = e.target;
    const current = sites.find((site) => site.id == target.getAttribute('data-site-id'));
    const marquee = document.getElementsByClassName('toolbar-current')[0];
    
    iframe.src = current.url;
    iframe.setAttribute('data-site-id', current.id);

    marquee.innerHTML = current.name;
    
    prev.removeEventListener('click', arguments.callee);
    prev.removeEventListener('touch end', arguments.callee);
    next.removeEventListener('click', arguments.callee);
    next.removeEventListener('touchend', arguments.callee);

    page.show(`/rings/${ring.id}/${current.url}`)
  }

  prev.addEventListener('click', onClick);
  prev.addEventListener('touchend', onClick);
  next.addEventListener('click', onClick);
  next.addEventListener('touchend', onClick);
}

const loadRing = function(ctx, next) {
  const ringId = ctx.params.id;
  const req    = new XMLHttpRequest();

  req.open('GET', `/rings/${ringId}`, true);
  req.setRequestHeader('Accept', 'application/json');
  req.onload = function () {
    if (req.status >= 200 && req.status < 400) {
      const data = JSON.parse(req.responseText);
      ctx.ring = data

      next();
    }
  }
  req.send();
}

page('/rings/:id/*', loadRing, explore);

document.addEventListener("DOMContentLoaded", function() {
  console.log('hey');
  page({click: false});
});
