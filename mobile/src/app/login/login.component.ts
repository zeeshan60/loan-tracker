import { ChangeDetectionStrategy, Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { Auth, getAuth, sendPasswordResetEmail, signInWithEmailAndPassword } from '@angular/fire/auth';
import { Router } from '@angular/router';
import { IonButton, IonContent, IonInput, IonItem, IonLabel } from '@ionic/angular/standalone';
import { AuthStore } from './auth.store';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HelperService } from '../helper.service';
import { FirebaseAuthError, FirebaseErrorCodeMessageEnum } from './types';
import { extractFirebaseErrorMessage } from '../utility-functions';
import { SignupComponent } from '../signup/signup.component';
import { isIos } from '../utils';

type ActiveUi = 'login' | 'signup' | 'forgotPassword';

@Component({
  selector: 'mr-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true,
  imports: [IonContent, IonButton, IonItem, IonLabel, IonInput, FormsModule, ReactiveFormsModule, SignupComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  fb = inject(FormBuilder);
  helperService = inject(HelperService);
  invalidCreds = signal<string>('');
  loading = signal(false);
  activeUi = signal<ActiveUi>('login')
  public auth = inject(Auth);
  public router = inject(Router);
  private authStore = inject(AuthStore);
  readonly loginForm = this.fb.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    password: this.fb.nonNullable.control('', [Validators.required]),
  });

  readonly forgotPasswordForm = this.fb.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
  });

  readonly isIos = isIos;

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
      const auth = getAuth();
      this.loading.set(true);
      signInWithEmailAndPassword(auth, this.loginForm.get('email').value, this.loginForm.get('password').value)
        .then(async () => {
          return this.authStore.login(await getAuth().currentUser?.getIdToken())
        })
        .then(() => {
          if (!this.authStore.user()?.phoneNumber) {
            this.authStore.askForPhoneNumber();
          }
        })
        .catch((error: FirebaseAuthError) => {
          let errorMessage = FirebaseErrorCodeMessageEnum[error.code] || extractFirebaseErrorMessage(error.message);
          if (!errorMessage) {
            errorMessage = 'Something went wrong. Please try again'
          }
          this.invalidCreds.set(errorMessage);
        })
        .finally(() => {
          this.loading.set(false);
        });
    } else {
      this.loginForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }


  async onPasswordRecovery() {
    if (this.forgotPasswordForm.valid) {
      this.loading.set(true);
      const auth = getAuth();
      sendPasswordResetEmail(auth, this.forgotPasswordForm.get('email').value)
        .then(() => {
          this.helperService.showToast(
            'If an account with given email exists, a password reset link has been sent. Please check your inbox.',
            3000,
            {
              color: 'success'
            }
          );
        })
        .catch((error: FirebaseAuthError) => {
          this.invalidCreds.set(extractFirebaseErrorMessage(error.message))
        })
        .finally(() => {
          this.loading.set(false);
        });
    } else {
      this.forgotPasswordForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  loginWithGoogle() {
    this.authStore.loginWithGoogle()
  }

  loginWithApple() {
    this.authStore.loginWithApple()
  }

  activateUi(ui: ActiveUi) {
    this.invalidCreds.set('');
    this.forgotPasswordForm.reset();
    this.loginForm.reset();
    this.activeUi.set(ui);
  }
}
