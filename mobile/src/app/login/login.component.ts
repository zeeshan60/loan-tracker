import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { GoogleAuthProvider } from "firebase/auth";
import { Auth, signInWithPopup } from '@angular/fire/auth';
import { Router } from '@angular/router';
import {
  IonButton,
  IonContent,
  IonHeader,
  IonIcon,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { ToastController } from '@ionic/angular/standalone';
import { UserCredential } from '@firebase/auth';
import { AuthStore } from './auth.store';

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
  private toastController = inject(ToastController);

  constructor() { }

  loginWithGoogle() {
    this.authStore.loginWithGoogle();
  }

  nativeLogin() {
    // todo: trigger native plugin here...
  }
}
