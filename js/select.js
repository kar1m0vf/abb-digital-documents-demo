import { escapeHtml as esc } from './utils.js';

// Shared, browser-independent rendering for the selects used in both portals.
export function selectMarkup({ id, label, value, options, variant = 'compact', context = '' }) {
  const selected = options.find(option => option.value === value) ?? options[0];
  const optionId = (index) => `${id}-option-${index}`;
  return `<div class="figma-select figma-select--${variant}" data-select-id="${esc(id)}" data-context="${esc(context)}">
    <input type="hidden" name="${esc(id)}" value="${esc(selected.value)}">
    <button id="${esc(id)}" type="button" class="figma-select-trigger ${variant === 'status' ? `status-badge ${esc(selected.value)}` : ''}" role="combobox" aria-label="${esc(label)}" aria-haspopup="listbox" aria-expanded="false" aria-controls="${esc(id)}-options">
      <span class="figma-select-value">${esc(selected.label)}</span><img src="./assets/figma/embassy-chevron.svg" width="12" height="6" alt="" aria-hidden="true">
    </button>
    <div id="${esc(id)}-options" class="figma-select-list" role="listbox" aria-label="${esc(label)}" popover="manual">${options.map((option, index) => `<button id="${esc(optionId(index))}" type="button" class="figma-select-option" role="option" data-value="${esc(option.value)}" aria-selected="${option.value === selected.value}">${esc(option.label)}</button>`).join('')}</div>
  </div>`;
}

let openWrapper = null;
let activeIndex = -1;
let searchText = '';
let searchTimer;
const triggerOf = wrapper => wrapper.querySelector('.figma-select-trigger');
const listOf = wrapper => document.getElementById(`${wrapper.dataset.selectId}-options`);
const optionsOf = wrapper => [...listOf(wrapper).querySelectorAll('[role="option"]')];

function position(wrapper) {
  const trigger = triggerOf(wrapper);
  const list = listOf(wrapper);
  const rect = trigger.getBoundingClientRect();
  const compact = !wrapper.classList.contains('figma-select--embassy');
  const width = Math.min(innerWidth - 16, compact ? Math.max(rect.width, wrapper.classList.contains('figma-select--status') ? 164 : 260) : rect.width);
  list.style.left = `${Math.max(8, Math.min(rect.left, innerWidth - width - 8))}px`;
  list.style.width = `${width}px`;
  const roomBelow = innerHeight - rect.bottom - 8;
  const roomAbove = rect.top - 8;
  const height = list.getBoundingClientRect().height;
  const above = height > roomBelow && roomAbove > roomBelow;
  list.style.top = `${above ? Math.max(8, rect.top - Math.min(height, roomAbove) - 1) : rect.bottom + 1}px`;
  list.style.maxHeight = `${Math.max(90, above ? roomAbove : roomBelow)}px`;
}

function activate(wrapper, index) {
  const options = optionsOf(wrapper);
  activeIndex = Math.max(0, Math.min(options.length - 1, index));
  options.forEach((option, i) => option.classList.toggle('is-active', i === activeIndex));
  triggerOf(wrapper).setAttribute('aria-activedescendant', options[activeIndex].id);
  options[activeIndex].scrollIntoView({ block: 'nearest' });
}

export function closeSelect({ focus = false } = {}) {
  if (!openWrapper) return;
  const wrapper = openWrapper;
  openWrapper = null;
  searchText = '';
  clearTimeout(searchTimer);
  const trigger = triggerOf(wrapper);
  const list = listOf(wrapper);
  trigger.setAttribute('aria-expanded', 'false');
  trigger.removeAttribute('aria-activedescendant');
  if (typeof list.hidePopover === 'function' && list.matches(':popover-open')) list.hidePopover();
  else list.hidden = true;
  if (list.parentElement !== wrapper) wrapper.append(list);
  if (focus) trigger.focus();
}

function openSelect(wrapper) {
  if (openWrapper === wrapper) { closeSelect({ focus: true }); return; }
  closeSelect();
  const trigger = triggerOf(wrapper);
  const list = listOf(wrapper);
  list.hidden = false;
  if (typeof list.showPopover === 'function') list.showPopover();
  else document.body.append(list);
  openWrapper = wrapper;
  trigger.setAttribute('aria-expanded', 'true');
  position(wrapper);
  activeIndex = optionsOf(wrapper).findIndex(option => option.getAttribute('aria-selected') === 'true');
  trigger.setAttribute('aria-activedescendant', optionsOf(wrapper)[activeIndex].id);
}

function choose(wrapper, option) {
  const value = option.dataset.value;
  if (!value) return;
  const oldValue = wrapper.querySelector('input[type="hidden"]').value;
  wrapper.querySelector('input[type="hidden"]').value = value;
  wrapper.querySelector('.figma-select-value').textContent = option.textContent;
  optionsOf(wrapper).forEach(item => item.setAttribute('aria-selected', String(item === option)));
  closeSelect({ focus: true });
  if (oldValue !== value) wrapper.dispatchEvent(new CustomEvent('figma-select-change', { bubbles: true, detail: { id: wrapper.dataset.selectId, context: wrapper.dataset.context, value } }));
}

export function installSelects() {
  document.addEventListener('pointerdown', event => {
    if (openWrapper && !openWrapper.contains(event.target) && !listOf(openWrapper).contains(event.target)) closeSelect();
  }, true);
  document.addEventListener('click', event => {
    const trigger = event.target.closest('.figma-select-trigger');
    if (trigger) { openSelect(trigger.closest('.figma-select')); return; }
    const option = event.target.closest('.figma-select-option');
    if (option && openWrapper && listOf(openWrapper).contains(option)) { choose(openWrapper, option); return; }
    if (openWrapper && !openWrapper.contains(event.target) && !listOf(openWrapper).contains(event.target)) closeSelect();
  });
  document.addEventListener('keydown', event => {
    const trigger = event.target.closest('.figma-select-trigger');
    if (trigger) {
      const wrapper = trigger.closest('.figma-select');
      if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
        event.preventDefault();
        if (openWrapper !== wrapper) openSelect(wrapper);
        const count = optionsOf(wrapper).length;
        activate(wrapper, event.key === 'Home' ? 0 : event.key === 'End' ? count - 1 : (activeIndex + (event.key === 'ArrowDown' ? 1 : count - 1)) % count);
      } else if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        if (openWrapper === wrapper) choose(wrapper, optionsOf(wrapper)[activeIndex]);
        else openSelect(wrapper);
      } else if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
        event.preventDefault();
        if (openWrapper !== wrapper) openSelect(wrapper);
        searchText += event.key.toLocaleLowerCase('az');
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => { searchText = ''; }, 600);
        const options = optionsOf(wrapper);
        let index = options.findIndex(option => option.textContent.trim().toLocaleLowerCase('az').startsWith(searchText));
        if (index < 0) index = options.findIndex(option => option.textContent.trim().toLocaleLowerCase('az').startsWith(event.key.toLocaleLowerCase('az')));
        if (index >= 0) activate(wrapper, index);
      }
    }
    if (openWrapper && (event.key === 'Escape' || event.key === 'Tab')) {
      if (event.key === 'Escape') { event.preventDefault(); closeSelect({ focus: true }); }
      else closeSelect();
    }
  });
  window.addEventListener('resize', () => { if (openWrapper) position(openWrapper); });
  window.addEventListener('scroll', () => { if (openWrapper) position(openWrapper); }, true);
}
