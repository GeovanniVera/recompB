import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface Tab {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-bottom-nav',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './bottom-nav.html',
  styleUrl: './bottom-nav.css',
})
export class BottomNav {
  protected readonly tabs: readonly Tab[] = [
    {
      path: '/today',
      label: 'Hoy',
      icon: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm0-5v2m0 14v2M3 12h2m14 0h2M5.6 5.6l1.4 1.4m10 10 1.4 1.4m0-12.8-1.4 1.4m-10 10-1.4 1.4',
    },
    {
      path: '/analytics',
      label: '14 Días',
      icon: 'M4 20V11m5 9V6m5 14v-7m5 7V4',
    },
    {
      path: '/plan',
      label: 'Mi Plan',
      icon: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0',
    },
  ];
}
