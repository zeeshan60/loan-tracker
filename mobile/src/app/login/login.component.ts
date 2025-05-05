import { ChangeDetectionStrategy, Component, inject, model, signal } from '@angular/core';
import {Auth} from '@angular/fire/auth';
import { Router, RouterLink } from '@angular/router';
import {
  IonButton,
  IonContent,
  IonHeader,
  IonIcon, IonInput, IonItem, IonLabel,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import {AuthStore} from './auth.store';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HelperService } from '../helper.service';
import { addCircleOutline } from 'ionicons/icons';

type ActiveUi = 'login' | 'signup' | 'forgotPassword';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonIcon, IonItem, IonLabel, IonInput, FormsModule, RouterLink, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  fb = inject(FormBuilder);
  helperService = inject(HelperService);
  invalidCreds = signal(false);
  loading = signal(false);
  activeUi = signal<ActiveUi>('login')
  public auth = inject(Auth);
  public router = inject(Router);
  private authStore = inject(AuthStore);
  readonly loginForm = this.fb.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    password: this.fb.nonNullable.control('', [Validators.required]),
  });
  readonly signUpForm = this.fb.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    password: this.fb.nonNullable.control('', [Validators.required]),
  });
  readonly forgotPasswordForm = this.fb.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
  });

  constructor() {}

  emailErrorMessage(form: FormGroup) {
    let emailControl = form.controls['email'];
    if (emailControl.hasError('email')) {
      return 'Invalid email.'
    } else if (emailControl.hasError('required')) {
      return 'Email is required'
    } else {
      return 'Invalid';
    }
  }
  async onLoginWithCreds() {
    if (this.loginForm.valid) {
      this.loading.set(true);
      setTimeout(() => {
        this.loading.set(false);
        this.invalidCreds.set(true);
      }, 1000)
    } else {
      this.loginForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  async onSignUpWithCreds() {
    if (this.signUpForm.valid) {
      this.loading.set(true);
      setTimeout(() => {
        this.loading.set(false);
        this.invalidCreds.set(true);
      }, 1000)
    } else {
      this.signUpForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  async onPasswordRecovery() {
    if (this.forgotPasswordForm.valid) {
      this.loading.set(true);
      setTimeout(() => {
        this.loading.set(false);
        this.invalidCreds.set(true);
      }, 1000)
    } else {
      this.forgotPasswordForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  loginWithGoogle() {
    this.authStore.loginWithGoogle()
      .then(() => {
        if (!this.authStore.user()?.phoneNumber) {
          this.authStore.askForPhoneNumber();
        }
      });
  }

  activateUi(ui: ActiveUi) {
    this.invalidCreds.set(false);
    this.signUpForm.reset();
    this.forgotPasswordForm.reset();
    this.loginForm.reset();
    this.activeUi.set(ui);
  }

  protected readonly addCircleOutline = addCircleOutline;
}
