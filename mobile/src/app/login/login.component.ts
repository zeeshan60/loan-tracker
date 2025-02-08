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
import { ToastController } from '@ionic/angular';

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
  private toastController = inject(ToastController);

  constructor() { }

  loginWithGoogle() {
    signInWithPopup(this.auth, new GoogleAuthProvider())
      .then(
        (response: any) => {
          this.router.navigate(['/']);
        },
        async (err: any) => {
          const toast = await this.toastController.create({
            message: 'Unable to login at the moment',
            duration: 1500
          });
          toast.present();
        }
      );
  }
}
