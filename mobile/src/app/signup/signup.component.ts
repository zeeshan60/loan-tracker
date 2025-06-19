import { Component, inject, OnInit, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { createUserWithEmailAndPassword, getAuth } from '@angular/fire/auth';
import { FirebaseAuthError, FirebaseErrorCodeMessageEnum } from '../login/types';
import { extractFirebaseErrorMessage } from '../utility-functions';
import { HelperService } from '../helper.service';
import { IonButton, IonInput, IonItem, IonLabel } from '@ionic/angular/standalone';
import { PhoneWithCountryComponent } from '../phone-with-country/phone-with-country.component';

@Component({
  selector: 'mr-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss'],
  imports: [
    IonItem,
    IonLabel,
    IonInput,
    ReactiveFormsModule,
    PhoneWithCountryComponent,
    IonButton,
  ],
})
export class SignupComponent {
  readonly signupComplete = output<boolean>();
  readonly fb = inject(FormBuilder);
  readonly helperService = inject(HelperService);

  readonly invalidCreds = signal<string>('');
  readonly passwordValidators = [
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
  ]
  readonly signUpForm = this.fb.group({
    name: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(100)
    ]),
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    passwords: this.fb.group({
      password: this.fb.nonNullable.control('', this.passwordValidators),
      confirmPassword: this.fb.nonNullable.control('', [...this.passwordValidators]),
    }, {
      validators: [
        (control: AbstractControl) => {
          const password = control.get('password') as AbstractControl;
          const confirmPassword = control.get('confirmPassword') as AbstractControl;
          if (!password.value || !confirmPassword.value) return null;
          const valid = password.value === confirmPassword.value;
          if (valid) {
            confirmPassword.setErrors(null)
            return null
          } else {
            confirmPassword.setErrors({ passwordsNotSame: 'Password and Confirm Password should be same.' })
            return { passwordsNotSame: 'Password and Confirm Password should be same.' };
          }
        }
      ]
    }),
  });
  loading = signal(false);

  constructor() { }

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

  getErrorMessage(control: AbstractControl | null, controlName: string = 'Input') {
    if (!control) {
      return ''; // Or throw an error, depending on your desired strictness
    }

    if (control.hasError('passwordStrength')) {
      return `${controlName} must contain numbers and alphabet both.`;
    } else if (control.hasError('required')) {
      return `${controlName} is required`;
    } else if (control.hasError('minlength') || control.hasError('maxlength')) {
      return `${controlName} should be 8 to 20 characters long`;
    }
    return 'Invalid'; // Default message if no specific error found
  }

  passwordErrorMessage(form: FormGroup): string {
    const passwordControl = form.get('passwords.password');
    return this.getErrorMessage(passwordControl, 'Password');
  }

  confirmPasswordErrorMessage(form: FormGroup): string {
    const confirmPasswordControl = form.get('passwords.confirmPassword');
    if (form.get('passwords')?.hasError('passwordsNotSame')) {
      return 'Confirm password is not same.';
    }
    return this.getErrorMessage(confirmPasswordControl, 'Confirm Password');
  }

  async onSignUpWithCreds() {
    if (this.signUpForm.valid) {
      this.loading.set(true);
      const auth = getAuth();
      createUserWithEmailAndPassword(auth, this.signUpForm.get('email').value, this.signUpForm.get('passwords.password').value)
        .then(async () => {
          let toast = await this.helperService.showToast(
            'You are successfully registered. You can login now.',
            3000,
            {
              color: 'success',
            },
          );
          toast.onDidDismiss();
          return this.signupComplete.emit(true);
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
}
