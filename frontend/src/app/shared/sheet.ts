import { Component, input, output } from '@angular/core';

/** Bottom drawer used by the "Hoy" actions. */
@Component({
  selector: 'app-sheet',
  templateUrl: './sheet.html',
  styleUrl: './sheet.css',
})
export class Sheet {
  readonly open = input(false);
  readonly title = input('');
  readonly closed = output<void>();
}
