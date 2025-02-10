import { inject, Injectable } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { DEFAULT_TOAST_DURATION } from './constants';

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
    toast.present();
  }
}
