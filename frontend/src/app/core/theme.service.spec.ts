import { ThemeService } from './theme.service';
import { THEME_STORAGE_KEY } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('arranca en grafito y lo aplica al documento', () => {
    const service = new ThemeService();

    expect(service.theme()).toBe('graphite');
    expect(document.documentElement.getAttribute('data-theme')).toBe('graphite');
  });

  it('persiste el tema elegido en localStorage', () => {
    const service = new ThemeService();

    service.setTheme('spruce');

    expect(service.theme()).toBe('spruce');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('spruce');
    expect(document.documentElement.getAttribute('data-theme')).toBe('spruce');
  });

  it('recupera el tema guardado al iniciar', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'spruce');

    const service = new ThemeService();

    expect(service.theme()).toBe('spruce');
    expect(document.documentElement.getAttribute('data-theme')).toBe('spruce');
  });

  it('ignora un valor desconocido y cae en el tema por defecto', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'neon');

    const service = new ThemeService();

    expect(service.theme()).toBe('graphite');
  });

  it('alterna entre los dos temas', () => {
    const service = new ThemeService();

    service.toggle();
    expect(service.theme()).toBe('spruce');

    service.toggle();
    expect(service.theme()).toBe('graphite');
  });
});
