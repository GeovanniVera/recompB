/**
 * Date helpers pinned to America/Mexico_City.
 *
 * Never derive "today" from `toISOString()`: that returns the UTC day, which is
 * already tomorrow for Mexico after 18:00 local time.
 */
export const APP_TIME_ZONE = 'America/Mexico_City';

function parts(now: Date): Record<string, string> {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: APP_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  return Object.fromEntries(formatter.formatToParts(now).map((part) => [part.type, part.value]));
}

/** Local calendar day in Mexico as `YYYY-MM-DD`. */
export function todayIso(now: Date = new Date()): string {
  const value = parts(now);
  return `${value['year']}-${value['month']}-${value['day']}`;
}

/** Local wall-clock time in Mexico as `HH:mm`. */
export function nowTime(now: Date = new Date()): string {
  const formatter = new Intl.DateTimeFormat('en-GB', {
    timeZone: APP_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  return formatter.format(now);
}

/** Shifts an ISO date by whole days without touching the local timezone. */
export function addDays(isoDate: string, days: number): string {
  const date = new Date(`${isoDate}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

/** "lun, 16 nov" for an ISO date. */
export function formatDate(isoDate: string): string {
  return new Intl.DateTimeFormat('es-MX', {
    timeZone: 'UTC',
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(new Date(`${isoDate}T12:00:00Z`));
}

/** "16 nov 2026" for an ISO date. */
export function formatDateLong(isoDate: string): string {
  return new Intl.DateTimeFormat('es-MX', {
    timeZone: 'UTC',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(`${isoDate}T12:00:00Z`));
}

export function isSameDay(a: string, b: string): boolean {
  return a === b;
}
