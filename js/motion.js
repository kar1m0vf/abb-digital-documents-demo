// Prototype reactions read from ABB-web, rather than invented page transitions.
const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
const running = new Map();
const reveals = new WeakMap();

function animate(element, keyframes, options, finish = () => {}) {
  running.get(element)?.cancel();
  running.delete(element);
  if (reducedMotion.matches) { finish(); return; }
  const animation = element.animate(keyframes, options);
  running.set(element, animation);
  animation.onfinish = () => {
    if (running.get(element) !== animation) return;
    running.delete(element);
    finish();
  };
}

reducedMotion.addEventListener('change', () => {
  if (reducedMotion.matches) for (const animation of [...running.values()]) animation.finish();
});

export function clearMotion() {
  for (const animation of running.values()) animation.cancel();
  running.clear();
}

// 157:830 → 157:829: expand the actual content height, 200 ms EASE_OUT.
// Read the current height before cancelling so rapid reversals do not jump.
export function revealEquivalent(element, expanded) {
  const wasAnimating = running.has(element);
  const from = element.hidden ? 0 : element.getBoundingClientRect().height;
  const fromOpacity = element.hidden ? 0 : Number(getComputedStyle(element).opacity);
  const fromMargin = element.hidden ? 0 : parseFloat(getComputedStyle(element).marginTop);
  running.get(element)?.cancel();
  if (!wasAnimating && element.hidden === !expanded) return;
  element.hidden = false;
  element.inert = !expanded;
  const to = expanded ? element.getBoundingClientRect().height : 0;
  const margin = expanded ? parseFloat(getComputedStyle(element).marginTop) : 0;
  reveals.set(element, expanded);
  animate(element, [
    { height: `${from}px`, opacity: fromOpacity, marginTop: `${fromMargin}px`, overflow: 'hidden' },
    { height: `${to}px`, opacity: expanded ? 1 : 0, marginTop: `${margin}px`, overflow: 'hidden' },
  ], { duration: 200, easing: 'ease-out' }, () => { element.hidden = !reveals.get(element); });
}

// Analytic spring response: preserve the physical constants from CUSTOM_SPRING.
function springFrames(duration, stiffness, damping, project) {
  const decay = damping / 2; // All source springs have mass=1, initialVelocity=0.
  const frequency = Math.sqrt(stiffness - decay * decay);
  return Array.from({ length: 121 }, (_, i) => {
    const t = duration / 1000 * i / 120;
    const value = i === 120 ? 1 : 1 - Math.exp(-decay * t) * (Math.cos(frequency * t) + decay / frequency * Math.sin(frequency * t));
    return { offset: i / 120, ...project(value) };
  });
}

// 351:630 ↔ 351:629 use different image layers: Smart Animate crossfades them.
// Figma exposes GENTLE + 1022.093773 ms, but omits its physical constants.
// 100/15 is a gentle spring approximation; see MOTION.md for this limitation.
export function showCardBack(back) {
  const image = document.querySelector('.payment-card-back-face');
  if (!image || image.dataset.back === String(back)) return;
  const from = Number(getComputedStyle(image).opacity);
  image.dataset.back = String(back);
  image.style.opacity = back ? '1' : '0';
  animate(image, springFrames(1022.093773, 100, 15, x => ({ opacity: from + ((back ? 1 : 0) - from) * x })), { duration: 1022.093773, easing: 'linear' });
}

// 419:1126, 433:909, 433:966, 433:1039, 442:1016.
// Each row starts 1200 ms after the previous; its four variant changes
// are separated by 300 ms holds and ~1.24 ms custom springs.
export function animateConfirmation(root) {
  root.querySelectorAll('.timeline > li').forEach((row, index) => {
    const duration = 1.24042586;
    const start = 300 + index * 1200;
    const circle = row.querySelector('.timeline-icon');
    const content = document.createElement('span');
    content.className = 'timeline-mark';
    content.append(...circle.childNodes);
    const counter = document.createElement('span');
    counter.className = 'timeline-number';
    counter.textContent = String(index + 1);
    counter.setAttribute('aria-hidden', 'true');
    circle.append(content, counter);
    const title = row.querySelector('strong');
    const detail = row.querySelector('p');
    const mark = row.querySelector('.timeline-mark');
    const number = row.querySelector('.timeline-number');
    const endBackground = getComputedStyle(circle).backgroundColor;
    const endColor = getComputedStyle(circle).color;
    const rowDuration = 900 + duration * 4;
    animate(circle, [
      { offset: 0, backgroundColor: '#eff1f8', color: '#606063' },
      { offset: duration / rowDuration, backgroundColor: '#0d63e0', color: '#fff' },
      { offset: 1 - duration / rowDuration, backgroundColor: '#0d63e0', color: '#fff' },
      { offset: 1, backgroundColor: endBackground, color: endColor },
    ], { duration: rowDuration, delay: start, fill: 'backwards' });
    animate(title, [{ color: '#d9d9d9' }, { color: '#000' }], { duration, delay: start + 300 + duration, fill: 'backwards' });
    animate(detail, [
      { offset: 0, opacity: 0 },
      { offset: duration / (300 + duration * 2), opacity: .7 },
      { offset: 1 - duration / (300 + duration * 2), opacity: .7 },
      { offset: 1, opacity: 1 },
    ], { duration: 300 + duration * 2, delay: start + 600 + duration * 2, fill: 'backwards' });
    // A pending business status keeps its clock icon; motion does not change data.
    animate(mark, [{ opacity: 0 }, { opacity: 1 }], { duration, delay: start + 900 + duration * 3, fill: 'backwards' });
    animate(number, [{ opacity: 1 }, { opacity: 0 }], { duration, delay: start + 900 + duration * 3, fill: 'backwards' });
  });
}
