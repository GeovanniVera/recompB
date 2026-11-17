import { Component, input } from '@angular/core';
import { WeekDeltas } from '../core/api.models';

@Component({
  selector: 'app-delta-list',
  templateUrl: './delta-list.html',
  styleUrl: './delta-list.css',
})
export class DeltaList {
  readonly deltas = input.required<WeekDeltas>();

  sign(value: number): string {
    if (value > 0) {
      return 'up';
    }
    return value < 0 ? 'down' : 'flat';
  }

  text(value: number): string {
    const rounded = Math.round(value * 10) / 10;
    return rounded > 0 ? `+${rounded}` : `${rounded}`;
  }

  rows(): { label: string; value: number; unit: string }[] {
    const deltas = this.deltas();
    return [
      { label: 'Calorías', value: deltas.kcalDelta, unit: 'kcal' },
      { label: 'Proteína', value: deltas.proteinDelta, unit: 'g' },
      { label: 'Días verdes', value: deltas.greenPctDelta, unit: 'pp' },
      { label: 'Limpieza', value: deltas.cleanPctDelta, unit: 'pp' },
    ];
  }
}
