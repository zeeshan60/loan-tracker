import { inject, Injectable } from '@angular/core';
import type { ModalOptions } from '@ionic/core/components';
import { ModalController } from '@ionic/angular/standalone';

export type ModalIndex = 0|1|2|3|4;

@Injectable({
  providedIn: 'root'
})
export class ModalService {
  /**
   * we can store up to 5 modal instance at the same time.
   * if the value is 'booked' it means that particular index is booked and
   * a model instance is being prepared to store in this slot. so no one else can overwrite
   * on this instance.
   * @private
   */
  private _modals:Record<ModalIndex, null | HTMLIonModalElement | 'booked'> = {
    0: null, 1: null, 2: null, 3: null, 4: null
  };
  private modalCtrl = inject(ModalController);
  constructor() { }

  public async showModal(options: ModalOptions): Promise<ModalIndex> {
    const modalIndex = this.getAvailableIndex();
    this._modals[modalIndex] = 'booked';
    const modalInstance = await this.modalCtrl.create({
      ...options,
      componentProps: {
        ...options.componentProps,
        modalIndex: modalIndex
      }
    });
    await modalInstance.present();
    this._modals[modalIndex] = modalInstance;

    // clear the space once the modal is closed.
    modalInstance.onDidDismiss().then(() => {
      this._modals[modalIndex] = null;
    })
    return modalIndex;
  }

  public async dismiss(modalIndex: ModalIndex, data?: any, role?: 'confirm') {
    await this.getModalByIndex(modalIndex)?.dismiss(data, role)
  }

  public getModalByIndex(index: ModalIndex): HTMLIonModalElement | null {
    return this._modals[index] as HTMLIonModalElement | null;
  }

  public async onWillDismiss<T>(index: ModalIndex) {
    return this.getModalByIndex(index).onWillDismiss<T>();
  }

  private getAvailableIndex(): ModalIndex {
    const index = Object.keys(this._modals).find((index) => !this._modals[+index as ModalIndex])
    return index ? +index as ModalIndex : 0;
  }
}
