// assets/www/utils/BufferGeometryUtils.js
import {
  BufferGeometry, BufferAttribute,
  TriangleStripDrawMode, TriangleFanDrawMode
} from '../lib/three.module.js';

/**
 * Convert non-triangle draw modes (strip/fan) to triangles.
 * Only attributes "position", "normal", "uv", "color", "tangent" 등을 그대로 복사합니다.
 */
export function toTrianglesDrawMode( geometry, drawMode ) {
  if ( drawMode !== TriangleStripDrawMode && drawMode !== TriangleFanDrawMode ) return geometry;

  const index = geometry.getIndex();
  const position = geometry.getAttribute('position');

  const hasIndex = index !== null;
  const idx = hasIndex ? index.array : null;
  const posCount = position.count;

  let triangles;

  if ( drawMode === TriangleStripDrawMode ) {
    // N개의 정점 → N-2개의 삼각형
    const triCount = (hasIndex ? index.count : posCount) - 2;
    triangles = new (posCount > 65535 ? Uint32Array : Uint16Array)(triCount * 3);

    for ( let i = 0; i < triCount; i++ ) {
      const a = hasIndex ? idx[i]     : i;
      const b = hasIndex ? idx[i + 1] : i + 1;
      const c = hasIndex ? idx[i + 2] : i + 2;

      if ( i % 2 === 0 ) {
        triangles[i * 3 + 0] = a;
        triangles[i * 3 + 1] = b;
        triangles[i * 3 + 2] = c;
      } else {
        triangles[i * 3 + 0] = b;
        triangles[i * 3 + 1] = a;
        triangles[i * 3 + 2] = c;
      }
    }
  } else { // TriangleFanDrawMode
    // N개의 정점 → N-2개의 삼각형, (0, i, i+1)
    const triCount = (hasIndex ? index.count : posCount) - 2;
    triangles = new (posCount > 65535 ? Uint32Array : Uint16Array)(triCount * 3);

    const first = hasIndex ? idx[0] : 0;
    for ( let i = 1; i <= triCount; i++ ) {
      const a = first;
      const b = hasIndex ? idx[i]     : i;
      const c = hasIndex ? idx[i + 1] : i + 1;

      triangles[(i - 1) * 3 + 0] = a;
      triangles[(i - 1) * 3 + 1] = b;
      triangles[(i - 1) * 3 + 2] = c;
    }
  }

  const newGeom = new BufferGeometry();
  newGeom.setIndex(new BufferAttribute(triangles, 1));

  // 기존 attribute 그대로 복사
  for ( const name of Object.keys(geometry.attributes) ) {
    newGeom.setAttribute(name, geometry.attributes[name]);
  }
  for ( const name of Object.keys(geometry.morphAttributes) ) {
    newGeom.morphAttributes[name] = geometry.morphAttributes[name];
  }
  newGeom.morphTargetsRelative = geometry.morphTargetsRelative;
  newGeom.copyGroups(geometry);
  newGeom.computeBoundingBox();
  newGeom.computeBoundingSphere();
  return newGeom;
}
