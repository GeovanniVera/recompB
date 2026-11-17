import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

type Step = 'email' | 'code';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  private timerId: ReturnType<typeof setInterval> | null = null;

  readonly step = signal<Step>('email');
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly secondsLeft = signal(0);
  readonly sentEmail = signal('');

  readonly emailForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  readonly codeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.stopTimer());
  }

  submitEmail(): void {
    if (this.busy()) {
      return;
    }
    this.error.set(null);
    this.notice.set(null);

    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }

    const email = this.emailForm.getRawValue().email.trim().toLowerCase();
    this.busy.set(true);

    this.auth.requestOtp(email).subscribe({
      next: (response) => {
        this.busy.set(false);
        this.sentEmail.set(email);
        this.codeForm.reset({ code: '' });
        this.step.set('code');
        this.notice.set(`Te mandamos un código de 6 dígitos a tu Telegram (válido ${Math.round(response.expiresInSeconds / 60)} minutos).`);
        this.startTimer(response.resendAfterSeconds);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.error.set(AuthService.messageFrom(error, 'No pudimos enviar el código. Intenta de nuevo.'));
      },
    });
  }

  submitCode(): void {
    if (this.busy()) {
      return;
    }
    this.error.set(null);

    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }

    this.busy.set(true);
    this.auth.verifyOtp(this.sentEmail(), this.codeForm.getRawValue().code).subscribe({
      next: () => {
        this.busy.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.error.set(AuthService.messageFrom(error, 'El código no es válido o ya venció.'));
      },
    });
  }

  resend(): void {
    if (this.busy() || this.secondsLeft() > 0) {
      return;
    }
    this.error.set(null);
    this.busy.set(true);
    this.auth.requestOtp(this.sentEmail()).subscribe({
      next: (response) => {
        this.busy.set(false);
        this.codeForm.reset({ code: '' });
        this.notice.set('Ya va un código nuevo.');
        this.startTimer(response.resendAfterSeconds);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.error.set(AuthService.messageFrom(error, 'No pudimos reenviar el código. Intenta de nuevo.'));
      },
    });
  }

  changeEmail(): void {
    this.stopTimer();
    this.secondsLeft.set(0);
    this.error.set(null);
    this.notice.set(null);
    this.step.set('email');
  }

  private startTimer(seconds: number): void {
    this.stopTimer();
    this.secondsLeft.set(Math.max(0, Math.floor(seconds)));
    if (this.secondsLeft() === 0) {
      return;
    }
    this.timerId = setInterval(() => {
      const next = this.secondsLeft() - 1;
      this.secondsLeft.set(Math.max(0, next));
      if (next <= 0) {
        this.stopTimer();
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
