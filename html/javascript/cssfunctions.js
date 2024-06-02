'use strict';

function sbCssToUrl(k, v) {
  if (v) {
    return 'url("' + v + '")';
  } else {
    return '';
  }
}

function sbCssToShadow(k, v) {
  if (v == null || v === '') {
    return '';
  }
  const shadow = '0px 0px 0.2em ' + v;
  return shadow + ', ' + shadow + ', ' + shadow;
}
