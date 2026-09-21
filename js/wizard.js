import { state } from './store.js';

// Storage steps stay stable; the statement follows the order shown in Figma.
export function wizardSteps() {
  return state.draft.type === 'statement'
    ? [
      {label:'Sənəd növü', step:1},
      {label:'Hesab seçimi', step:3},
      {label:'Çıxarış detalları', step:4},
      {label:'Təqdim ediləcək yer', step:4, substep:'destination'},
      {label:'Yoxlama', step:5}, {label:'Ödəniş', step:6}, {label:'Təsdiq', step:7}
    ]
    : [
      {label:'Sənəd növü', step:1}, {label:'Səfirlik məlumatları', step:2},
      {label:'Hesab seçimi', step:3}, {label:'Çıxarış detalları', step:4},
      {label:'Yoxlama', step:5}, {label:'Ödəniş', step:6}, {label:'Təsdiq', step:7}
    ];
}
export function visualStep() {
  if (state.draft.type !== 'statement' || state.step === 1 || state.step >= 5) return state.step;
  if (state.step <= 3) return 2;
  return state.substep === 'destination' ? 4 : 3;
}
