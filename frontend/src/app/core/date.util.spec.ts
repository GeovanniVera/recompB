import { addDays, formatDate, nowTime, todayIso } from './date.util';

describe('date.util', () => {
  it('resuelve el día local de México, no el día UTC', () => {
    // 03:30 UTC del 17 de noviembre = 21:30 del 16 en Ciudad de México.
    const lateNightUtc = new Date('2026-11-17T03:30:00Z');
    expect(todayIso(lateNightUtc)).toBe('2026-11-16');
  });

  it('resuelve el mismo día cuando ya amaneció en México', () => {
    const morningUtc = new Date('2026-11-17T16:00:00Z');
    expect(todayIso(morningUtc)).toBe('2026-11-17');
  });

  it('devuelve la hora local en formato HH:mm', () => {
    const lateNightUtc = new Date('2026-11-17T03:30:00Z');
    expect(nowTime(lateNightUtc)).toBe('21:30');
  });

  it('suma y resta días sin desfase de zona', () => {
    expect(addDays('2026-11-16', 1)).toBe('2026-11-17');
    expect(addDays('2026-11-01', -1)).toBe('2026-10-31');
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
  });

  it('formatea fechas en español', () => {
    expect(formatDate('2026-11-16')).toContain('16');
  });
});
