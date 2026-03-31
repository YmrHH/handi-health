"use strict";
function _set(obj, path, value) {
  if (Object(obj) !== obj)
    return obj;
  if (!Array.isArray(path))
    path = path.toString().match(/[^.[\]]+/g) || [];
  path.slice(0, -1).reduce(
    (a, c, i) => (
      // Iterate all of them except the last one
      Object(a[c]) === a[c] ? a[c] : a[c] = Math.abs(path[i + 1]) >> 0 === +path[i + 1] ? [] : {}
    ),
    // No: assign a new plain object
    obj
  )[path[path.length - 1]] = value;
  return obj;
}
exports._set = _set;
