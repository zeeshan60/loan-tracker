import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import { Auth, getAuth, signOut } from '@angular/fire/auth';
import {Router} from '@angular/router';
import {
  IonButton,
  IonContent,
  IonHeader,
  IonIcon,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import {AuthStore} from './auth.store';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  public auth = inject(Auth);
  public router = inject(Router);
  private authStore = inject(AuthStore);

  constructor() {}

  signOutNow() {
    signOut(getAuth());
  }
  loginWithGoogle() {
    this.authStore.loginWithGoogle()
      .then(() => {
        if (!this.authStore.user()?.phoneNumber) {
          this.authStore.askForPhoneNumber();
        }
      });
  }
}
