import { inject, Injectable } from '@angular/core';
import { LoadingController, ToastController } from '@ionic/angular/standalone';
import { DEFAULT_TOAST_DURATION } from './constants';
import { getAuth } from '@angular/fire/auth';
import { AlertController } from '@ionic/angular/standalone';
import { ToastOptions } from '@ionic/angular';

@Injectable({
  providedIn: 'root'
})
export class HelperService {
  readonly toastCtrl = inject(ToastController);
  readonly alertCtrl = inject(AlertController);
  readonly loadingCtrl = inject(LoadingController);
  #toastsMuted = true;
  constructor() { }

  muteToasts(mute: boolean) {
    this.#toastsMuted = mute;
    // this logic below will mute the notifications for 2 seconds only.
    if (this.#toastsMuted) {
      setTimeout(() => {
        this.muteToasts(false);
      }, 2000);
    }
  }
  async showToast(message: string, duration = DEFAULT_TOAST_DURATION, options?: ToastOptions) {
    if (!this.#toastsMuted) {
      return null;
    }
    const toast = await this.toastCtrl.create({
      message,
      duration,
      swipeGesture: 'vertical',
      ...options
    });
    await toast.present();
    return toast;
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
      cssClass: 'danger-alert',
      buttons: [
        {
          text: 'Cancel',
          role: 'cancel',
        },
        {
          text: okButtonText,
          role: 'confirm',
          cssClass: 'danger'
        },
      ]
    })
    alert.present();
    return alert.onWillDismiss()
  }

  async withLoader<T>(callback: () => Promise<T>): Promise<T> {
    const loader = await this.loadingCtrl.create();
    try {
      await loader.present();
      return await callback(); // run the async function
    } catch (e) {
      throw e;
    } finally {
      await loader.dismiss();
    }
  }
}
