import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  Auth,
  createUserWithEmailAndPassword,
  getAuth,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
} from '@angular/fire/auth';
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
import { AbstractControl, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HelperService } from '../helper.service';
import { addCircleOutline } from 'ionicons/icons';
import {
  FirebaseAuthError,
  FirebaseErrorCodeMessageEnum,
} from './types';
import { extractFirebaseErrorMessage } from '../utility-functions';

type ActiveUi = 'login' | 'signup' | 'forgotPassword';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonIcon, IonItem, IonLabel, IonInput, FormsModule, ReactiveFormsModule],
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
  readonly signUpForm = this.fb.group({
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    password: this.fb.nonNullable.control('', [
      Validators.minLength(6),
      Validators.maxLength(20),
      Validators.required,
      (control: AbstractControl) => {
        const value = control.value;
        if (!value) return null;
        const hasLetter = /[a-zA-Z]/.test(value);
        const hasNumber = /\d/.test(value);
        const valid = hasLetter && hasNumber;
        return valid ? null : { passwordStrength: 'Password must include letters and numbers' };
      }
    ]),
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

  passwordErrorMessage(form: FormGroup) {
    let passwordControl = form.controls['password'];
    if (passwordControl.hasError('passwordStrength')) {
      return 'Password must contain numbers and alphabet both.'
    } else if (passwordControl.hasError('required')) {
      return 'Password is required'
    } else if (passwordControl.hasError('minlength') || passwordControl.hasError('maxlength')) {
      return 'Password should be 8 to 20 characters long'
    }else {
      return 'Invalid';
    }
  }
  async onLoginWithCreds() {
    if (this.loginForm.valid) {
      const auth = getAuth();
      this.loading.set(true);
      signInWithEmailAndPassword(auth, this.loginForm.get('email').value, this.loginForm.get('password').value)
        .then(async () => this.authStore.login(await getAuth().currentUser?.getIdToken()))
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

  async onSignUpWithCreds() {
    if (this.signUpForm.valid) {
      this.loading.set(true);
      const auth = getAuth();
      createUserWithEmailAndPassword(auth, this.signUpForm.get('email').value, this.signUpForm.get('password').value)
        .then(async () => {
          let toast = await this.helperService.showToast(
            'You are successfully registered. You can login now.',
            3000,
            {
              color: 'success',
            },
          );
          await toast.onDidDismiss();
          return this.activeUi.set('login');
        })
        .catch((error: FirebaseAuthError) => {
          this.invalidCreds.set(FirebaseErrorCodeMessageEnum[error.code] || extractFirebaseErrorMessage(error.message));
        })
        .finally(() => {
          this.loading.set(false);
        });
    } else {
      this.signUpForm.markAllAsTouched();
      await this.helperService.showToast('Please fill in the correct values');
    }
  }

  async onPasswordRecovery() {
    if (this.forgotPasswordForm.valid) {
      this.loading.set(true);
      const auth = getAuth();
      sendPasswordResetEmail(auth, this.forgotPasswordForm.get('email').value)
        .then(() => {
          return this.helperService.showToast(
            'If an account with that email exists, a password reset link has been sent. Please check your inbox.',
            3000,
            {
              color: 'success'
            }
          )
            .then(toast => toast.onDidDismiss()
              .then(() => this.activeUi.set('login')))
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

  activateUi(ui: ActiveUi) {
    this.invalidCreds.set('');
    this.signUpForm.reset();
    this.forgotPasswordForm.reset();
    this.loginForm.reset();
    this.activeUi.set(ui);
  }

  protected readonly addCircleOutline = addCircleOutline;
}
