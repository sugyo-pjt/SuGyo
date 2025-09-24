// assets/www/boneMapping.js
import * as THREE from 'three';

/* =========================
 * Mixamo 기본 본 이름 매핑
 * ========================= */
export const FINGERS = {
  L: {
    thumb:  ['mixamorigLeftHandThumb1','mixamorigLeftHandThumb2','mixamorigLeftHandThumb3'],
    index:  ['mixamorigLeftHandIndex1','mixamorigLeftHandIndex2','mixamorigLeftHandIndex3'],
    middle: ['mixamorigLeftHandMiddle1','mixamorigLeftHandMiddle2','mixamorigLeftHandMiddle3'],
    ring:   ['mixamorigLeftHandRing1','mixamorigLeftHandRing2','mixamorigLeftHandRing3'],
    pinky:  ['mixamorigLeftHandPinky1','mixamorigLeftHandPinky2','mixamorigLeftHandPinky3'],
    wrist:  'mixamorigLeftHand'
  },
  R: {
    thumb:  ['mixamorigRightHandThumb1','mixamorigRightHandThumb2','mixamorigRightHandThumb3'],
    index:  ['mixamorigRightHandIndex1','mixamorigRightHandIndex2','mixamorigRightHandIndex3'],
    middle: ['mixamorigRightHandMiddle1','mixamorigRightHandMiddle2','mixamorigRightHandMiddle3'],
    ring:   ['mixamorigRightHandRing1','mixamorigRightHandRing2','mixamorigRightHandRing3'],
    pinky:  ['mixamorigRightHandPinky1','mixamorigRightHandPinky2','mixamorigRightHandPinky3'],
    wrist:  'mixamorigRightHand'
  }
};

export const FINGER_TUNING = {
  L: {
    thumb:  { axis:'z', sign:-1, clamp:[-2.0, 2.0] },
    index:  { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
    middle: { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
    ring:   { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
    pinky:  { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
  },
  R: {
    thumb:  { axis:'z', sign:+1, clamp:[-2.0, 2.0] },
    index:  { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
    middle: { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
    ring:   { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
    pinky:  { axis:'x', sign:+1, clamp:[-2.0, 2.0] },
  }
};

/* =========================
 * 내부 상수/임시 객체(재사용)
 * ========================= */
const AXIS_V = Object.freeze({
  x: new THREE.Vector3(1,0,0),
  y: new THREE.Vector3(0,1,0),
  z: new THREE.Vector3(0,0,1),
});
const _tmp = {
  q: new THREE.Quaternion(),
};
const deg = THREE.MathUtils.degToRad;

/* =========================
 * BoneMapping
 *  - 본/REST 쿼터니언 캐시(Map)
 *  - 본 탐색을 avatar root에서 자동
 *  - 손가락/엄지 유틸
 * ========================= */
export class BoneMapping {
  constructor(mapping = FINGERS) {
    this.mapping = mapping;
    this.bones = new Map();   // name -> THREE.Bone
    this.rest  = new Map();   // name -> THREE.Quaternion
    this.skeleton = null;     // 가장 먼저 만난 스켈레톤
  }

  /** 아바타 루트에서 본 자동 바인딩 */
  bindFrom(root) {
    this.bones.clear();
    this.rest.clear();
    this.skeleton = null;

    root.traverse(o => {
      if (o.isSkinnedMesh && !this.skeleton) this.skeleton = o.skeleton;
      if (o.isBone) {
        this.bones.set(o.name, o);
        this.rest.set(o.name, o.quaternion.clone());
      }
    });

    console.log('[BoneMapping] bound bones:', this.bones.size, 'skeleton:', !!this.skeleton);
    return this;
  }

  /** 과거 API 호환용 */
  extractBoneInfo(skinned) {
    if (!skinned?.skeleton) return;
    this.bones.clear(); this.rest.clear();
    skinned.skeleton.bones.forEach(b => {
      this.bones.set(b.name, b);
      this.rest.set(b.name, b.quaternion.clone());
    });
    this.skeleton = skinned.skeleton;
    console.log('[BoneMapping] extractBoneInfo:', this.bones.size);
  }

  getBone(name){ return this.bones.get(name); }
  getRest(name){ return this.rest.get(name); }

  /** 특정 본을 REST 기준으로 상대 회전 */
  setFingerJointRelative(boneName, angleRad, axis='x', sign=+1, clamp=[-1.5,1.5], slerp=0.35) {
    const b  = this.getBone(boneName);
    const rq = this.getRest(boneName);
    if (!b || !rq) return false;
    const a  = THREE.MathUtils.clamp(sign * angleRad, clamp[0], clamp[1]);
    const ax = AXIS_V[axis]; if (!ax) return false;

    _tmp.q.setFromAxisAngle(ax, a);
    const target = rq.clone().multiply(_tmp.q);

    if (slerp >= 1) b.quaternion.copy(target);
    else b.quaternion.slerp(target, slerp);
    return true;
  }

  applyFingerAngles(side, finger, a1, a2, a3) {
    const cfg   = FINGER_TUNING[side][finger];
    const names = this.mapping[side][finger];
    this.setFingerJointRelative(names[0], a1, cfg.axis, cfg.sign, cfg.clamp);
    this.setFingerJointRelative(names[1], a2, cfg.axis, cfg.sign, cfg.clamp);
    this.setFingerJointRelative(names[2], a3, cfg.axis, cfg.sign, cfg.clamp);
  }

  setThumb1Across(
    side,
    flexRad = deg(45),
    acrossRad = deg(58),
    flexAxis='z',
    abductAxis='y',
    slerp=0.35
  ) {
    const name = this.mapping[side].thumb[0];
    const b  = this.getBone(name);
    const rq = this.getRest(name);
    if (!b || !rq) return false;

    const flexSign   = (side === 'R') ? +1 : -1;
    const abductSign = (side === 'R') ? -1 : +1;

    const qAbd  = new THREE.Quaternion().setFromAxisAngle(AXIS_V[abductAxis], abductSign * acrossRad);
    const qFlex = new THREE.Quaternion().setFromAxisAngle(AXIS_V[flexAxis],  flexSign   * flexRad);
    const target = rq.clone().multiply(qAbd).multiply(qFlex);

    if (slerp >= 1) b.quaternion.copy(target);
    else b.quaternion.slerp(target, slerp);
    return true;
  }

  resetPose() {
    this.bones.forEach((b, name) => {
      const rq = this.rest.get(name);
      if (rq) b.quaternion.copy(rq);
    });
  }

  getConfig(){ return JSON.parse(JSON.stringify(FINGER_TUNING)); }
  setConfig(side, finger, patch){ Object.assign(FINGER_TUNING[side][finger], patch); }
}
