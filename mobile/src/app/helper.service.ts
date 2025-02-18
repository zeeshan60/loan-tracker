import { inject, Injectable } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { DEFAULT_TOAST_DURATION } from './constants';
import { getAuth } from '@angular/fire/auth';

@Injectable({
  providedIn: 'root'
})
export class HelperService {
  readonly toastCtrl = inject(ToastController);
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

  async getUser() {
    return getAuth().currentUser;
  }
}
