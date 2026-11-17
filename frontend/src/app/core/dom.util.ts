/** Small, zoneless-safe form helpers: read values from native events. */
export function inputValue(event: Event): string {
  const target = event.target as HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement | null;
  return target?.value ?? '';
}

export function inputNumber(event: Event): number | null {
  const raw = inputValue(event).trim().replace(',', '.');
  if (raw === '') {
    return null;
  }
  const value = Number(raw);
  return Number.isFinite(value) ? value : null;
}

export function round1(value: number): number {
  return Math.round(value * 10) / 10;
}

export function clampPercent(value: number): number {
  return Math.max(0, Math.min(100, value));
}
