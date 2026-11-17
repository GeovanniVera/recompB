import { Injectable, signal } from '@angular/core';

export type Theme = 'graphite' | 'spruce';

export const THEME_STORAGE_KEY = 'recomp3b.theme';

export const THEMES: readonly { id: Theme; label: string; description: string }[] = [
  { id: 'graphite', label: 'Grafito & Militar', description: 'Oscuro · por defecto' },
  { id: 'spruce', label: 'Crema, Abeto & Tomate', description: 'Claro · 60/30/10' },
];

function readStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return stored === 'spruce' ? 'spruce' : 'graphite';
  } catch {
    return 'graphite';
  }
}

/**
 * Applies the theme on <html data-theme="..."> and persists the choice.
 * A tiny inline script in index.html sets the same attribute before the first
 * paint, so switching themes never flashes the wrong palette.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(readStoredTheme());

  constructor() {
    this.apply(this.theme());
  }

  setTheme(theme: Theme): void {
    this.theme.set(theme);
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Private mode: the theme still applies for this session.
    }
    this.apply(theme);
  }

  toggle(): void {
    this.setTheme(this.theme() === 'graphite' ? 'spruce' : 'graphite');
  }

  private apply(theme: Theme): void {
    if (typeof document === 'undefined') {
      return;
    }
    document.documentElement.setAttribute('data-theme', theme);
    // Keep the mobile browser chrome in sync with the palette.
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', theme === 'spruce' ? '#EEE2CB' : '#141615');
  }
}
