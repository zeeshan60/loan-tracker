import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {GoogleAuthProvider} from "firebase/auth";
import {Auth, signInWithPopup} from '@angular/fire/auth';
import {Router} from '@angular/router';
import {
  IonButton,
  IonContent,
  IonHeader,
  IonIcon,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import {ToastController} from '@ionic/angular/standalone';
import {UserCredential} from '@firebase/auth';
import {AuthStore} from './auth.store';
import {LoginPlugin} from "zeenom";

const inputValue = JSON.stringify({
  CLIENT_ID: "336545645239-ppcpb0k5hc8303p9ek0793f8lkbbqbku.apps.googleusercontent.com",
  REVERSED_CLIENT_ID: "com.googleusercontent.apps.336545645239-ppcpb0k5hc8303p9ek0793f8lkbbqbku",
  API_KEY: "AIzaSyB1qt9hOwlzyBWGIe-grrg0Vgp53tcwoLE",
  GCM_SENDER_ID: "336545645239",
  PLIST_VERSION: "1",
  BUNDLE_ID: "com.zeenomlabs.loantracker",
  PROJECT_ID: "loan-tracker-9b25d",
  STORAGE_BUCKET: "loan-tracker-9b25d.firebasestorage.app",
  IS_ADS_ENABLED: false,
  IS_ANALYTICS_ENABLED: false,
  IS_APPINVITE_ENABLED: true,
  IS_GCM_ENABLED: true,
  IS_SIGNIN_ENABLED: true,
  GOOGLE_APP_ID: "1:336545645239:ios:90e69a58265af386220332"
});

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

  constructor() {
  }

  loginWithGoogle() {
    this.authStore.loginWithGoogle();
  }

  nativeLogin() {

    LoginPlugin.echo({value: inputValue}).then(result => {
        console.log("js: " + JSON.stringify(result));
      }
    );
  }
}
