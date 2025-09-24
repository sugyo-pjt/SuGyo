// assets/www/retargeting.js
import * as THREE from 'three';
import { FINGERS, FINGER_TUNING } from './boneMapping.js';

/* ======================================================
 * 리타게팅 관련 클래스
 * ==================================================== */
export class Retargeting {
  constructor(boneMapping) {
    this.boneMapping = boneMapping;
    this.ema = { L: { landmarks: null, alpha: 0.6 }, R: { landmarks: null, alpha: 0.6 } };
  }

  // 손 데이터를 벡터 배열로 변환
  toVecArray(hand) { 
    return hand.map(p => new THREE.Vector3(p.x, p.y, p.z)); 
  }

  // 손바닥 좌표계 계산
  palmBasisFromHand(L) {
    const wrist = L[0], idxMCP = L[5], midMCP = L[9], pkyMCP = L[17];
    const x  = idxMCP.clone().sub(pkyMCP).normalize();
    const y  = midMCP.clone().sub(wrist).normalize();
    const z  = new THREE.Vector3().crossVectors(x, y).normalize();
    const y2 = new THREE.Vector3().crossVectors(z, x).normalize();
    return new THREE.Matrix4().makeBasis(x, y2, z);
  }

  // 각도 계산
  angleAt(B, A, C) {
    const BA = B.clone().sub(A).normalize();
    const CA = C.clone().sub(A).normalize();
    return Math.acos(THREE.MathUtils.clamp(BA.dot(CA), -1, 1));
  }

  // 손 각도 계산
  computeHandAngles(L) {
    const rad = (b, a, c) => this.angleAt(L[b], L[a], L[c]);
    const d = {};
    d.index  = [rad(0,5,6),  rad(5,6,7),  rad(6,7,8)];
    d.middle = [rad(0,9,10), rad(9,10,11), rad(10,11,12)];
    d.ring   = [rad(0,13,14),rad(13,14,15),rad(14,15,16)];
    d.pinky  = [rad(0,17,18),rad(17,18,19),rad(18,19,20)];

    const flex  = rad(1,2,3) + 0.6 * rad(2,3,4);
    const basis = this.palmBasisFromHand(L);
    const xPalm = new THREE.Vector3().setFromMatrixColumn(basis, 0);
    const yPalm = new THREE.Vector3().setFromMatrixColumn(basis, 1);
    const zPalm = new THREE.Vector3().setFromMatrixColumn(basis, 2);
    const thumbVec = L[3].clone().sub(L[2]);
    const proj = thumbVec.clone().sub(zPalm.clone().multiplyScalar(thumbVec.dot(zPalm))).normalize();
    const across = Math.atan2(proj.dot(yPalm), proj.dot(xPalm));
    d.thumb = { flex, across };
    return d;
  }

  // 손목 적용
  applyWrist(side, basis) {
    const wristName = FINGERS[side].wrist;
    const rq = this.boneMapping.restQuat[wristName], b = this.boneMapping.bones[wristName];
    if (!rq || !b) return;
    const qPalm = new THREE.Quaternion().setFromRotationMatrix(basis);
    const qTarget = rq.clone().multiply(qPalm);
    b.quaternion.slerp(qTarget, 0.35);
  }

  // 각도에서 손가락 적용
  applyFingersFromAngles(side, A) {
    const r2d = (r) => THREE.MathUtils.radToDeg(r);
    for (const f of ['index','middle','ring','pinky']) {
      const [mcp, pip, dip] = A[f].map(r2d);
      this.boneMapping.applyFingerAngles(side, f, mcp, pip, dip);
    }
    this.boneMapping.setThumb1Across(side, A.thumb.flex, A.thumb.across, 'z', 'y');
    const cfg = FINGER_TUNING[side].thumb;
    this.boneMapping.setFingerJointRelative(FINGERS[side].thumb[1], A.thumb.flex * 0.9, cfg.axis, cfg.sign, cfg.clamp);
    this.boneMapping.setFingerJointRelative(FINGERS[side].thumb[2], A.thumb.flex * 0.7, cfg.axis, cfg.sign, cfg.clamp);
  }

  // 손 리타게팅 메인 함수
  retargetHand(side, handArr21) {
    const current = this.toVecArray(handArr21);
    const conf = handArr21.map(p => (p.w ?? 1));
    const state = this.ema[side];

    if (!state.landmarks) {
      state.landmarks = current.map(v => v.clone());
    } else {
      for (let i = 0; i < current.length; i++) {
        const a = state.alpha * (0.5 + 0.5 * conf[i]);
        state.landmarks[i].lerp(current[i], a);
      }
    }

    const L = state.landmarks;
    const basis = this.palmBasisFromHand(L);
    this.applyWrist(side, basis);

    const A = this.computeHandAngles(L);
    this.applyFingersFromAngles(side, A);
  }
}
