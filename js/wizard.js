import { state } from './store.js';

// Keep the seven visible stages aligned with the Figma progress indicator.
export function wizardSteps() {
  return state.draft.type === 'statement'
    ? [
      {label:'Sənəd növü', step:1},
      {label:'Sənəd növü', step:2},
      {label:'Hesab seçimi', step:3},
      {label:'Çıxarış detalları', step:4},
      {label:'Yoxlama', step:5}, {label:'Ödəniş', step:6}, {label:'Təsdiq', step:7}
    ]
    : [
      {label:'Sənəd növü', step:1}, {label:'Səfirlik məlumatları', step:2},
      {label:'Hesab seçimi', step:3}, {label:'Çıxarış detalları', step:4},
      {label:'Yoxlama', step:5}, {label:'Ödəniş', step:6}, {label:'Təsdiq', step:7}
    ];
}
export function visualStep() {
  return state.step;
}
