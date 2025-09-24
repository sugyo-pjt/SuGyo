// assets/www/boneMapping.js
import * as THREE from 'three';

/* ======================================================
 * 본매핑 관련 상수 및 설정
 * ==================================================== */
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

/* ======================================================
 * 본매핑 클래스
 * ==================================================== */
export class BoneMapping {
  constructor() {
    this.bones = {};      // boneName -> Bone
    this.restQuat = {};   // boneName -> REST local quaternion
  }

  // 스켈레톤에서 본 정보 추출
  extractBoneInfo(skinned) {
    if (!skinned || !skinned.skeleton) return;
    
    this.bones = {};
    this.restQuat = {};
    
    skinned.skeleton.bones.forEach(bone => {
      this.bones[bone.name] = bone;
      this.restQuat[bone.name] = bone.quaternion.clone();
    });
    
    console.log('본 정보 추출 완료:', Object.keys(this.bones).length, '개');
  }

  // 손가락 관절 설정
  setFingerJointRelative(boneName, angleRad, axis = 'x', sign = +1, clamp = [-1.5, 1.5], slerp = 0.35) {
    const b = this.bones[boneName]; 
    if (!b) return;
    
    const rq = this.restQuat[boneName]; 
    if (!rq) return;
    
    const AXIS_V = {
      x: new THREE.Vector3(1, 0, 0),
      y: new THREE.Vector3(0, 1, 0),
      z: new THREE.Vector3(0, 0, 1),
    };
    
    const a = THREE.MathUtils.clamp(sign * angleRad, clamp[0], clamp[1]);
    const qDelta  = new THREE.Quaternion().setFromAxisAngle(AXIS_V[axis], a);
    const qTarget = rq.clone().multiply(qDelta);
    b.quaternion.slerp(qTarget, slerp);
  }

  // 손가락 각도 적용
  applyFingerAngles(side, finger, a1, a2, a3) {
    const cfg = FINGER_TUNING[side][finger];
    const names = FINGERS[side][finger];
    this.setFingerJointRelative(names[0], a1, cfg.axis, cfg.sign, cfg.clamp);
    this.setFingerJointRelative(names[1], a2, cfg.axis, cfg.sign, cfg.clamp);
    this.setFingerJointRelative(names[2], a3, cfg.axis, cfg.sign, cfg.clamp);
  }

  // 엄지 첫 번째 관절 설정 (특별한 처리)
  setThumb1Across(
    side,
    flexRad = THREE.MathUtils.degToRad(45),
    acrossRad = THREE.MathUtils.degToRad(58),
    flexAxis = 'z',
    abductAxis = 'y',
    slerp = 0.35
  ) {
    const name = FINGERS[side].thumb[0];
    const b = this.bones[name], rq = this.restQuat[name];
    if (!b || !rq) return;

    const AXIS_V = {
      x: new THREE.Vector3(1, 0, 0),
      y: new THREE.Vector3(0, 1, 0),
      z: new THREE.Vector3(0, 0, 1),
    };

    const flexSign   = (side === 'R') ? +1 : -1;
    const abductSign = (side === 'R') ? -1 : +1;

    const qAbd   = new THREE.Quaternion().setFromAxisAngle(AXIS_V[abductAxis], abductSign * acrossRad);
    const qFlex  = new THREE.Quaternion().setFromAxisAngle(AXIS_V[flexAxis],  flexSign   * flexRad);
    const qTarget = rq.clone().multiply(qAbd).multiply(qFlex);
    b.quaternion.slerp(qTarget, slerp);
  }

  // 포즈 리셋
  resetPose() {
    Object.keys(this.bones).forEach(n => { 
      const b = this.bones[n], rq = this.restQuat[n]; 
      if (b && rq) b.quaternion.copy(rq); 
    });
  }

  // 설정 가져오기
  getConfig() { 
    return JSON.parse(JSON.stringify(FINGER_TUNING)); 
  }

  // 설정 업데이트
  setConfig(side, finger, patch) { 
    Object.assign(FINGER_TUNING[side][finger], patch); 
  }
}
