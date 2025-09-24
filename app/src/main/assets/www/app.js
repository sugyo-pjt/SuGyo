// assets/www/app.js
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { BoneMapping, FINGERS, FINGER_TUNING } from './boneMapping.js';
import { Retargeting } from './retargeting.js';

/* ======================================================
 * 1) Scene / Camera / Lights
 * ==================================================== */
const container = document.getElementById('container');
const canvas   = document.getElementById('c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false });
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
renderer.setClearColor(0x111111, 1); // 어두운 배경으로 투명 착시 차단

// JavaScript 실행 확인
const debugJS = document.getElementById('debugJS');
if (debugJS) {
  debugJS.textContent = 'JS 실행됨!';
  debugJS.style.background = 'green';
}

// WebGL 컨텍스트 확인
console.log('WebGL 렌더러 초기화 완료');
console.log('WebGL 컨텍스트:', renderer.getContext());
console.log('컨테이너 크기:', container.clientWidth, 'x', container.clientHeight);
console.log('캔버스 크기:', canvas.width, 'x', canvas.height);
console.log('캔버스 client 크기:', canvas.clientWidth, 'x', canvas.clientHeight);
console.log('캔버스 offset 크기:', canvas.offsetWidth, 'x', canvas.offsetHeight);

// 사이즈 적용 함수 (확정 후 1~2회만 반영)
function applySizeAndCamera() {
  // 강제로 WebView 크기 사용
  const w = Math.max(container.clientWidth, 800); // 최소 800px
  const h = Math.max(container.clientHeight, 600); // 최소 600px
  
  console.log(`applySizeAndCamera 호출 - 컨테이너 크기: ${container.clientWidth}x${container.clientHeight}`);
  console.log(`강제 크기 적용: ${w}x${h}`);
  
  // 컨테이너 크기 강제 설정
  container.style.width = w + 'px';
  container.style.height = h + 'px';
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';
  
  renderer.setSize(w, h, true); // true: CSS 크기도 함께 조정
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  
  console.log(`렌더러 크기 적용 완료: ${w}x${h}, aspect: ${camera.aspect.toFixed(3)}`);
  
  // 컨테이너 위치 확인 (오프스크린 방지)
  const bbox = container.getBoundingClientRect();
  console.log('컨테이너 bbox:', bbox);
  if (bbox.top < 0 || bbox.left < 0 || bbox.bottom > window.innerHeight || bbox.right > window.innerWidth) {
    console.warn('컨테이너가 화면 밖에 있음!');
    
    // 강제로 컨테이너를 화면 중앙에 배치
    container.style.position = 'fixed';
    container.style.top = '0px';
    container.style.left = '0px';
    container.style.width = '100vw';
    container.style.height = '100vh';
    container.style.zIndex = '1000';
    
    console.log('컨테이너를 화면 중앙에 강제 배치 완료');
  }
  
  // 아바타가 있으면 카메라 피팅
  if (avatar) {
    frameAvatarFullBody(); // 전신 프레이밍으로 통일
  }
}

function fitCameraToAvatar() {
  if (!avatar) return;
  console.log('fitCameraToAvatar 호출');
  
  const box = new THREE.Box3().setFromObject(avatar);
  const size = box.getSize(new THREE.Vector3());
  const center = box.getCenter(new THREE.Vector3());

  const vFov = THREE.MathUtils.degToRad(camera.fov);
  const dist = (size.y * 0.5) / Math.tan(vFov / 2);
  const margin = 1.2;

  const dir = new THREE.Vector3(0, 0, 1); // +Z에서 바라보는 구성
  camera.position.copy(center).addScaledVector(dir, dist * margin);
  camera.lookAt(center);
  
  if (controls) {
    controls.target.copy(center);
    controls.update();
  }
  
  console.log(`카메라 피팅 완료 - 위치: ${camera.position.x.toFixed(3)}, ${camera.position.y.toFixed(3)}, ${camera.position.z.toFixed(3)}`);
}

const scene  = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100); // aspect는 동적으로 갱신

const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.08;
controls.enablePan = true;
controls.panSpeed = 0.5;
controls.rotateSpeed = 0.5;
controls.zoomSpeed = 0.8;

// ↙︎ 여기에 추가
function syncCamRigFromCamera() {
  if (!controls) return;

  const target      = controls.target.clone();
  const camNoOffset = camera.position.clone().sub(camRig.offset); // ★ 오프셋 제거
  const toTarget    = target.clone().sub(camNoOffset);
  const dist        = toTarget.length();
  if (dist === 0) return;

  const d = toTarget.normalize();
  camRig.target.copy(target);
  camRig.dist  = dist;
  camRig.yaw   = THREE.MathUtils.radToDeg(Math.atan2(d.x, d.z));
  camRig.pitch = THREE.MathUtils.radToDeg(Math.asin(d.y));

  // ★ 사용자가 컨트롤을 움직여도 X+100 오프셋을 유지
  placeCameraFromRig({ updateControls: false });
}
controls.addEventListener('change', syncCamRigFromCamera);


// Lights - 조명 강화 및 위치 설정
scene.add(new THREE.AmbientLight(0xffffff, 0.8)); // 환경광 강화
scene.add(new THREE.HemisphereLight(0xffffff, 0x444444, 1.0)); // 반구광 강화

const dir = new THREE.DirectionalLight(0xffffff, 1.2);
dir.position.set(3, 5, 2);
scene.add(dir);

const dir2 = new THREE.DirectionalLight(0xffffff, 0.6);
dir2.position.set(-3, 3, -2); // 두 번째 방향광 위치 설정
scene.add(dir2);

console.log('조명 설정 완료 - Ambient, Hemisphere, Directional x2');

/* ======================================================
 * 2) Avatar / Skeleton
 * ==================================================== */
const loader = new GLTFLoader();
// 텍스처 경로 설정 (텍스처 파일이 models 폴더에 있다고 가정)
// loader.setPath('./models/'); // 모델 로드 시 경로 중복 방지를 위해 주석 처리
let avatar = null, skinned = null, skeleton = null;

// 본매핑 및 리타게팅 인스턴스 생성
const boneMapping = new BoneMapping();
const retargeting = new Retargeting(boneMapping);

/* ======================================================
 * 2-1) 범용 카메라 피팅 함수 (제미나이 제안)
 * ==================================================== */
function fitCameraToObject(camera, object, offset, controls) {
  // 바운딩 박스를 명시적으로 계산
  const box = new THREE.Box3().setFromObject(object);
  const center = box.getCenter(new THREE.Vector3());
  const size = box.getSize(new THREE.Vector3());

  console.log('fitCameraToObject - 바운딩 박스:');
  console.log('  center:', center.x.toFixed(3), center.y.toFixed(3), center.z.toFixed(3));
  console.log('  size:', size.x.toFixed(3), size.y.toFixed(3), size.z.toFixed(3));

  // 모델의 가장 큰 축을 기준으로 카메라 거리 계산
  const maxDim = Math.max(size.x, size.y, size.z);
  const fov = camera.fov * (Math.PI / 180);
  let cameraZ = Math.abs(maxDim / 2 / Math.tan(fov / 2)) * offset;
  
  // 카메라 위치를 계산하여 객체 중앙을 바라보게 함 (앞쪽에서 바라보기)
  camera.position.set(center.x, center.y + size.y * 0.2, center.z - cameraZ);
  camera.lookAt(center);

  // 카메라 near/far 클리핑 평면 설정
  camera.near = Math.max(0.01, cameraZ * 0.01);
  camera.far = Math.max(100, cameraZ * 50);
  camera.updateProjectionMatrix();

  // 컨트롤러가 있다면 업데이트
  if (controls) {
    controls.target.copy(center);
    controls.minDistance = cameraZ * 0.5;
    controls.maxDistance = cameraZ * 5.0;
    controls.update();
  }

  console.log(`fitCameraToObject 완료 - 거리: ${cameraZ.toFixed(3)}, near: ${camera.near}, far: ${camera.far}`);
  return cameraZ;
}

// === 전신 프레이밍(가로/세로 둘 다 꽉 차게) ===
// === 아바타 기준 카메라 리그(orbit/dolly/target/snap) ===
const camRig = {
  target: new THREE.Vector3(),
  dist: 2,
  yaw: 0,
  pitch: -5,
  offset: new THREE.Vector3(100, 100, 0), // ★ X축 +100 오프셋 (월드 기준)
};

// 아바타의 바운딩 박스 기반 타겟 지점 계산 (Y 바이어스로 원하는 높이 조정)
function getAvatarTarget(targetBiasY = 0.55) {
  const box = new THREE.Box3().setFromObject(avatar);
  const size = box.getSize(new THREE.Vector3());
  const center = box.getCenter(new THREE.Vector3());
  return new THREE.Vector3(center.x, box.min.y + size.y * targetBiasY, center.z);
}

// camRig 상태대로 실제 카메라 배치
function placeCameraFromRig({ updateControls = true } = {}) {
  const dir = new THREE.Vector3(0, 0, 1)
    .applyAxisAngle(new THREE.Vector3(1, 0, 0), THREE.MathUtils.degToRad(camRig.pitch))
    .applyAxisAngle(new THREE.Vector3(0, 1, 0), THREE.MathUtils.degToRad(camRig.yaw))
    .normalize();

  camera.position.copy(camRig.target).addScaledVector(dir, -camRig.dist).add(camRig.offset);
  camera.lookAt(camRig.target);

  camera.near = Math.max(0.01, camRig.dist * 0.01);
  camera.far  = Math.max(100,  camRig.dist * 50);
  camera.updateProjectionMatrix();

  if (updateControls && controls) {
    controls.target.copy(camRig.target);
    controls.minDistance = camRig.dist * 0.35;
    controls.maxDistance = camRig.dist * 6.0;
    controls.update();
  }
}

// 전신 프레이밍하면서 camRig 갱신(초기화에도 사용)
function frameAndStoreFullBody({
  margin = 1.12,
  headroom = 0.08,
  targetBiasY = 0.55,
  yawDeg = 0,
  pitchDeg = -5,
  minFar = 100,
} = {}) {
  if (!avatar) return;

  const box  = new THREE.Box3().setFromObject(avatar);
  const size = box.getSize(new THREE.Vector3());
  const vFov = THREE.MathUtils.degToRad(camera.fov);
  const hFov = 2 * Math.atan(Math.tan(vFov / 2) * camera.aspect);

  const halfH = (size.y * (1 + headroom)) * 0.5;
  const halfW = (size.x * 1.05) * 0.5;

  const distV = halfH / Math.tan(vFov / 2);
  const distH = halfW / Math.tan(hFov / 2);
  const dist  = Math.max(distV, distH) * margin;

  camRig.target.copy(getAvatarTarget(targetBiasY));
  camRig.dist  = dist;
  camRig.yaw   = yawDeg;
  camRig.pitch = pitchDeg;

  placeCameraFromRig();
  hasInitialFit = true;
  return dist;
}

// 전역 헬퍼(콘솔/버튼에서 바로 조작)
window.cam = {
  // 전신 프레임 후 camRig 저장(초기화용)
  frame(opts) { return frameAndStoreFullBody(opts); },

  // 타깃 높이만 바꾸고 재배치
  setTargetBiasY(v = 0.55) { camRig.target.copy(getAvatarTarget(v)); placeCameraFromRig(); },

  // 각도 설정(절대/증가치): add=true면 누적, false면 절대값
  orbit({ yaw = 0, pitch = 0, add = true } = {}) {
    if (add) { camRig.yaw += yaw; camRig.pitch += pitch; }
    else { camRig.yaw = yaw; camRig.pitch = pitch; }
    placeCameraFromRig();
  },

  // 거리 직접 설정 or 배율로 돌리(줌)
  setDist(d) { camRig.dist = Math.max(0.05, d); placeCameraFromRig(); },
  dolly(k = 1.1) { camRig.dist = Math.max(0.05, camRig.dist * k); placeCameraFromRig(); },

  // 부드러운 스윕(간단 rAF 트윈)
  sweep({ yawBy = 360, pitchBy = 0, seconds = 3, easing = t => t } = {}) {
    const y0 = camRig.yaw, p0 = camRig.pitch;
    const y1 = y0 + yawBy, p1 = p0 + pitchBy;
    const t0 = performance.now();
    const step = (now) => {
      const t = Math.min(1, (now - t0) / (seconds * 1000));
      const s = easing(t);
      camRig.yaw   = y0 + (y1 - y0) * s;
      camRig.pitch = p0 + (p1 - p0) * s;
      placeCameraFromRig();
      if (t < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  },

  // 스냅샷 찍기 (AndroidBridge 지원 시 브릿지로 전달)
  snap(name = 'shot.png') {
    try {
      const url = renderer.domElement.toDataURL('image/png');
      if (window.AndroidBridge?.onSnapshot) {
        AndroidBridge.onSnapshot(url, name);
      } else {
        const a = document.createElement('a');
        a.href = url; a.download = name; a.click();
      }
    } catch (e) { console.error('snap failed', e); }
  },
};


//function frameAvatarFullBody({
//  margin = 1.12,
//  headroom = 0.08,
//  yawDeg = 0,
//  pitchDeg = -5,
//  targetBiasY = 0.55,
//  minFar = 100,
//} = {}) {
//  if (!avatar) return;
//
//  // 바운딩 박스
//  const box    = new THREE.Box3().setFromObject(avatar);
//  const size   = box.getSize(new THREE.Vector3());
//  const center = box.getCenter(new THREE.Vector3());
//  const target = new THREE.Vector3(center.x, box.min.y + size.y * targetBiasY, center.z);
//
//  // 종횡비 고려 거리
//  const vFov = THREE.MathUtils.degToRad(camera.fov);
//  const hFov = 2 * Math.atan(Math.tan(vFov / 2) * camera.aspect);
//
//  const halfH = (size.y * (1 + headroom)) * 0.5;
//  const halfW = (size.x * 1.05) * 0.5;
//
//  const distV = halfH / Math.tan(vFov / 2);
//  const distH = halfW / Math.tan(hFov / 2);
//  const dist  = Math.max(distV, distH) * margin;
//
//  // 카메라 방향 (yaw/pitch)
//  const dir = new THREE.Vector3(0, 0, 1)
//    .applyAxisAngle(new THREE.Vector3(1, 0, 0), THREE.MathUtils.degToRad(pitchDeg))
//    .applyAxisAngle(new THREE.Vector3(0, 1, 0), THREE.MathUtils.degToRad(yawDeg))
//    .normalize();
//
//  camera.position.copy(target).addScaledVector(dir, -dist);
//  camera.lookAt(target);
//
//  camera.near = Math.max(0.01, dist * 0.01);
//  camera.far  = Math.max(minFar, dist * 50);
//  camera.updateProjectionMatrix();
//
//  if (controls) {
//    controls.target.copy(target);
//    controls.minDistance = dist * 0.35;
//    controls.maxDistance = dist * 6.0;
//    controls.update();
//  }
//
//  hasInitialFit = true;
//  return dist;
//}

// 기존 frameAvatarFullBody 본문을 지우고 아래 한 줄로 교체
function frameAvatarFullBody(opts = {}) {
  return frameAndStoreFullBody(opts);
}


// 디버그용 글로벌 헬퍼
window.frameFull = (opts) => frameAvatarFullBody(opts);


let hasInitialFit = false;
let fitDebounce   = null;

function refitUpperBody(offset = 1.2) {
  if (!avatar) return;
//  console.log('refitUpperBody 호출 - 새로운 fitCameraToObject 사용');
//  const dist = fitCameraToObject(camera, avatar, offset, controls);
//  hasInitialFit = true;
//  return dist;
    console.log('refitUpperBody 호출 - frameAvatarFullBody 사용');
    return frameAvatarFullBody({ margin: 1.12 });
}

/* ======================================================
 * 3) Finger helpers (이제 boneMapping 클래스에서 처리)
 * ==================================================== */

/* ======================================================
 * 4) Debug API
 * ==================================================== */
const deg = THREE.MathUtils.degToRad;
window.debug = {
  skeleton(on = true) {
    if (!skinned) return;
    if (!scene.__skel) { scene.__skel = new THREE.SkeletonHelper(skinned); scene.add(scene.__skel); }
    scene.__skel.visible = !!on;
  },
  finger(side, which, d1 = 40, d2 = 60, d3 = 40, axis, sign) {
    if (axis) FINGER_TUNING[side][which].axis = axis;
    if (sign != null) FINGER_TUNING[side][which].sign = sign;
    boneMapping.applyFingerAngles(side, which, deg(d1), deg(d2), deg(d3));
  },
  handPose: {
    fist(side = 'R', tight = 1.0) {
      const base = {
        index:  [95, 115, 130],
        middle: [100, 120, 135],
        ring:   [95, 115, 130],
        pinky:  [95, 120, 140],
      };
      Object.entries(base).forEach(([f, arr]) => {
        const [mcp, pip, dip] = arr.map(v => v * tight);
        window.debug.finger(side, f, mcp, pip, dip);
      });
      boneMapping.setThumb1Across(side, deg(45 * tight), deg(58 * tight));
      const cfg = FINGER_TUNING[side].thumb;
      boneMapping.setFingerJointRelative(FINGERS[side].thumb[1], deg(58 * tight), cfg.axis, cfg.sign, cfg.clamp);
      boneMapping.setFingerJointRelative(FINGERS[side].thumb[2], deg(48 * tight), cfg.axis, cfg.sign, cfg.clamp);
      boneMapping.setFingerJointRelative(FINGERS[side].wrist, deg(10 * tight), 'x', +1, [-0.6, 0.6], 0.3);
    },
    open(side = 'R') {
      ['thumb','index','middle','ring','pinky'].forEach(f => window.debug.finger(side, f, 0, 0, 0));
//      const w = FINGERS[side].wrist; if (w) boneMapping.bones[w]?.quaternion.copy(boneMapping.restQuat[w]);
      const w = FINGERS[side].wrist;
      const bw = boneMapping.getBone(w);
      const rq = boneMapping.getRest(w);
      if (bw && rq) bw.quaternion.copy(rq);
    }
  },
  bend(boneName, degVal, axis = 'x', sign = +1) { boneMapping.setFingerJointRelative(boneName, deg(degVal), axis, sign); },
  resetPose() { boneMapping.resetPose(); },
  getConfig() { return boneMapping.getConfig(); },
  setConfig(side, finger, patch) { boneMapping.setConfig(side, finger, patch); }
};

/* ======================================================
 * 5) 재생/큐/EMA (UI 버튼)
 * ==================================================== */
const $mode = document.getElementById('mode');
const $qsz  = document.getElementById('qsz');
const $fps  = document.getElementById('fps');
document.getElementById('play')   ?.addEventListener('click', () => { playing = true; lastAdvance = performance.now(); });
document.getElementById('pause')  ?.addEventListener('click', () => { playing = false; });
document.getElementById('speed24')?.addEventListener('click', () => { playbackFps = 24; if ($fps) $fps.textContent = '24'; });
document.getElementById('speed30')?.addEventListener('click', () => { playbackFps = 30; if ($fps) $fps.textContent = '30'; });
document.getElementById('openL')  ?.addEventListener('click', () => window.debug.handPose.open('L'));
document.getElementById('openR')  ?.addEventListener('click', () => window.debug.handPose.open('R'));
document.getElementById('fistL')  ?.addEventListener('click', () => window.debug.handPose.fist('L', 1.1));
document.getElementById('fistR')  ?.addEventListener('click', () => window.debug.handPose.fist('R', 1.1));

let playing     = true;
let playbackFps = 30;
let lastAdvance = performance.now();

// retargetHand 함수는 이제 retargeting 클래스의 메서드로 대체됨

/* ======================================================
 * 6) 손바닥 좌표계 & 각도 계산 (이제 retargeting.js로 이동)
 * ==================================================== */

/* ======================================================
 * 7) Android ↔ JS 브리지
 * ==================================================== */
window.playQueue = [];
window.updateHandFrame = (frame) => {
  if (frame) {
    window.playQueue = [frame];
    if ($qsz)  $qsz.textContent  = String((window.playQueue?.length) || 0);
  }
};
if (window.AndroidBridge?.onReady) { try { AndroidBridge.onReady(); } catch(e){} }

// Android 브리지 함수들
window.AndroidBridge = {
  onReady: () => {
    console.log('JS bridge ready');
  },
  onWebViewReady: (width, height) => {
    console.log('WebView 준비 완료 - 크기:', width, 'x', height);
    // WebView 크기 정보를 받았을 때 강제로 컨테이너 크기 설정
    setTimeout(() => {
      console.log('WebView 크기 정보로 컨테이너 크기 강제 설정');
      
      // 컨테이너와 캔버스 크기를 WebView 크기로 강제 설정
      container.style.width = width + 'px';
      container.style.height = height + 'px';
      canvas.style.width = width + 'px';
      canvas.style.height = height + 'px';
      
      // 렌더러 크기 강제 업데이트
      renderer.setSize(width, height, true);
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      
      console.log(`WebView 크기로 강제 설정 완료: ${width}x${height}`);
      
      // 아바타가 로드되었다면 카메라 피팅도 다시 실행
      if (avatar) {
        console.log('아바타 전신 프레이밍 재실행');
        frameAndStoreFullBody({ margin: 1.15, headroom: 0.10, pitchDeg: -8 });
      }
    }, 100);
  },
  onAvatarLoaded: () => {
    console.log('아바타 모델 로드 완료');
  },
  onRenderStart: () => {
    console.log('3D 렌더링 시작');
  }
};

/* ======================================================
 * 8) 아바타 로드 + 최초 1회 피팅 (레이아웃 안정 후)
 * ==================================================== */
// 모델 로드 시에만 경로 설정
loader.setPath('./models/');
loader.load('avatar.glb', (gltf) => {
  avatar = gltf.scene;
  console.log('아바타 모델 로드 성공');

  // 아바타 스케일 초기화 및 크기 확인
  avatar.scale.set(1, 1, 1);
  console.log('아바타 스케일 초기화: 1, 1, 1');

  // 중앙/바닥 정렬
  const box = new THREE.Box3().setFromObject(avatar);
  const center = box.getCenter(new THREE.Vector3());
  const size = box.getSize(new THREE.Vector3());
  
  console.log('아바타 바운딩 박스 (원본):', {
    center: `${center.x.toFixed(3)}, ${center.y.toFixed(3)}, ${center.z.toFixed(3)}`,
    size: `${size.x.toFixed(3)}, ${size.y.toFixed(3)}, ${size.z.toFixed(3)}`
  });
  
  // 아바타 크기 확인 및 스케일 조정
  const maxSize = Math.max(size.x, size.y, size.z);
  console.log(`아바타 원본 크기: ${maxSize.toFixed(3)}`);
  
  // 아바타가 보이지 않는 문제를 해결하기 위해 더 큰 스케일 적용
  if (maxSize < 1.0) { // 임계값을 1.0으로 올림
    const targetSize = 1.5; // 목표 크기를 1.5로 설정 (적절한 크기)
    const scaleFactor = targetSize / maxSize;
    avatar.scale.setScalar(scaleFactor);
    console.log(`아바타 스케일 조정: ${scaleFactor.toFixed(2)} (원본: ${maxSize.toFixed(3)} → 목표: ${targetSize})`);
    
    // 스케일 조정 후 바운딩 박스 재계산
    box.setFromObject(avatar);
    const newSize = box.getSize(new THREE.Vector3());
    console.log('아바타 바운딩 박스 (스케일 조정 후):');
    console.log('  size:', newSize.x.toFixed(3), newSize.y.toFixed(3), newSize.z.toFixed(3));
  } else {
    console.log(`아바타 크기 적절함: ${maxSize.toFixed(3)} (스케일 조정 불필요)`);
  }
  
  avatar.position.sub(center);
  box.setFromObject(avatar);
  avatar.position.y += -box.min.y;

  scene.add(avatar);
  console.log('아바타를 씬에 추가 완료 - 최종 위치:', 
    `${avatar.position.x.toFixed(3)}, ${avatar.position.y.toFixed(3)}, ${avatar.position.z.toFixed(3)}`);
  
  // 텍스처가 없을 때의 fallback 처리
  avatar.traverse((child) => {
    if (child.isMesh && child.material) {
      console.log(`메시 재질 확인: ${child.name}`);
      console.log(`  - 텍스처 맵: ${child.material.map ? '있음' : '없음'}`);
      console.log(`  - 색상: ${child.material.color ? child.material.color.getHexString() : '없음'}`);
      console.log(`  - 투명도: ${child.material.transparent}, opacity: ${child.material.opacity}`);
      
      // 텍스처가 없는 경우 기본 색상으로 설정
      if (!child.material.map && !child.material.color) {
        console.log(`텍스처 없는 메시 발견: ${child.name}, 기본 색상 적용`);
        child.material.color = new THREE.Color(0x888888); // 회색
        child.material.needsUpdate = true;
      }
      // 투명한 재질인 경우 불투명하게 설정
      if (child.material.transparent || child.material.opacity < 1.0) {
        console.log(`투명한 재질 발견: ${child.name}, 불투명하게 설정`);
        child.material.transparent = false;
        child.material.opacity = 1.0;
        child.material.needsUpdate = true;
      }
      // 색상이 검은색인 경우 밝은 색으로 변경
      if (child.material.color && child.material.color.getHex() === 0x000000) {
        console.log(`검은색 재질 발견: ${child.name}, 밝은 색으로 변경`);
        child.material.color = new THREE.Color(0x666666); // 밝은 회색
        child.material.needsUpdate = true;
      }
    }
  });
  
  // 아바타 추가 직후 즉시 구조 확인
  console.log('=== 아바타 구조 즉시 확인 ===');
  let meshCount = 0;
  let visibleMeshCount = 0;
  avatar.traverse((child) => {
    if (child.isMesh || child.isSkinnedMesh) {
      meshCount++;
      if (child.visible) visibleMeshCount++;
      console.log(`메시: ${child.name || 'unnamed'}, 타입: ${child.type}, visible: ${child.visible}`);
      if (child.material) {
        console.log(`  재질: ${child.material.type}, 투명: ${child.material.transparent}, opacity: ${child.material.opacity}`);
      } else {
        console.log(`  재질: 없음!`);
      }
    }
  });
  console.log(`총 메시: ${meshCount}, 가시 메시: ${visibleMeshCount}`);

  avatar.traverse(o => {
    if (o.isSkinnedMesh) { skinned = o; skeleton = o.skeleton; }
    // 본 정보는 이제 boneMapping 클래스에서 처리
    
    // 메시에 재질이 없거나 투명한 경우 강제로 재질 설정
    if (o.isMesh || o.isSkinnedMesh) {
      if (!o.material || o.material.transparent || o.material.opacity < 1.0) {
        console.log(`메시 ${o.name || 'unnamed'} 재질 문제 감지 - 강제 재질 설정`);
        o.material = new THREE.MeshStandardMaterial({
          color: 0x888888,
          transparent: false,
          opacity: 1.0,
          side: THREE.DoubleSide
        });
        console.log(`메시 ${o.name || 'unnamed'} 재질 교체 완료`);
      }
    }
  });
  
  // 본 정보 추출
  if (skinned) {
    boneMapping.extractBoneInfo(skinned); // 기존 호환
  }
  // ✅ 루트 기준 전체 본을 캐싱(복수 skinnedMesh도 대응)
  boneMapping.bindFrom(avatar);
  
  // 테스트용 큐브 추가 (아바타가 보이지 않는 경우 확인용)
  const testCube = new THREE.Mesh(
    new THREE.BoxGeometry(0.5, 0.5, 0.5),
    new THREE.MeshStandardMaterial({ color: 0xff0000 })
  );
  testCube.position.set(1, 1, 0);
  scene.add(testCube);
  console.log('테스트 큐브 추가됨 (빨간색)');

  // Android 브리지에 알림
  if (window.AndroidBridge?.onAvatarLoaded) {
    try { window.AndroidBridge.onAvatarLoaded(); } catch(e){}
  }
  
  // 아바타 로드 후 사이즈 확정 → 피팅 순서 보장
  requestAnimationFrame(() => {
    applySizeAndCamera(); // 사이즈 먼저
    requestAnimationFrame(() => frameAvatarFullBody({ margin: 1.15, headroom: 0.10, pitchDeg: -8 }));
  });

  // 앱 환경에서 안정적인 피팅을 위한 강화된 대기 로직
  const waitForCanvasSize = () => {
    return new Promise((resolve) => {
      const checkSize = () => {
        const canvas = renderer.domElement;
        const width = canvas.clientWidth;
        const height = canvas.clientHeight;
        
        console.log(`캔버스 크기 확인: ${width}x${height}`);
        
        if (width > 0 && height > 0) {
          console.log('캔버스 크기 유효 - 피팅 진행');
          resolve({ width, height });
        } else {
          console.log('캔버스 크기 무효 - 100ms 후 재확인');
          setTimeout(checkSize, 100);
        }
      };
      checkSize();
    });
  };

  const performCameraFitting = async () => {
    try {
      // 캔버스 크기가 유효해질 때까지 대기
      const { width, height } = await waitForCanvasSize();
      
      console.log(`캔버스 크기 확정됨: ${width}x${height} - 피팅 시작`);
      
      // 렌더러 크기 조정
      if (resizeRendererToDisplaySize(renderer, camera)) {
        // 새로운 fitCameraToObject 함수 사용
//        const dist = fitCameraToObject(camera, avatar, 1.2, controls);
//        hasInitialFit = true;
          const dist = frameAvatarFullBody({ margin: 1.15, headroom: 0.10, pitchDeg: -8 });

        const $loading = document.getElementById('loading');
        if ($loading) $loading.style.display = 'none';
        
        console.log('아바타 초기 피팅 완료 - 최종 캔버스 크기:', 
          renderer.domElement.clientWidth, 'x', renderer.domElement.clientHeight);
        console.log('카메라 위치:', camera.position.x, camera.position.y, camera.position.z);
        console.log('카메라 타겟:', controls.target.x, controls.target.y, controls.target.z);
        console.log('카메라 near/far:', camera.near, camera.far);
        console.log('카메라 fov:', camera.fov);
        
        // 아바타가 카메라 시야에 있는지 확인 (안전한 방법)
        const avatarBox = new THREE.Box3().setFromObject(avatar);
        console.log('아바타 바운딩 박스 (피팅 후):');
        console.log('  min:', avatarBox.min.x.toFixed(3), avatarBox.min.y.toFixed(3), avatarBox.min.z.toFixed(3));
        console.log('  max:', avatarBox.max.x.toFixed(3), avatarBox.max.y.toFixed(3), avatarBox.max.z.toFixed(3));
        
        // 카메라와 아바타 간의 거리 확인
        const distance = camera.position.distanceTo(avatar.position);
        console.log('카메라-아바타 거리:', distance.toFixed(3));
        console.log('카메라 near/far 범위:', camera.near, '~', camera.far);
        
        // 아바타가 렌더링 범위에 있는지 간단히 확인
        const isInRange = distance >= camera.near && distance <= camera.far;
        console.log('아바타가 렌더링 범위에 있음:', isInRange);
        
        // 아바타의 가시성 확인
        const avatarCenter = avatarBox.getCenter(new THREE.Vector3());
        const direction = new THREE.Vector3().subVectors(avatarCenter, camera.position).normalize();
        const cameraDirection = new THREE.Vector3().setFromMatrixColumn(camera.matrix, 2).negate();
        const dot = direction.dot(cameraDirection);
        console.log('아바타가 카메라를 향하고 있음:', dot > 0);
        
        // 아바타 로드 후 렌더링 통계 재확인
        setTimeout(() => {
          const info = renderer.info;
          console.log('=== 아바타 로드 후 렌더링 통계 ===');
          console.log('렌더링된 삼각형 수:', info.render.triangles);
          console.log('씬 객체 수:', scene.children.length);
          console.log('씬 객체들:', scene.children.map(child => child.type || child.constructor.name));
          
          // 카메라와 아바타 위치 상세 확인
          console.log('=== 카메라와 아바타 위치 확인 ===');
          console.log('카메라 위치:', camera.position.x.toFixed(3), camera.position.y.toFixed(3), camera.position.z.toFixed(3));
          console.log('카메라 타겟:', controls.target ? 
            `${controls.target.x.toFixed(3)}, ${controls.target.y.toFixed(3)}, ${controls.target.z.toFixed(3)}` : '없음');
          console.log('아바타 위치:', avatar.position.x.toFixed(3), avatar.position.y.toFixed(3), avatar.position.z.toFixed(3));
          console.log('아바타 스케일:', avatar.scale.x.toFixed(3), avatar.scale.y.toFixed(3), avatar.scale.z.toFixed(3));
          
          // 카메라에서 아바타까지의 거리
          const distance = camera.position.distanceTo(avatar.position);
          console.log('카메라-아바타 거리:', distance.toFixed(3));
          
          // 아바타가 카메라 시야 내에 있는지 확인
          const avatarBox = new THREE.Box3().setFromObject(avatar);
          const avatarCenter = avatarBox.getCenter(new THREE.Vector3());
          const avatarSize = avatarBox.getSize(new THREE.Vector3());
          console.log('아바타 중심:', avatarCenter.x.toFixed(3), avatarCenter.y.toFixed(3), avatarCenter.z.toFixed(3));
          console.log('아바타 크기:', avatarSize.x.toFixed(3), avatarSize.y.toFixed(3), avatarSize.z.toFixed(3));
          
          // 아바타 재질 상태 확인
          console.log('=== 아바타 재질 상태 확인 ===');
          let materialCount = 0;
          let visibleMeshCount = 0;
          avatar.traverse((child) => {
            if (child.isMesh) {
              materialCount++;
              if (child.visible) visibleMeshCount++;
              console.log(`메시 ${materialCount}: ${child.name || 'unnamed'}`);
              console.log(`  - visible: ${child.visible}`);
              console.log(`  - material: ${child.material ? '있음' : '없음'}`);
              if (child.material) {
                console.log(`  - material type: ${child.material.type}`);
                console.log(`  - transparent: ${child.material.transparent}`);
                console.log(`  - opacity: ${child.material.opacity}`);
                if (child.material.color) {
                  console.log(`  - color: #${child.material.color.getHexString()}`);
                }
              }
            }
          });
          console.log(`총 메시 수: ${materialCount}, 보이는 메시 수: ${visibleMeshCount}`);
        }, 1000);
        
      } else {
        console.error('렌더러 크기 조정 실패');
      }
    } catch (error) {
      console.error('카메라 피팅 중 오류:', error);
    }
  };
  
  // DOM이 완전히 로드된 후 2초 대기 후 피팅 시작
  setTimeout(() => {
    console.log('카메라 피팅 프로세스 시작');
    
    // WebView에서 강제로 레이아웃 재계산
    document.body.style.display = 'none';
    document.body.offsetHeight; // 강제 리플로우
    document.body.style.display = '';
    
    performCameraFitting();
  }, 2000);
}, undefined, (e) => {
  console.error('GLB 로드 실패:', e);
  // 텍스처 로딩 오류인 경우 추가 처리
  if (e.message && e.message.includes('texture')) {
    console.log('텍스처 로딩 오류 감지 - fallback 처리');
  }
});

/* ======================================================
 * 9) Resize 안정화 대응 (앱/WebView 필수)
 * ==================================================== */
function resizeRendererToDisplaySize(renderer, camera) {
  const canvas = renderer.domElement;
  const width  = canvas.clientWidth;
  const height = canvas.clientHeight;
  
  // 캔버스 크기가 유효하지 않으면 기본값 사용
  if (width <= 0 || height <= 0) {
    console.warn(`캔버스 크기 무효: ${width}x${height} - 기본값 사용`);
    return false;
  }
  
  if (canvas.width !== width || canvas.height !== height) {
    renderer.setSize(width, height, false);
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    console.log(`렌더러 크기 업데이트: ${width}x${height}, aspect: ${camera.aspect}`);
  }
  return true;
}

function onResize() {
  resizeRendererToDisplaySize(renderer, camera);
  controls.update();
  if (avatar && hasInitialFit) placeCameraFromRig();
}
window.addEventListener('resize', onResize);

// 캔버스 크기 변화를 관찰 → 디바운스 후 1회 재피팅
const ro = new ResizeObserver(() => {
  console.log('ResizeObserver 트리거됨');
  cancelAnimationFrame(applySizeAndCamera._raf);
  applySizeAndCamera._raf = requestAnimationFrame(applySizeAndCamera);
});
ro.observe(container);

// 방향 전환 & DPR 변화도 커버
window.addEventListener('orientationchange', () => {
  setTimeout(() => { 
    resizeRendererToDisplaySize(renderer, camera); 
    if (avatar) placeCameraFromRig();
  }, 220);
});
let lastDPR = window.devicePixelRatio;
setInterval(() => {
  if (window.devicePixelRatio !== lastDPR) {
    lastDPR = window.devicePixelRatio;
    renderer.setPixelRatio(lastDPR || 1);
    resizeRendererToDisplaySize(renderer, camera);
    if (avatar && hasInitialFit) placeCameraFromRig();
  }
}, 900);

/* ======================================================
 * 10) Render Loop
 * ==================================================== */
function render() {
  // 매 프레임 setSize 호출 제거 - ResizeObserver로 대체
  controls.update();

  // 최신 프레임만 적용
  if (window.playQueue.length) {
    const f = window.playQueue[0];
    window.playQueue.length = 0;
    if (avatar) {
      if (Array.isArray(f.left)  && f.left.length  >= 21) retargeting.retargetHand('L', f.left);
      if (Array.isArray(f.right) && f.right.length >= 21) retargeting.retargetHand('R', f.right);
    }
  }

  // 아바타 렌더링 상태 확인 (첫 10프레임만)
  if (avatar && window.renderFrameCount < 10) {
    window.renderFrameCount = (window.renderFrameCount || 0) + 1;
    if (window.renderFrameCount === 1) {
      console.log('렌더링 시작 - 아바타 존재:', !!avatar);
      console.log('씬 객체 수:', scene.children.length);
      console.log('씬 객체들:', scene.children.map(child => child.type || child.constructor.name));
      
      // 아바타의 메시 정보 확인
      let meshCount = 0;
      let skinnedMeshCount = 0;
      avatar.traverse((child) => {
        if (child.isMesh) {
          meshCount++;
          console.log(`메시 발견: ${child.name || 'unnamed'}, visible: ${child.visible}, material: ${child.material ? '있음' : '없음'}`);
          if (child.material) {
            console.log(`  재질 타입: ${child.material.type}, 투명도: ${child.material.transparent}, opacity: ${child.material.opacity}`);
          }
        }
        if (child.isSkinnedMesh) {
          skinnedMeshCount++;
          console.log(`스킨드 메시 발견: ${child.name || 'unnamed'}, visible: ${child.visible}`);
        }
      });
      console.log('아바타 메시 수:', meshCount, '스킨드 메시 수:', skinnedMeshCount);
      
      // 아바타의 전체 구조 확인
      console.log('아바타 전체 구조:');
      avatar.traverse((child) => {
        console.log(`  ${child.type}: ${child.name || 'unnamed'}, visible: ${child.visible}`);
      });
    }
  }

  // 렌더링 통계 확인 (첫 10프레임)
  window.renderFrameCount = (window.renderFrameCount || 0) + 1;
  if (window.renderFrameCount <= 10) {
    const info = renderer.info;
    if (window.renderFrameCount === 1) {
      console.log('=== 렌더링 통계 (첫 프레임) ===');
      console.log('렌더링된 프레임 수:', info.render.frame);
      console.log('렌더링된 삼각형 수:', info.render.triangles);
      console.log('렌더링된 포인트 수:', info.render.points);
      console.log('렌더링된 라인 수:', info.render.lines);
      console.log('씬 객체 수:', scene.children.length);
      console.log('씬 객체들:', scene.children.map(child => child.type || child.constructor.name));
    }
    
    // 매 프레임마다 삼각형 수 확인
    if (info.render.triangles > 0) {
      console.log(`프레임 ${window.renderFrameCount}: 삼각형 ${info.render.triangles}개 렌더링됨`);
    } else if (window.renderFrameCount <= 5) {
      console.log(`프레임 ${window.renderFrameCount}: 렌더링된 삼각형 없음`);
    }
  }

  renderer.render(scene, camera);
  
  // WebView 렌더링 강제 갱신 (첫 10프레임)
  if (window.renderFrameCount <= 10) {
    // 강제로 DOM 갱신
    document.body.style.display = 'none';
    document.body.offsetHeight; // 강제 리플로우
    document.body.style.display = '';
    
    // 캔버스 강제 갱신
    canvas.style.transform = 'translateZ(0)';
    canvas.style.willChange = 'transform';
  }
  
  requestAnimationFrame(render);
}

// WebGL 컨텍스트 손실 처리
renderer.domElement.addEventListener('webglcontextlost', (e) => {
  console.warn('WebGL 컨텍스트 손실됨');
  e.preventDefault();
});

renderer.domElement.addEventListener('webglcontextrestored', () => {
  console.log('WebGL 컨텍스트 복원됨');
  // 필요한 경우 씬 재구성
});

// 렌더링 시작 알림
if (window.AndroidBridge?.onRenderStart) {
  try { window.AndroidBridge.onRenderStart(); } catch(e){}
}

// ResizeObserver는 위에서 이미 설정됨

// 첫 진입 시 사이즈 적용
requestAnimationFrame(applySizeAndCamera);

// 렌더링 루프 시작
console.log('렌더링 루프 시작!');
render();

/* ======================================================
 * 11) UI 초기값 표기
 * ==================================================== */
if ($mode) $mode.textContent = 'EMBEDDED';
if ($qsz) $qsz.textContent = String((window.playQueue?.length) || 0);
if ($fps)  $fps.textContent  = String(playbackFps);
