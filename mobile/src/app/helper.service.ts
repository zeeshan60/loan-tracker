import { inject, Injectable } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { DEFAULT_TOAST_DURATION } from './constants';
import { getAuth } from '@angular/fire/auth';
import { AlertController } from '@ionic/angular/standalone';

@Injectable({
  providedIn: 'root'
})
export class HelperService {
  readonly toastCtrl = inject(ToastController);
  readonly alertCtrl = inject(AlertController);
  constructor() { }

  async showToast(message: string, duration = DEFAULT_TOAST_DURATION) {
    const toast = await this.toastCtrl.create({
      message,
      duration,
      swipeGesture: 'vertical'
    });
    return toast.present();
  }

  async getFirebaseAccessToken() {
    return getAuth().currentUser?.getIdToken();
  }

  getTimeZone() {
    return Intl.DateTimeFormat().resolvedOptions().timeZone
  }

  async showConfirmAlert(customMessage = '', okButtonText = 'Yes') {
    const alert = await this.alertCtrl.create({
      message: customMessage,
      header: 'Are you sure?',
      buttons: [
        {
          text: 'Cancel',
          role: 'cancel',
        },
        {
          text: okButtonText,
          role: 'confirm',
        },
      ]
    })
    alert.present();
    return alert.onWillDismiss()
  }
}
